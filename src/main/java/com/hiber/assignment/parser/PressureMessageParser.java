package com.hiber.assignment.parser;

import com.hiber.assignment.kaitai.PressureParser;
import com.hiber.assignment.model.PressureData;
import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;

import java.util.HexFormat;

/**
 * Parses sensor payloads from HEX strings into structured {@link PressureData}.
 * Uses Kaitai Struct for binary format parsing.
 */
public final class PressureMessageParser {

    private PressureMessageParser() {}

    /**
     * Parses a HEX string representation of a pressure sensor payload.
     *
     * @param hexInput HEX string (e.g. "6502A0410000A04164")
     * @return parsed pressure data
     * @throws IllegalArgumentException if the HEX string is invalid
     */
    public static PressureData parse(String hexInput) {
        byte[] bytes = hexStringToBytes(hexInput);
        return parse(bytes);
    }

    /**
     * Parses a byte array containing a pressure sensor payload.
     *
     * @param bytes raw payload bytes
     * @return parsed pressure data
     */
    public static PressureData parse(byte[] bytes) {
        KaitaiStream stream = new ByteBufferKaitaiStream(bytes);
        PressureParser parser = new PressureParser(stream);

        return new PressureData(
            parser.timestamp(),
            parser.pressureData(),
            parser.temperatureCelsius(),
            parser.battery()
        );
    }

    /**
     * Converts a HEX string to a byte array.
     *
     * @param hex HEX string (spaces are ignored)
     * @return decoded bytes
     * @throws IllegalArgumentException if the string has odd length or invalid characters
     */
    public static byte[] hexStringToBytes(String hex) {
        String normalized = hex.replaceAll("\\s", "").toLowerCase();
        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException("HEX string must have even length");
        }
        return HexFormat.of().parseHex(normalized);
    }
}
