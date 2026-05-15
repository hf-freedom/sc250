package com.hospital.triage.enums;

public enum PatientPriority {
    EMERGENCY(0),
    NORMAL(1),
    LATE(2);

    private final int level;

    PatientPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
