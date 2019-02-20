package com.github.xg.utgen.core;

import com.github.xg.utgen.core.data.PrimitiveType;
import com.github.xg.utgen.core.data.WrapperType;
import com.github.xg.utgen.core.model.ConstructHierarchy;
import com.github.xg.utgen.core.util.Tuple;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.*;
import java.util.*;

import static com.github.xg.utgen.core.util.Formats.BLANK_4;


/**
 * Created by yuxiangshi on 2017/8/29.
 */
public class TypeValueCode {

    private final List<String> dependencyClasses;
    private final String typeName;
    List<String> clauses = new ArrayList<>();
    private static List<String> supportTypes = new ArrayList<>();
    private ImplementFinder implementFinder;
    private ConstructHierarchy constructRoot;
    boolean endConstruct = false;
    private VariableNames variableNames;

    static {
        supportTypes.add("Collection");
        supportTypes.add("Map");
        supportTypes.add("List");
        supportTypes.add("Set");
    }

    public static void main(String[] args) {
        String type = "java.util.Set<T>";
        //String type = "List<HashMap<Integer, List<Map<Integer, Collection<String>>>>>";
        //String type = "LinkedList<Thread>";
        //String type = "int[][][][]";
        //String type = "java.util.Map<? super Integer,? extends Object>";

        final List<String> strings = Lists.newArrayList(LinkedList.class.getName(), Thread.class.getName(), HashMap.class.getName(), List.class.getName(), Integer.class.getName(), Map.class.getName(), Collection.class.getName(), String.class.getName());
        TypeValueCode clause = new TypeValueCode(type, strings, new ImplementFinder(null), new VariableNames());

        final String infer = clause.infer(null);
        clause.clauses.forEach(e -> System.out.println(e));

        System.out.println(Joiner.on(", ").join(new String[]{"a", "b"}));
        System.out.println(Strings.repeat("[]", 0));
        System.out.println(Collection.class.isAssignableFrom(Stack.class));

    }

    public TypeValueCode(String typeName, List<String> dependencyClasses, ImplementFinder implementFinder, VariableNames variableNames) {
        this.typeName = typeName;
        this.dependencyClasses = dependencyClasses;
        this.implementFinder = implementFinder;
        this.variableNames = variableNames;

        Tuple tuple = splitType(typeName);
        String outerType = tuple.first();
        constructRoot = new ConstructHierarchy(getClass(outerType));
    }

    private boolean supportOrNot(String type) {
        return supportTypes.contains(type);
    }

    public String infer(String expectVarName) {
        return get(typeName, constructRoot, expectVarName);
    }

    public void complexParameter(List<HashMap<Integer, List<Map<Integer, Collection<String>>>>> hashMapList, Map<? super Object, Set<? extends Map>> map) {
        System.out.println(map.getClass().toString() + hashMapList.getClass());

    }

    public String get(String type, ConstructHierarchy constructHierarchy, String expectVarName) {
        String formerVarName_1 = null;
        String formerVarName_2 = null;

        Tuple tuple = splitType(type);
        String outerType = tuple.first();
        String innerType = tuple.second();

        int dimension = tuple.third();
        Class c = getClass(outerType);
        if (c == null) {
            //System.out.println("can not get class :" + outerType);
            return null;
        }

        if (endConstruct) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            clauses.add(tp.second() + "null;");
            endConstruct = false;
            return tp.first();
        }

        if (innerType != null) {
            String[] types = new String[2];
            types[0] = innerType;
            if (!innerType.contains(",")) {
                //types[0] = innerType;
                types[1] = null;
            } else {
                int ltCnt = 0;
                for (int i = 0; i < innerType.length(); i++) {
                    char cha = innerType.charAt(i);
                    if (cha == ',' && ltCnt == 0) {
                        types[0] = innerType.substring(0, i).trim();
                        types[1] = innerType.substring(i + 1).trim();
                        break;
                    }
                    if (cha == '<') {
                        ltCnt++;
                    } else if (cha == '>') {
                        ltCnt--;
                    }
                }
            }

            if (types[1] == null) {
                formerVarName_1 = get(types[0], constructHierarchy, null);
            } else {
                formerVarName_1 = get(types[0], constructHierarchy, null);
                formerVarName_2 = get(types[1], constructHierarchy, null);
            }
        }
        String currentVarName;

