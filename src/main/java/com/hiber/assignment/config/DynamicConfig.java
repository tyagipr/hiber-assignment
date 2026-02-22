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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads configuration using a hybrid approach:
 * 1. Tries external file paths first (enables file watching and hot reload)
 * 2. Falls back to classpath (config.yaml inside JAR) when no external file found
 *
 * File watching and hot reload only work when using an external config file.
 */
public class DynamicConfig implements java.io.Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfig.class);
    private static final String CONFIG_RESOURCE = "config.yaml";

    private final File configFile;
    private final ConfigFileWatcher fileWatcher;
    private final AtomicReference<Config> cachedConfig = new AtomicReference<>();
    private volatile long lastModified;

    public DynamicConfig(File configFile, ConfigFileWatcher fileWatcher) {
        this.configFile = configFile;
        this.fileWatcher = fileWatcher;

        if (configFile != null && fileWatcher != null) {
            this.cachedConfig.set(loadFromFile());
            this.lastModified = configFile.lastModified();
            this.fileWatcher.start(this::onFileChanged);
        } else {
            this.cachedConfig.set(loadFromClasspath());
        }
    }

    /**
     * Factory method. Creates DynamicConfig with NioConfigFileWatcher.
     */
    public static DynamicConfig fromPath(String dir, String fileName) {
        File configFile = new File(dir, fileName);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("Config file does not exist: " + configFile.getAbsolutePath());
        }
        return new DynamicConfig(configFile, new NioConfigFileWatcher(configFile));
    }

    /**
     * Creates DynamicConfig using hybrid loading:
     * 1. Searches external paths: ., config/, target/classes/, src/main/resources/
     * 2. Falls back to classpath (config.yaml inside JAR)
     */
    public static DynamicConfig create() {
        for (String[] pair : new String[][]{{".", "config.yaml"}, {"config", "config.yaml"}, {"target/classes", "config.yaml"}, {"src/main/resources", "config.yaml"}}) {
            File f = new File(pair[0], pair[1]);
            if (f.exists()) {
                logger.info("Using external config from {}", f.getAbsolutePath());
                return fromPath(pair[0], pair[1]);
            }
        }
        logger.info("No external config found, loading from classpath (bundled in JAR)");
        return new DynamicConfig(null, null);
    }

    public Config getConfig() {
        if (configFile != null) {
            long currentLastModified = configFile.lastModified();
            if (currentLastModified > lastModified) {
                logger.info("Config file changed, reloading...");
                reloadConfig();
            }
        }
        return cachedConfig.get();
    }

    private void reloadConfig() {
        Config newConfig = loadFromFile();
        cachedConfig.set(newConfig);
        lastModified = configFile.lastModified();
    }

    private Config loadFromFile() {
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

    private Config loadFromClasspath() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (in == null) {
                logger.warn("config.yaml not found on classpath, using empty config");
                return ConfigFactory.empty();
            }
            Map<String, Object> yamlMap = new Yaml().load(in);
            Config config = ConfigFactory.parseMap(yamlMap != null ? yamlMap : Map.of()).resolve();
            logger.info("Loaded config from classpath ({})", CONFIG_RESOURCE);
            return config;
        } catch (Exception e) {
            logger.error("Failed to load config from classpath", e);
            return ConfigFactory.empty();
        }
    }

    private void onFileChanged() {
        lastModified = 0;
    }

    @Override
    public void close() throws IOException {
        if (fileWatcher != null) {
            fileWatcher.close();
            logger.info("DynamicConfig closed.");
        }
    }
}
