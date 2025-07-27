package Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static Utils.FileUtils.ALLURE_REPORT_DIR;
import static Utils.FileUtils.ALLURE_RESULTS_DIR;

public final class ReportUtils {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private ReportUtils() {
    }

    public static void generateAllureReport() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String allureCmd = osName.contains("win") ? "allure-wrapper.bat" : "allure";
            ProcessBuilder builder = new ProcessBuilder(allureCmd, "generate", ALLURE_RESULTS_DIR, "-o", ALLURE_REPORT_DIR, "--clean");
            builder.directory(new File(System.getProperty("user.dir")));
            builder.inheritIO();
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ReportGenerationException("Allure report generation failed with code: " + exitCode, null);
            }
            System.out.println("Allure report generated at: " + Paths.get(ALLURE_REPORT_DIR).toAbsolutePath());
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate Allure report", e);
        }
    }

    public static String getEnhancedSummaryHtml() {
        try {
            Path summaryPath = Paths.get(ALLURE_REPORT_DIR, "widgets", "summary.json");
            return generateHtmlSummary(summaryPath.toFile());
        } catch (Exception e) {
            return createErrorHtml("Failed to generate report summary", e);
        }
    }

    private static String generateHtmlSummary(File summaryJson) throws IOException {
        JsonNode root = jsonMapper.readTree(summaryJson);
        JsonNode statsNode = root.path("statistic");
        JsonNode timingNode = root.path("time");
        TestStats testStats = extractTestStats(statsNode);
        TestTiming testTiming = extractTestTiming(timingNode);
        return buildSummaryHtml(testStats, testTiming);
    }

    private static TestStats extractTestStats(JsonNode statsNode) {
        return new TestStats(statsNode.path("total").asInt(), statsNode.path("passed").asInt(), statsNode.path("failed").asInt(), statsNode.path("skipped").asInt());
    }

    private static TestTiming extractTestTiming(JsonNode timeNode) {
        long duration = timeNode.path("duration").asLong();
        long start = timeNode.path("start").asLong();
        long stop = timeNode.path("stop").asLong();
        if (duration == 0 && start > 0 && stop > start) {
            duration = stop - start;
        }
        return new TestTiming(duration, start > 0 ? Instant.ofEpochMilli(start) : null, stop > 0 ? Instant.ofEpochMilli(stop) : null);
    }

    private static String buildSummaryHtml(TestStats stats, TestTiming timing) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        .summary { border-collapse: collapse; width: 100%%; max-width: 600px; }
                        .summary th, .summary td { padding: 10px; text-align: center; border: 1px solid #ddd; }
                        .passed { color: #4CAF50; }
                        .failed { color: #F44336; }
                        .skipped { color: #FF9800; }
                        .meta { margin-top: 20px; color: #666; }
                    </style>
                </head>
                <body>
                    <h2>Test Execution Summary</h2>
                    <table class="summary">
                        <tr>
                            <th>Total</th>
                            <th class="passed">Passed</th>
                            <th class="failed">Failed</th>
                            <th class="skipped">Skipped</th>
                            <th>Duration</th>
                        </tr>
                        <tr>
                            <td>%d</td>
                            <td class="passed">%d</td>
                            <td class="failed">%d</td>
                            <td class="skipped">%d</td>
                            <td>%s</td>
                        </tr>
                    </table>
                    <div class="meta">
                        <p><b>Start:</b> %s</p>
                        <p><b>End:</b> %s</p>
                        <p><b>Report Generated:</b> %s</p>
                    </div>
                </body>
                </html>
                """,
                stats.total, stats.passed, stats.failed, stats.skipped, formatDuration(timing.duration),
                timing.start != null ? formatter.format(timing.start) : "N/A",
                timing.stop != null ? formatter.format(timing.stop) : "N/A",
                LocalDateTime.now().format(formatter));
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        return String.format("%d min %d sec", duration.toMinutes(), duration.getSeconds() % 60);
    }

    private static String createErrorHtml(String message, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return String.format("""
                <html>
                <body>
                    <h2 style="color:red">%s</h2>
                    <pre>%s</pre>
                </body>
                </html>
                """, message, sw.toString());
    }

    private static class TestStats {
        final int total;
        final int passed;
        final int failed;
        final int skipped;

        TestStats(int total, int passed, int failed, int skipped) {
            this.total = total;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
        }
    }

    private static class TestTiming {
        final long duration;
        final Instant start;
        final Instant stop;

        TestTiming(long duration, Instant start, Instant stop) {
            this.duration = duration;
            this.start = start;
            this.stop = stop;
        }
    }

    private static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}