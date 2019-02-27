package org.apache.jmeter.visualizers.gui;


import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.ReportCollector;

import javax.swing.*;
import java.awt.*;

public class LocalHtmlReportGui extends AbstractListenerGui {
    private JTextField reportNameTextField;
    private JComboBox<String> isAppendComboBox;

    public LocalHtmlReportGui() {
        super();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());
        Box box = Box.createVerticalBox();
        box.add(makeTitlePanel());
        box.add(createReportNamePanel());
        box.add(createIsAppendPanel());
        box.add(createNote());
        add(box, BorderLayout.NORTH);
    }

    @Override
    public String getStaticLabel() {
        return "Local HTML Report";
    }

    @Override
    public String getLabelResource() {
        return null;
    }

    @Override
    public TestElement createTestElement() {
        ReportCollector info = new ReportCollector();
        modifyTestElement(info);
        return info;
    }

    @Override
    public void configure(TestElement el) {
        super.configure(el);
        reportNameTextField.setText(el.getPropertyAsString(ReportCollector.REPORT_NAME));
        isAppendComboBox.setSelectedItem(el.getPropertyAsString(ReportCollector.IS_APPEND));
    }

    @Override
    public void modifyTestElement(TestElement el) {
        super.configureTestElement(el);
        el.setProperty(ReportCollector.REPORT_NAME, reportNameTextField.getText());
        el.setProperty(ReportCollector.IS_APPEND, (String) isAppendComboBox.getSelectedItem());
    }

    @Override
    public void clearGui() {
        super.clearGui();
        reportNameTextField.setText("");
        isAppendComboBox.setSelectedItem("");
    }

    private JPanel createReportNamePanel() {
        reportNameTextField = new JTextField(10);
        reportNameTextField.setName(ReportCollector.REPORT_NAME);
        JLabel label = new JLabel(ReportCollector.REPORT_NAME);
        label.setLabelFor(reportNameTextField);
        JPanel jPanel = new JPanel(new BorderLayout(5, 0));
        jPanel.add(label, BorderLayout.WEST);
        jPanel.add(reportNameTextField, BorderLayout.CENTER);
        return jPanel;
    }

    private JPanel createIsAppendPanel() {
        isAppendComboBox = new JComboBox<>();
        isAppendComboBox.setName(ReportCollector.IS_APPEND);
        isAppendComboBox.addItem("true");
        isAppendComboBox.addItem("false");
        JLabel label = new JLabel(ReportCollector.IS_APPEND);
        label.setLabelFor(isAppendComboBox);
        JPanel jPanel = new JPanel(new BorderLayout(5, 0));
        jPanel.add(label, BorderLayout.WEST);
        jPanel.add(isAppendComboBox, BorderLayout.CENTER);
        return jPanel;
    }

    private JTextArea createNote() {
        String note = "\n说明：\n" +
                "1. reportName为测试报告名称，输出路径为 jmeterHome/htmlreport/${reportName}.html\n" +
                "2. isAppend为是否追加模式写报告，枚举为 true|false\n" +
                "3. 执行前必须先在 jmeterHome 下创建 htmlreport 目录\n" +
                "4. Non-Gui模式命令解释：\n" +
                "   a. 存在 -JreportName 参数时，优先读取 ${__P(reportName)} HTML报告名称\n" +
                "   b. 存在 -JisAppend 参数时，优先读取 ${__P(isAppend)} 追加模式\n" +
                "   c. 存在 -JdataFileName 参数时，优先读取 ${__P(dataFileName)} 数据文件名称";
        JTextArea textArea = new JTextArea(note);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setBackground(this.getBackground());
        return textArea;
    }
}