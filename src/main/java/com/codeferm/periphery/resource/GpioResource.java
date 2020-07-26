/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.resource;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Common.free;

/**
 * AutoCloseable GPIO wrapper. try-with-resources automatically closes handle and deallocate optional config label.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class GpioResource extends Gpio implements AutoCloseable {

    /**
     * GPIO handle.
     */
    final private long handle;
    /**
     * Gpio config struct.
     */
    final private GpioConfig config;

    /**
     * Open the character device GPIO with the specified GPIO line and direction at the specified character device GPIO chip path.
     *
     * @param path GPIO chip character device path.
     * @param line GPIO line number.
     * @param direction One of the direction values.
     */
    public GpioResource(final String path, final int line, final int direction) {
        // Config not used
        config = null;
        // Allocate handle
        handle = gpioNew();
        if (handle == 0) {
            throw new RuntimeException("Handle cannot be NULL");
        }
        // Open line
        if (gpioOpen(handle, path, line, direction) != GPIO_SUCCESS) {
            // Free handle before throwing exception
            gpioFree(handle);
            throw new RuntimeException(gpioErrMessage(handle));
        }
    }

    /**
     * Open the character device GPIO with the specified GPIO name and direction at the specified character device GPIO chip path
     * (e.g. /dev/gpiochip0).
     *
     * @param path GPIO chip character device path.
     * @param name GPIO line name.
     * @param direction One of the direction values.
     */
    public GpioResource(final String path, String name, int direction) {
        // Config not used
        config = null;
        // Allocate handle
        handle = gpioNew();
        if (handle == 0) {
            throw new RuntimeException("Handle cannot be NULL");
        }
        // Open line
        if (gpioOpenName(handle, path, name, direction) != GPIO_SUCCESS) {
            // Free handle before throwing exception
            gpioFree(handle);
            throw new RuntimeException(gpioErrMessage(handle));
        }
    }

    /**
     * Open the character device GPIO with the specified GPIO line and configuration at the specified character device GPIO chip
     * path (e.g. /dev/gpiochip0).
     *
     * @param path GPIO chip character device path.
     * @param line GPIO line number.
     * @param config Configuration struct.
     */
    public GpioResource(final String path, final int line, final GpioConfig config) {
        this.config = config;
        // Allocate handle
        handle = gpioNew();
        if (handle == 0) {
            // Deallocate label before throwing exception
            if (config.getLabel() != 0) {
                free(config.getLabel());
            }
            throw new RuntimeException("Handle cannot be NULL");
        }
        // Open line
        if (gpioOpenAdvanced(handle, path, line, config) != GPIO_SUCCESS) {
            // Free handle before throwing exception
            gpioFree(handle);
            // Deallocate label before throwing exception
            if (config.getLabel() != 0) {
                free(config.getLabel());
            }
            throw new RuntimeException(gpioErrMessage(handle));
        }
    }

    /**
     * Open the character device GPIO with the specified GPIO name and configuration at the specified character device GPIO chip
     * path (e.g. /dev/gpiochip0).
     *
     * @param path GPIO chip character device path.
     * @param name GPIO line name.
     * @param config Configuration struct.
     */
    public GpioResource(final String path, final String name, final GpioConfig config) {
        this.config = config;
        // Allocate handle
        handle = gpioNew();
        if (handle == 0) {
            // Deallocate label before throwing exception
            if (config.getLabel() != 0) {
                free(config.getLabel());
            }
            throw new RuntimeException("Handle cannot be NULL");
        }
        // Open line
        if (gpioOpenNameAdvanced(handle, path, name, config) != GPIO_SUCCESS) {
            // Free handle before throwing exception
            gpioFree(handle);
            // Deallocate label before throwing exception
            if (config.getLabel() != 0) {
                free(config.getLabel());
            }
            throw new RuntimeException(gpioErrMessage(handle));
        }
    }

    /**
     * Open the sysfs GPIO with the specified line and direction.
     *
     * @param line GPIO line number.
     * @param direction One of the direction values.
     */
    public GpioResource(final int line, final int direction) {
        // Config not used
        config = null;
        // Allocate handle
        handle = gpioNew();
        if (handle == 0) {
            throw new RuntimeException("Handle cannot be NULL");
        }
        // Open line
        if (gpioOpenSysfs(handle, line, direction) != GPIO_SUCCESS) {
            // Free handle before throwing exception
            gpioFree(handle);
            throw new RuntimeException(gpioErrMessage(handle));
        }
    }

    /**
     * Close handle and free line label if allocated.
     */
    @Override
    public void close() {
        // Close handle
        gpioClose(handle);
        // Free handle
        gpioFree(handle);
        // Deallocate label
        if (config != null && config.getLabel() != 0) {
            free(config.getLabel());
        }
    }

    /**
     * Handle accessor.
     *
     * @return Handle.
     */
    public long getHandle() {
        return handle;
    }

    /**
     * Config accessor.
     *
     * @return Config.
     */
    public GpioConfig getConfig() {
        return config;
    }
}
