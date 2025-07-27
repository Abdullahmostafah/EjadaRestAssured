package Utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileUtils {
    public static final String LOG_DIR = "target/logs";
    public static final String ALLURE_RESULTS_DIR = "target/allure-results";
    public static final String ALLURE_REPORT_DIR = "target/allure-report";
    public static final String EXTENT_REPORT_DIR = "target/extent-reports";
    public static final String ARCHIVE_DIR = "target/archive";

    private FileUtils() {
    }

    public static void createDirectoryIfNotExists(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("Path exists but is not a directory: " + path.toAbsolutePath());
        }
    }

    public static void cleanDirectory(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to delete: " + p, e);
                    }
                });

        // Recreate the directory
        Files.createDirectories(path);
    }

    public static void archivePreviousReports() throws IOException {
        createDirectoryIfNotExists(ARCHIVE_DIR);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path archivePath = Paths.get(ARCHIVE_DIR, timestamp);
        Files.createDirectories(archivePath);

        archiveDirectory(ALLURE_RESULTS_DIR, archivePath.resolve("allure-results.zip"));
        archiveDirectory(ALLURE_REPORT_DIR, archivePath.resolve("allure-report.zip"));
        archiveDirectory(EXTENT_REPORT_DIR, archivePath.resolve("extent-reports.zip"));
        archiveDirectory(LOG_DIR, archivePath.resolve("logs.zip"));
    }

    private static void archiveDirectory(String sourceDir, Path zipPath) throws IOException {
        if (!Files.exists(Paths.get(sourceDir))) {
            return;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walk(Paths.get(sourceDir))
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String relativePath = Paths.get(sourceDir).relativize(path).toString();
                            zos.putNextEntry(new ZipEntry(relativePath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to archive file: " + path, e);
                        }
                    });
        }
    }

    public static String getFormattedTimestamp() {
        return DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss")
                .format(LocalDateTime.now());
    }

    public static boolean isDirectoryWritable(String path) {
        try {
            createDirectoryIfNotExists(path);
            Path testFile = Paths.get(path, "write-test-" + System.currentTimeMillis() + ".tmp");
            Files.createFile(testFile);
            Files.delete(testFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}