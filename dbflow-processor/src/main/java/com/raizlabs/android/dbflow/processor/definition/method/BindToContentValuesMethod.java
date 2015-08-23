package com.raizlabs.android.dbflow.processor.definition.method;

import com.raizlabs.android.dbflow.processor.ClassNames;
import com.raizlabs.android.dbflow.processor.definition.BaseTableDefinition;
import com.raizlabs.android.dbflow.processor.definition.column.ColumnDefinition;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.lang.Override;import java.lang.String;import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Description: Writes the bind to content values method in the ModelDAO.
 */
public class BindToContentValuesMethod implements MethodDefinition {

    public static final String PARAM_CONTENT_VALUES = "values";
    public static final String PARAM_MODEL = "model";

    private BaseTableDefinition baseTableDefinition;

    public BindToContentValuesMethod(BaseTableDefinition baseTableDefinition) {
        this.baseTableDefinition = baseTableDefinition;
    }

    @Override
    public MethodSpec getMethodSpec() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bindToContentValues")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ClassNames.CONTENT_VALUES, PARAM_CONTENT_VALUES)
                .addParameter(baseTableDefinition.elementClassName, PARAM_MODEL)
                .returns(TypeName.VOID);

        List<ColumnDefinition> columnDefinitionList = baseTableDefinition.getColumnDefinitions();
        for (ColumnDefinition columnDefinition : columnDefinitionList) {
            methodBuilder.addCode(columnDefinition.getContentValuesStatement());
        }

        return methodBuilder.build();
    }
}
