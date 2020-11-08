/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import java.util.Objects;

/**
 * GPIO pin based on using MMIO.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class Pin {

    private int chip;
    private int pin;
    private String name;
    private int configOffest;
    private int configMask;
    private int dataOffset;
    private int dataMask;

    public Pin(int chip, int pin, String name, int configOffest, int configMask, int dataOffset, int dataMask) {
        this.chip = chip;
        this.pin = pin;
        this.name = name;
        this.configOffest = configOffest;
        this.configMask = configMask;
        this.dataOffset = dataOffset;
        this.dataMask = dataMask;
    }

    public int getChip() {
        return chip;
    }

    public Pin setChip(int chip) {
        this.chip = chip;
        return this;
    }

    public int getPin() {
        return pin;
    }

    public Pin setPin(int pin) {
        this.pin = pin;
        return this;
    }

    public String getName() {
        return name;
    }

    public Pin setName(String name) {
        this.name = name;
        return this;
    }

    public int getConfigOffest() {
        return configOffest;
    }

    public Pin setConfigOffest(int configOffest) {
        this.configOffest = configOffest;
        return this;
    }

    public int getConfigMask() {
        return configMask;
    }

    public Pin setConfigMask(int configMask) {
        this.configMask = configMask;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.chip;
        hash = 97 * hash + this.pin;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + this.configOffest;
        hash = 97 * hash + this.configMask;
        hash = 97 * hash + this.dataMask;
        hash = 97 * hash + this.dataOffset;
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
        if (this.chip != other.chip) {
            return false;
        }
        if (this.pin != other.pin) {
            return false;
        }
        if (this.configOffest != other.configOffest) {
            return false;
        }
        if (this.configMask != other.configMask) {
            return false;
        }
        if (this.dataMask != other.dataMask) {
            return false;
        }
        if (this.dataOffset != other.dataOffset) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Pin{" + "chip=" + chip + ", pin=" + pin + ", name=" + name + ", configOffest=" + configOffest + ", configMask="
                + configMask + ", dataMask=" + dataMask + ", dataOffset=" + dataOffset + '}';
    }
}
