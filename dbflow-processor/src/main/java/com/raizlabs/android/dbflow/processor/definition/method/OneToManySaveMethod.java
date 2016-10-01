package com.raizlabs.android.dbflow.processor.definition.method;

import com.raizlabs.android.dbflow.processor.ClassNames;
import com.raizlabs.android.dbflow.processor.definition.OneToManyDefinition;
import com.raizlabs.android.dbflow.processor.definition.TableDefinition;
import com.raizlabs.android.dbflow.processor.utils.ModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

/**
 * Description: Overrides the save, update, and insert methods if the {@link com.raizlabs.android.dbflow.annotation.OneToMany.Method#SAVE} is used.
 */
public class OneToManySaveMethod implements MethodDefinition {
    public static final String METHOD_SAVE = "save";
    public static final String METHOD_UPDATE = "update";
    public static final String METHOD_INSERT = "insert";

    private final TableDefinition tableDefinition;
    private final boolean isModelContainerAdapter;
    private final String methodName;
    private final boolean useWrapper;

    public OneToManySaveMethod(TableDefinition tableDefinition,
                               boolean isModelContainerAdapter, String methodName, boolean useWrapper) {
        this.tableDefinition = tableDefinition;
        this.isModelContainerAdapter = isModelContainerAdapter;
        this.methodName = methodName;
        this.useWrapper = useWrapper;
    }

    @Override
    public MethodSpec getMethodSpec() {
        if (!tableDefinition.getOneToManyDefinitions().isEmpty() || !isModelContainerAdapter && tableDefinition.getCachingEnabled()) {
            CodeBlock.Builder code = CodeBlock.builder();
            for (OneToManyDefinition oneToManyDefinition : tableDefinition.getOneToManyDefinitions()) {
                switch (methodName) {
                    case METHOD_SAVE:
                        oneToManyDefinition.writeSave(code, useWrapper);
                        break;
                    case METHOD_UPDATE:
                        oneToManyDefinition.writeUpdate(code, useWrapper);
                        break;
                    case METHOD_INSERT:
                        oneToManyDefinition.writeInsert(code, useWrapper);
                        break;
                }
            }

            code.addStatement("super.$L($L$L)", methodName,
                    ModelUtils.getVariable(),
                    useWrapper ? (", " + ModelUtils.getWrapper()) : "");

            if (!isModelContainerAdapter && tableDefinition.getCachingEnabled()) {
                code.addStatement("getModelCache().addModel(getCachingId($L), $L)", ModelUtils.getVariable(),
                        ModelUtils.getVariable());
            }

            MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(tableDefinition.getElementClassName(), ModelUtils.getVariable())
                    .addCode(code.build());
            if (useWrapper) {
                builder.addParameter(ClassNames.DATABASE_WRAPPER, ModelUtils.getWrapper());
            }

            return builder.build();
        } else {
            return null;
        }
    }
}
