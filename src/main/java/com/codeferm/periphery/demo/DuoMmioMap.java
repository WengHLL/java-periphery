/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import static com.codeferm.periphery.Common.cString;
import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_DIR_IN;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import static com.codeferm.periphery.Gpio.GPIO_DRIVE_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_NONE;
import com.codeferm.periphery.Mmio;
import static com.codeferm.periphery.demo.DuoMmioMap.PinMode.INPUT;
import static com.codeferm.periphery.demo.DuoMmioMap.PinMode.OUTPUT;
import static com.codeferm.periphery.demo.DuoMmioMap.PinState.OFF;
import static com.codeferm.periphery.demo.DuoMmioMap.PinState.ON;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * NanoPi Duo GPIO register mapping.
 *
 * Use MMIO to detect changes in registers on NanoPi Duo (Allwinner H2+ CPU). Make sure you disable all hardware in armbian-config
 * System, Hardware and remove console=serial from /boot/armbianEnv.txt. You want multi-function pins to act as GPIO pins. The idea
 * here is to generate masks, so you can build MMIO based GPIO code without doing it manually via datasheet. NanoPi Duo does not
 * have PUD resistors, so we skip mask generation.
 *
 * See datasheet starting at page 317 http://wiki.friendlyarm.com/wiki/images/0/08/Allwinner_H2%2B_Datasheet_V1.2.pdf for details.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "duommiomap", mixinStandardHelpOptions = true, version = "duommiomap 1.0.0",
        description = "NanoPi Duo MMIO GPIO mapping")
