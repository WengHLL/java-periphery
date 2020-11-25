/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import static com.codeferm.periphery.Common.cString;
import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_DISABLE;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_PULL_DOWN;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_PULL_UP;
import static com.codeferm.periphery.Gpio.GPIO_DIR_IN;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import static com.codeferm.periphery.Gpio.GPIO_DRIVE_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_NONE;
import com.codeferm.periphery.Mmio;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;

/**
 * GPIO register offset and mask generator for configuration, data and PUD registers using MMIO.
 *
 * Make sure you disable all hardware in armbian-config System, Hardware and remove console=serial from /boot/armbianEnv.txt. You
 * want multi-function pins to act as GPIO pins. The idea here is to generate register offsets and masks, so you can build MMIO
 * based GPIO code without doing it manually from the datasheet. The only thing you need the datasheet for is building the property
 * file and validation.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class Gen implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Gen.class);
    /**
     * Input file.
     */
    @CommandLine.Option(names = {"-i", "--in"}, description = "Input property file name")
    private String inFileName = "duo.properties";
    /**
     * Ouptut file.
     */
    @CommandLine.Option(names = {"-o", "--out"}, description = "Output property file name")
    private String outFileName = "out.properties";

    /**
     * Return values from all registers.
     *
     * @param mmioHandle MMIO handles.
     * @param portChip Chips ports are on.
     * @param portOffset Port offsets in chip.
     * @param registerOffset Register offsets in chip.
     * @return List of register values.
     */
    public List<Integer> getRegValues(final List<Long> mmioHandle, final List<Integer> portChip, final List<Integer> portOffset,
            final List<Integer> registerOffset) {
        final var list = new ArrayList<Integer>();
        final var value = new int[1];
        // Read all ports
        for (int chip = 0; chip < portChip.size(); chip++) {
            // Read all registers
            for (int register = 0; register < registerOffset.size(); register++) {
                Mmio.mmioRead32(mmioHandle.get(portChip.get(chip)), portOffset.get(chip) + registerOffset.get(register), value);
                list.add(value[0]);
            }
        }
        return list;
    }

    /**
     * Compare list values and return index where difference is found. Filter is used to select on desired register name. A filter
     * is required because setting GPIO mode also sets data register sometimes. This prevents reading the wrong register.
     *
     * @param list1 First list.
     * @param list2 Second list.
     * @param filter Include filter.
     * @param registerName Register names.
     * @return Index of difference.
     */
    public int listDiff(final List<Integer> list1, final List<Integer> list2, final String filter, final List<String> registerName) {
        var i = 0;
        // Look for difference based on filter and exit on first instance
        while (i < list1.size() && !(!list1.get(i).equals(list2.get(i)) && registerName.get(i % registerName.size()).
                contains(filter))) {
            i++;
        }
        // No difference
        if (i == list1.size()) {
            i = -1;
        }
        return i;
    }

    /**
     * Set configuration register info in pin DTO.
     *
     * @param pin Pin DTO.
     * @param filter Include filter.
     * @param mmioHandle MMIO handles.
     * @param portChip Chips ports are on.
     * @param portOffset Port offsets in chip.
     * @param registerOffset Register offsets in chip.
     * @param registerName Register names.
     * @param portName Port name.
     */
    public void setConfigReg(final Pin pin, final String filter, final List<Long> mmioHandle, final List<Integer> portChip,
            final List<Integer> portOffset, final List<Integer> registerOffset, final List<String> registerName,
            final List<String> portName) {
        final var dev = String.format("/dev/gpiochip%d", pin.getKey().getChip());
        // Set pin for input, output and look for delta
        try (final var gpio = new Gpio(dev, pin.getKey().getPin(), new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_IN).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                Gen.class.getSimpleName())))) {
            Gpio.gpioSetDirection(gpio.getHandle(), GPIO_DIR_IN);
            final var list1 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            Gpio.gpioSetDirection(gpio.getHandle(), GPIO_DIR_OUT);
            final var list2 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            // Find the register delta
            final var reg = listDiff(list1, list2, filter, registerName);
            // Make sure a delta is detected
            if (reg >= 0) {
                pin.setConfigMask(list2.get(reg) - list1.get(reg)).setConfigOffset(portOffset.get(reg / registerOffset.size())
                        + registerOffset.get(reg % registerOffset.size())).setPortName(portName.get(reg / registerOffset.size())).
                        setConfigName(registerName.get(reg % registerName.size()));
            } else {
                logger.warn(String.format("Chip %d Pin %3d configuration register may not be supported", pin.getKey().getChip(),
                        pin.getKey().getPin()));
            }
        } catch (RuntimeException e) {
            logger.error(String.format("Chip %d Pin %3d Error %s", pin.getKey().getChip(), pin.getKey().getPin(), e.getMessage()));
        }
    }

    /**
     * Set data register info in pin DTO.
     *
     * @param pin Pin DTO.
     * @param filter Include filter.
     * @param mmioHandle MMIO handles.
     * @param portChip Chips ports are on.
     * @param portOffset Port offsets in chip.
     * @param registerOffset Register offsets in chip.
     * @param registerName Register names.
     */
    public void setDataReg(final Pin pin, final String filter, final List<Long> mmioHandle, final List<Integer> portChip,
            final List<Integer> portOffset, final List<Integer> registerOffset, final List<String> registerName) {
        final var dev = String.format("/dev/gpiochip%d", pin.getKey().getChip());
        // Set pin for input, output and look for delta
        try (final var gpio = new Gpio(dev, pin.getKey().getPin(), new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_OUT).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                Gen.class.getSimpleName())))) {
            Gpio.gpioWrite(gpio.getHandle(), false);
            final var list1 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            Gpio.gpioWrite(gpio.getHandle(), true);
            final var list2 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            // Find the register delta
            final var reg = listDiff(list1, list2, filter, registerName);
            // Make sure a delta is detected
            if (reg >= 0) {
                pin.setDataMask(list2.get(reg) - list1.get(reg)).setDataOffset(portOffset.get(reg / registerOffset.size())
                        + registerOffset.get(reg % registerOffset.size())).setDataName(registerName.get(reg % registerName.size()));
            } else {
                logger.warn(String.format("Chip %d Pin %3d data register may not be supported", pin.getKey().getChip(),
                        pin.getKey().getPin()));
            }
        } catch (RuntimeException e) {
            logger.error(String.format("Chip %d Pin %3d Error %s", pin.getKey().getChip(), pin.getKey().getPin(), e.getMessage()));
        }
    }

    /**
     * Set pull register info in pin DTO.
     *
     * @param pin Pin DTO.
     * @param filter Include filter.
     * @param mmioHandle MMIO handles.
     * @param portChip Chips ports are on.
     * @param portOffset Port offsets in chip.
     * @param registerOffset Register offsets in chip.
     * @param registerName Register names.
     */
    public void setPullReg(final Pin pin, final String filter, final List<Long> mmioHandle, final List<Integer> portChip,
            final List<Integer> portOffset, final List<Integer> registerOffset, final List<String> registerName) {
        final var dev = String.format("/dev/gpiochip%d", pin.getKey().getChip());
        // Set pin for input, output and look for delta
        try (final var gpio = new Gpio(dev, pin.getKey().getPin(), new Gpio.GpioConfig().setBias(GPIO_BIAS_DISABLE).
                setDirection(GPIO_DIR_IN).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                Gen.class.getSimpleName())))) {
            var list1 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            Gpio.gpioSetBias(gpio.getHandle(), GPIO_BIAS_PULL_UP);
            var list2 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
            // Find the register delta
            var reg = listDiff(list1, list2, filter, registerName);
            // Make sure a delta is detected
            if (reg >= 0) {
                pin.setPullUpMask(list2.get(reg) - list1.get(reg)).setPullOffset(portOffset.get(reg / registerOffset.size())
                        + registerOffset.get(reg % registerOffset.size())).setPullName(registerName.get(reg % registerName.size()));
                Gpio.gpioSetBias(gpio.getHandle(), GPIO_BIAS_DISABLE);
                list1 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
                Gpio.gpioSetBias(gpio.getHandle(), GPIO_BIAS_PULL_DOWN);
                list2 = getRegValues(mmioHandle, portChip, portOffset, registerOffset);
                // Find the register delta
                reg = listDiff(list1, list2, filter, registerName);
                pin.setPullDownMask(list2.get(reg) - list1.get(reg));
            } else {
                logger.warn(String.format("Chip %d Pin %3d pull register may not be supported", pin.getKey().getChip(),
                        pin.getKey().getPin()));
            }
        } catch (RuntimeException e) {
            logger.error(String.format("Chip %d Pin %3d Error %s", pin.getKey().getChip(), pin.getKey().getPin(), e.getMessage()));
        }
    }

    /**
     * Detect changes made by GPIO at register level.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        final var file = new File();
        final Map<PinKey, Pin> pinMap = file.parseInput(inFileName);
        // Make sure we have pins loaded
        if (!pinMap.isEmpty()) {
            final List<Long> mmioHandle = new ArrayList<>();
            // Open MMIO for each chip
            for (int i = 0; i < file.getChips().size(); i++) {
                final var mmio = new Mmio(file.getChips().get(i), file.getMmioSize().get(i));
                mmioHandle.add(mmio.getHandle());
            }
            // Set register offset and mask for each pin
            pinMap.entrySet().stream().map((entry) -> entry.getValue()).forEachOrdered((value) -> {
                setConfigReg(value, file.getConfigFilter(), mmioHandle, file.getPortChip(), file.getPortOffset(), file.
                        getRegisterOffset(), file.getRegisterName(), file.getPortName());
                setDataReg(value, file.getDataFilter(), mmioHandle, file.getPortChip(), file.getPortOffset(), file.
                        getRegisterOffset(), file.getRegisterName());
                setPullReg(value, file.getPullFilter(), mmioHandle, file.getPortChip(), file.getPortOffset(), file.
                        getRegisterOffset(), file.getRegisterName());
            });
            // Generate properties file
            file.genProperties(pinMap, inFileName, outFileName);
            // Close MMIO for each handle
            mmioHandle.forEach((handle) -> {
                Mmio.mmioClose(handle);
            });
        } else {
            logger.error("Pin map empty. Make sure you have a valid property file.");
            exitCode = 1;
        }
        return exitCode;
    }

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        System.exit(new CommandLine(new Gen()).execute(args));
    }
}
