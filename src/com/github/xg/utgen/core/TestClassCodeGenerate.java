package com.github.xg.utgen.core;

import com.github.xg.utgen.core.data.PrimitiveType;
import com.github.xg.utgen.core.data.WrapperType;
import com.github.xg.utgen.core.dependency.MethodDependency;
import com.github.xg.utgen.core.dependency.MethodDependencyViaBytecode;
import com.github.xg.utgen.core.model.ClassInfo;
import com.github.xg.utgen.core.model.GenerateStatus;
import com.github.xg.utgen.core.util.IoUtil;
import com.github.xg.utgen.core.util.StrKit;
import com.github.xg.utgen.core.util.Tuple;
import static com.github.xg.utgen.core.util.Formats.*;
import com.github.xg.utgen.ui.MockSelectDialog;
import com.google.common.base.CharMatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by yuxiangshi on 2017/8/10.
 */

public final class TestClassCodeGenerate extends AbstractCodeGenerate {
    static String SUFFIX = "AutoTest";
    private VariableNames variableNames = new VariableNames();
    private static final List<Method> objectMethods = new ArrayList<>();
    private ClassInfo classInfo;
    private StringBuilder importSb = new StringBuilder();
    private StringBuilder bodySb = new StringBuilder();
    private StringBuilder methodSb = new StringBuilder();
    private ImplementFinder implementFinder;
    private String oldContent;
    private GenerateStatus status = GenerateStatus.None;
    private int testCaseCnt = 0;
    private String testInstanceName = "testInstance";
    private String resultVarName = "actualResult";
    private boolean closeMock = false;
    private boolean singleTestcase = false;
    private boolean closeDefaultCallToFail = false;
    private boolean closeFieldSetGetCode = false;
    private TestProject testProject;

    static {
        for (Method m : Object.class.getMethods()) {
            objectMethods.add(m);
        }
    }

    public GenerateStatus getStatus() {
        return status;
    }

    public int getTestCaseCnt() {
        return testCaseCnt;
    }


    public TestClassCodeGenerate(TestProject testProject, ClassInfo classInfo, ImplementFinder implementFinder) {
        this.testProject = testProject;
        this.classInfo = classInfo;
        this.implementFinder = implementFinder;

        if (Boolean.TRUE.equals(testProject.closeMock)) {
            this.closeMock = true;
        }
        if (Boolean.TRUE.equals(testProject.singleTestcase)) {
            this.singleTestcase = true;
        }
        if (Boolean.TRUE.equals(testProject.closeDefaultCallToFail)) {
            this.closeDefaultCallToFail = true;
        }
        if (Boolean.TRUE.equals(testProject.closeFieldSetGetCode)) {
            this.closeFieldSetGetCode = true;
        }

        importSb.append("package " + classInfo.getPackageName() + ";" + RT_2);
        importClasses.add("org.junit.*");
        importClasses.add("static org.junit.Assert.*");
        if (!this.closeMock) {
            importClasses.add("org.junit.runner.RunWith");
            importClasses.add("mockit.integration.junit4.JMockit");
        }
    }

    class TestClassLoader extends ClassLoader {
        private byte[] toByteArray(InputStream inputStream) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            byte[] byteArray = new byte[1000];
            while ((read = inputStream.read(byteArray, 0, byteArray.length)) != -1) {
                out.write(byteArray, 0, read);
            }
            out.flush();
            return out.toByteArray();
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // load other classes normally using System ClassLoader
//            if (!name.equals("Test")) {
//                return super.loadClass(name);
//            }

            InputStream in = null;
            try {
                // get input stream linked to this class
                in = ClassLoader.getSystemResourceAsStream(name.replace('.', '\\') + ".class");
                if (in != null) {
                    byte[] classData = toByteArray(in);
                    // converts a byte array to a instance of class java.lang.Class
                    Class<?> clazz = defineClass(name, classData, 0, classData.length);
                    return clazz;
                }

                return ClassLoader.getSystemClassLoader().loadClass(name);

            } catch (Exception e) {
                throw new ClassNotFoundException();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }


    private Class<?> reloadClass(Class<?> oldClass) throws Exception {
        System.out.println("reloading");
        URL[] urls = {oldClass.getProtectionDomain().getCodeSource().getLocation()};
        ClassLoader delegateParent = oldClass.getClassLoader().getParent();
        Class<?> reloaded;
        try (URLClassLoader cl = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            reloaded = cl.loadClass(oldClass.getName());
        }
        return reloaded;
    }