public class DuoMmioMap implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(DuoMmioMap.class);
    /**
     * Device option.
     */
    @CommandLine.Option(names = {"-d", "--device"}, description = "GPIO device defaults to 0")
    private int device = 0;
    /**
     * Line option.
     */
    @CommandLine.Option(names = {"-l", "--line"}, description = "GPIO line defaults to 203 IOG11 for NanoPi Duo")
    private int line = 203;    
    /**
     * GPIO chips sometimes called banks.
     */
    private static final long CHIP[] = {0x1c20800L, 0x1f02c00L};
    /**
     * MMIO handle for each unique chip.
     */
    private static final long MMIO_HANDLE[] = {0, 0};
    /**
     * MMIO size for each chip.
     */
    private static final long MMIO_SIZE[] = {252, 36};
    /**
     * GPIO CHIP used by pin.
     */
    private static final int PIN_CHIP[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1};
    /**
     * GPIOD pin numbers, no weirdo wiringPi or BCM numbers.
     */
    private static final int PIN[] = {4, 5, 11, 12, 13, 14, 15, 16, 198, 199, 203, 3, 11};
    /**
     * Pin names.
     */
    private static final String[] PIN_NAME = {"GPIOA4", "GPIOA5", "GPIOA11", "GPIOA12", "GPIOA13", "GPIOA14", "GPIOA15", "GPIOA16",
        "GPIOG6", "GPIOG7", "GPIOG11", "BUTTON", "GPIOL11"};
    /**
     * Configuration register offset for PIN[].
     */
    private static final int CFG_OFFSET[] = {0x00, 0x00, 0x04, 0x04, 0x04, 0x04, 0x04, 0x08, 0xd8, 0xd8, 0xdc, 0x00, 0x04};
    /**
     * Configuration register mask for PIN[].
     */
    private static final int CFG_MASK[] = {0x00010000, 0x00100000, 0x00001000, 0x00010000, 0x00100000, 0x01000000, 0x10000000,
        0x00000001, 0x01000000, 0x10000000, 0x00001000, 0x00001000, 0x00001000};
    /**
     * Data register offset for PIN[].
     */
    private static final int DAT_OFFSET[] = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xe8, 0xe8, 0xe8, 0x10, 0x10};
    /**
     * Data register mask for PIN[].
     */
    private static final int DAT_MASK[] = {0x00000010, 0x00000020, 0x00000800, 0x00001000, 0x00002000, 0x00004000, 0x00008000,
        0x00010000, 0x00000040, 0x00000080, 0x00000800, 0x00000008, 0x00000800};
    /**
     * Use PinKey (chip and pin) to uniquely define pin.
     */
    private static final Map<PinKey, Pin> pinMap = new TreeMap<>();

    /**
     * Pin mode.
     */
    public enum PinMode {
        INPUT, OUTPUT;
    }

    /**
     * Pin state.
     */
    public enum PinState {
        ON, OFF;
    }
    /**
     * PORT offset.
     */
    private static final int PORT_OFFSET[] = {0x00, 0x48, 0x6c, 0x90, 0xb4, 0xd8, 0x00};
    /**
     * Chip port is on.
     */
    private static final int PORT_CHIP[] = {0, 0, 0, 0, 0, 0, 1};
    /**
     * Port names.
     */
    private static final String[] PORT_NAME = {"A", "C", "D", "E", "F", "G", "L"};
    /**
     * Register names.
     */
    private static final String[] REGISTER_NAME = {"CFG0", "CFG1", "CFG2", "CFG3", "DAT ", "DRV0", "DRV1", "PUL0", "PUL1"};

    /**
     * Get pin mode.
     *
     * @param pin Pin.
     * @return INPUT or OUTPUT.
     */
    public PinMode getMode(final Pin pin) {
        final var value = new int[1];
        Mmio.mmioRead32(MMIO_HANDLE[pin.getChip()], pin.getConfigOffest(), value);
        PinMode ret;
        if ((value[0] & pin.getConfigMask()) == 0) {
            ret = INPUT;
        } else {
            ret = OUTPUT;
        }
        return ret;
    }

    /**
     * Set pin mode.
     *
     * @param pin Pin.
     * @param value Pin mode.
     */
    public void setMode(final Pin pin, final PinMode value) {
        final var reg = new int[1];
        // Get current register value
        Mmio.mmioRead32(MMIO_HANDLE[pin.getChip()], pin.getConfigOffest(), reg);
        if (value == INPUT) {
            Mmio.mmioWrite32(MMIO_HANDLE[pin.getChip()], pin.getConfigOffest(), reg[0] & (pin.getConfigMask() ^ 0xffffffff));
        } else {
            Mmio.mmioWrite32(MMIO_HANDLE[pin.getChip()], pin.getConfigOffest(), reg[0] | pin.getConfigMask());
        }
    }

    /**
     * Read pin value.
     *
     * @param pin Pin.
     * @return ON or OFF.
     */
    public PinState read(final Pin pin) {
        final var value = new int[1];
        Mmio.mmioRead32(MMIO_HANDLE[pin.getChip()], pin.getDataOffset(), value);
        PinState ret;
        if ((value[0] & pin.getDataMask()) == 0) {
            ret = OFF;
        } else {
            ret = ON;
        }
        return ret;
    }

    /**
     * Write pin value.
     *
     * @param pin Pin.
     * @param value Pin mode.
     */
    public void write(final Pin pin, final PinState value) {
        final var reg = new int[1];
        final var chip = pin.getChip();
        final var offest = pin.getDataOffset();
        final var mask = pin.getDataMask();
        // Get current register value
        Mmio.mmioRead32(MMIO_HANDLE[chip], offest, reg);
        if (value == OFF) {
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, reg[0] & (mask ^ 0xffffffff));
        } else {
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, reg[0] | mask);
        }
    }

    /**
     * Return values from all registers.
     *
     * @return List of register values.
     */
    public List<Integer> getRegValues() {
        final var list = new ArrayList<Integer>();
        final var value = new int[1];
        // Read all ports
        for (int chip = 0; chip < PORT_CHIP.length; chip++) {
            // Read all registers
            for (int register = 0; register < REGISTER_NAME.length; register++) {
                Mmio.mmioRead32(MMIO_HANDLE[PORT_CHIP[chip]], PORT_OFFSET[chip] + (register * 4), value);
                list.add(value[0]);
            }
        }
        return list;
    }

    /**
     * Compare list values and log difference. Filter is used to select on desired register name.
     *
     * @param pin Pin.
     * @param list1 First list.
     * @param list2 Second list.
     * @param filter Include filter.
     */
    public void compareLists(final Pin pin, final List<Integer> list1, final List<Integer> list2, final String filter) {
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                // Apply filter for something like CFG when only concerned with CFG* registers
                if (REGISTER_NAME[i % 9].contains(filter)) {
                    logger.info(String.format("Chip %d Pin %3d %7s Port %s Reg %s Mask %08X", pin.getChip(), pin.getPin(), pin.
                            getName(), PORT_NAME[i / 9], REGISTER_NAME[i % 9], list2.get(i) - list1.get(i)));
                }
            }
        }
    }

    /**
     * Display data register differences.
     *
     * @param pin Pin.
     */
    public void dataPin(final Pin pin) {
        final var dev = String.format("/dev/gpiochip%d", pin.getChip());
        // Set pin for input and turn off
        try (final var gpio = new Gpio(dev, pin.getPin(), new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_OUT).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                LedBlink.class.getSimpleName())))) {
            Gpio.gpioWrite(gpio.getHandle(), false);
            final var list1 = getRegValues();
            Gpio.gpioWrite(gpio.getHandle(), true);
            final var list2 = getRegValues();
            compareLists(pin, list1, list2, "DAT");
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Display configuration register differences. Sometimes (at least with Armbian) you will get DAT register changes which should
     * be ignored.
     *
     * @param pin Pin number.
     */
    public void configPin(final Pin pin) {
        final var dev = String.format("/dev/gpiochip%d", pin.getChip());
        // Set pin for input and turn off
        try (final var gpio = new Gpio(dev, pin.getPin(), new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_IN).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                LedBlink.class.getSimpleName())))) {
            Gpio.gpioSetDirection(gpio.getHandle(), GPIO_DIR_IN);
            final var list1 = getRegValues();
            Gpio.gpioSetDirection(gpio.getHandle(), GPIO_DIR_OUT);
            final var list2 = getRegValues();
            compareLists(pin, list1, list2, "CFG");
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }
    
    /**
     * Performance test using GPIOD.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfGpiod(final Pin pin, final long samples) {
        final var dev = String.format("/dev/gpiochip%d", pin.getChip());
        final var line = pin.getPin();
        try (final var gpio = new Gpio(dev, line, GPIO_DIR_OUT)) {
            var handle = gpio.getHandle();
            logger.info(String.format("Running GPIOD write test with %d samples", samples));
            final var start = Instant.now();
            // Turn pin on and off, so we can see on a scope
            for (var i = 0; i < samples; i++) {
                Gpio.gpioWrite(handle, true);
                Gpio.gpioWrite(handle, false);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f writes per second", ((double) samples / (double) timeElapsed) * 2000));
        }        
    }    

    /**
     * Performance test using MMIO write method.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfGood(final Pin pin, final long samples) {
        setMode(pin, OUTPUT);
        logger.info(String.format("Running good MMIO write test with %d samples", samples));
        final var start = Instant.now();
        // Turn pin on and off, so we can see on a scope
        for (var i = 0; i < samples; i++) {
            write(pin, ON);
            write(pin, OFF);
        }
        final var finish = Instant.now();
        // Elapsed milliseconds
        final var timeElapsed = Duration.between(start, finish).toMillis();
        logger.info(String.format("%.2f writes per second", ((double) samples / (double) timeElapsed) * 2000));
    }

    /**
     * Performance test doing same thing as MMIO write without method overhead.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfBetter(final Pin pin, final long samples) {
        setMode(pin, OUTPUT);
        final var reg = new int[1];
        final var chip = pin.getChip();
        final var offest = pin.getDataOffset();
        final var mask = pin.getDataMask();
        final var maskInv = mask ^ 0xffffffff;
        logger.info(String.format("Running better MMIO write test with %d samples", samples));
        final var start = Instant.now();
        // Turn pin on and off, so we can see on a scope
        for (var i = 0; i < samples; i++) {
            Mmio.mmioRead32(MMIO_HANDLE[chip], offest, reg);
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, reg[0] | mask);
            Mmio.mmioRead32(MMIO_HANDLE[chip], offest, reg);
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, reg[0] & maskInv);
        }
        final var finish = Instant.now();
        // Elapsed milliseconds
        final var timeElapsed = Duration.between(start, finish).toMillis();
        logger.info(String.format("%.2f writes per second", ((double) samples / (double) timeElapsed) * 2000));
    }
    
    /**
     * Performance test using raw MMIO and only reading register before writes.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfBest(final Pin pin, final long samples) {
        setMode(pin, OUTPUT);
        final var reg = new int[1];
        final var chip = pin.getChip();
        final var offest = pin.getDataOffset();
        // Only do read one time to get current value
         Mmio.mmioRead32(MMIO_HANDLE[chip], offest, reg);
        final var on = reg[0] | pin.getDataMask();
        final var off = reg[0] & (pin.getDataMask() ^ 0xffffffff);
        logger.info(String.format("Running best MMIO write test with %d samples", samples));
        final var start = Instant.now();
        // Turn pin on and off, so we can see on a scope
        for (var i = 0; i < samples; i++) {
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, on);
            Mmio.mmioWrite32(MMIO_HANDLE[chip], offest, off);
        }
        final var finish = Instant.now();
        // Elapsed milliseconds
        final var timeElapsed = Duration.between(start, finish).toMillis();
        logger.info(String.format("%.2f writes per second", ((double) samples / (double) timeElapsed) * 2000));
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
        // Config registers
        logger.info("Configuration registers");
        pinMap.entrySet().stream().map((entry) -> entry.getValue()).forEachOrdered((value) -> {
            configPin(value);
        });
        // Data registers
        logger.info("Data registers");
        pinMap.entrySet().stream().map((entry) -> entry.getValue()).forEachOrdered((value) -> {
            dataPin(value);
        });
        // Performance
        final var pin = pinMap.get(new PinKey(device, line));
        perfGpiod(pin, 10000000);
        perfGood(pin, 10000000);
        perfBetter(pin, 10000000);
        perfBest(pin, 10000000);
        return exitCode;
    }

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        // Populate pin map.
        for (int i = 0; i < PIN.length; i++) {
            pinMap.put(new PinKey(PIN_CHIP[i], PIN[i]), new Pin(PIN_CHIP[i], PIN[i], PIN_NAME[i], CFG_OFFSET[i], CFG_MASK[i],
                    DAT_OFFSET[i], DAT_MASK[i]));
        }
        // Initialize MMIO for each chip
        for (int i = 0; i < MMIO_HANDLE.length; i++) {
            final var mmio = new Mmio(CHIP[i], MMIO_SIZE[i]);
            MMIO_HANDLE[i] = mmio.getHandle();
        }
        var exitCode = new CommandLine(new DuoMmioMap()).execute(args);
        // Close MMIO for each chip
        for (int i = 0; i < MMIO_HANDLE.length; i++) {
            Mmio.mmioClose(MMIO_HANDLE[i]);
        }
        System.exit(exitCode);
    }
}
