package com.github.xg.utgen.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;

public class CheckBoxTreeNodeSelectionListener extends MouseAdapter
{
    @Override
    public void mouseClicked(MouseEvent event)
    {
        JTree tree = (JTree)event.getSource();
        int x = event.getX();
        int y = event.getY();
        int row = tree.getRowForLocation(x, y);
        TreePath path = tree.getPathForRow(row);
        if(path != null)
        {
            CheckBoxTreeNode node = (CheckBoxTreeNode)path.getLastPathComponent();
            if(node != null)
            {
                boolean isSelected = !node.isSelected();
                node.setSelected(isSelected);
                ((DefaultTreeModel)tree.getModel()).nodeStructureChanged(node);
            }
        }
    }
}