package Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public final class ConfigReaderWriter {

    private static final String CONFIG_PATH = "src/test/resources/config.properties";
    private static final Properties properties = new Properties();
    private static final ReentrantLock lock = new ReentrantLock();

    static {
        loadProperties();
    }

    private ConfigReaderWriter() {
    }

    private static void loadProperties() {
        lock.lock();
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load config.properties", e);
        } finally {
            lock.unlock();
        }
    }

    public static String getPropKey(String key) {
        lock.lock();
        try {
            return properties.getProperty(key);
        } finally {
            lock.unlock();
        }
    }

    public static void setPropKey(String key, String value) {
        lock.lock();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
            properties.setProperty(key, value);
            properties.store(fos, "Updated configuration");
        } catch (IOException e) {
            throw new ConfigurationException("Failed to update config.properties", e);
        } finally {
            lock.unlock();
        }
    }

    private static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}