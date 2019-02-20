package com.github.xg.utgen.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuxiangshi on 2017/9/15.
 */
public class ConstructHierarchy {

    final Class type;

    List<ConstructHierarchy> children;

    public void setParent(ConstructHierarchy parent) {
        this.parent = parent;
    }

    public void addChildren(ConstructHierarchy child) {
        children.add(child);
    }

    ConstructHierarchy parent;

    public ConstructHierarchy(Class type) {
        this.type = type;
        children = new ArrayList<>();
    }

    public boolean repeatWithParent() {
        ConstructHierarchy ch = parent;
        do {
            if (type.equals(ch.type)) {
                return true;
            }
        } while ((ch = ch.parent) != null);
        return false;
    }

}
