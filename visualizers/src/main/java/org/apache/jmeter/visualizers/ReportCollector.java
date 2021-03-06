package org.apache.jmeter.visualizers;


import org.apache.jmeter.common.CliOption;
import org.apache.jmeter.common.utils.FileUtil;
import org.apache.jmeter.common.utils.TimeUtil;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.data.TestCaseData;
import org.apache.jmeter.visualizers.data.TestCaseStepData;
import org.apache.jmeter.visualizers.data.TestSuiteData;

import java.io.File;


/**
 * @author KelvinYe
 */
public class ReportCollector extends AbstractTestElement
        implements TestStateListener, ThreadListener, SampleListener, Interruptible, NoThreadClone {

    public static final String DATE_FORMAT_PATTERN = "yyyy.MM.dd HH:mm:ss";
    public static final String REPORT_NAME = "ReportCollector.reportName";
    public static final String IS_APPEND = "ReportCollector.isAppend";

    private int startCount;
    private String scriptName;
    private String reportName;

    public ReportCollector() {
        super();
    }

    public ReportCollector(String name) {
        this();
        setName(name);
    }

    @Override
    public void testStarted() {
        testStarted("localhost");
    }

    /**
     * 测试计划（TestPlan）开始时创建 testSuite
     */
    @Override
    public void testStarted(String host) {
        if (startCount == 0) {
            scriptName = getScriptName();
            reportName = getReportName();
            TestSuiteData testSuiteData = new TestSuiteData();
            testSuiteData.setTitle(scriptName);
            testSuiteData.setStartTime(getStringTime());
            ReportManager.getReport().putTestSuite(testSuiteData);
        }
        startCount++;
    }

    @Override
    public void testEnded() {
        testEnded("localhost");
    }

    /**
     * 测试计划（TestPlan）执行结束后把数据写入 HTML文件中
     */
    @Override
    public void testEnded(String host) {
        startCount--;
        if (startCount == 0) {
            TestSuiteData testSuiteData = ReportManager.getReport().getTestSuite(scriptName);
            testSuiteData.setEndTime(getStringTime());
            testSuiteData.setElapsedTime(getElapsedTime(testSuiteData.getStartTime(), testSuiteData.getEndTime()));

            // 如判断为追加模式且 html文件存在时，以追加模式写入数据，否则以新建模式写入数据
            if (Boolean.parseBoolean(getIsAppend()) && FileUtil.exists(getReportPath())) {
                ReportManager.appendDataToHtmlFile(getReportPath());
            } else {
                ReportManager.flush(getReportPath());
            }

            // 测试结束时重置测试数据集
            ReportManager.clearReportDataSet();
        }
    }

    /**
     * 线程(TestCase)开始时创建 testCase
     */
    @Override
    public void threadStarted() {
        TestSuiteData testSuiteData = ReportManager.getReport().getTestSuite(getScriptName());
        TestCaseData testCaseData = new TestCaseData(String.valueOf(testSuiteData.getTestCaseStartID()));
        testCaseData.setTitle(getThreadName());
        testCaseData.setStartTime(getStringTime());
        testSuiteData.putTestCase(testCaseData);
    }

    @Override
    public void threadFinished() {
        // pass
    }

    /**
     * 将测试样本（TestSample）的标题、请求和响应数据写入 HTML
     */
    @Override
    public void sampleOccurred(SampleEvent sampleEvent) {
        TestSuiteData testSuite = ReportManager.getReport().getTestSuite(getScriptName());
        TestCaseData testCase = testSuite.getTestCase(getThreadName());
        TestCaseStepData testCaseStep = new TestCaseStepData();

        // 设置测试数据
        SampleResult result = sampleEvent.getResult();
        testCaseStep.setTile(result.getSampleLabel());
        testCaseStep.setElapsedTime(getSampleElapsedTime(result));
        testCaseStep.setRequest(result.getSamplerData());
        testCaseStep.setResponse(result.getResponseDataAsString());

        // 设置测试结果
        if (result.isSuccessful()) {
            testCaseStep.pass();
        } else {
            testCaseStep.fail();
            testCase.fail();
            testSuite.fail();
        }

        // 每次sample执行完毕覆盖testCase的完成时间和耗时
        testCase.setEndTime(getStringTime());
        testCase.setElapsedTime(getElapsedTime(testCase.getStartTime(), testCase.getEndTime()));

        // 把测试步骤数据添加至测试案例集中
        testCase.putTestCaseStep(testCaseStep);

        // 另外把 sample 执行结果打印到控制台
        consoleInfo(result.isSuccessful(), getThreadName());
    }

    @Override
    public void sampleStarted(SampleEvent sampleEvent) {
        // pass
    }

    @Override
    public void sampleStopped(SampleEvent sampleEvent) {
        // pass
    }

    /**
     * 获取脚本名称（去文件后缀）
     */
    private String getScriptName() {
        String scriptName = FileServer.getFileServer().getScriptName();
        return scriptName.substring(0, scriptName.length() - 4).trim();
    }

    @Override
    public String getThreadName() {
        return JMeterContextService.getContext().getThread().getThreadName();
    }

    private String getReportName() {
        // Non-Gui下，命令行存在 -JreportName 选项时，优先读取 reportName
        return JMeterUtils.getPropDefault(CliOption.REPORT_NAME, getPropertyAsString(REPORT_NAME));
    }

    private String getIsAppend() {
        // Non-Gui下，命令行存在 -JisAppend 选项时，优先读取 isAppend
        return JMeterUtils.getPropDefault(CliOption.IS_APPEND, getPropertyAsString(IS_APPEND));
    }

    /**
     * 获取测试报告路径
     */
    private String getReportPath() {
        return JMeterUtils.getJMeterHome() + File.separator + "htmlreport" + File.separator + appendHTMLSuffix(reportName);
    }

    /**
     * 为测试报告名称添加.html后缀
     */
    private String appendHTMLSuffix(String name) {
        if (name.endsWith(ReportManager.HTML_SUFFIX)) {
            return name;
        } else {
            return name + ReportManager.HTML_SUFFIX;
        }
    }

    /**
     * 控制台打印执行状态信息
     */
    private void consoleInfo(boolean isSuccessful, String message) {
        if (isSuccessful) {
            System.out.println("[true] - " + message);
        } else {
            System.err.println("[false]- " + message);
        }
    }

    /**
     * 获取 sample的耗时
     *
     * @param result SampleResult对象
     * @return 耗时(xxms)
     */
    private String getSampleElapsedTime(SampleResult result) {
        return result.getEndTime() - result.getStartTime() + "ms";
    }

    @Override
    public boolean interrupt() {
        testEnded();
        return true;
    }

    private String getStringTime() {
        return TimeUtil.currentTimeAsString(DATE_FORMAT_PATTERN);
    }

    private String getElapsedTime(String startTime, String endTime) {
        return TimeUtil.formatElapsedTimeAsHMS(startTime, endTime, DATE_FORMAT_PATTERN);
    }

}