        outerType = outerType.contains(".") ? outerType.substring(outerType.lastIndexOf(".") + 1) : outerType;
        //isArray
        if (dimension > 0) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            clauses.add((tp.second() + "new " + outerType + "[1]" + Strings.repeat("[]", dimension - 1) + ";"));
            clauses.add((currentVarName = tp.first()) + "[0] = " + formerVarName_1 + ";");

        } else if (Collection.class.isAssignableFrom(c)) {
            addJavaUtilPackage();
            if (outerType.endsWith("List") || outerType.endsWith("Collection")) {
                if (outerType.equals("List") || outerType.equals("AbstractList") || outerType.equals("Collection")) {
                    Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                    currentVarName = tp.first();
                    clauses.add(tp.second() + "new ArrayList<>();");
                } else {
                    currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
                }

            } else if (outerType.endsWith("Set")) {
                if (outerType.equals("Set") || outerType.equals("AbstractSet")) {
                    Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                    currentVarName = tp.first();
                    clauses.add(tp.second() + "new HashSet<>();");
                } else {
                    currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
                }

            } else if (outerType.endsWith("Queue")) {
                if (outerType.equals("Queue") || outerType.equals("AbstractQueue")) {
                    Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                    currentVarName = tp.first();
                    clauses.add(tp.second() + "new LinkedList<>();");
                } else {
                    currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
                }
            } else {
                currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
            }
            clauses.add(currentVarName + ".add(" + formerVarName_1 + ");");

        } else if (Map.class.isAssignableFrom(c) || Dictionary.class.isAssignableFrom(c)) {
            addJavaUtilPackage();
            if (outerType.endsWith("Map")) {
                if (outerType.equals("Map") || outerType.equals("AbstractMap") || outerType.equals("HashMap")) {
                    Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                    currentVarName = tp.first();
                    clauses.add(tp.second() + "new HashMap<>();");
                } else {
                    currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
                }
            } else if (outerType.equals("Dictionary") || outerType.endsWith("Hashtable")) {
                Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                currentVarName = tp.first();
                clauses.add(tp.second() + "new Hashtable<>();");
            } else {
                currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
            }

            if (formerVarName_1 != null || formerVarName_2 != null || c.getName().startsWith("java.util")) {
                clauses.add(currentVarName + ".put(" + formerVarName_1 + ", " + formerVarName_2 + ");");
            }

        } else if (c.isPrimitive()) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            currentVarName = tp.first();
            clauses.add(tp.second() + PrimitiveType.get(c) + ";");
        } else if (WrapperType.contains(c)) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            currentVarName = tp.first();
            clauses.add(tp.second() + WrapperType.getValue(c) + ";");
        } else if (c.isEnum()) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            currentVarName = tp.first();
            Field[] fields = c.getFields();
            String enumTypeName = tp.second().toString().split(" ")[0];
            if ("Enum".equals(enumTypeName) || fields.length == 0) {
                clauses.add(tp.second() + "null;");
            } else {
                clauses.add(tp.second() + enumTypeName + "." + fields[0].getName() + ";");
            }
        } else {
            currentVarName = other(c, type, dimension, constructHierarchy, expectVarName);
        }

        return currentVarName;
    }

    private void addJavaUtilPackage() {
        if (!dependencyClasses.contains("java.util.*")) {
            dependencyClasses.add("java.util.*");
        }
    }

    private Tuple getDeclaration(Class c, String type, int dimension, String expectVarName) {
        String currentVarName;
        if (expectVarName == null) {
            if (dimension > 0) {
                currentVarName = variableNames.get(Array.newInstance(c, 0).getClass());
            } else {
                currentVarName = variableNames.get(c);
            }
        } else {
            currentVarName = variableNames.get(expectVarName);
        }

        //better simplify all class eg. java.util.List<java.lang.String> --> List<String>
        int i = type.indexOf("<");
        String type1 = type;

        boolean hasDollarCharacter = false;
        if (i != -1) {
            type1 = type.substring(0, i);

            int $index = type.indexOf("$");
            if ($index > i) {
                hasDollarCharacter = true;
            }
        }

        Class c1 = c;
        while (c1.isArray()) {
            c1 = c1.getComponentType();
        }

        Class outClass = c1.getEnclosingClass();
        if (outClass != null) {
            String outClassName = outClass.getTypeName();
            String s = c.getTypeName().substring(0, outClassName.length()) + "." + c.getTypeName().substring(outClassName.length() + 1);
            return Tuple.of(currentVarName, s + " " + currentVarName + " = ", false);
        }

        boolean useSimpleName = true;
        if (!dependencyClasses.contains(c1.getName())) {
            useSimpleName = false;
            //declareType = c.getTypeName();
        }

        String declareType;
        if (!useSimpleName) {
            declareType = type;
        } else {
            declareType = type.substring(type1.lastIndexOf('.') + 1);
        }

        if (hasDollarCharacter) {
            declareType = declareType.substring(0, declareType.indexOf("<"));
        }
        return Tuple.of(currentVarName, declareType + " " + currentVarName + " = ", useSimpleName);
    }


    private String other(Class c, String type, int dimension, ConstructHierarchy constructHierarchy, String expectVarName) {
        Class imp;
        String varName;
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
            imp = scanImp(c);
            if (imp != null) {
                c = imp;
                addDependency(c);
            } else {
                type = type.contains("<") ? type.substring(0, type.indexOf("<")) : type;
                Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                varName = tp.first();
                clauses.add(tp.second() + "null;");
                return varName;
            }
        }
        varName = useConstructor(c, type, dimension, constructHierarchy, expectVarName);
        return varName;

    }

    private String useConstructor(Class c, String type, int dimension, ConstructHierarchy constructHierarchy, String expectVarName) {
        String varName;
        if (c.getSimpleName().equals("String")) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            varName = tp.first();
            clauses.add(tp.second() + "\"hello\";");
            return varName;
        }

        //inner class
        if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) {
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            varName = tp.first();
            clauses.add(tp.second() + "null;");
            return varName;
        }
        if (c.getConstructors().length > 0) {
            Constructor ctor = MethodHelper.chooseConstructor(c.getConstructors(), TestProject.preferConstructorWithParams);
            if (ctor != null) {
                String[] expectedVarNames = MethodHelper.getExpectedVarNames(ctor, c);

                Class[] ctorParams = ctor.getParameterTypes();
                addDependency(ctorParams);

                String[] ctorArgs = new String[ctorParams.length];
                for (int i = 0; i < ctorParams.length; i++) {
                    ConstructHierarchy child = new ConstructHierarchy(ctorParams[i]);
                    child.setParent(constructHierarchy);
                    constructHierarchy.addChildren(child);
                    if (child.repeatWithParent()) {
                        endConstruct = true;
                    }
                    ctorArgs[i] = get(ctorParams[i].getTypeName(), child, expectedVarNames[i]);
                }
                Tuple tp = getDeclaration(c, type, dimension, expectVarName);
                varName = tp.first();
                String s = tp.third() ? c.getSimpleName() : getName(c);

                String ex = MethodHelper.needTryCatch(ctor);
                if (ex != null) {
                    clauses.add(tp.second() + "null;");
                    clauses.add("try {");
                    clauses.add(BLANK_4 + varName + " = new " + s + "(" + Joiner.on(", ").join(ctorArgs) + ");");
                    String exceptionVarName = variableNames.get("ex", false);
                    clauses.add("} catch (" + ex + " " + exceptionVarName + ") {");
                    clauses.add(BLANK_4 + exceptionVarName + ".printStackTrace();");
                    clauses.add(BLANK_4 + "fail(\"Unexpected exception: \"" + " + " + exceptionVarName + ");");
                    clauses.add("}");
                } else {
                    clauses.add(tp.second() + "new " + s + "(" + Joiner.on(", ").join(ctorArgs) + ");");
                }
                return varName;
            }

        }

        Method factoryMethod;
        if ((factoryMethod = getFactoryMethod(c)) != null) {
            String[] expectedVarNames = MethodHelper.getExpectedVarNames(factoryMethod, c);
            Class[] params = factoryMethod.getParameterTypes();
            addDependency(params);

            String[] args = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                ConstructHierarchy child = new ConstructHierarchy(params[i]);
                child.setParent(constructHierarchy);
                constructHierarchy.addChildren(child);
                if (child.repeatWithParent()) {
                    endConstruct = true;
                }
                args[i] = get(params[i].getTypeName(), child, expectedVarNames[i]);
            }
            Tuple tp = getDeclaration(c, type, dimension, expectVarName);
            varName = tp.first();
            String typeName = tp.third() ? c.getSimpleName() : getName(c);

            String ex = MethodHelper.needTryCatch(factoryMethod);
            if (ex != null) {
                clauses.add(tp.second() + "null;");
                clauses.add("try {");
                clauses.add(BLANK_4 + varName + " = " + typeName + "." + factoryMethod.getName() + "(" + Joiner.on(", ").join(args) + ");");
                String exceptionVarName = variableNames.get("ex", false);
                clauses.add("} catch (" + ex + " " + exceptionVarName + ") {");
                clauses.add(BLANK_4 + exceptionVarName + ".printStackTrace();");
                clauses.add(BLANK_4 + "fail(\"Unexpected exception: \"" + " + " + exceptionVarName + ");");
                clauses.add("}");
            } else {
                clauses.add(tp.second() + typeName + "." + factoryMethod.getName() + "(" + Joiner.on(", ").join(args) + ");");
            }

            return varName;
        }
        Tuple tp = getDeclaration(c, type, dimension, expectVarName);
        varName = tp.first();
        clauses.add(tp.second() + "null;");
        return varName;

    }

    String getName(Class<?> cls) {
        String s = cls.getTypeName();
        Class outClass = cls.getEnclosingClass();
        if (outClass != null) {
            String outClassName = outClass.getTypeName();
            s = cls.getTypeName().substring(0, outClassName.length()) + "." + cls.getTypeName().substring(outClassName.length() + 1);
        }
        return s;
    }

    private Method getFactoryMethod(Class c) {
        for (Method method : c.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                if (method.getReturnType().equals(c)) {
                    if (method.getGenericReturnType() instanceof ParameterizedTypeImpl) {
                        return null;
                    }
                    return method;
                }
            }
        }
        return null;
    }

    private Tuple splitType(String type) {
        String outer, inner;
        int dimension = 0;

        if (type.endsWith("[]")) {
            String typeWithOutSpace = type.replace(" ", "");
            for (int i = typeWithOutSpace.length() - 1; i >= 0; i -= 2) {
                if (typeWithOutSpace.charAt(i) == ']') {
                    dimension++;
                } else {
                    break;
                }
            }

            if (type.contains("<")) {
                outer = type.substring(0, type.indexOf('<'));
            } else {
                outer = type.substring(0, type.indexOf('['));
            }
            inner = type.substring(0, type.lastIndexOf('['));

        } else if (type.contains("<")) {
            outer = type.substring(0, type.indexOf('<'));
            inner = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
        } else {
            outer = type;
            inner = null;

        }
        return Tuple.of(outer, inner, dimension);

    }

    private void addDependency(Class... classes) {
        for (int i = 0; i < classes.length; i++) {
            if (!classes[i].getName().contains(".")) {
                continue;
            }

            Class c = classes[i];
            while (c.isArray()) {
                c = c.getComponentType();
            }

            //nested class
            if (c.getDeclaringClass() != null) {
                return;
            }

            String name = c.getName();
            String shortName = name.substring(name.lastIndexOf(".") + 1);

            boolean b = false;
            for (int j = 0; j < dependencyClasses.size(); j++) {
                String importClass = dependencyClasses.get(j);
                if (importClass.substring(importClass.lastIndexOf(".") + 1).equals(shortName)) {
                    b = true;
                    break;
                }
            }
            if (b) {
                continue;
            }
            dependencyClasses.add(name);
        }
    }

    private Class getClass(String type) {
        Class result = null;
        switch (type) {
            case "int":
                result = int.class;
                break;
            case "long":
                result = long.class;
                break;
            case "short":
                result = short.class;
                break;
            case "char":
                result = char.class;
                break;
            case "float":
                result = float.class;
                break;
            case "double":
                result = double.class;
                break;
            case "boolean":
                result = boolean.class;
                break;
            case "byte":
                result = byte.class;
                break;
            case "String":
                result = String.class;
                break;
        }

        if (result == null) {
            try {
                result = Class.forName(type, false, ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                //System.out.println("can't find class " + type);
            }
        }

        /*if (Objects.equals(null, result)) {
            for (String dependClass : dependencyClasses) {
                String shortName = dependClass.substring(dependClass.lastIndexOf(".") + 1);

                if (dependClass.equals(type) || shortName.equals(type))
                    try {
                        result = Class.forName(dependClass);
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
            }
        }*/

        if (result != null) {
            addDependency(result);
        }
        return result;
    }

    private Class scanImp(Class interfaceClass) {
        return implementFinder.find(interfaceClass);
    }

}