    public void generateTestSource(final String saveFilePath, final boolean interactiveMock) throws ClassNotFoundException {
        if (saveFilePath.toLowerCase().contains("package-info")) {
            return;
        }
//        Class c = Class.forName(classInfo.getClassFullName(), false, ClassLoader.getSystemClassLoader());
        Class c = null;
        try {
            c = new TestClassLoader().loadClass(classInfo.getClassFullName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Method[] declaredMethods = c.getDeclaredMethods();
        List<Method> needTestMethods = new ArrayList<>();
        for (Method method : declaredMethods) {
            if (!MethodHelper.filterMethod(method)) {
                continue;
            }
            needTestMethods.add(method);
        }
        Map<Method, String> testMethodNames = getMethodNames(needTestMethods);

        //delta
        List<Method> needAddMethods = new ArrayList<>();
        boolean needAppend = false;
        int importStartIndex = -1;
        if (new File(saveFilePath).exists()) {
            oldContent = IoUtil.readFile(saveFilePath);
            int index = oldContent.indexOf("package");
            if (index == -1) {
                return;
            }

            importStartIndex = oldContent.indexOf(";", index);
            if (importStartIndex == -1) {
                return;
            }
            needAppend = true;

            for (Method method : needTestMethods) {
                String methodName = testMethodNames.get(method);
                String testMethodDeclareName = "test" + StrKit.CapsFirst(methodName);

                if (!oldContent.contains(testMethodDeclareName + "(")) {
                    needAddMethods.add(method);
                }
            }
        }

        String testClassName = getClassName(c);

        bodySb.append(testProject.getHEADER());
        if (!closeMock) {
            bodySb.append("@RunWith(JMockit.class)" + RT_1);
        }

        String mockSuffix = " extends " + testClassName + MockCodeGenerate.suffix;
        if (closeMock) {
            mockSuffix = "";
        }
        bodySb.append("public class " + testClassName + SUFFIX + mockSuffix + " {" + RT_2);

        MethodDependency methodDependency = new MethodDependencyViaBytecode(c);

        bodySb.append(BLANK_4 + "@BeforeClass" + RT_1
                + BLANK_4 + "public static void setUpClass() {" + RT_1
                + BLANK_8 + "//operation before all tests" + RT_1
                + BLANK_4 + "}" + RT_2);
        bodySb.append(BLANK_4 + "@AfterClass" + RT_1
                + BLANK_4 + "public static void tearDownClass() {" + RT_1
                + BLANK_8 + "//operation after all tests" + RT_1
                + BLANK_4 + "}" + RT_2);

        bodySb.append(BLANK_4 + "//test class instance, can be reused" + RT_1);
        bodySb.append(BLANK_4 + "private " + testClassName + " " + testInstanceName + ";" + RT_2);

        StringBuilder beforeSb = new StringBuilder();
        TypeValueCode tvClause = new TypeValueCode(c.getTypeName(), importClasses, implementFinder, variableNames);
        tvClause.infer(testInstanceName);
        variableNames.resetCnt();
        List<String> list = tvClause.clauses;

        //include try catch
        if (list.get(list.size() - 1).equals("}")) {
            for (int i = 0; i < list.size(); i++) {
                if (i == (list.size() - 7)) {
                    continue;
                }

                if (i != list.size() - 5) {
                    beforeSb.append(BLANK_8 + list.get(i) + RT_1);
                } else {
                    String substring = list.get(i).substring(list.get(i).indexOf("="));
                    beforeSb.append(BLANK_8 + BLANK_4 + testInstanceName + " " + substring + RT_1);
                }
            }

        } else {
            for (int i = 0; i < list.size(); i++) {
                if (i != list.size() - 1) {
                    beforeSb.append(BLANK_8 + list.get(i) + RT_1);
                } else {
                    String substring = list.get(i).substring(list.get(i).indexOf("="));
                    beforeSb.append(BLANK_8 + testInstanceName + " " + substring + RT_1);
                }
            }
        }

        bodySb.append((BLANK_4 + "@Before" + RT_1
                + BLANK_4 + "public void setUp() {" + RT_1
                + BLANK_8 + "//operation before each test" + RT_1
                + beforeSb.toString()
                + BLANK_4 + "}" + RT_2));

        StringBuilder afterSb = new StringBuilder();
        bodySb.append((BLANK_4 + "@After" + RT_1
                + BLANK_4 + "public void tearDown() {" + RT_1
                + BLANK_8 + "//operation after each test" + RT_1
                + afterSb + BLANK_4 + "}" + RT_2));

        HashMap<String, Integer> methodCount = new HashMap<>();

        Map<Method, Map<Class, List<Method>>> methodDependencies = null;

        String mockCodeSavePath = saveFilePath.substring(0, saveFilePath.lastIndexOf(File.separator));
        MockCodeGenerate mockCodeGenerate = new MockCodeGenerate(testProject, c, testClassName, classInfo.getPackageName(), mockCodeSavePath, closeFieldSetGetCode);

        for (Method method : needTestMethods) {
            if (needAppend && !needAddMethods.contains(method)) {
                continue;
            }
            testCaseCnt++;
            String testMethodName = testMethodNames.get(method);
            if (methodDependencies == null) {
                try {
                    methodDependencies = methodDependency.getDependencies();
                    if (interactiveMock && methodDependencies != null && methodDependencies.size() > 0) {
                        int itemCount = 0;
                        for (Method md : methodDependencies.keySet()) {
                            if (needAppend && !needAddMethods.contains(md)) {
                                continue;
                            }
                            itemCount += methodDependencies.get(md).size();
                        }
                        if (itemCount > 0) {
                            Map<Method, Map<Class, List<Method>>> addMethodDependencies = new HashMap<>();
                            for (Method md1 : methodDependencies.keySet()) {
                                if (needAppend && !needAddMethods.contains(md1)) {
                                    continue;
                                }
                                addMethodDependencies.put(md1, methodDependencies.get(md1));
                            }
                            List<List<String>> selectedMockMethods = openMockSelectionDialog(c.toString(), addMethodDependencies);

                            List<String> currentMethodPath = new ArrayList<>();
                            for (Method md : methodDependencies.keySet()) {
                                currentMethodPath.add(MethodHelper.getMethodString(md));
                                Map<Class, List<Method>> classListMap = methodDependencies.get(md);

                                for (Class aClass : classListMap.keySet()) {
                                    currentMethodPath.add(aClass.toString());

                                    Iterator<Method> iterator = classListMap.get(aClass).iterator();
                                    while (iterator.hasNext()) {
                                        Method next = iterator.next();
                                        currentMethodPath.add(MethodHelper.getMethodString(next));
                                        if (!selectedMockMethods.contains(currentMethodPath)) {
                                            iterator.remove();
                                        }
                                        currentMethodPath.remove(currentMethodPath.size() - 1);
                                    }
                                    currentMethodPath.remove(currentMethodPath.size() - 1);
                                }
                                currentMethodPath.remove(currentMethodPath.size() - 1);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Map<Class, List<Method>> map = null;
            if (methodDependencies != null) {
                map = methodDependencies.get(method);
            }
            if (map != null) {
                List<?> classMethods = map.get(c);
                //exclude method itself
                if (classMethods != null) {
                    if (classMethods.contains(method)) {
                        classMethods.remove(method);
                    }
                }

                Iterator<Class> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    Class nextClass = iterator.next();
                    if (map.get(nextClass) == null || map.get(nextClass).size() == 0) {
                        iterator.remove();
                    }
                }
            }

            Tuple tuple = null;
            if (!closeMock) {
                tuple = mockCodeGenerate.addMethod(method, testMethodName, map);
            }

            String[] expectedCallVarNames = MethodHelper.getExpectedVarNames(method, c);

            String testMethodDeclareName = "test" + StrKit.CapsFirst(testMethodName);
            if (methodCount.containsKey(testMethodDeclareName)) {
                methodCount.put(testMethodDeclareName, methodCount.get(testMethodDeclareName) + 1);
            } else {
                methodCount.put(testMethodDeclareName, 0);
            }
            testMethodDeclareName = methodCount.get(testMethodDeclareName) == 0 ? testMethodDeclareName : testMethodDeclareName + methodCount.get(testMethodDeclareName);

            int caseCount = 2;
            if (singleTestcase) {
                caseCount = 1;
            }

            for (int i = 0; i < caseCount; i++) {
                methodSb.append(ANNOTATION_TEST);
                testMethodDeclareName += i == 0 ? "" : "WithSimpleArgs";
                methodSb.append(BLANK_4 + "public void " + testMethodDeclareName + "() {" + RT_1);

                //mock code
                if (!closeMock && tuple != null) {
                    addJavaUtilPackage();
                    if (interactiveMock) {
                        methodSb.append(BLANK_8 + "//TODO: review the generated mock code, modify the given mock values" + RT_1);
                    } else {
                        methodSb.append(BLANK_8 + "//TODO: review the generated mock code, remove unnecessary mocks and modify the given mock values" + RT_1);
                    }
                    methodSb.append(BLANK_8 + "//mock all dependencies of method " + testClassName + "#"
                            + (!testMethodName.endsWith("_") ? testMethodName : testMethodName.substring(0, testMethodName.length() - 1)) + RT_1);
                    Map<String, List<Tuple>> mockMethodArgs = tuple.second();

                    for (Map.Entry<String, List<Tuple>> entry : mockMethodArgs.entrySet()) {
                        methodSb.append(BLANK_8 + "//mock class " + entry.getKey().substring(4) + RT_1);
                        List<Tuple> classes = entry.getValue();
                        String mockArgs = "";

                        for (int j = 0; j < classes.size(); j++) {
                            Class argClass = classes.get(j).first();
                            String argName = variableNames.get(classes.get(j).<String>second());

                            String arg;
                            if (i == 0 && !argClass.getTypeName().contains("$")) {
                                TypeValueCode tvc = new TypeValueCode(argClass.getTypeName(), importClasses, implementFinder, variableNames);
                                arg = tvc.infer(null);
                                appendLines(tvc.clauses, 2);
                            } else {
                                arg = getSimpleArgs(new Class[]{argClass}, null);
                            }

                            if (j < classes.size() - 1) {
                                mockArgs += argName + ", ";

                            } else {
                                mockArgs += argName;
                            }
                            methodSb.append(BLANK_8 + "Map<Integer, " + (argClass.isPrimitive() ? MockCodeGenerate.getBoxedTypeName(argClass.getTypeName()) : getClassName(argClass)) + "> " + argName + " = new HashMap<>();" + RT_1);
                            methodSb.append(BLANK_8 + argName + ".put(0, " + arg + ");" + RT_1);
                        }

                        methodSb.append(BLANK_8 + tuple.first() + "." + entry.getKey() + "(" + mockArgs + ");" + RT_2);
                    }
                }

                Class returnType = method.getReturnType();
                String returnTypeName = getClassName(returnType);

                boolean methodHasParams = method.getParameterTypes().length > 0;
                methodSb.append(BLANK_8 + "//TODO: review the generated test code"
                        + (methodHasParams ? ", modify the given method call arguments" : "")
                        + (returnTypeName.equals("void") ? "" : " and " + (methodHasParams ? "" : "modify the given ") + "assert clause") + RT_1);
                methodSb.append(BLANK_8 + "//" + (methodHasParams ? "prepare arguments and " : "") + "call the test method" + RT_1);

                String hasException = MethodHelper.needTryCatch(method);

                boolean isStatic = Modifier.isStatic(method.getModifiers());
                String instanceName = null;
                if (!isStatic) {
                    if (i == 0) {
                        instanceName = testInstanceName;
                    } else {
                        if (!Modifier.isAbstract(c.getModifiers())) {
                            Constructor ctor = MethodHelper.chooseConstructor(c.getConstructors(), TestProject.preferConstructorWithParams);
                            if (ctor != null) {
                                String[] expectedVarNames = MethodHelper.getExpectedVarNames(ctor, c);
                                Class[] paramTypes = ctor.getParameterTypes();
                                String args = getSimpleArgs(paramTypes, expectedVarNames);

                                instanceName = variableNames.get(c);
                                String ex = MethodHelper.needTryCatch(ctor);
                                if (ex != null) {
                                    methodSb.append(BLANK_8 + testClassName + " " + instanceName + " = null;" + RT_1);
                                    methodSb.append(BLANK_8 + "try {" + RT_1
                                            + BLANK_8 + BLANK_4 + instanceName + " = new " + testClassName + "(" + args + ");" + RT_1);
                                    String exceptionVarName = variableNames.get("ex", false);
                                    methodSb.append(BLANK_8 + "} catch (" + ex + " " + exceptionVarName + ") {" + RT_1
                                            + BLANK_8 + BLANK_4 + exceptionVarName + ".printStackTrace();" + RT_1
                                            + BLANK_8 + BLANK_4 + "fail(\"Unexpected exception: \"" + " + " + exceptionVarName + ");" + RT_1 + BLANK_8 + "}" + RT_1);
                                } else {
                                    methodSb.append(BLANK_8 + testClassName + BLANK_1 + instanceName + " = new " + testClassName + "(" + args + ");" + RT_1);
                                }
                            } else {
                                instanceName = variableNames.get(c);
                                methodSb.append(BLANK_8 + testClassName + BLANK_1 + instanceName + " = null;" + RT_1);
                            }
                        } else {
                            instanceName = variableNames.get(c);
                            methodSb.append(BLANK_8 + testClassName + BLANK_1 + instanceName + " = null;" + RT_1);
                        }

                    }
                }

                String prefix = (returnTypeName.equals("void")) ? BLANK_8 : BLANK_8 + returnTypeName + " " + resultVarName + " = ";

                String invokeParams;
                if (i == 0) {
                    if (!canUseGenericMode(method)) {
                        invokeParams = getArgs(method.getParameterTypes(), expectedCallVarNames);
                    } else {
                        final Type[] genericParameterTypes = method.getGenericParameterTypes();
                        invokeParams = getArgs(genericParameterTypes, expectedCallVarNames);
                    }
                } else {
                    invokeParams = getSimpleArgs(method.getParameterTypes(), expectedCallVarNames);
                    if (method.getParameterTypes().length > 0) {
                        methodSb.append(RT_1);
                    }
                }

                boolean isPrivate = Modifier.isPrivate(method.getModifiers());
                if (hasException == null && isPrivate) {
                    hasException = "Exception";
                }
                if (hasException != null) {
                    if (isPrivate && !"".equals(invokeParams)) {
                        methodSb.append(BLANK_8 + "Object[] testMethodArgs = {" + invokeParams + "};" + RT_1);
                    }
                    methodSb.append(BLANK_8 + "try {" + RT_1);
                    prefix = BLANK_4 + prefix;

                    if (isPrivate) {
                        if (!importClasses.contains("java.lang.reflect.Method")) {
                            importClasses.add("java.lang.reflect.Method");
                        }

                        String methodParamClasses = "";
                        for (int j = 0; j < method.getParameterTypes().length; j++) {
                            String typeName = method.getParameterTypes()[j].getTypeName();
                            if (typeName.contains("$")) {
                                typeName = typeName.replace("$", ".");
                            }
                            methodParamClasses += ", " + typeName + ".class";
                        }

                        String methodDeclaringName = "method" + StrKit.CapsFirst(method.getName());
                        methodSb.append(BLANK_8 + BLANK_4 + "Method " + methodDeclaringName + " = " + testClassName + ".class.getDeclaredMethod(" + "\"" + method.getName() + "\"" + methodParamClasses + ");" + RT_1);
                        methodSb.append(BLANK_8 + BLANK_4 + methodDeclaringName + ".setAccessible(true);" + RT_1);

                        String strongConvert = (returnTypeName.equals("void")) ? "" : "(" + returnTypeName + ") ";
                        String invokeObj = instanceName;
                        if (Modifier.isStatic(method.getModifiers())) {
                            invokeObj = "null";
                        }
                        methodSb.append(prefix + strongConvert + methodDeclaringName + ".invoke(" + invokeObj + ("".equals(invokeParams) ? "" : ", testMethodArgs") + ");" + RT_1);
                    }
                }
                if (!isPrivate) {
                    if (isStatic) {
                        methodSb.append(prefix + testClassName + "." + method.getName() + "(" + invokeParams + ");" + RT_1);
                    } else {
                        methodSb.append(prefix + instanceName + "." + method.getName() + "(" + invokeParams + ");" + RT_1);
                    }
                }

                if (!returnTypeName.equals("void")) {
                    methodSb.append(BLANK_8 + (hasException != null ? BLANK_4 : "") + AssertClause.get(returnType, resultVarName) + RT_1);
                }

                if (hasException != null) {
                    String exceptionVarName = variableNames.get("ex", true);
                    methodSb.append(BLANK_8 + "} catch (" + hasException + " " + exceptionVarName + ") {" + RT_1 + BLANK_8 + BLANK_4 + exceptionVarName + ".printStackTrace();" + RT_1 + BLANK_4 + BLANK_8 + "fail(\"Unexpected exception: \"" + " + " + exceptionVarName + ");" + RT_1 + BLANK_8 + "}" + RT_1);
                }

                if (!closeDefaultCallToFail) {
                    methodSb.append(BLANK_8 + "//TODO: remove this default call to fail" + RT_1);
                    methodSb.append(BLANK_8 + "fail(\"This test case is a prototype.\");" + RT_1);
                }

                methodSb.append(BLANK_4 + "}" + RT_2);
                variableNames.resetCnt();
            }
        }

        List<String> effectiveImports = new ArrayList<>();
        for (String importClass : importClasses) {

            if (importClass.contains("java.lang") && CharMatcher.is('.').countIn(importClass) == 2) {
                continue;
            }
            if (importClass.startsWith(classInfo.getPackageName()) && !importClass.substring(classInfo.getPackageName().length() + 1).contains(".")) {
                continue;
            }
            effectiveImports.add(importClass);
        }

        String[] imports = new String[effectiveImports.size()];
        imports = effectiveImports.toArray(imports);
        Arrays.sort(imports);
        for (int i = 0; i < imports.length; i++) {
            importSb.append("import " + imports[i] + ";" + RT_1);
        }

        String content = "";
        boolean needWrite = false;
        if (needAppend && !methodSb.toString().isEmpty()) {
            needWrite = true;
            String packageClause = oldContent.substring(0, importStartIndex + 1);
            String middle = oldContent.substring(importStartIndex + 1, oldContent.lastIndexOf("}"));
            content = packageClause + RT_2 + ImportCodeHelper.toString(effectiveImports, oldContent) + middle + BLANK_4 + "//add new method on " + new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()) + "." + RT_1 + methodSb.toString() + "}";
            status = GenerateStatus.Update;

        } else if (!needAppend) {
            needWrite = true;
            bodySb.append(methodSb);
            bodySb.append("}");
            content = importSb.toString() + RT_1 + bodySb.toString();
            status = GenerateStatus.NewCreate;
        }
        if (needWrite) {
            if (!closeMock) {
                mockCodeGenerate.generate();
            }
            IoUtil.writeFile(saveFilePath, content);
        }
    }

    private void addJavaUtilPackage() {
        if (!importClasses.contains("java.util.*")) {
            importClasses.add("java.util.*");
        }
    }

    Map<Method, String> getMethodNames(List<Method> methods) {
        Map<String, Integer> methodNameCnt = new HashMap<>();
        for (Method method : methods) {
            if (methodNameCnt.containsKey(method.getName())) {
                methodNameCnt.put(method.getName(), methodNameCnt.get(method.getName()) + 1);
            } else {
                methodNameCnt.put(method.getName(), 1);
            }
        }

        Map<Method, String> map = new HashMap<>();
        for (Method method : methods) {
            if (methodNameCnt.get(method.getName()) > 1) {
                String suffix = "_";
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    suffix = "_EmptyParams";
                } else {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        String typeName = parameterTypes[i].getSimpleName();
                        if (typeName.endsWith("[]")) {
                            typeName = typeName.replace("[]", "s");
                        }
                        suffix += typeName;
                    }
                }
                map.put(method, method.getName() + suffix);
            } else {
                map.put(method, method.getName());
            }
        }
        return map;
    }

    private String getSimpleArgs(Class[] paramTypes, String[] expectedVarNames) {
        Objects.requireNonNull(paramTypes);
        String args = "";
        for (int i = 0; i < paramTypes.length; i++) {
            Class paramType = paramTypes[i];
            String paramTypeName = getClassName(paramType);
            String variableName;
            if (expectedVarNames != null && expectedVarNames[i] != null) {
                variableName = variableNames.get(expectedVarNames[i]);
            } else {
                variableName = variableNames.get(paramType);
            }

            if (paramType.isPrimitive() || "String".equals(paramType.getSimpleName())) {
                methodSb.append(BLANK_8 + paramTypeName + " " + variableName + " = " + PrimitiveType.get(paramType) + ";" + RT_1);

            } else if (WrapperType.contains(paramType)) {
                methodSb.append(BLANK_8 + paramTypeName + " " + variableName + " = " + WrapperType.getValue(paramType) + ";" + RT_1);

            } else {
                Constructor constructor = null;
                if (!Modifier.isAbstract(paramType.getModifiers())) {
                    constructor = MethodHelper.chooseConstructor(paramType.getConstructors(), TestProject.preferConstructorWithParams);
                }

                //inner class
                boolean useCtor = true;
                if (paramType.getEnclosingClass() != null && !Modifier.isStatic(paramType.getModifiers())) {
                    useCtor = false;
                }
                if (constructor != null && useCtor) {
                    String ex = MethodHelper.needTryCatch(constructor);
                    if (ex != null) {
                        methodSb.append(BLANK_8 + paramTypeName + " " + variableName + " = null;" + RT_1);
                        methodSb.append(BLANK_8 + "try {" + RT_1
                                + BLANK_8 + BLANK_4 + variableName + " = new " + paramTypeName + "(" + getInvokeParameters(constructor.getParameterTypes()) + ");" + RT_1);

                        String exceptionVarName = variableNames.get("ex", false);
                        methodSb.append(BLANK_8 + "} catch (" + ex + " " + exceptionVarName + ") {" + RT_1
                                + BLANK_8 + BLANK_4 + exceptionVarName + ".printStackTrace();" + RT_1
                                + BLANK_4 + BLANK_8 + "fail(\"Unexpected exception: \"" + " + " + exceptionVarName + ");" + RT_1 + BLANK_8 + "}" + RT_1);

                    } else {
                        String s = "new " + paramTypeName + "(" + getInvokeParameters(constructor.getParameterTypes()) + ");";
                        methodSb.append(BLANK_8 + paramTypeName + " " + variableName + " = " + s + RT_1);
                    }
                } else {
                    methodSb.append(BLANK_8 + paramTypeName + " " + variableName + " = null;" + RT_1);
                }

            }
            args += variableName + ", ";
        }

        args = args.contains(",") ? args.substring(0, args.lastIndexOf(",")) : args;
        return args;
    }


    public String getArgs(Type[] argTypes, String[] expectedVarNames) {
        Objects.requireNonNull(argTypes);
        String args = "";
        for (int j = 0; j < argTypes.length; j++) {
            TypeValueCode valueClause = new TypeValueCode(argTypes[j].getTypeName(), importClasses, implementFinder, variableNames);
            String arg = valueClause.infer(expectedVarNames[j]);
            if (j < argTypes.length - 1) {
                args += arg + ", ";
            } else {
                args += arg;
            }
            appendLines(valueClause.clauses, 2);
        }
        if (argTypes.length > 0) {
            methodSb.append(RT_1);
        }
        return args;
    }


    boolean canUseGenericMode(Method method) {
        if (method.getTypeParameters().length > 0) {
            return false;
        }

        for (TypeVariable<? extends Class<?>> classTypeVariable : method.getDeclaringClass().getTypeParameters()) {
            String ctv = classTypeVariable.toString();
            for (Type type : method.getGenericParameterTypes()) {
                if (type.toString().equals(ctv) || type.toString().contains("<" + ctv + ">")) {
                    return false;
                }
            }
        }

        boolean b = false;
        for (Type type : method.getGenericParameterTypes()) {
            if (type.getTypeName().contains("<")) {
                b = true;
                break;
            }
        }
        if (!b) {
            return false;
        }
        return true;
    }

    @Override
    protected void generate() {
    }

    private String getInvokeParameters(Class[] paramTypes) {
        String params = "";
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].getName().equals("byte")) {
                params += "(byte)" + PrimitiveType.get(paramTypes[i]) + ", ";

            } else if (paramTypes[i].getName().equals("short")) {
                params += "(short)" + PrimitiveType.get(paramTypes[i]) + ", ";
            } else {
                String s = PrimitiveType.get(paramTypes[i]);
                if (s.equals("null")) {
                    s = "(" + getClassName(paramTypes[i]) + ") " + s;
                }
                params += s + ", ";
            }
        }
        return params.contains(",") ? params.substring(0, params.lastIndexOf(",")) : params;
    }

    private void appendLines(List<String> list, int tabCnt) {
        String s = "";
        for (int i = 0; i < tabCnt; i++) {
            s += BLANK_4;
        }
        for (int i = 0; i < list.size(); i++) {
            methodSb.append(s + list.get(i) + RT_1);
        }
    }

    private List<List<String>> openMockSelectionDialog(String className, Map<Method, Map<Class, List<Method>>> methodDependencies) {
        MockSelectDialog dialog = new MockSelectDialog(className, methodDependencies);
        dialog.pack();
        dialog.setSize(1200, 500);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return dialog.getSelectedMethods();
    }

}
