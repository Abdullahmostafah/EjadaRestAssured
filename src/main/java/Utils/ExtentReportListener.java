package Utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Paths;

public class ExtentReportListener implements ITestListener {
    private ExtentReports extent;
    public ExtentTest test;
    public static String currentDir = System.getProperty("user.dir");
    String reportName = "ExtentReport.html";

    @Override
    public void onStart(ITestContext context) {
        try {
            // Use the constant from FileUtils
            FileUtils.cleanDirectory(FileUtils.EXTENT_REPORT_DIR);

            String reportPath = Paths.get(currentDir, FileUtils.EXTENT_REPORT_DIR, reportName).toString();
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);

            sparkReporter.config().setDocumentTitle("Test Automation Report");
            sparkReporter.config().setReportName("Test Execution Report");

            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);

            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Extent Reports", e);
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        test = extent.createTest(result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        test.pass("Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        test.fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        test.skip(result.getThrowable());
    }

    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }
}