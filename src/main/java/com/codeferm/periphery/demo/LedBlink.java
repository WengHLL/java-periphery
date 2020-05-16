/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import static com.codeferm.periphery.Gpio.GPIO_SUCCESS;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Blink LED.
 *
 * Using the NanoPi Duo connect a 220Ω resistor to ground, then the resistor to the cathode (the short pin) of the LED. Connect the
 * anode (the long pin) of the LED to line 203 (IOG11).
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "ledblink", mixinStandardHelpOptions = true, version = "ledblink 1.0.0",
        description = "Turn LED on and off.")
public class LedBlink implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(LedBlink.class);
    /**
     * Device option.
     */
    @Option(names = {"-d", "--device"}, description = "GPIO device defaults to /dev/gpiochip0")
    private String device = "/dev/gpiochip0";
    /**
     * Line option.
     */
    @Option(names = {"-l", "--line"}, description = "GPIO line defaults to 203 IOG11 for NanoPi Duo")
    private int line = 203;

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        var exitCode = new CommandLine(new LedBlink()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Blink LED.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        final var handle = Gpio.gpioNew();
        if (Gpio.gpioOpen(handle, device, line, GPIO_DIR_OUT) == GPIO_SUCCESS) {
            logger.info("Blinking LED");
            var i = 0;
            while (i < 10) {
                Gpio.gpioWrite(handle, true);
                TimeUnit.SECONDS.sleep(1);
                Gpio.gpioWrite(handle, false);
                TimeUnit.SECONDS.sleep(1);
                i++;
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
