/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery;

import static com.codeferm.periphery.Common.MAX_CHAR_ARRAY_LEN;
import static com.codeferm.periphery.Common.jString;
import static com.codeferm.periphery.Common.memMove;
import static org.fusesource.hawtjni.runtime.FieldFlag.CONSTANT;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniField;
import org.fusesource.hawtjni.runtime.JniMethod;
import org.fusesource.hawtjni.runtime.Library;
import static org.fusesource.hawtjni.runtime.MethodFlag.CONSTANT_INITIALIZER;

/**
 * c-periphery GPIO wrapper methods for Linux userspace character device gpio-cdev and sysfs GPIOs.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@JniClass
public class Gpio {

    /**
     * gpioPoll returned event.
     */
    public static final int GPIO_POLL_EVENT = 1;
    /**
     * gpioPoll timeout.
     */
    public static final int GPIO_POLL_TIMEOUT = 0;
    /**
     * Function was successful.
     */
    public static final int GPIO_SUCCESS = 0;
    /**
     * java-periphery library.
     */
    private static final Library LIBRARY = new Library("java-periphery", Gpio.class);

    /**
     * Load library.
     */
    static {
        LIBRARY.load();
        init();
    }

    /**
     * Load constants.
     */
    @JniMethod(flags = {CONSTANT_INITIALIZER})
    private static native void init();
    /**
     * Error constants.
     */
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_ARG;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_OPEN;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_NOT_FOUND;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_QUERY;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_CONFIGURE;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_UNSUPPORTED;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_INVALID_OPERATION;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_IO;
    @JniField(flags = {CONSTANT})
    public static int GPIO_ERROR_CLOSE;
    /**
     * Direction constants.
     */
    @JniField(flags = {CONSTANT})
    public static int GPIO_DIR_IN;
    @JniField(flags = {CONSTANT})
    public static int GPIO_DIR_OUT;
    @JniField(flags = {CONSTANT})
    public static int GPIO_DIR_OUT_LOW;
    @JniField(flags = {CONSTANT})
    public static int GPIO_DIR_OUT_HIGH;
    /**
     * Edge constants.
     */
    @JniField(flags = {CONSTANT})
    public static int GPIO_EDGE_NONE;
    @JniField(flags = {CONSTANT})
    public static int GPIO_EDGE_RISING;
    @JniField(flags = {CONSTANT})
    public static int GPIO_EDGE_FALLING;
    @JniField(flags = {CONSTANT})
    public static int GPIO_EDGE_BOTH;

    /**
     * Allocate a GPIO handle. Returns a valid handle on success, or NULL on failure.
     *
     * @return A valid handle on success, or NULL on failure.
     */
    @JniMethod(accessor = "gpio_new")
    public static final native long gpioNew();

    /**
     * Open the character device GPIO with the specified GPIO line and direction at the specified character device GPIO chip path.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param path GPIO chip character device path.
     * @param line GPIO line number.
     * @param direction One of the direction values.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_open")
    public static native int gpioOpen(long gpio, String path, int line, int direction);

    /**
     * Open the character device GPIO with the specified GPIO name and direction at the specified character device GPIO chip path
     * (e.g. /dev/gpiochip0).
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param path GPIO chip character device path.
     * @param name GPIO line name.
     * @param direction One of the direction values.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_open_name")
    public static native int gpioOpenName(long gpio, String path, String name, int direction);

    /**
     * Open the sysfs GPIO with the specified line and direction.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param line GPIO line number.
     * @param direction One of the direction values.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_open_sysfs")
    public static native int gpioOpenSysfs(long gpio, int line, int direction);

    /**
     * Read the state of the GPIO into value.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param value Pointer to an allocated bool.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_read")
    public static native int gpioRead(long gpio, boolean[] value);

    /**
     * Set the state of the GPIO to value.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param value True of false.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_write")
    public static native int gpioWrite(long gpio, boolean value);

    /**
     * Poll a GPIO for the edge event configured with gpio_set_edge(). For character device GPIOs, the edge event should be consumed
     * with gpio_read_event(). For sysfs GPIOs, the edge event should be consumed with gpio_read().
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param timeoutMs Positive number for a timeout in milliseconds, 0 for a non-blocking poll, or a negative number for a
     * blocking poll.
     * @return 1 on success (an edge event occurred), 0 on timeout, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_poll")
    public static native int gpioPoll(long gpio, int timeoutMs);

    /**
     * Close the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_close")
    public static native int gpioClose(long gpio);

    /**
     * Free a GPIO handle.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     */
    @JniMethod(accessor = "gpio_free")
    public static native void gpioFree(long gpio);

    /**
     * Read the edge event that occurred with the GPIO. This method is intended for use with character device GPIOs and is
     * unsupported by sysfs GPIOs.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param edge Pointer to an allocated int.
     * @param timestamp Pointer to an allocated long.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_read_event")
    public static native int gpioReadEvent(long gpio, int[] edge, long[] timestamp);

    /**
     * Get the configured direction of the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param direction Pointer to an allocated int.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_get_direction")
    public static native int gpioGetDirection(long gpio, int[] direction);

    /**
     * Get the configured interrupt edge of the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param edge Pointer to an allocated int.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_get_edge")
    public static native int gpioGetEdge(long gpio, int[] edge);

    /**
     * Set the direction of the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param direction One of the direction values.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_set_direction")
    public static native int gpioSetDirection(long gpio, int direction);

    /**
     * Set the interrupt edge of the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param edge One of the direction values.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_set_edge")
    public static native int gpioSetEdge(long gpio, int edge);

    /**
     * Return the line the GPIO handle was opened with.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return Line the GPIO handle was opened with.
     */
    @JniMethod(accessor = "gpio_line")
    public static native int gpioLine(long gpio);

    /**
     * Return the line file descriptor of the GPIO handle.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return Line file descriptor.
     */
    @JniMethod(accessor = "gpio_fd")
    public static native int gpioFd(long gpio);

    /**
     * Return the line name of the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param str Line name.
     * @param len Length of char array.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_name")
    public static native int gpioName(long gpio, byte[] str, long len);

    /**
     * Return the line name of the GPIO. Wraps native method and simplifies.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return Line name.
     */
    public static String gpioName(long gpio) {
        var str = new byte[MAX_CHAR_ARRAY_LEN];
        if (gpioName(gpio, str, str.length) < 0) {
            throw new RuntimeException(gpioErrMessage(gpio));
        }
        return jString(str);
    }

    /**
     * Return the GPIO chip file descriptor of the GPIO handle.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return GPIO chip file descriptor.
     */
    @JniMethod(accessor = "gpio_chip_fd")
    public static native int gpioChipFd(long gpio);

    /**
     * Return the name of the GPIO chip associated with the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param str Name of the GPIO chip.
     * @param len Length of char array.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_chip_name")
    public static native int gpioChipName(long gpio, byte[] str, long len);

    /**
     * Return the name of the GPIO chip associated with the GPIO. Wraps native method and simplifies.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return
     */
    public static String gpioChipName(long gpio) {
        var str = new byte[MAX_CHAR_ARRAY_LEN];
        if (gpioChipName(gpio, str, str.length) < 0) {
            throw new RuntimeException(gpioErrMessage(gpio));
        }
        return jString(str);
    }

    /**
     * Return the label of the GPIO chip associated with the GPIO.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param str Label of the GPIO chip.
     * @param len Length of char array.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_chip_label")
    public static native int gpioChipLabel(long gpio, byte[] str, long len);

    /**
     * Return the label of the GPIO chip associated with the GPIO. Wraps native method and simplifies.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return GPIO label.
     */
    public static String gpioChipLabel(long gpio) {
        var str = new byte[MAX_CHAR_ARRAY_LEN];
        if (gpioChipLabel(gpio, str, str.length) < 0) {
            throw new RuntimeException(gpioErrMessage(gpio));
        }
        return jString(str);
    }

    /**
     *
     * Return a string representation of the GPIO handle.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @param str String representation of the GPIO handle.
     * @param len Length of char array.
     * @return 0 on success, or a negative GPIO error code on failure.
     */
    @JniMethod(accessor = "gpio_tostring")
    public static native int gpioToString(long gpio, byte[] str, long len);

    /**
     * Return a string representation of the GPIO handle. Wraps native method and simplifies.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return GPIO handle as String.
     */
    public static String gpioToString(long gpio) {
        var str = new byte[MAX_CHAR_ARRAY_LEN];
        if (gpioToString(gpio, str, str.length) < 0) {
            throw new RuntimeException(gpioErrMessage(gpio));
        }
        return jString(str);
    }

    /**
     * Return the libc errno of the last failure that occurred.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return libc errno.
     */
    @JniMethod(accessor = "gpio_errno")
    public static native int gpioErrNo(long gpio);

    /**
     * Return a human readable error message pointer of the last failure that occurred.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return Error message pointer.
     */
    @JniMethod(accessor = "gpio_errmsg")
    public static native long gpioErrMsg(long gpio);

    /**
     * Return a human readable error message of the last failure that occurred. Converts const char * returned by gpio_errmsg to a
     * Java String.
     *
     * @param gpio Valid pointer to an allocated GPIO handle structure.
     * @return Error message.
     */
    public static String gpioErrMessage(long gpio) {
        var ptr = gpioErrMsg(gpio);
        var str = new byte[MAX_CHAR_ARRAY_LEN];
        memMove(str, ptr, str.length);
        return jString(str);
    }
}
