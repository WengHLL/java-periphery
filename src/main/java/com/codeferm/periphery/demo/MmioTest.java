/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import static com.codeferm.periphery.Common.cString;
import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_BIAS_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import static com.codeferm.periphery.Gpio.GPIO_DRIVE_DEFAULT;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_NONE;
import com.codeferm.periphery.Mmio;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * MMIO Test playground.
 *
 * Use MMIO to detect changes in MMIO registers on NanoPi Duo (Allwinner H2+ CPU).
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "mmiotest", mixinStandardHelpOptions = true, version = "mmiotest 1.0.0",
        description = "Turn LED on and off.")
public class MmioTest implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(MmioTest.class);
    /**
     * MMIO base address option.
     */
    @Option(names = {"-b", "--base"}, description = "MMIO base defaults to 0x1c20800, but could be 0x1f02c00")
    private long base = 0x1c20800L;
    /**
     * MMIO size.
     */
    @Option(names = {"-s", "--size"}, description = "MMIO size defaults to 4096")
    private long size = 4096;
    /**
     * Pin names.
     */
    private static final String[] PIN_NAME = {"MIC_N", "EPhySPD", "MIC_P", "EPhyLinK", "LineOutR", "EPhyTXP", "LineOutL", "EPhyTXN",
        "CVBS", "EPhyRXP", "GPIOG6", "EPhyRXN", "GPIOG7", "USB-DP2", "GPIOA15", "USB-DM2", "GPIOA16", "USB-DP3", "GPIOA14",
        "USB-DM3", "GPIOA13", "GPIOG11", "GPIOA12", "GPIOL11", "GPIOA11", "0v", "0v", "3.3v", "GPIOA4", "5v", "GPIOA5", "5v"};

    /**
     * Get H2+ port controller register.
     *
     * @param handle MMIO handle.
     * @param port Port 0-6.
     * @param register Register 0-8.
     * @return 32 bit register value.
     */
    public int getReg(final long handle, final int port, final int register) {
        final var value = new int[1];
        Mmio.mmioRead32(handle, (port * 36) + (register * 4), value);
        return value[0];
    }

    /**
     * Set H2+ port controller register.
     *
     * @param handle MMIO handle.
     * @param port Port 0-6.
     * @param register Register 0-8.
     * @param value 32 bit register value.
     */
    public void setReg(final long handle, final int port, final int register, final int value) {
        Mmio.mmioWrite32(handle, (port * 36) + (register * 4), value);
    }

    /**
     * Return values from all registers.
     *
     * @return List of register values.
     */
    public List<Integer> getRegValues() {
        final var list = new ArrayList<Integer>();
        try (final var mmio = new Mmio(base, size)) {
            final var handle = mmio.getHandle();
            for (int port = 0; port < 7; port++) {
                for (int register = 0; register < 9; register++) {
                    list.add(getReg(handle, port, register));
                }
            }
        }
        return list;
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
        // Set pin for output and turn off
        logger.info("LED OFF");
        try (final var gpio = new Gpio("/dev/gpiochip0", 203, new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_OUT).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(cString(
                LedBlink.class.getSimpleName())))) {
            Gpio.gpioWrite(gpio.getHandle(), false);
        }
        final var list1 = getRegValues();
        // Set pin for output and turn on
        logger.info("LED ON");
        try (final var gpio = new Gpio("/dev/gpiochip0", 203, new Gpio.GpioConfig().setBias(GPIO_BIAS_DEFAULT).
                setDirection(GPIO_DIR_OUT).setDrive(GPIO_DRIVE_DEFAULT).setEdge(GPIO_EDGE_NONE).setInverted(false).setLabel(
                cString(LedBlink.class.getSimpleName())))) {
            Gpio.gpioWrite(gpio.getHandle(), true);
        }
        final var list2 = getRegValues();
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                logger.info(String.format("Port %d Reg %d = %08X %08X", i / 9, i % 9, list1.get(i), list2.get(i)));
            }
        }
        // Turn off LED via MMIO
        try (final var mmio = new Mmio(base, size)) {
            final var handle = mmio.getHandle();
            final var start = Instant.now();
            for (int i = 0; i < 100000000; i++) {
                Mmio.mmioWrite32(handle, 232, 0x2800);
                Mmio.mmioWrite32(handle, 232, 0x2000);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f writes per second", ((double) 100000000 / (double) timeElapsed) * 2000));
        }
        return exitCode;
    }

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        var exitCode = new CommandLine(new MmioTest()).execute(args);
        System.exit(exitCode);
    }
}
