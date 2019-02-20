package com.github.xg.utgen;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;

/**
 * @author yuxiangshi
 */
public class Parameters {

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String REPO_PATH_PARAM = "repoPath";
    public static final String OUTPUT_PATH_PARAM = "outputPath";
    public static final String CLOSE_MOCK_PARAM = "closeMock";
    public static final String CLOSE_FAIL_PARAM = "closeFail";
    public static final String INTERACTIVE_MOCK_PARAM = "interactiveMock";
    public static final String FILE_SUFFIX = "fileSuffix";

    public String getLocalRepoPath() {
        return localRepoPath;
    }

    public void setLocalRepoPath(String localRepoPath) {
        this.localRepoPath = localRepoPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public boolean isCloseMock() {
        return closeMock;
    }

    public void setCloseMock(boolean closeMock) {
        this.closeMock = closeMock;
    }

    public boolean isCloseFail() {
        return closeFail;
    }

    public void setCloseFail(boolean closeFail) {
        this.closeFail = closeFail;
    }

    public boolean isInteractiveMock() {
        return interactiveMock;
    }

    public void setInteractiveMock(boolean interactiveMock) {
        this.interactiveMock = interactiveMock;
    }

    private String localRepoPath;
    private String outputPath;
    private String fileSuffix;
    private boolean closeMock;
    private boolean closeFail;
    private boolean interactiveMock;

    private Parameters() {
    }

    private static Parameters singleton = new Parameters();

    public static Parameters getInstance() {
        return singleton;
    }

    public void save(Project project) {
        PropertiesComponent component = PropertiesComponent.getInstance(project);
        component.setValue(REPO_PATH_PARAM, localRepoPath);
        component.setValue(OUTPUT_PATH_PARAM, outputPath);
        component.setValue(FILE_SUFFIX, fileSuffix);
        component.setValue(CLOSE_MOCK_PARAM, closeMock ? TRUE : FALSE);
        component.setValue(CLOSE_FAIL_PARAM, closeFail ? TRUE : FALSE);
        component.setValue(INTERACTIVE_MOCK_PARAM, interactiveMock ? TRUE : FALSE);
    }

    public void load(Project project) {
        PropertiesComponent component = PropertiesComponent.getInstance(project);
        localRepoPath = component.getValue(REPO_PATH_PARAM);
        outputPath = component.getValue(OUTPUT_PATH_PARAM);
        fileSuffix = component.getValue(FILE_SUFFIX);
        closeMock = TRUE.equals(component.getValue(CLOSE_MOCK_PARAM));
        closeFail = TRUE.equals(component.getValue(CLOSE_FAIL_PARAM));
        interactiveMock = TRUE.equals(component.getValue(INTERACTIVE_MOCK_PARAM));
    }

}
