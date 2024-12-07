package com.example.avr_ballbalancerapp;

public enum enumPID {
    PID_Kp("Proportional Gain - PID Controller"),
    PID_Ki("Integral Gain - PID Controller"),
    PID_Kd("Derivative Gain - PID Controller");

    private final String description; // Field to store the value


    // Constructor to initialize the value
    enumPID(String description) {
        this.description = description;
    }

    // Getter method to access the value
    public String getDescription() {
        return description;
    }
}
