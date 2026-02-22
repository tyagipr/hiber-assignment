// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

package com.hiber.assignment.kaitai;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;

public class PressureParser extends KaitaiStruct {
    public static PressureParser fromFile(String fileName) throws IOException {
        return new PressureParser(new ByteBufferKaitaiStream(fileName));
    }

    public PressureParser(KaitaiStream _io) {
        this(_io, null, null);
    }

    public PressureParser(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public PressureParser(KaitaiStream _io, KaitaiStruct _parent, PressureParser _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }
    private void _read() {
        this.timestamp = this._io.readU4le();
        this.pressureData = this._io.readF4le();
        this.temperatureCelsius = this._io.readF4le();
        this.battery = this._io.readU1();
    }
    private long timestamp;
    private float pressureData;
    private float temperatureCelsius;
    private int battery;
    private PressureParser _root;
    private KaitaiStruct _parent;

    /**
     * Timestamp of the data
     */
    public long timestamp() { return timestamp; }

    /**
     * Pressure measurement in bar
     */
    public float pressureData() { return pressureData; }

    /**
     * Temperature in degrees Celsius
     */
    public float temperatureCelsius() { return temperatureCelsius; }

    /**
     * Sensor battery level percentage
     */
    public int battery() { return battery; }
    public PressureParser _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
