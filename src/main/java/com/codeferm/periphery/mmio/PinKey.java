/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.mmio;

import java.util.Comparator;

/**
 * GPIO pin key used for easy lookup and sorting.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
public class PinKey implements Comparable<PinKey> {

    private int chip;
    private int pin;

    public PinKey() {
    }

    public PinKey(int chip, int pin) {
        this.chip = chip;
        this.pin = pin;
    }

    public int getChip() {
        return chip;
    }

    public PinKey setChip(int chip) {
        this.chip = chip;
        return this;
    }

    public int getPin() {
        return pin;
    }

    public PinKey setPin(int pin) {
        this.pin = pin;
        return this;
    }

    @Override
    public int compareTo(final PinKey key) {
        return Comparator.comparing(PinKey::getChip).thenComparing(PinKey::getPin).compare(this, key);

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + this.chip;
        hash = 41 * hash + this.pin;
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
        final PinKey other = (PinKey) obj;
        if (this.chip != other.chip) {
            return false;
        }
        if (this.pin != other.pin) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PinKey{" + "chip=" + chip + ", pin=" + pin + '}';
    }
}
