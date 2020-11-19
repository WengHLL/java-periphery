/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import com.codeferm.periphery.Mmio;
import com.codeferm.periphery.mmio.Pin.PinMode;
import static com.codeferm.periphery.mmio.Pin.PinMode.INPUT;
import static com.codeferm.periphery.mmio.Pin.PinMode.OUTPUT;
import com.codeferm.periphery.mmio.Pin.PinState;
import static com.codeferm.periphery.mmio.Pin.PinState.OFF;
import static com.codeferm.periphery.mmio.Pin.PinState.ON;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;

/**
 * GPIO performance using MMIO.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@CommandLine.Command(name = "mmioperf", mixinStandardHelpOptions = true, version = "mmioperf 1.0.0",
        description = "Show performance of MMIO based GPIO")
public class Perf implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Perf.class);
    /**
     * Input file.
     */
    @CommandLine.Option(names = {"-i", "--in"}, description = "Input property file name")
    private String inFileName = "duo-map.properties";
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
     * Get pin mode.
     *
     * @param pin Pin.
     * @return INPUT or OUTPUT.
     */
    public PinMode getMode(final Pin pin) {
        final var value = new int[1];
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getConfigOffset(), value);
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
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getConfigOffset(), reg);
        if (value == INPUT) {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getConfigOffset(), reg[0] & (pin.getConfigMask()
                    ^ 0xffffffff));
        } else {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getConfigOffset(), reg[0] | pin.getConfigMask());
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
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getDataOffset(), value);
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
        // Get current register value
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getDataOffset(), reg);
        if (value == OFF) {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getDataOffset(), reg[0] & (pin.getDataMask() ^ 0xffffffff));
        } else {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getDataOffset(), reg[0] | pin.getDataMask());
        }
    }

    /**
     * Performance test using GPIOD.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfGpiod(final Pin pin, final long samples) {
        try (final var gpio = new Gpio(String.format("/dev/gpiochip%d", pin.getKey().getChip()), pin.getKey().getPin(), GPIO_DIR_OUT)) {
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
            logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
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
        logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
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
        final var offest = pin.getDataOffset();
        final var mask = pin.getDataMask();
        final var maskInv = mask ^ 0xffffffff;
        logger.info(String.format("Running better MMIO write test with %d samples", samples));
        final var start = Instant.now();
        // Turn pin on and off, so we can see on a scope
        for (var i = 0; i < samples; i++) {
            Mmio.mmioRead32(pin.getMmioHadle(), offest, reg);
            Mmio.mmioWrite32(pin.getMmioHadle(), offest, reg[0] | mask);
            Mmio.mmioRead32(pin.getMmioHadle(), offest, reg);
            Mmio.mmioWrite32(pin.getMmioHadle(), offest, reg[0] & maskInv);
        }
        final var finish = Instant.now();
        // Elapsed milliseconds
        final var timeElapsed = Duration.between(start, finish).toMillis();
        logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
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
        final var offest = pin.getDataOffset();
        final var handle = pin.getMmioHadle();
        // Only do read one time to get current value
        Mmio.mmioRead32(handle, offest, reg);
        final var on = reg[0] | pin.getDataMask();
        final var off = reg[0] & (pin.getDataMask() ^ 0xffffffff);
        logger.info(String.format("Running best MMIO write test with %d samples", samples));
        final var start = Instant.now();
        // Turn pin on and off, so we can see on a scope
        for (var i = 0; i < samples; i++) {
            Mmio.mmioWrite32(handle, offest, on);
            Mmio.mmioWrite32(handle, offest, off);
        }
        final var finish = Instant.now();
        // Elapsed milliseconds
        final var timeElapsed = Duration.between(start, finish).toMillis();
        logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
    }

    /**
     * Read pin map properties and run performance test.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        final var file = new File();
        // Build pin Map
        final Map<PinKey, Pin> pinMap = file.loadPinMap(inFileName);
        final List<Long> mmioHandle = new ArrayList<>();
        // Open MMIO for each chip
        for (int i = 0; i < file.getChips().size(); i++) {
            final var mmio = new Mmio(file.getChips().get(i), file.getMmioSize().get(i));
            mmioHandle.add(mmio.getHandle());
        }
        // Set MMIO handle for each pin
        pinMap.entrySet().forEach((entry) -> {
            entry.getValue().setMmioHadle(mmioHandle.get(entry.getKey().getChip()));
        });
        final var pin = pinMap.get(new PinKey(device, line));
        perfGpiod(pin, 10000000);
        perfGood(pin, 10000000);
        perfBetter(pin, 10000000);
        perfBest(pin, 10000000);
        mmioHandle.forEach((handle) -> {
            Mmio.mmioClose(handle);
        });
        return exitCode;
    }

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        System.exit(new CommandLine(new Perf()).execute(args));
    }
}
