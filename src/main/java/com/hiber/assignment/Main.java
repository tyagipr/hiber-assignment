package com.hiber.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiber.assignment.config.DynamicConfig;
import com.hiber.assignment.parser.PressureMessageParser;
import com.typesafe.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Command-line runner for the pressure parser.
 * Streams and parses HEX messages from stdin continuously until EOF (Ctrl+D) or interrupt (Ctrl+C).
 * Uses DynamicConfig for pressure multiplier and output settings (reloads automatically on config change).
 */
public class Main {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        DynamicConfig dynamicConfig = DynamicConfig.create();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dynamicConfig.close();
            } catch (Exception e) {
                // ignore
            }
        }));

        System.err.println("Pressure Parser - streaming mode. Enter HEX messages (one per line). Ctrl+D to exit.");
        System.err.println("Example: 00E95365000048410000C84155");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().replaceAll("\\s", "");

                if (line.isEmpty()) {
                    continue;
                }

                try {
                    var data = PressureMessageParser.parse(line);
                    Config config = dynamicConfig.getConfig();

                    double multiplier = config.getDouble("pressure.multiplier");
                    double pressureData = data.pressureData() * multiplier;

                    boolean showTimestamp = config.getBoolean("output.showTimestamp");
                    boolean showPressure = config.getBoolean("output.showPressure");
                    boolean showTemperature = config.getBoolean("output.showTemperature");
                    boolean showBattery = config.getBoolean("output.showBattery");

                    Map<String, Object> json = new LinkedHashMap<>();
                    if (showTimestamp) json.put("timestamp", data.timestamp());
                    if (showPressure) json.put("pressure_bar", pressureData);
                    if (showTemperature) json.put("temperature_celsius", data.temperatureCelsius());
                    if (showBattery) json.put("battery_percent", data.batteryPercent());

                    System.out.println(JSON.writeValueAsString(json));

                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid HEX input: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Parse error: " + e.getMessage());
                }
            }
        }
    }
}
