package com.github.xg.utgen;

import com.github.xg.utgen.core.TestProject;
import com.github.xg.utgen.core.model.TestProjectInfo;
import com.google.common.base.Strings;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class PluginExecutor {

    private static PluginExecutor singleton = new PluginExecutor();

    private volatile Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(false);


    private PluginExecutor() {
    }

    public static PluginExecutor getInstance() {
        return singleton;
    }


    public boolean isAlreadyRunning() {
        return running.get();
    }

    public synchronized void stopRun() {
        if (isAlreadyRunning()) {
            thread.interrupt();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
            }
        }
    }


    public synchronized void run(final Project project, final Parameters params, final Set<String> modules, final Set<String> classes, final AsyncGUINotifier notifier)
            throws IllegalArgumentException, IllegalStateException {

        if (params == null) {
            throw new IllegalArgumentException("No defined parameters");
        }

        if (isAlreadyRunning()) {
            throw new IllegalStateException("UT Plugin already running");
        }

        Task.Backgroundable task = new MyTask(project, "Generating UT", true, null, modules, classes, notifier, params);
        BackgroundableProcessIndicator progressIndicator = new MyIndicator(task);
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator);
    }


    private class MyIndicator extends BackgroundableProcessIndicator {

        public MyIndicator(@NotNull Task.Backgroundable task) {
            super(task);
        }

        @Override
        protected void delegateRunningChange(@NotNull IndicatorAction action) {
            try {
                Field f = com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase.class.getDeclaredField("CANCEL_ACTION");
                f.setAccessible(true);
                Object obj = f.get(null);
                if (action.equals(obj)) {
                    thread.interrupt();
                }
            } catch (Exception e) {
            }
            super.delegateRunningChange(action);
        }
    }


    private class MyTask extends Task.Backgroundable {

        private final Project project;
        private final Set<String> modules;
        private final Set<String> classes;
        private final AsyncGUINotifier notifier;
        private final Parameters params;

        public MyTask(@Nullable Project project,
                      @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title,
                      boolean canBeCancelled,
                      @Nullable PerformInBackgroundOption backgroundOption, Set<String> modules,
                      Set<String> classes, AsyncGUINotifier notifier, Parameters params) {
            super(project, title, canBeCancelled, backgroundOption);
            this.project = project;
            this.modules = modules;
            this.classes = classes;
            this.notifier = notifier;
            this.params = params;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            running.set(true);
            notifier.printOnConsole("UT Plugin is running...\n\n");
            thread = Thread.currentThread();
            String projectPath = project.getBasePath();

            String outputPath = null;
            if (!Strings.isNullOrEmpty(params.getOutputPath())) {
                outputPath = params.getOutputPath();
            }

            boolean needGetLocalRepo = false;
            if (Strings.isNullOrEmpty(params.getLocalRepoPath())) {
                needGetLocalRepo = true;
                params.setLocalRepoPath(getLocalRepoPath());
            }
            if (Strings.isNullOrEmpty(params.getLocalRepoPath())) {
                String msg = "获取Maven本地仓库地址失败！请检查本地Maven环境配置";
                notifier.failed(msg);
                return;
            }

            String[] classArray = new String[classes.size()];
            classes.toArray(classArray);

            TestProjectInfo testProjectInfo = new TestProjectInfo(projectPath, params.getLocalRepoPath(), false, null
                    , null, null, null, null, null,
                    classArray, null, null, params.isCloseMock(),
                    outputPath, params.getFileSuffix(), true,
                    params.isCloseFail(), false, null);
            TestProject testProject = new TestProject(testProjectInfo);
            testProject.setNotifier(notifier);

            boolean openMockDialog = !params.isCloseMock() && params.isInteractiveMock();
            if (openMockDialog && classArray.length > 50) {
                String msg = "交互式Mock时选择的类不要大于50个";
                notifier.failed(msg);
                return;
            }

            if (needGetLocalRepo) {
                notifier.printOnConsole("Executing command \"mvn help:effective-settings\" to get local maven repository\n");
            }

            try {
                testProject.generateTestSources(modules, openMockDialog);
            } catch (Exception e) {
                running.set(false);
                notifier.failed(e.getMessage());
                return;
            }
            running.set(false);

            VirtualFileManager.getInstance().asyncRefresh(null);
            notifier.success("UT Generation is completed");
        }

        @Override
        public void onCancel() {
            notifier.printOnConsole("\n\n\nUT Generation has been cancelled\n");
            running.set(false);
        }

        @Override
        public void onSuccess() {
            running.set(false);
        }

        private String getLocalRepoPath() {
            try {
                Runtime rt = Runtime.getRuntime();
                Process process = rt.exec("cmd /c mvn help:effective-settings");

                InputStreamReader reader = new InputStreamReader(process.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.indexOf("<localRepository") != -1) {
                        return line.substring(line.indexOf('>') + 1, line.lastIndexOf('<'));
                    }
                }
                process.waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
