package com.github.xg.utgen;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;


public class StopAction extends AnAction {

    private final AsyncGUINotifier notifier;

    public StopAction(AsyncGUINotifier notifier){
        super("Stop UT Generation");
        getTemplatePresentation().setIcon(AllIcons.Actions.CloseNew);
        getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseNewHovered);
        this.notifier = notifier;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PluginExecutor.getInstance().stopRun();
        notifier.printOnConsole("\n\nUT Generation has been cancelled\n");
    }
}
