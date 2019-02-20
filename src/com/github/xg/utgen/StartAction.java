package com.github.xg.utgen;

import com.github.xg.utgen.core.util.Tuple;
import com.github.xg.utgen.ui.StartDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author yuxiangshi
 */
public class StartAction extends DumbAwareAction {

    public StartAction() {
        super("Generate UT",
                "Open GUI dialog to configure and start to generate JUnit tests automatically",
                loadIcon());
    }

    static Icon loadIcon() {
        try {
            Image image = ImageIO.read(StartAction.class.getClassLoader().getResourceAsStream("icon/u.png"));
            image = image.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(image);
            return icon;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        String title = "UT Plugin";
        Project project = event.getProject();

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("UT Plugin");
        final AsyncGUINotifier notifier = IntelliJNotifier.getNotifier(project);

        if (PluginExecutor.getInstance().isAlreadyRunning()) {
            Messages.showMessageDialog(project, "An instance is already running",
                    title, Messages.getErrorIcon());
            return;
        }

        Tuple tuple = getCUTsToTest(event);
        Map<String, Set<String>> map = tuple.first();
        Set<String> selectedModules = tuple.second();

        if (map == null || map.isEmpty() || map.values().stream().mapToInt(Set::size).sum() == 0) {
            Messages.showMessageDialog(project, "No '.java' file or non-empty source folder was selected in a valid module",
                    title, Messages.getErrorIcon());
            return;
        }

        Set<String> classes = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            classes.addAll(entry.getValue());
        }

        StartDialog dialog = new StartDialog();
        dialog.initFields(Parameters.getInstance());
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(500, 240);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        dialog.setResizable(false);
        if (dialog.isWasOK()) {
            toolWindow.show(() -> notifier.clearConsole());
            Parameters.getInstance().save(project);
            PluginExecutor.getInstance().run(project, Parameters.getInstance(), selectedModules, classes, notifier);
        }
    }


    private Tuple getCUTsToTest(AnActionEvent event) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        Project project = event.getData(PlatformDataKeys.PROJECT);
        ModulesInfo modulesInfo = new ModulesInfo(project);

        if (!modulesInfo.hasRoots()) {
            return null;
        }

        Set<String> selectedModules = new HashSet<>();
        Set<String> alreadyHandled = new LinkedHashSet<>();

        for (VirtualFile virtualFile : event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY)) {
            String selectedFilePath = new File(virtualFile.getCanonicalPath()).getAbsolutePath();

            String selectedModulePath = "";
            for (String modulePath : modulesInfo.getModulePathsView()) {
                if (selectedFilePath.startsWith(modulePath)) {
                    if (modulePath.length() > selectedModulePath.length()) {
                        selectedModulePath = modulePath;
                    }
                }
            }
            selectedModules.add(selectedModulePath);
            recursiveHandle(map, modulesInfo, alreadyHandled, selectedFilePath);
        }
        if (selectedModules.contains(event.getProject().getBasePath().replace('/', '\\'))) {
            selectedModules.addAll(modulesInfo.getModulePathsView());
        }

        return Tuple.of(map, selectedModules);
    }


    private void recursiveHandle(Map<String, Set<String>> map, ModulesInfo modulesInfo, Set<String> alreadyHandled, String path) {

        if (alreadyHandled.contains(path)) {
            return;
        }

        Set<String> skip = handleSelectedPath(map, modulesInfo, path);
        alreadyHandled.add(path);

        for (String s : skip) {
            recursiveHandle(map, modulesInfo, alreadyHandled, s);
        }
    }


    private Set<String> handleSelectedPath(Map<String, Set<String>> map, ModulesInfo modulesInfo, String selectedFilePath) {

         /*
                if Module A includes sub-module B, the source roots in B should
                not be marked for A
             */
        Set<String> skip = new LinkedHashSet<>();

        String module = modulesInfo.getModuleFolder(selectedFilePath);
        File selectedFile = new File(selectedFilePath);

        if (module == null) {
            return skip;
        }

        Set<String> classes = map.getOrDefault(module, new LinkedHashSet<>());

        String root = modulesInfo.getSourceRootForFile(selectedFilePath);

        if (root == null) {
            /*
                the chosen file is not in a source folder.
                Need to check if its parent of any of them
             */
            Set<String> included = modulesInfo.getIncludedSourceRoots(selectedFilePath);
            if (included == null || included.isEmpty()) {
                return skip;
            }

            for (String otherModule : modulesInfo.getModulePathsView()) {

                if (otherModule.length() > module.length() && otherModule.startsWith(module)) {
                    //the considered module has a sub-module
                    included.stream().filter(inc -> inc.startsWith(otherModule)).forEach(skip::add);
                }
            }

            for (String sourceFolder : included) {
                if (skip.contains(sourceFolder)) {
                    continue;
                }
                scanFolder(new File(sourceFolder), classes, sourceFolder);
            }

        } else {
            if (!selectedFile.isDirectory()) {
                if (!selectedFilePath.endsWith(".java")) {
                    // likely a resource file
                    return skip;
                }

                String name = getCUTName(selectedFilePath, root);
                classes.add(name);
            } else {
                scanFolder(selectedFile, classes, root);
            }

        }

        if (!classes.isEmpty()) {
            map.put(module, classes);
        }

        return skip;
    }

    private void scanFolder(File file, Set<String> classes, String root) {
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                scanFolder(child, classes, root);
            } else {
                String path = child.getAbsolutePath();
                if (path.endsWith(".java")) {
                    String name = getCUTName(path, root);
                    classes.add(name);
                }
            }
        }
    }

    private String getCUTName(String path, String root) {
        String name = path.substring(root.length() + 1, path.length() - ".java".length());
        name = name.replace('/', '.'); //posix
        name = name.replace("\\", ".");  // windows
        final String s = ".src.main.java.";
        int index;
        if ((index = name.indexOf(s)) != -1) {
            name = name.substring(index + s.length());
        }
        return name;
    }
}
