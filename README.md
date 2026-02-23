# Pressure Parser

A command-line application that parses sensor payloads represented as HEX strings into structured, human-readable output using Kaitai Struct. Supports streaming input and dynamic configuration reload.

## Prerequisites

- **Java 17** or later
- **Maven 3.6** or later

## Execution Instructions

### Build

```bash
mvn clean install
```

This compiles, creates the runnable JAR, and installs to the local Maven repository.

### Run

**Option 1: Using Maven Exec Plugin (recommended)**

```bash
mvn exec:java
```

**Option 2: From IDE**

Run the `com.hiber.assignment.Main` class.

### Usage

Once running, the application streams HEX input from stdin. Enter one HEX message per line.

- **Example input:** `00E95365000048410000C84155`
- **Exit:** `Ctrl+D` (EOF) or `Ctrl+C` (interrupt)

---

## High-level design

```
  stdin (HEX lines)     config.yaml (external)
         │                        │
         ▼                        ▼
  ┌──────────────┐         ┌──────────────┐
  │    Main      │◄────────│ DynamicConfig│
  │  (orchestr.) │         │ + file watch │
  └──────┬───────┘         └──────────────┘
         │
         ▼
  ┌──────────────┐         ┌──────────────┐
  │ PressureMsg  │────────►│ PressureData │
  │   Parser     │         │   (model)    │
  └──────┬───────┘         └──────┬───────┘
         │                        │
         │    Kaitai (generated)   │
         └────────┬───────────────┘
                  ▼
           JSON to stdout
```

- **Main:** Reads stdin line-by-line, delegates parsing to `PressureMessageParser`, applies config (multiplier, output flags), and writes one JSON object per line to stdout. Registers a shutdown hook to close the config watcher.
- **PressureMessageParser:** Decodes HEX to bytes and uses the Kaitai-generated `PressureParser` to interpret the fixed 13-byte layout; returns a `PressureData` record (timestamp, pressure, temperature, battery).
- **DynamicConfig:** Loads a single external `config.yaml` from a fixed search order, caches it as Typesafe `Config`, and uses a file watcher to detect changes. Reload is lazy (on next `getConfig()` after a change).
- **Model:** `PressureData` is a simple record; no business logic. Output shape is built in Main from config flags and written with Jackson.

---

## Technical Choices

### Assumptions

- **Payload format:** Fixed 13-byte binary layout : 4-byte timestamp (u4), 4-byte pressure (float), 4-byte temperature (float), 1-byte battery (u1). Defined in `pressure_parser.ksy`.
- **HEX input:** One message per line; whitespace is stripped. Input is case-insensitive.
- **Config:** YAML format in `config.yaml`.
- **File watching:** Config is always loaded from an external file, so changes are watched and reloaded automatically.

### Trade-offs

- **Lazy reload on access:** Config is reloaded when `getConfig()` is called and the file’s `lastModified` has changed, instead of reacting immediately in the watcher thread. This simplifies threading and avoids locks, at the cost of a short delay until the next read.
- **No-op watcher fallback:** If the watcher cannot start (e.g. missing parent dir), config still loads; file watching is optional.
- **Streaming vs. batch:** The app processes line-by-line from stdin. Large inputs are streamed instead of loading everything into memory.

### Edge Cases Considered

- **Invalid HEX:** Odd length or non-hex characters throw `IllegalArgumentException`; the process continues with the next line.
- **Invalid payload length:** Shorter or longer than 13 bytes may cause parse errors from Kaitai; these are caught and reported per line.
- **Config reload failure:** If the config file becomes invalid (e.g. bad YAML), the last valid config is reused instead of failing the application.
- **Empty lines:** Ignored. Empty or whitespace-only input is skipped.
- **Config file not found:** Application fails at startup with a clear message; config must be placed in one of the search paths.

---

## Configuration

A single external `config.yaml` is used and searched for (in order) in: `./config.yaml`. The file is watched for changes and reloaded automatically (no restart needed).

### Config options

| Key | Description | Default |
|-----|-------------|---------|
| `pressure.multiplier` | Multiplier applied to raw pressure value | `1.0` |
| `output.showTimestamp` | Include timestamp in output | `true` |
| `output.showPressure` | Include pressure in output | `true` |
| `output.showTemperature` | Include temperature in output | `true` |
| `output.showBattery` | Include battery % in output | `true` |

## Project structure

```
src/main/java/com/hiber/assignment/
├── Main.java                 # Application entry point
├── model/
│   └── PressureData.java     # Parsed data model
├── parser/
│   └── PressureMessageParser.java
├── config/
│   ├── DynamicConfig.java    # Config loading & reload
│   ├── ConfigFileWatcher.java
│   └── NioConfigFileWatcher.java
└── kaitai/                   # Generated (Kaitai Struct)
    └── PressureParser.java

src/main/resources/
└── kaitai/
    └── pressure_parser.ksy   # Kaitai Struct definition
```
