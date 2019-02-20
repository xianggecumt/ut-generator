package com.github.xg.utgen.core.model;

/**
 * Created by yuxiangshi on 2017/10/23.
 */
public class TestProjectInfo {

    String projectPath;
    String localRepoPath;
    Boolean preferConstructorWithParams;
    String[] addClassPaths;
    String[] excludeMockPackages;
    String[] excludeMockMethodPrefixes;
    String[] excludeMockMethodSuffixes;
    String[] excludeMockPackageMethodPrefixes;
    String[] excludeMockPackageMethodSuffixes;
    String[] includeGeneratePackages;
    String[] excludeGeneratePackages;
    String cmdTarget;
    Boolean closeMock;
    String outputPath;
    String fileSuffix;
    Boolean singleTestcase;
    Boolean closeDefaultCallToFail;
    Boolean closeFieldSetGetCode;
    String[] onlyIncludeMockPackageMethods;


    public String getProjectPath() {
        return projectPath;
    }

    public String getLocalRepoPath() {
        return localRepoPath;
    }

    public Boolean getPreferConstructorWithParams() {
        return preferConstructorWithParams;
    }

    public String[] getAddClassPaths() {
        return addClassPaths;
    }

    public String[] getExcludeMockPackages() {
        return excludeMockPackages;
    }

    public String[] getExcludeMockMethodPrefixes() {
        return excludeMockMethodPrefixes;
    }

    public String[] getExcludeMockMethodSuffixes() {
        return excludeMockMethodSuffixes;
    }

    public String[] getExcludeMockPackageMethodPrefixes() {
        return excludeMockPackageMethodPrefixes;
    }

    public String[] getExcludeMockPackageMethodSuffixes() {
        return excludeMockPackageMethodSuffixes;
    }

    public String[] getIncludeGeneratePackages() {
        return includeGeneratePackages;
    }

    public String[] getExcludeGeneratePackages() {
        return excludeGeneratePackages;
    }

    public String getCmdTarget() {
        return cmdTarget;
    }

    public Boolean getCloseMock() {
        return closeMock;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public Boolean getSingleTestcase() {
        return singleTestcase;
    }

    public Boolean getCloseDefaultCallToFail() {
        return closeDefaultCallToFail;
    }

    public Boolean getCloseFieldSetGetCode() {
        return closeFieldSetGetCode;
    }

    public String[] getOnlyIncludeMockPackageMethods() {
        return onlyIncludeMockPackageMethods;
    }

    public TestProjectInfo(String projectPath,
                           String localRepoPath,
                           Boolean preferConstructorWithParams,
                           String[] addClassPaths,
                           String[] excludeMockPackages,
                           String[] excludeMockMethodPrefixes,
                           String[] excludeMockMethodSuffixes,
                           String[] excludeMockPackageMethodPrefixes,
                           String[] excludeMockPackageMethodSuffixes,
                           String[] includeGeneratePackages,
                           String[] excludeGeneratePackages,
                           String cmdTarget,
                           Boolean closeMock,
                           String outputPath,
                           String fileSuffix,
                           Boolean singleTestcase,
                           Boolean closeDefaultCallToFail,
                           Boolean closeFieldSetGetCode,
                           String[] onlyIncludeMockPackageMethods) {

        this.projectPath = projectPath;
        this.localRepoPath = localRepoPath;
        this.addClassPaths = addClassPaths;
        this.preferConstructorWithParams = preferConstructorWithParams;
        this.excludeMockPackages = excludeMockPackages;
        this.excludeMockMethodPrefixes = excludeMockMethodPrefixes;
        this.excludeMockMethodSuffixes = excludeMockMethodSuffixes;
        this.excludeMockPackageMethodPrefixes = excludeMockPackageMethodPrefixes;
        this.excludeMockPackageMethodSuffixes = excludeMockPackageMethodSuffixes;
        this.includeGeneratePackages = includeGeneratePackages;
        this.excludeGeneratePackages = excludeGeneratePackages;
        this.cmdTarget = cmdTarget;
        this.closeMock = closeMock;
        this.outputPath = outputPath;
        this.fileSuffix = fileSuffix;
        this.singleTestcase = singleTestcase;
        this.closeDefaultCallToFail = closeDefaultCallToFail;
        this.closeFieldSetGetCode = closeFieldSetGetCode;
        this.onlyIncludeMockPackageMethods = onlyIncludeMockPackageMethods;
    }

    public TestProjectInfo(String projectPath, String localRepoPath) {
        this.projectPath = projectPath;
        this.localRepoPath = localRepoPath;
    }
}
