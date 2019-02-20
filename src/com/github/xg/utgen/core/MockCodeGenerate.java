package com.github.xg.utgen.core;

import com.github.xg.utgen.core.util.IoUtil;
import com.github.xg.utgen.core.util.StrKit;
import com.github.xg.utgen.core.util.Tuple;
import static com.github.xg.utgen.core.util.Formats.*;
import com.google.common.base.CharMatcher;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by yuxiangshi on 2017/9/4.
 */
public class MockCodeGenerate extends AbstractCodeGenerate {

    private String className;
    private String packageName;
    private String saveFilePath;

    private StringBuilder sb = new StringBuilder();
    private StringBuilder mockClassesSb = new StringBuilder();

    private StringBuilder importSb = new StringBuilder();
    private HashMap<String, Integer> methodCount = new HashMap<>();
    private String oldContent;
    static String suffix = "AutoMock";
    private boolean hasCtorCode;
    private boolean ctorCodeExistMap;

    public MockCodeGenerate(TestProject testProject, Class clazz, String className, String packageName, String saveFilePath, boolean closeFieldSetGetCode) {
        this.className = className;
        this.packageName = packageName;
        this.saveFilePath = saveFilePath + File.separator + className + suffix + ".java";
        importClasses.add("java.util.Map");
        importClasses.add("mockit.Mock");
        importClasses.add("mockit.MockUp");
        if (!closeFieldSetGetCode) {
            importClasses.add("java.lang.reflect.Field");
            importClasses.add("java.lang.reflect.Modifier");
            importClasses.add("mockit.Deencapsulation");
        }
        sb.append(testProject.getHEADER());
        sb.append("public class " + className + suffix + " {" + RT_2);
        getConstructorMockCode(clazz);
        if (!closeFieldSetGetCode) {
            sb.append(AdditionalMockCode.clause);
        }
    }

    private void getConstructorMockCode(Class<?> clazz) {
        ConstructorMockCode cmc = new ConstructorMockCode(clazz, this);
        String s = cmc.get();
        hasCtorCode = !StrKit.blank(s);
        ctorCodeExistMap = cmc.existMapType;
        sb.append(s);
    }

