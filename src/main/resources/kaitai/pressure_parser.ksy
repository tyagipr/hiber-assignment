meta:
  id: pressure_parser
  application: "Pressure Parser for Assignment"
  endian: le
seq:
  - id: timestamp
    type: u4
    doc: Timestamp of the data
  - id: pressure_data
    type: f4
    doc: Pressure measurement in bar
  - id: temperature_celsius
    type: f4
    doc: Temperature in degrees Celsius
  - id: battery
    type: u1
    doc: Sensor battery level percentage
