/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import java.util.Objects;

/**
 * GPIO pin based on using MMIO.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class Pin {

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

    private PinKey key;
    private String name;
    private String portName;
    private String configName;
    private int configOffset;
    private int configMask;
    private String dataName;
    private int dataOffset;
    private int dataMask;
    private String pullName;
    private int pullOffset;
    private int pullUpMask;
    private int pullDownMask;
    private long mmioHadle;

    public Pin() {
    }

    public Pin(PinKey key) {
        this.key = key;
    }

    public Pin(PinKey key, String name) {
        this.key = key;
        this.name = name;
    }

    public PinKey getKey() {
        return key;
    }

    public Pin setKey(PinKey key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public Pin setName(String name) {
        this.name = name;
        return this;
    }

    public String getPortName() {
        return portName;
    }

    public Pin setPortName(String portName) {
        this.portName = portName;
        return this;
    }

    public String getConfigName() {
        return configName;
    }

    public Pin setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public int getConfigOffset() {
        return configOffset;
    }

    public Pin setConfigOffset(int configOffset) {
        this.configOffset = configOffset;
        return this;
    }

    public int getConfigMask() {
        return configMask;
    }

    public Pin setConfigMask(int configMask) {
        this.configMask = configMask;
        return this;
    }

    public String getDataName() {
        return dataName;
    }

    public Pin setDataName(String dataName) {
        this.dataName = dataName;
        return this;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public Pin setDataOffset(int dataOffset) {
        this.dataOffset = dataOffset;
        return this;
    }

    public int getDataMask() {
        return dataMask;
    }

    public Pin setDataMask(int dataMask) {
        this.dataMask = dataMask;
        return this;
    }

    public String getPullName() {
        return pullName;
    }

    public Pin setPullName(String pullName) {
        this.pullName = pullName;
        return this;
    }

    public int getPullOffset() {
        return pullOffset;
    }

    public Pin setPullOffset(int pullOffset) {
        this.pullOffset = pullOffset;
        return this;
    }

    public int getPullUpMask() {
        return pullUpMask;
    }

    public Pin setPullUpMask(int pullUpMask) {
        this.pullUpMask = pullUpMask;
        return this;
    }

    public int getPullDownMask() {
        return pullDownMask;
    }

    public Pin setPullDownMask(int pullDownMask) {
        this.pullDownMask = pullDownMask;
        return this;
    }

    public long getMmioHadle() {
        return mmioHadle;
    }

    public Pin setMmioHadle(long mmioHadle) {
        this.mmioHadle = mmioHadle;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.key);
        hash = 41 * hash + Objects.hashCode(this.name);
        hash = 41 * hash + Objects.hashCode(this.portName);
        hash = 41 * hash + Objects.hashCode(this.configName);
        hash = 41 * hash + this.configOffset;
        hash = 41 * hash + this.configMask;
        hash = 41 * hash + Objects.hashCode(this.dataName);
        hash = 41 * hash + this.dataOffset;
        hash = 41 * hash + this.dataMask;
        hash = 41 * hash + Objects.hashCode(this.pullName);
        hash = 41 * hash + this.pullOffset;
        hash = 41 * hash + this.pullUpMask;
        hash = 41 * hash + this.pullDownMask;
        hash = 41 * hash + (int) (this.mmioHadle ^ (this.mmioHadle >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pin other = (Pin) obj;
        if (this.configOffset != other.configOffset) {
            return false;
        }
        if (this.configMask != other.configMask) {
            return false;
        }
        if (this.dataOffset != other.dataOffset) {
            return false;
        }
        if (this.dataMask != other.dataMask) {
            return false;
        }
        if (this.pullOffset != other.pullOffset) {
            return false;
        }
        if (this.pullUpMask != other.pullUpMask) {
            return false;
        }
        if (this.pullDownMask != other.pullDownMask) {
            return false;
        }
        if (this.mmioHadle != other.mmioHadle) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.portName, other.portName)) {
            return false;
        }
        if (!Objects.equals(this.configName, other.configName)) {
            return false;
        }
        if (!Objects.equals(this.dataName, other.dataName)) {
            return false;
        }
        if (!Objects.equals(this.pullName, other.pullName)) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Pin{" + "key=" + key + ", name=" + name + ", portName=" + portName + ", configName=" + configName
                + ", configOffset=" + configOffset + ", configMask=" + configMask + ", dataName=" + dataName + ", dataOffset="
                + dataOffset + ", dataMask=" + dataMask + ", pullName=" + pullName + ", pullOffset=" + pullOffset + ", pullUpMask="
                + pullUpMask + ", pullDownMask=" + pullDownMask + ", mmioHadle=" + mmioHadle + '}';
    }
}
