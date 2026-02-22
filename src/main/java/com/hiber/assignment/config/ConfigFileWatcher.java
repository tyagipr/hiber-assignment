package com.hiber.assignment.config;

import java.io.Closeable;

/**
 * Watches a config file for changes and invokes a callback when the file is modified.
 */
public interface ConfigFileWatcher extends Closeable {

    /**
     * Starts watching the config file. When a change is detected, the given callback is invoked.
     *
     * @param onFileChanged callback to run when the file changes (e.g. to trigger reload)
     */
    void start(Runnable onFileChanged);
}
