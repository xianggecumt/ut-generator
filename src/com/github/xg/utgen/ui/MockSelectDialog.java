package com.github.xg.utgen.ui;

import com.github.xg.utgen.core.MethodHelper;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class MockSelectDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTree tree;


    public MockSelectDialog(String className, Map<Method, Map<Class, List<Method>>> methodDependencies) {
        setContentPane(contentPane);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        CheckBoxTreeNode rootNode = new CheckBoxTreeNode(className);

        for (Method method : methodDependencies.keySet()) {
            Map<Class, List<Method>> classListMap = methodDependencies.get(method);
            if (classListMap == null || classListMap.size() == 0) {
                continue;
            }

            CheckBoxTreeNode methodNode = new CheckBoxTreeNode(MethodHelper.getMethodString(method));

            for (Class dependClass : classListMap.keySet()) {
                CheckBoxTreeNode classNode = new CheckBoxTreeNode(dependClass.toString());

                for (Method dependMethod : classListMap.get(dependClass)) {
                    CheckBoxTreeNode dependMethodNode = new CheckBoxTreeNode(MethodHelper.getMethodString(dependMethod));
                    classNode.add(dependMethodNode);
                }

                methodNode.add(classNode);
            }
            rootNode.add(methodNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());
        tree.setModel(model);
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());
        expandAll(tree);
    }

    private List<List<String>> selectedMethods = new ArrayList<>();

    private void onOK() {
        // add your code here
        TreeModel model = tree.getModel();
        CheckBoxTreeNode rootNode = ((CheckBoxTreeNode) model.getRoot());
        selectedMethods = rootNode.getSelectedMethodPaths();
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        MockSelectDialog dialog = new MockSelectDialog(null, null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public List<List<String>> getSelectedMethods() {
        return selectedMethods;
    }

    public void expandAll(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root));
    }

    private void expandAll(JTree tree, TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path);
            }
        }
        tree.expandPath(parent);
        // tree.collapsePath(parent);
    }
}
