package com.raizlabs.android.dbflow.processor.definition.method;

import com.raizlabs.android.dbflow.processor.ClassNames;
import com.raizlabs.android.dbflow.processor.definition.TableDefinition;
import com.raizlabs.android.dbflow.processor.definition.column.ColumnDefinition;
import com.raizlabs.android.dbflow.processor.utils.ModelUtils;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.lang.model.element.Modifier;

/**
 * Description:
 */
public class BindToStatementMethod implements MethodDefinition {

    public static final String PARAM_STATEMENT = "statement";
    public static final String PARAM_MODEL = "model";
    public static final String PARAM_START = "start";

    private TableDefinition tableDefinition;
    private boolean isInsert;
    private boolean isModelContainerAdapter;

    public BindToStatementMethod(TableDefinition tableDefinition, boolean isInsert, boolean isModelContainerAdapter) {

        this.tableDefinition = tableDefinition;
        this.isInsert = isInsert;
        this.isModelContainerAdapter = isModelContainerAdapter;
    }

    @Override
    public MethodSpec getMethodSpec() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(isInsert ? "bindToInsertStatement" : "bindToStatement")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ClassNames.DATABASE_STATEMENT, PARAM_STATEMENT)
                .addParameter(tableDefinition.getParameterClassName(),
                        ModelUtils.getVariable())
                .returns(TypeName.VOID);

        // write the reference method
        if (isInsert) {
            methodBuilder.addParameter(TypeName.INT, PARAM_START);
            List<ColumnDefinition> columnDefinitionList = tableDefinition.getColumnDefinitions();
            AtomicInteger realCount = new AtomicInteger(1);
            for (ColumnDefinition columnDefinition : columnDefinitionList) {
                if (!columnDefinition.isPrimaryKeyAutoIncrement() && !columnDefinition.getIsRowId()) {
                    methodBuilder.addCode(columnDefinition.getSQLiteStatementMethod(realCount));
                    realCount.incrementAndGet();
                }
            }

            if (tableDefinition.getImplementsSqlStatementListener()) {
                methodBuilder.addStatement("$L.onBindTo$LStatement($L)",
                        ModelUtils.getVariable(), isInsert ? "Insert" : "", PARAM_STATEMENT);
            }
        } else {
            int start = 0;
            if (tableDefinition.getHasAutoIncrement() || tableDefinition.getHasRowID()) {
                ColumnDefinition autoIncrement = tableDefinition.getAutoIncrementPrimaryKey();
                methodBuilder.addCode(autoIncrement.getSQLiteStatementMethod(new AtomicInteger(++start)));
            }

            methodBuilder.addStatement("bindToInsertStatement($L, $L, $L)", PARAM_STATEMENT, ModelUtils.getVariable(), start);
            if (tableDefinition.getImplementsSqlStatementListener()) {
                methodBuilder.addStatement("$L.onBindTo$LStatement($L)",
                        ModelUtils.getVariable(), isInsert ? "Insert" : "", PARAM_STATEMENT);
            }
        }

        return methodBuilder.build();
    }
}
