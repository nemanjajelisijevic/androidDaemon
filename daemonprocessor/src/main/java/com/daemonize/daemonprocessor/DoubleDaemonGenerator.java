package com.daemonize.daemonprocessor;

import com.daemonize.daemonprocessor.annotations.CallingThread;
import com.daemonize.daemonprocessor.annotations.Daemonize;
import com.daemonize.daemonprocessor.annotations.SideQuest;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class DoubleDaemonGenerator extends BaseDaemonGenerator {

    protected MainQuestDaemonGenerator mainGenerator;
    protected SideQuestDaemonGenerator sideGenerator;

    private final String MAIN_DAEMON_ENGINE_STRING = "mainDaemonEngine";
    private final String SIDE_DAEMON_ENGINE_STRING = "sideDaemonEngine";

    public DoubleDaemonGenerator(TypeElement classElement) {
        super(classElement);
        this.mainGenerator = new MainQuestDaemonGenerator(
                classElement,
                true,
                classElement.getAnnotation(Daemonize.class).consumer(),
                classElement.getAnnotation(Daemonize.class).markDaemonMethods()
        );
        this.sideGenerator = new SideQuestDaemonGenerator(classElement);

        mainGenerator.setDaemonEngineString(MAIN_DAEMON_ENGINE_STRING).setAutoGenerateApiMethods(false);
        sideGenerator.setDaemonEngineString(SIDE_DAEMON_ENGINE_STRING).setAutoGenerateApiMethods(false);
    }


    @Override
    public TypeSpec generateDaemon(List<ExecutableElement> publicPrototypeMethods) {

        daemonInterface = ClassName.get(
                DAEMON_ENGINE_PACKAGE_ROOT,
                "EagerDaemon"
        );

        daemonClassName = ClassName.get(packageName, daemonSimpleName);

        TypeSpec.Builder daemonClassBuilder = TypeSpec.classBuilder(daemonSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(daemonInterface, daemonClassName));

        implementInterfaces(daemonClassBuilder, daemonClassName.box());

//        for (TypeElement intf : interfaces) {
//            TypeMirror intfMirror = intf.asType();
//
//            printer.print("Interface: " + intf.toString());
//
//            List<? extends TypeParameterElement> typeParams = intf.getTypeParameters();
//
//            boolean builder = false;
//
//            if (!typeParams.isEmpty()) {
//
//                for (TypeParameterElement type : typeParams) {
//
//                    printer.print(type.getBounds().get(0).toString());
//
//                    if (!type.getBounds().isEmpty() && type.getBounds().get(0).getClass().equals(intfMirror.getClass())) {// builder
//                        builder = true;
//                        printer.print("BUILDER TRUE");
//                    } else {
//                        daemonClassBuilder.addTypeVariable(TypeVariableName.get(type));
//                        printer.print("BUILDER FALSE");
//                    }
//                }
//            }
//
//            daemonClassBuilder.addSuperinterface(builder ? ParameterizedTypeName.get(ClassName.get(intf), daemonClassName.box()) : TypeName.get(intf.asType()));
//        }

//        for (TypeElement intf : interfaces)
//            daemonClassBuilder.addSuperinterface(TypeName.get(intf.asType()));

        //daemonClassBuilder.addSuperinterfaces(interfaces);

        if (mainGenerator.isConsumer())
            daemonClassBuilder.addSuperinterface(consumerInterface);

        daemonClassBuilder = addTypeParameters(classElement, daemonClassBuilder);

        //private field for prototype
        FieldSpec prototype = FieldSpec.builder(
                ClassName.get(classElement.asType()),
                PROTOTYPE_STRING
        ).addModifiers(Modifier.PRIVATE).build();

        //main quest daemon engine
        ClassName mainDaemonEngineClass = ClassName.get(
                mainGenerator.getDaemonPackage(),
                mainGenerator.getDaemonEngineSimpleName()
        );

        FieldSpec mainDaemonEngine = FieldSpec.builder(
                mainDaemonEngineClass,
                MAIN_DAEMON_ENGINE_STRING
        )
        .addModifiers(Modifier.PROTECTED)
        .build();

        //side quest daemon engine
        ClassName sideDaemonEngineClass = ClassName.get(
                sideGenerator.getDaemonPackage(),
                sideGenerator.getDaemonEngineSimpleName()
        );

        FieldSpec sideDaemonEngine = FieldSpec.builder(
                sideDaemonEngineClass,
                SIDE_DAEMON_ENGINE_STRING
        )
        .addModifiers(Modifier.PROTECTED)
        .build();

        daemonClassBuilder.addField(prototype);
        daemonClassBuilder.addField(mainDaemonEngine);
        daemonClassBuilder.addField(sideDaemonEngine);

        //daemon construct
        MethodSpec.Builder daemonConstructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(consumer, "consumer")
                //.addParameter(consumer, "sideConsumer")
                .addParameter(ClassName.get(classElement.asType()), PROTOTYPE_STRING)
                .addStatement("this.$N = new $N(consumer).setName(this.getClass().getSimpleName())", MAIN_DAEMON_ENGINE_STRING, mainGenerator.getDaemonEngineSimpleName())
                .addStatement("this.$N = new $N().setName(this.getClass().getSimpleName() + \" - SIDE\")", SIDE_DAEMON_ENGINE_STRING, sideGenerator.getDaemonEngineSimpleName());


        //add dedicated daemon engines
        Set<String> dedNameSet = new HashSet<>(mainGenerator.dedicatedEnginesNameSet);

        for (Map.Entry<ExecutableElement, Pair<String, FieldSpec>> entry : mainGenerator.getDedicatedThreadEngines().entrySet()) {
            if (dedNameSet.contains(entry.getValue().getFirst())) {
                daemonClassBuilder.addField(entry.getValue().getSecond());
                daemonConstructorBuilder.addStatement(
                        "this." + entry.getValue().getFirst() +
                                " = new $N(consumer).setName(this.getClass().getSimpleName() + \" - "
                                + entry.getValue().getFirst() + "\")",
                        mainGenerator.getDaemonEngineSimpleName()
                );

                dedNameSet.remove(entry.getValue().getFirst());
            }
        }

        MethodSpec daemonConstructor = daemonConstructorBuilder
                .addStatement("this.$N = $N", PROTOTYPE_STRING, PROTOTYPE_STRING)
                .build();

        daemonClassBuilder.addMethod(daemonConstructor);

        //sideQuest daemon fields and methods
        List<Pair<ExecutableElement, SideQuest>> sideQuests
                = getSideQuestMethods(publicPrototypeMethods);

        List<Pair<TypeSpec, MethodSpec>> sideQuestFields = new ArrayList<>();

        for (Pair<ExecutableElement, SideQuest> sideQuestPair : sideQuests) {
            sideQuestFields.add(sideGenerator.createSideQuest(sideQuestPair));
        }

        //add side quest setters
        for (Pair<TypeSpec, MethodSpec> sideQuestField : sideQuestFields) {
            daemonClassBuilder.addMethod(sideQuestField.getSecond());
        }

        Map<TypeSpec, MethodSpec> mainQuestsAndApiMethods = new LinkedHashMap<>();

        for (ExecutableElement method : publicPrototypeMethods) {

            PrototypeMethodData overridenMethodData = new PrototypeMethodData(method);

            if (method.getAnnotation(CallingThread.class) != null || overriddenMethods.contains(overridenMethodData)) {
                daemonClassBuilder.addMethod(overriddenMethods.contains(overridenMethodData) ? mainGenerator.wrapIntfMethod(method) : mainGenerator.wrapMethod(method));
                continue;
            }

            if (mainGenerator.getDedicatedThreadEngines().containsKey(method)) {
                mainQuestsAndApiMethods.put(
                        mainGenerator.createMainQuest(method),
                        mainGenerator.createApiMethod(
                                method,
                                mainGenerator.getDedicatedThreadEngines().get(method).getFirst()
                        )
                );
            } else {
                mainQuestsAndApiMethods.put(
                        mainGenerator.createMainQuest(method),
                        mainGenerator.createApiMethod(method, mainGenerator.getDaemonEngineString())
                );
            }
        }

        if (!sideQuestFields.isEmpty()) {
            daemonClassBuilder.addMethod(sideGenerator.generateCurrentSideQuestGetter());
        }

        //add side quests
        for (Pair<TypeSpec, MethodSpec> sideQuestField : sideQuestFields) {
            daemonClassBuilder.addType(sideQuestField.getFirst());
        }

        //add main quest methods
        for (Map.Entry<TypeSpec, MethodSpec> entry : mainQuestsAndApiMethods.entrySet()) {
            daemonClassBuilder.addMethod(entry.getValue());
        }

        //Add API METHODS
        List<MethodSpec> apiMethods = new ArrayList<>(7);

        apiMethods.add(generateGetPrototypeDaemonApiMethod());
        apiMethods.add(generateSetPrototypeDaemonApiMethod());
        apiMethods.add(generateStartDaemonApiMethod());
        apiMethods.add(generateStopDaemonApiMethod());
        apiMethods.add(generateQueueStopDaemonApiMethod());//TODO override !!!!!!!!!!!!!!!!!!!!!!!!!!
        apiMethods.add(generateClearDaemonApiMethod());
        apiMethods.add(generateGetEnginesStateDaemonApiMethod());
        apiMethods.add(generateGetEnginesQueueSizeDaemonApiMethod());
        apiMethods.add(generateSetNameDaemonApiMethod());
        apiMethods.add(mainGenerator.generateGetNameDaemonApiMethod());//TODO CHECK THISSS!!!!!!!
        apiMethods.add(generateSetMainConsumerDaemonApiMethod());
        apiMethods.add(generateSetSideConsumerDaemonApiMethod());
        apiMethods.add(generateSetConsumerDaemonApiMethod());
        apiMethods.add(mainGenerator.generateGetConsumerDaemonApiMethod());

        apiMethods.add(mainGenerator.generateInterruptMethod());
        apiMethods.add(mainGenerator.generateClearAndInterruptMethod());

        if (mainGenerator.isConsumer())
            apiMethods.add(mainGenerator.generateConsumeMethod());

        for(MethodSpec apiMethod : apiMethods) {
            daemonClassBuilder.addMethod(apiMethod);
        }

        //add main quests
        for (Map.Entry<TypeSpec, MethodSpec> entry : mainQuestsAndApiMethods.entrySet()) {
            daemonClassBuilder.addType(entry.getKey());
        }

        return daemonClassBuilder.build();

    }


    @Override
    public MethodSpec generateStartDaemonApiMethod() {
        MethodSpec.Builder methodSpecBuilder =  MethodSpec.methodBuilder("start")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement(MAIN_DAEMON_ENGINE_STRING + ".start()");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            methodSpecBuilder.addStatement(dedicatedEngine + ".start()");

        methodSpecBuilder.addStatement(SIDE_DAEMON_ENGINE_STRING + ".start()")
                .addStatement("return this");
        return methodSpecBuilder.build();
    }

    @Override
    public MethodSpec generateStopDaemonApiMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("stop")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addStatement(mainGenerator.getDaemonEngineString() + ".stop()")
                .addStatement(sideGenerator.getDaemonEngineString() + ".stop()");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement(dedicatedEngine + ".stop()");

        return builder.build();
    }

    @Override
    public MethodSpec generateQueueStopDaemonApiMethod() {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("queueStop")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(daemonClassName)
                .addStatement(mainGenerator.getDaemonEngineString() + ".queueStop(this)")
                .addStatement("return this");

        return builder.build();
    }

    @Override
    public MethodSpec generateSetNameDaemonApiMethod() {

        MethodSpec.Builder builder =  MethodSpec.methodBuilder("setName")
                .addParameter(String.class, "name")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement(mainGenerator.getDaemonEngineString() + ".setName(name)")
                .addStatement(sideGenerator.getDaemonEngineString() + ".setName(name + \" - SIDE\")");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement(dedicatedEngine + ".setName(name + \" - " + dedicatedEngine + "\")");

        return builder.addStatement("return this").build();
    }

    public MethodSpec generateSetMainConsumerDaemonApiMethod() {
        MethodSpec.Builder builder =   MethodSpec.methodBuilder("setMainQuestConsumer")
                .addParameter(consumer, "consumer")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement(mainGenerator.getDaemonEngineString() + ".setConsumer(consumer)");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement(dedicatedEngine + ".setConsumer(consumer)");

        return builder.addStatement("return this").build();
    }

    public MethodSpec generateSetSideConsumerDaemonApiMethod() {
        return MethodSpec.methodBuilder("setSideQuestConsumer")
                .addParameter(consumer, "consumer")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement(sideGenerator.getDaemonEngineString() + ".setConsumer(consumer)")
                .addStatement("return this")
                .build();
    }

    @Override
    public MethodSpec generateSetConsumerDaemonApiMethod() { //TODO check whether to throw or set consumer to both engines
        return MethodSpec.methodBuilder("setConsumer")
                .addParameter(consumer, "consumer")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement("throw new $T(\"This method is unusable in DoubleDaemon. Please use setMainQuestConsumer(Consumer consumer) or setSideQuestConsumer(Consumer consumer)\")", IllegalStateException.class)
                .build();
    }


    @Override
    public MethodSpec generateGetEnginesStateDaemonApiMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getEnginesState")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), daemonStateClassName))
                .addStatement("$T ret = new $T()", ParameterizedTypeName.get(ClassName.get(List.class), daemonStateClassName), ParameterizedTypeName.get(ClassName.get(ArrayList.class), daemonStateClassName))
                .addStatement("ret.add(" + mainGenerator.getDaemonEngineString() + ".getState())");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement("ret.add(" + dedicatedEngine + ".getState())");

        return builder.addStatement("ret.add(" + sideGenerator.getDaemonEngineString() +".getState())")
                .addStatement("return ret")
                .build();
    }

    @Override
    public MethodSpec generateGetEnginesQueueSizeDaemonApiMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getEnginesQueueSizes")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Integer.class)))
                .addStatement("$T ret = new $T()", ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Integer.class)), ParameterizedTypeName.get(ClassName.get(ArrayList.class), ClassName.get(Integer.class)))
                .addStatement("ret.add(" + mainGenerator.getDaemonEngineString() + ".queueSize())");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement("ret.add(" + dedicatedEngine + ".queueSize())");

        return builder.addStatement("return ret")
                .build();
    }

    public MethodSpec generateClearDaemonApiMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("clear")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(packageName, daemonSimpleName))
                .addStatement(mainGenerator.getDaemonEngineString() + ".clear()");

        for (String dedicatedEngine : mainGenerator.dedicatedEnginesNameSet)
            builder.addStatement(dedicatedEngine + ".clear()");

        return builder.addStatement("return this").build();
    }

}
