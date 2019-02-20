package com.github.xg.utgen.core.dependency;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Created by yuxiangshi on 2017/8/28.
 */
public interface MethodDependency {
    Map<Method, Map<Class, List<Method>>> getDependencies() throws Exception;
}