    public Tuple addMethod(Method method, String methodName, Map<Class, List<Method>> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<String, List<Tuple>> classMethodsMap = new LinkedHashMap<>();

        StringBuilder methodSb = new StringBuilder();
        String testMethodName = StrKit.CapsFirst(methodName);

        if (methodCount.containsKey(testMethodName)) {
            methodCount.put(testMethodName, methodCount.get(testMethodName) + 1);
        } else {
            methodCount.put(testMethodName, 0);
        }

        if (methodCount.get(testMethodName) != 0) {
            testMethodName += methodCount.get(testMethodName);
        }

        if (testMethodName.equals(className)) {
            testMethodName = "$" + testMethodName;
        }
        methodSb.append(BLANK_4 + "/**" + RT_1);
        methodSb.append(BLANK_4 + " * Dependency mock code of test method {@link " + className + "#" + method.getName() + "}" + RT_1);
        methodSb.append(BLANK_4 + " */" + RT_1);
        methodSb.append(BLANK_4 + "static class " + testMethodName + "Mock {" + RT_1);

        List<String> mockedClassNames = new ArrayList<>();
        for (Class c : map.keySet()) {
            boolean isAbstract = false;
            if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
                isAbstract = true;
            }

            String mockClassName = getClassName(c);
            boolean repeatClassName = false;
            if (!mockedClassNames.contains(mockClassName)) {
                mockedClassNames.add(mockClassName);
            } else {
                repeatClassName = true;
            }
            if (repeatClassName) {
                mockClassName = c.getName();
            }

            List<Tuple> argTypes = new ArrayList<>();
            String mockMethodName;
            if (classMethodsMap.containsKey(mockMethodName = "mock" + c.getSimpleName())) {
                mockMethodName = "mock";
                String[] strings = c.getName().split("\\.");
                for (int i = 0; i < strings.length; i++) {
                    String s = StrKit.CapsFirst(strings[i]);
                    mockMethodName += s;
                }
            }
            classMethodsMap.put(mockMethodName, argTypes);

            String genericParam = isAbstract ? " <T extends " + mockClassName + ">" : "";
            methodSb.append(BLANK_8 + "static" + genericParam + " void " + mockMethodName + "(");

            int i = 0;
            Map<String, Integer> methodNameCountMap = new HashMap<>();
            Map<String, Integer> methodNameCountMap1 = new HashMap<>();

            List<Tuple> fields = new ArrayList<>();
            String params = "";
            for (Method m : map.get(c)) {
                String mName = m.getName();
                Class mReturnType = m.getReturnType();
                String returnType = getClassName(mReturnType);

                if (!returnType.equals("void")) {
                    String capsMethodName = StrKit.CapsFirst(mName);

                    if (!methodNameCountMap.containsKey(mName)) {
                        methodNameCountMap.put(mName, 1);
                    } else {
                        int cnt = methodNameCountMap.get(mName);
                        methodNameCountMap.put(mName, cnt + 1);
                        mName = mName + cnt;
                    }

                    if (!methodNameCountMap1.containsKey(capsMethodName)) {
                        methodNameCountMap1.put(capsMethodName, 1);
                    } else {
                        int cnt = methodNameCountMap1.get(capsMethodName);
                        methodNameCountMap1.put(capsMethodName, cnt + 1);
                        capsMethodName = capsMethodName + cnt;
                    }

                    fields.add(Tuple.of(mName + "Count", returnType + " last" + capsMethodName + "MockValue;", mName + "MockValue"));
                }

                params += returnType.equals("void") ? "" : ((i++ == 0 ? "" : ", ") + "final Map<Integer, " + (mReturnType.isPrimitive() ? getBoxedTypeName(returnType) : returnType) + "> " + mName + "MockValue");
                if (!returnType.equals("void")) {
                    argTypes.add(Tuple.of(mReturnType, mName + "MockValue"));
                }
            }
            methodSb.append(params + ") {" + RT_1);

            methodSb.append(BLANK_8 + BLANK_4 + "new MockUp<" + (isAbstract ? "T" : mockClassName) + ">() {" + RT_1);

            for (int j = 0; j < fields.size(); j++) {
                methodSb.append(BLANK_8 + BLANK_8 + "int " + fields.get(j).first() + " = 0;" + RT_1);
                methodSb.append(BLANK_8 + BLANK_8 + fields.get(j).second() + RT_2);
            }

            i = 0;
            for (Method m : map.get(c)) {
                methodSb.append(BLANK_8 + BLANK_8 + "@Mock" + RT_1);
                boolean needInvocation = false;
                if (isAbstract && c.isAssignableFrom(method.getDeclaringClass())) {
                    if (MethodHelper.methodEqualsIgnoringDeclaringClass(method, m)) {
                        needInvocation = true;
                        if (!importClasses.contains("mockit.Invocation")) {
                            importClasses.add("mockit.Invocation");
                        }
                    }
                }

                String methodParams = "";
                Class[] parameterTypes = m.getParameterTypes();

                if (needInvocation) {
                    methodParams = parameterTypes.length != 0 ? "Invocation invo, " : "Invocation invo";
                }
                for (int j = 0; j < parameterTypes.length; j++) {
                    String paramName = " param" + j;
                    String typeName = getClassName(parameterTypes[j]);
                    methodParams += (j == 0) ? typeName + paramName : ", " + typeName + paramName;
                }

                Class returnType = m.getReturnType();
                String returnTypeName = getClassName(returnType);
                StringBuilder mockMethodBody = new StringBuilder();
                String prefix = BLANK_8 + BLANK_8 + BLANK_4;

                if (needInvocation) {
                    mockMethodBody.append(prefix + "if (invo.getInvokedInstance() instanceof " + getClassName(method.getDeclaringClass()) + ") {" + RT_1);
                    mockMethodBody.append(prefix + BLANK_4 + "//execute the original method" + RT_1);
                    mockMethodBody.append(prefix + BLANK_4 + ("void".equals(returnTypeName) ? "" : "return ") + "invo.proceed(invo.getInvokedArguments());" + RT_1);
                    mockMethodBody.append(prefix + "}" + RT_2);
                }
                if (!"void".equals(returnTypeName)) {
                    Tuple tuple = fields.get(i++);
                    String lastValueVarName = tuple.second().toString().split(" ")[1];
                    lastValueVarName = lastValueVarName.substring(0, lastValueVarName.length() - 1);

                    mockMethodBody.append(prefix + returnTypeName + " result = " + lastValueVarName + ";" + RT_1);
                    mockMethodBody.append(prefix + "if (" + tuple.third() + ".containsKey(" + tuple.first() + ")) {" + RT_1);
                    mockMethodBody.append(prefix + BLANK_4 + "result = " + tuple.third() + ".get(" + tuple.first() + ");" + RT_1);
                    mockMethodBody.append(prefix + BLANK_4 + lastValueVarName + " = result;" + RT_1);
                    mockMethodBody.append(prefix + "}" + RT_1);
                    mockMethodBody.append(prefix + tuple.first() + "++;" + RT_1);
                    mockMethodBody.append(prefix + "return result;" + RT_1);
                } else {
                    mockMethodBody.append(RT_1);
                }

                String modifier = Modifier.toString(m.getModifiers());
                modifier = MethodHelper.filterMethodModifier(modifier);
                methodSb.append(BLANK_8 + BLANK_8 + modifier + ("".equals(modifier) ? "" : " ") + returnTypeName + " " + m.getName() + "(" + methodParams + ") {" + RT_1
                        + mockMethodBody
                        + BLANK_8 + BLANK_8 + "}" + RT_2);
            }
            methodSb.append(BLANK_8 + BLANK_4 + "};" + RT_1);
            methodSb.append(BLANK_8 + "}" + RT_2);
        }

