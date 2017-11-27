package com.github.lessjava.visitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.lessjava.types.ast.ASTExpression;
import com.github.lessjava.types.ast.ASTFunction;
import com.github.lessjava.types.ast.ASTFunction.Parameter;
import com.github.lessjava.types.ast.ASTFunctionCall;
import com.github.lessjava.types.ast.ASTProgram;
import com.github.lessjava.types.ast.ASTVoidFunctionCall;
import com.github.lessjava.types.inference.HMType;
import com.github.lessjava.types.inference.impl.HMTypeBase;
import com.github.lessjava.types.inference.impl.HMTypeVar;
import com.github.lessjava.visitor.LJAbstractAssignTypes;

public class LJInstantiateFunctions extends LJAbstractAssignTypes {
    private static ASTProgram program;
    private List<ASTFunction> functionsToAdd;

    @Override
    public void preVisit(ASTProgram node) {
        super.preVisit(node);
        if (LJInstantiateFunctions.program == null) {
            LJInstantiateFunctions.program = node;
        }

        this.functionsToAdd = new ArrayList<>();
    }

    @Override
    public void postVisit(ASTProgram node) {
        super.preVisit(node);

        program.functions.addAll(functionsToAdd);
    }

    @Override
    public void postVisit(ASTFunctionCall node) {
        super.postVisit(node);

        ASTFunction f = nameparamFunctionMap.get(node.getNameArgString());

        if (f != null && f.concrete) {
            return;
        }

        f = instantiateFunction(node.name, node.arguments);

        if (f != null) {
            if (functionsToAdd.contains(f)) {
                return;
            }

            functionsToAdd.add(f);
            nameparamFunctionMap.put(node.getNameArgString(), f);
        }
    }

    @Override
    public void postVisit(ASTVoidFunctionCall node) {
        super.postVisit(node);

        ASTFunction f = nameparamFunctionMap.get(node.getNameArgString());

        if (f != null && f.concrete) {
            return;
        }

        f = instantiateFunction(node.name, node.arguments);

        if (f != null) {
            if (functionsToAdd.contains(f)) {
                return;
            }

            functionsToAdd.add(f);
            nameparamFunctionMap.put(node.getNameArgString(), f);
        }
    }

    private ASTFunction instantiateFunction(String name, List<ASTExpression> arguments) {
        if (arguments.stream().anyMatch(arg -> !arg.type.isConcrete)) {
            // Can't instantiate; arg isn't concrete
            return null;
        }

        List<ASTFunction> functions = program.functions.stream().filter(func -> func.name.equals(name))
                .collect(Collectors.toList());

        Optional<ASTFunction> prototype = functions.stream().filter(func -> !func.concrete).findFirst();

        if (!prototype.isPresent()) {
            // Can't instantiate
            return null;
        }

        ASTFunction functionInstance = new ASTFunction(prototype.get().name, prototype.get().returnType,
                prototype.get().body);

        functionInstance.concrete = true;
        functionInstance.parameters = new ArrayList<>();

        for (int i = 0; i < arguments.size(); i++) {
            String pname = prototype.get().parameters.get(i).name;
            HMType type = new HMTypeBase(((HMTypeBase) arguments.get(i).type).getBaseType());
            Parameter parameter = new Parameter(pname, type);
            functionInstance.parameters.add(parameter);
        }

        for (ASTFunction f : program.functions) {
            if (functionInstance.equals(f)) {
                return null;
            }
        }

        functionInstance.setParent(program);
        functionInstance.setDepth(2);

        return functionInstance;
    }

}
