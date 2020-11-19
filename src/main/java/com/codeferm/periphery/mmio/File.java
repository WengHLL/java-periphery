/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;

/**
 * Parse property files containing board specific information.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class File {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(File.class);

    /**
     * GPIO chips sometimes called banks
     */
    private List<Long> chips;
    /**
     * MMIO size for each chip
     */
    private List<Long> mmioSize;
    /**
     * GPIOD pin numbers, no weirdo wiringPi or BCM numbers
     */
    private List<Integer> pins;
    /**
     * GPIO CHIP used by pin
     */
    private List<Integer> pinChip;
    /**
     * Pin name
     */
    private List<String> pinName;
    /**
     * Port offset inside chip
     */
    private List<Integer> portOffset;
    /**
     * Chip port os on
     */
    private List<Integer> portChip;
    /**
     * Name of port
     */
    private List<String> portName;
    /**
     * Register offset inside chip
     */
    private List<Integer> registerOffset;
    /**
     * Register name
     */
    private List<String> registerName;
    /**
     * Configuration register filter.
     */
    private String configFilter;
    /**
     * Data register filter.
     */
    private String dataFilter;
    /**
     * Pull register filter.
     */
    private String pullFilter;
    /**
     * File description.
     */
    private String description;

    /**
     * Default constructor.
     */
    public File() {
    }

    public List<Long> getChips() {
        return chips;
    }

    public void setChips(final List<Long> chips) {
        this.chips = chips;
    }

    public List<Long> getMmioSize() {
        return mmioSize;
    }

    public void setMmioSize(final List<Long> mmioSize) {
        this.mmioSize = mmioSize;
    }

    public List<Integer> getPins() {
        return pins;
    }

    public void setPins(final List<Integer> pins) {
        this.pins = pins;
    }

    public List<Integer> getPinChip() {
        return pinChip;
    }

    public void setPinChip(final List<Integer> pinChip) {
        this.pinChip = pinChip;
    }

    public List<String> getPinName() {
        return pinName;
    }

    public void setPinName(final List<String> pinName) {
        this.pinName = pinName;
    }

    public List<Integer> getPortOffset() {
        return portOffset;
    }

    public void setPortOffset(final List<Integer> portOffset) {
        this.portOffset = portOffset;
    }

    public List<Integer> getPortChip() {
        return portChip;
    }

    public void setPortChip(final List<Integer> portChip) {
        this.portChip = portChip;
    }

    public List<String> getPortName() {
        return portName;
    }

    public void setPortName(final List<String> portName) {
        this.portName = portName;
    }

    public List<Integer> getRegisterOffset() {
        return registerOffset;
    }

    public void setRegisterOffset(final List<Integer> registerOffset) {
        this.registerOffset = registerOffset;
    }

    public List<String> getRegisterName() {
        return registerName;
    }

    public void setRegisterName(final List<String> registerName) {
        this.registerName = registerName;
    }

    public String getConfigFilter() {
        return configFilter;
    }

    public void setConfigFilter(final String configFilter) {
        this.configFilter = configFilter;
    }

    public String getDataFilter() {
        return dataFilter;
    }

    public void setDataFilter(final String dataFilter) {
        this.dataFilter = dataFilter;
    }

    public String getPullFilter() {
        return pullFilter;
    }

    public void setPullFilter(final String pullFilter) {
        this.pullFilter = pullFilter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Load properties file from file path or fail back to class path.
     *
     * @param propertyFile Name of property file.
     * @return Properties.
     */
    public Properties loadProperties(final String propertyFile) {
        Properties props = new Properties();
        try {
            // Get properties from file
            props.load(new FileInputStream(propertyFile));
            logger.debug("Properties loaded from file {}", propertyFile);
        } catch (IOException e1) {
            logger.warn("Properties file not found {}", propertyFile);
            // Get properties from classpath
            try (final var stream = File.class.getClassLoader().getResourceAsStream(propertyFile)) {
                props.load(stream);
                logger.debug("Properties loaded from class path {}", propertyFile);
            } catch (IOException e2) {
                throw new RuntimeException("No properties found", e2);
            }
        }
        return props;
    }

    /**
     * Return List of Long from comma delimited string. Spaces are stripped out.
     *
     * @param str Comma delimited hex string.
     * @return List of Long.
     */
    public List<Long> hexToLongList(final String str) {
        return Arrays.stream(str.replace(" ", "").split(",")).map(Long::decode).collect(Collectors.toList());
    }

    /**
     * Return List of Long from comma delimited string. Spaces are stripped out.
     *
     * @param str Comma delimited decimal string.
     * @return List of Long.
     */
    public List<Long> decToLongList(final String str) {
        return Arrays.stream(str.replace(" ", "").split(",")).map(Long::parseLong).collect(Collectors.toList());
    }

    /**
     * Return List of Integer from comma delimited string. Spaces are stripped out.
     *
     * @param str Comma delimited hex string.
     * @return List of Integer.
     */
    public List<Integer> hexToIntList(final String str) {
        return Arrays.stream(str.replace(" ", "").split(",")).map(Integer::decode).collect(Collectors.toList());
    }

    /**
     * Return List of Integer from comma delimited string. Spaces are stripped out.
     *
     * @param str Comma delimited decimal string.
     * @return List of Integer.
     */
    public List<Integer> decToIntList(final String str) {
        return Arrays.stream(str.replace(" ", "").split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    /**
     * Return List of String from comma delimited string. Spaces are stripped out.
     *
     * @param str Comma delimited string.
     * @return List of String.
     */
    public List<String> strToStrList(final String str) {
        return Arrays.asList(str.replace(" ", "").split(","));
    }

    /**
     * Parse input property file.
     *
     * @param fileName Property file name.
     * @return Pin Map.
     */
    public Map<PinKey, Pin> parseInput(final String fileName) {
        final Map<PinKey, Pin> pinMap = new TreeMap<>();
        final var properties = loadProperties(fileName);
        // Make sure properties loaded
        if (!properties.isEmpty()) {
            chips = hexToLongList(properties.getProperty("chips"));
            mmioSize = decToLongList(properties.getProperty("chip.size"));
            pins = decToIntList(properties.getProperty("pins"));
            pinChip = decToIntList(properties.getProperty("pin.chip"));
            pinName = strToStrList(properties.getProperty("pin.name"));
            portOffset = hexToIntList(properties.getProperty("port.offset"));
            portChip = decToIntList(properties.getProperty("port.chip"));
            portName = strToStrList(properties.getProperty("port.name"));
            registerOffset = hexToIntList(properties.getProperty("register.offset"));
            registerName = strToStrList(properties.getProperty("register.name"));
            configFilter = properties.getProperty("config.filter");
            dataFilter = properties.getProperty("data.filter");
            pullFilter = properties.getProperty("pull.filter");
            description = properties.getProperty("description");
            // Create minimal pin Map with chip, pin and pin name
            for (int i = 0; i < pins.size(); i++) {
                PinKey key = new PinKey(pinChip.get(i), pins.get(i));
                pinMap.put(key, new Pin(key, pinName.get(i)));
            }
        }
        return pinMap;
    }

    /**
     * Generate property file from pin Map.
     *
     * @param pinMap Pin Map.
     * @param inFileName Input property file.
     * @param outFileName Output property file.
     */
    public void genProperties(final Map<PinKey, Pin> pinMap, final String inFileName, final String outFileName) {
        logger.debug("Generating output file {}", outFileName);
        final var properties = loadProperties(inFileName);
        try (final var writer = new BufferedWriter(new FileWriter(outFileName))) {
            writer.write(String.format(
                    "#\n# Generated by %s on %s\n#\n# Format: pin.chip.number = port name, pin name, config name, "
                    + "config offset, config mask, data name, data offset, data mask, pull, name, pull offset, pull up mask, "
                    + "pull down mask\n#\n\n", this.getClass().getCanonicalName(), DateTimeFormatter.ISO_INSTANT.format(Instant.
                    now())));
            writer.write(String.format("description = %s\nchips = %s\nchip.size = %s\n", properties.getProperty("description"),
                    properties.getProperty("chips"), properties.getProperty("chip.size")));
            // Write entry for each pin
            for (final var entry : pinMap.entrySet()) {
                PinKey key = entry.getKey();
                Pin value = entry.getValue();
                writer.write(String.format(
                        "pin.%d.%d = %s, %s, %s, 0x%02x, 0x%08x, %s, 0x%02x, 0x%08x, %s, 0x%02x, 0x%08x, 0x%08x\n",
                        key.getChip(), key.getPin(), value.getPortName(), value.getName(), value.getConfigName(), value.
                        getConfigOffset(), value.getConfigMask(), value.getDataName(), value.getDataOffset(), value.getDataMask(),
                        value.getPullName(), value.getPullOffset(), value.getPullUpMask(), value.getPullDownMask()));
            }
        } catch (IOException e) {
            logger.error(String.format("Error %s", e.getMessage()));
        }
    }

    /**
     * Convert hex string to int.
     *
     * @param str Hex string.
     * @return int or -1 for null/"null";
     */
    public int hexToInt(final String str) {
        var i = -1;
        if (str != null && !str.toLowerCase().contains("null")) {
            i = Integer.decode(str.trim());
        }
        return i;
    }

    /**
     * Convert string to string handling "null".
     *
     * @param str String.
     * @return String null for null/"null";
     */
    public String strToStr(final String str) {
        String s = null;
        if (str != null && !str.toLowerCase().contains("null")) {
            s = str.trim();
        }
        return s;
    }

    /**
     * Parse property file into Map.
     *
     * @param inFileName Property file name.
     * @return Pin Map.
     */
    public Map<PinKey, Pin> loadPinMap(final String inFileName) {
        final var properties = loadProperties(inFileName);
        chips = hexToLongList(properties.getProperty("chips"));
        mmioSize = decToLongList(properties.getProperty("chip.size"));
        final Map<PinKey, Pin> pinMap = new TreeMap<>();
        // Process all properties
        properties.entrySet().forEach((entry) -> {
            final var key = ((String) entry.getKey()).split(Pattern.quote("."));
            // Only process pins
            if (key[0].contains("pin")) {
                final var value = ((String) entry.getValue()).split(",");
                final var pinKey = new PinKey(Integer.parseInt(key[1]), Integer.parseInt(key[2]));
                final var pin = new Pin(pinKey).setPortName(strToStr(value[0])).setName(strToStr(value[1])).setConfigName(
                        strToStr(value[2])).setConfigOffset(hexToInt(value[3])).setConfigMask(hexToInt(value[4])).setDataName(
                        strToStr(value[5])).setDataOffset(hexToInt(value[6])).setDataMask(hexToInt(value[7])).setPullName(strToStr(
                        value[8])).setPullUpMask(hexToInt(value[9])).setPullDownMask(hexToInt(value[10]));
                pinMap.put(pinKey, pin);
            }
        });
        return pinMap;
    }
}