        methodSb.append(BLANK_4 + "}" + RT_2);
        mockClassesSb.append(methodSb);

        return Tuple.of(testMethodName + "Mock", classMethodsMap);
    }

    @Override
    public void generate() {
        //delta
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
        }
        importSb.append("package " + packageName + ";" + RT_2);

        List<String> effectiveImports = new ArrayList<>();
        for (String importClass : importClasses) {
            if (importClass.contains("java.lang") && CharMatcher.is('.').countIn(importClass) == 2) {
                continue;
            }
            if (importClass.startsWith(packageName) && !importClass.substring(packageName.length() + 1).contains(".")) {
                continue;
            }

            effectiveImports.add(importClass);
        }

        String[] imports = new String[effectiveImports.size()];
        imports = effectiveImports.toArray(imports);
        Arrays.sort(imports);
        for (int i = 0; i < imports.length; i++) {
            String s = imports[i];
            importSb.append("import " + s + ";" + RT_1);
        }

        String content = "";
        boolean needWrite = false;
        if (needAppend && !mockClassesSb.toString().isEmpty()) {
            needWrite = true;
            String packageClause = oldContent.substring(0, importStartIndex + 1);
            String middle = oldContent.substring(importStartIndex + 1, oldContent.lastIndexOf("}"));
            content = packageClause + RT_2 + ImportCodeHelper.toString(effectiveImports, oldContent)
                    + middle
                    + BLANK_4 + "//add new mock on " + new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()) + "." + RT_1
                    + mockClassesSb.toString() + "}";

        } else if (!needAppend) {
            needWrite = true;
            sb.append(mockClassesSb);
            sb.append("}");

            String importString = "package " + packageName + ";" + RT_2;

            if (!mockClassesSb.toString().isEmpty()) {
                importString = importSb.toString();
            } else {
                if (hasCtorCode) {
                    importString = importSb.toString();
                    if (!ctorCodeExistMap) {
                        importString = importString.replace("import java.util.Map;" + RT_1, "");
                    }
                } else {
                    importString += "import mockit.Deencapsulation;" + RT_1;
                    importString += "import java.lang.reflect.Field;" + RT_1;
                    importString += "import java.lang.reflect.Modifier;" + RT_1;
                }
            }
            content = importString + RT_1 + sb.toString();
        }
        if (needWrite) {
            IoUtil.writeFile(saveFilePath, content);
        }

    }

    public static String getBoxedTypeName(String type) {
        Class result;
        switch (type) {
            case "int":
                result = Integer.class;
                break;
            case "long":
                result = Long.class;
                break;
            case "short":
                result = Short.class;
                break;
            case "char":
                result = Character.class;
                break;
            case "float":
                result = Float.class;
                break;
            case "double":
                result = Double.class;
                break;
            case "boolean":
                result = Boolean.class;
                break;
            case "byte":
                result = Byte.class;
                break;
            default:
                throw new RuntimeException("unexpected type: " + type);
        }
        return result.getSimpleName();
    }

}
