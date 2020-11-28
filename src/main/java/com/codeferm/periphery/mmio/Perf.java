/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import com.codeferm.periphery.Mmio;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
     * Read pin value.
     *
     * @param pin Pin.
     * @return True = on, false = off..
     */
    public boolean read(final Pin pin) {
        final var value = new int[1];
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getDataIn().getOffset(), value);
        boolean ret;
        if ((value[0] & pin.getDataIn().getMask()) == 0) {
            ret = false;
        } else {
            ret = true;
        }
        return ret;
    }

    /**
     * Write pin value.
     *
     * @param pin Pin.
     * @param value Pin mode.
     */
    public void write(final Pin pin, final boolean value) {
        final var reg = new int[1];
        // Get current register value
        Mmio.mmioRead32(pin.getMmioHadle(), pin.getDataOut().getOffset(), reg);
        if (!value) {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getDataOut().getOffset(), reg[0] & (pin.getDataOut().getMask() ^ 0xffffffff));
        } else {
            Mmio.mmioWrite32(pin.getMmioHadle(), pin.getDataOut().getOffset(), reg[0] | pin.getDataOut().getMask());
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
        try (final var gpio = new Gpio(String.format("/dev/gpiochip%d", pin.getKey().getChip()), pin.getKey().getPin(), GPIO_DIR_OUT)) {
            logger.info(String.format("Running good MMIO write test with %d samples", samples));
            final var start = Instant.now();
            // Turn pin on and off, so we can see on a scope
            for (var i = 0; i < samples; i++) {
                write(pin, true);
                write(pin, false);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
        }
    }

    /**
     * Performance test doing same thing as MMIO write without method overhead.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfBetter(final Pin pin, final long samples) {
        try (final var gpio = new Gpio(String.format("/dev/gpiochip%d", pin.getKey().getChip()), pin.getKey().getPin(), GPIO_DIR_OUT)) {
            final var reg = new int[1];
            final var offest = pin.getDataOut().getOffset();
            final var mask = pin.getDataOut().getMask();
            final var maskInv = mask ^ 0xffffffff;
            final var handle = pin.getMmioHadle();
            logger.info(String.format("Running better MMIO write test with %d samples", samples));
            final var start = Instant.now();
            // Turn pin on and off, so we can see on a scope
            for (var i = 0; i < samples; i++) {
                Mmio.mmioRead32(handle, offest, reg);
                Mmio.mmioWrite32(handle, offest, reg[0] | mask);
                Mmio.mmioRead32(handle, offest, reg);
                Mmio.mmioWrite32(handle, offest, reg[0] & maskInv);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f KHz", ((double) samples / (double) timeElapsed)));
        }
    }

    /**
     * Performance test using raw MMIO and only reading register once before writes.
     *
     * @param pin Pin number.
     * @param samples How many samples to run.
     */
    public void perfBest(final Pin pin, final long samples) {
        try (final var gpio = new Gpio(String.format("/dev/gpiochip%d", pin.getKey().getChip()), pin.getKey().getPin(), GPIO_DIR_OUT)) {
            final var reg = new int[1];
            final var offest = pin.getDataOut().getOffset();
            final var handle = pin.getMmioHadle();
            // Only do read one time to get current value
            Mmio.mmioRead32(handle, offest, reg);
            final var on = reg[0] | pin.getDataOut().getMask();
            final var off = reg[0] & (pin.getDataOut().getMask() ^ 0xffffffff);
            logger.info(String.format("Running best MMIO write test with %d samples", samples));
            final var start = Instant.now();
            // Turn pin on and off, so we can see on a scope
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
