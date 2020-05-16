/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_IN;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_BOTH;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_FALLING;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_RISING;
import static com.codeferm.periphery.Gpio.GPIO_POLL_EVENT;
import static com.codeferm.periphery.Gpio.GPIO_SUCCESS;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Blocking event using button.
 *
 * Should work on any board with a button built in or wire one up. Just change device and line arguments as needed.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "buttonwait", mixinStandardHelpOptions = true, version = "buttonwait 1.0.0",
        description = "Uses edge detection to wait for button press.")
public class ButtonWait implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ButtonWait.class);
    /**
     * Device option.
     */
    @Option(names = {"-d", "--device"}, description = "GPIO device defaults to /dev/gpiochip1")
    private String device = "/dev/gpiochip1";
    /**
     * Line option.
     */
    @Option(names = {"-l", "--line"}, description = "GPIO line defaults to 3 for NanoPi Duo built-in button")
    private int line = 3;

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        var exitCode = new CommandLine(new ButtonWait()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Use blocking edge detection.
     *
     * @return Exit code.
     */
    @Override
    public Integer call() {
        var exitCode = 0;
        final var handle = Gpio.gpioNew();
        if (Gpio.gpioOpen(handle, device, line, GPIO_DIR_IN) == GPIO_SUCCESS) {
            final var formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            final var edge = new int[1];
            final var timestamp = new long[1];
            Gpio.gpioSetEdge(handle, GPIO_EDGE_BOTH);
            logger.info("Press button, stop pressing button for 10 seconds to exit");
            // Poll for event and timeout in 10 seconds if no event
            while (Gpio.gpioPoll(handle, 10000) == GPIO_POLL_EVENT) {
                Gpio.gpioReadEvent(handle, edge, timestamp);
                final var date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp[0] / 1000000), ZoneId.systemDefault());
                if (edge[0] == GPIO_EDGE_RISING) {
                    logger.info(String.format("Edge rising, %s", date.format(formatter)));
                } else if (edge[0] == GPIO_EDGE_FALLING) {
                    logger.info(String.format("Edge falling %s", date.format(formatter)));
                } else {
                    logger.info(String.format("Invalid edge %d, %s", edge[0], date.format(formatter)));
                }
            }
            Gpio.gpioClose(handle);
        } else {
            exitCode = Gpio.gpioErrNo(handle);
            logger.error(Gpio.gpioErrMessage(handle));
        }
        Gpio.gpioFree(handle);
        return exitCode;
    }
}
