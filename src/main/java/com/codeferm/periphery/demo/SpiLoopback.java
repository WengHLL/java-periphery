/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Spi;
import static com.codeferm.periphery.Spi.SPI_SUCCESS;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * SPI loopback.
 *
 * Connect wire between MOSI and MISO pins.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "spiloopback", mixinStandardHelpOptions = true, version = "spiloopback 1.0.0",
        description = "Send data between MOSI and MISO pins.")
public class SpiLoopback implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(SpiLoopback.class);
    /**
     * Device option.
     */
    @Option(names = {"-d", "--device"}, description = "SPI device defaults to /dev/spidev1.0")
    private String device = "/dev/spidev1.0";

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        var exitCode = new CommandLine(new SpiLoopback()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Send data via loopback.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        final var handle = Spi.spiNew();
        if (Spi.spiOpen(handle, device, 0, 500000) == SPI_SUCCESS) {
            final var txBuf = new byte[128];
            // Change some data at beginning and end.
            txBuf[0] = (byte) 0xff;
            txBuf[127] = (byte) 0x80;
            final var rxBuf = new byte[128];
            Spi.spiTransfer(handle, txBuf, rxBuf, txBuf.length);
            logger.info(String.format("%02X, %02X", (short) rxBuf[0] & 0xff, (short) rxBuf[127] & 0xff));
            Spi.spiClose(handle);
        } else {
            exitCode = Spi.spiErrNo(handle);
            logger.error(Spi.spiErrMessage(handle));
        }
        Spi.spiFree(handle);
        return exitCode;
    }
}
