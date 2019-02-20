package com.github.xg.utgen.ui;

import com.github.xg.utgen.Parameters;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author yuxiangshi
 */
public class StartDialog extends JDialog {

    private JPanel configPanel;
    private JTextField outputPathField;
    private JTextField mavenRepoField;
    private JCheckBox closeFailCheckBox;
    private JCheckBox closeMockCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox mockDialogCheckBox;
    private JTextField fileSuffixField;
    private volatile Parameters params;
    private volatile boolean wasOK = false;


    public StartDialog() {
        setContentPane(configPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        closeFailCheckBox.addActionListener(e -> params.setCloseFail(closeFailCheckBox.isSelected()));
        closeMockCheckBox.addActionListener(e -> {
            params.setCloseMock(closeMockCheckBox.isSelected());
            mockDialogCheckBox.setEnabled(!closeMockCheckBox.isSelected());
            if (!mockDialogCheckBox.isEnabled()) {
                mockDialogCheckBox.setSelected(false);
            }

        });
        mockDialogCheckBox.addActionListener(e -> params.setInteractiveMock(mockDialogCheckBox.isSelected()));
        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    public void initFields(Parameters params) {
        this.params = params;
        mavenRepoField.setText(params.getLocalRepoPath());
        outputPathField.setText(params.getOutputPath());
        fileSuffixField.setText(params.getFileSuffix());
        closeFailCheckBox.setSelected(params.isCloseFail());
        closeMockCheckBox.setSelected(params.isCloseMock());
        mockDialogCheckBox.setSelected(params.isInteractiveMock());
        if (params.isCloseMock()) {
            mockDialogCheckBox.setEnabled(false);
            mockDialogCheckBox.setSelected(false);
        }
    }

    private void onOK() {
        params.setLocalRepoPath(mavenRepoField.getText().trim());
        params.setOutputPath(outputPathField.getText().trim());
        params.setFileSuffix(fileSuffixField.getText().trim());
        wasOK = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isWasOK() {
        return wasOK;
    }

}
