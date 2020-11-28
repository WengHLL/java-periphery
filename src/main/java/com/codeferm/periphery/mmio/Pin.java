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
     * Pin key.
     */
    private PinKey key;
    /**
     * Pin group name (port, package, etc.)
     */
    private String groupName;
    /**
     * Pin name.
     */
    private String name;
    /**
     * Pin data input register.
     */
    private Register dataIn;
    /**
     * Pin data output register.
     */
    private Register dataOut;
    /**
     * MMIO handle.
     */
    private long mmioHadle;

    /**
     * Default constructor.
     */
    public Pin() {
    }

    /**
     * Pin key only constructor.
     *
     * @param key Pin key.
     */
    public Pin(final PinKey key) {
        this.key = key;
    }

    /**
     * Pin key and pin name constructor.
     *
     * @param key Pin key.
     * @param name Pin name.
     */
    public Pin(final PinKey key, final String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * All fields constructor.
     *
     * @param key Pin key.
     * @param groupName Group name.
     * @param name Pin name.
     * @param dataIn Pin data input register.
     * @param dataOut Pin data output register.
     */
    public Pin(PinKey key, String groupName, String name, Register dataIn, Register dataOut) {
        this.key = key;
        this.groupName = groupName;
        this.name = name;
        this.dataIn = dataIn;
        this.dataOut = dataOut;
    }

    public PinKey getKey() {
        return key;
    }

    public Pin setKey(final PinKey key) {
        this.key = key;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public Pin setGroupName(final String groupName) {
        this.groupName = groupName;
        return this;
    }

    public String getName() {
        return name;
    }

    public Pin setName(final String name) {
        this.name = name;
        return this;
    }

    public Register getDataIn() {
        return dataIn;
    }

    public Pin setDataIn(final Register dataIn) {
        this.dataIn = dataIn;
        return this;
    }

    public Register getDataOut() {
        return dataOut;
    }

    public Pin setDataOut(final Register dataOut) {
        this.dataOut = dataOut;
        return this;
    }

    public long getMmioHadle() {
        return mmioHadle;
    }

    public Pin setMmioHadle(final long mmioHadle) {
        this.mmioHadle = mmioHadle;
        return this;
    }

    /**
     * Object hash code.
     *
     * @return Hash code.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.key);
        hash = 89 * hash + Objects.hashCode(this.groupName);
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.dataIn);
        hash = 89 * hash + Objects.hashCode(this.dataOut);
        hash = 89 * hash + (int) (this.mmioHadle ^ (this.mmioHadle >>> 32));
        return hash;
    }

    /**
     * Object equals.
     *
     * @param obj Object to compare to.
     * @return True if equal.
     */
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
        if (this.mmioHadle != other.mmioHadle) {
            return false;
        }
        if (!Objects.equals(this.groupName, other.groupName)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        if (!Objects.equals(this.dataIn, other.dataIn)) {
            return false;
        }
        if (!Objects.equals(this.dataOut, other.dataOut)) {
            return false;
        }
        return true;
    }

    /**
     * String representation of Object.
     *
     * @return String of Object fields.
     */
    @Override
    public String toString() {
        return "Pin{" + "key=" + key + ", groupName=" + groupName + ", name=" + name + ", dataIn=" + dataIn + ", dataOut=" + dataOut
                + ", mmioHadle=" + mmioHadle + '}';
    }
}
