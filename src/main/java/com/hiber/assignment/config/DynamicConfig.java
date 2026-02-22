package com.hiber.assignment.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads configuration from a file, watches for changes using a ConfigFileWatcher,
 * and automatically reloads when the file is modified. Always returns the latest
 * config via {@link #getConfig()}.
 */
public class DynamicConfig implements java.io.Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfig.class);

    private final File configFile;
    private final ConfigFileWatcher fileWatcher;
    private final AtomicReference<Config> cachedConfig = new AtomicReference<>();
    private volatile long lastModified;

    public DynamicConfig(File configFile, ConfigFileWatcher fileWatcher) {
        this.configFile = Objects.requireNonNull(configFile, "Config file must not be null");
        this.fileWatcher = Objects.requireNonNull(fileWatcher, "FileWatcher must not be null");
        this.cachedConfig.set(loadConfigOrDefault());
        this.lastModified = configFile.lastModified();
        this.fileWatcher.start(this::onFileChanged);
    }

    /**
     * Factory method for convenience. Creates DynamicConfig with NioConfigFileWatcher.
     *
     * @param dir      directory containing the config file
     * @param fileName config file name (e.g. "config.yaml")
     * @return DynamicConfig instance
     * @throws IllegalArgumentException if the config file does not exist
     */
    public static DynamicConfig fromPath(String dir, String fileName) {
        File configFile = new File(dir, fileName);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("Config file does not exist: " + configFile.getAbsolutePath());
        }
        return new DynamicConfig(configFile, new NioConfigFileWatcher(configFile));
    }

    /**
     * Creates DynamicConfig by searching common locations for config.yaml.
     *
     * @return DynamicConfig instance
     * @throws IllegalStateException if config file is not found
     */
    public static DynamicConfig create() {
        for (String[] pair : new String[][]{{".", "config.yaml"}, {"config", "config.yaml"}, {"target/classes", "config.yaml"}, {"src/main/resources", "config.yaml"}}) {
            File f = new File(pair[0], pair[1]);
            if (f.exists()) {
                return fromPath(pair[0], pair[1]);
            }
        }
        String override = System.getProperty("config.file");
        if (override != null && !override.isBlank()) {
            File f = new File(override);
            if (f.exists()) {
                return fromPath(f.getParent(), f.getName());
            }
        }
        throw new IllegalStateException("config.yaml not found. Place it in ., config/, target/classes/, or src/main/resources/");
    }

    public Config getConfig() {
        long currentLastModified = configFile.lastModified();
        if (currentLastModified > lastModified) {
            logger.info("Config file changed, reloading...");
            reloadConfig();
        }
        return cachedConfig.get();
    }

    private void reloadConfig() {
        Config newConfig = loadConfigOrDefault();
        cachedConfig.set(newConfig);
        lastModified = configFile.lastModified();
    }

    private Config loadConfigOrDefault() {
        try {
            Map<String, Object> yamlMap;
            try (InputStream in = new FileInputStream(configFile)) {
                yamlMap = new Yaml().load(in);
            }
            Config config = ConfigFactory.parseMap(yamlMap != null ? yamlMap : Map.of()).resolve();
            logger.info("Loaded config from {}", configFile.getAbsolutePath());
            return config;
        } catch (Exception e) {
            logger.error("Failed to load config, using last known good config", e);
            Config fallback = cachedConfig.get();
            return fallback != null ? fallback : ConfigFactory.empty();
        }
    }

    private void onFileChanged() {
        lastModified = 0; // Force reload on next access
    }

    @Override
    public void close() throws IOException {
        fileWatcher.close();
        logger.info("DynamicConfig closed.");
    }
}
