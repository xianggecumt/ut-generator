package com.github.xg.utgen.ui;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    protected JCheckBox check;
    protected CheckBoxTreeLabel label;

    public CheckBoxTreeCellRenderer() {
        setLayout(null);
        add(check = new JCheckBox());
        add(label = new CheckBoxTreeLabel());
        check.setBackground(UIManager.getColor("Tree.textBackground"));
        label.setForeground(UIManager.getColor("Tree.textForeground"));
    }

    /**
     * 返回的是一个<code>JPanel</code>对象，该对象中包含一个<code>JCheckBox</code>对象
     * 和一个<code>JLabel</code>对象。并且根据每个结点是否被选中来决定<code>JCheckBox</code>
     * 是否被选中。
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        setEnabled(tree.isEnabled());
        check.setSelected(((CheckBoxTreeNode) value).isSelected());
        label.setFont(tree.getFont());
        label.setText(stringValue);
        label.setSelected(selected);
        label.setFocus(hasFocus);
        if (leaf)
            label.setIcon(UIManager.getIcon("Tree.leafIcon"));
        else if (expanded)
            label.setIcon(UIManager.getIcon("Tree.openIcon"));
        else
            label.setIcon(UIManager.getIcon("Tree.closedIcon"));


        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

        if (node.isRoot()){
            label.setForeground(Color.MAGENTA);
            label.setFont(new Font("宋体", Font.BOLD, 20));
        }
        else if (node.getChildCount() > 0){
            if (node.getChildAt(0).isLeaf()) {
                label.setForeground(Color.BLUE);
            }else {
                label.setForeground(Color.RED);
                label.setFont(new Font("宋体", Font.ITALIC, 19));
            }
        } else if (node.isLeaf())
            label.setForeground(UIManager.getColor("Tree.textForeground"));

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dCheck = check.getPreferredSize();
        Dimension dLabel = label.getPreferredSize();
        return new Dimension(dCheck.width + dLabel.width + 8, dCheck.height < dLabel.height ? dLabel.height : dCheck.height);
    }

    @Override
    public void doLayout() {
        Dimension dCheck = check.getPreferredSize();
        Dimension dLabel = label.getPreferredSize();
        int yCheck = 0;
        int yLabel = 0;
        if (dCheck.height < dLabel.height)
            yCheck = (dLabel.height - dCheck.height) / 2;
        else
            yLabel = (dCheck.height - dLabel.height) / 2;
        check.setLocation(0, yCheck);
        check.setBounds(0, yCheck, dCheck.width, dCheck.height + 2);
        label.setLocation(dCheck.width, yLabel);
        //label.setBounds(dCheck.width, yLabel, dLabel.width, dLabel.height);
        label.setBounds(24, yLabel, dLabel.width + 8, dLabel.height);
    }

    @Override
    public void setBackground(Color color) {
        if (color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }
}