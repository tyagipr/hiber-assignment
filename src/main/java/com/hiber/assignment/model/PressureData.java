package com.hiber.assignment.model;

/**
 * Immutable container for parsed pressure sensor data.
 */
public record PressureData(
    long timestamp,
    float pressureData,
    float temperatureCelsius,
    int batteryPercent
) {}
