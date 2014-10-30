package com.grosner.processor.definition;

import com.google.common.collect.Sets;
import com.grosner.processor.Classes;
import com.grosner.processor.model.ProcessorManager;
import com.grosner.processor.writer.*;
import com.squareup.javawriter.JavaWriter;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
public class ModelContainerDefinition implements FlowWriter {

    public static final String DBFLOW_MODEL_CONTAINER_TAG = "$Container";

    private final TypeElement classElement;

    private final ProcessorManager manager;

    private String sourceFileName;

    private String packageName;

    private ContentValuesWriter mContentValuesWriter;

    private ExistenceWriter mExistenceWriter;

    private WhereQueryWriter mWhereQueryWriter;

    private ToModelWriter mToModelWriter;

    private LoadCursorWriter mLoadCursorWriter;

    private DeleteWriter mDeleteWriter;

    public ModelContainerDefinition(String packageName, TypeElement classElement, ProcessorManager manager) {
        this.classElement = classElement;
        this.manager = manager;
        this.sourceFileName = classElement.getSimpleName().toString() + DBFLOW_MODEL_CONTAINER_TAG;
        this.packageName = packageName;

        TableDefinition tableDefinition = manager.getTableDefinition(classElement);
        mContentValuesWriter = new ContentValuesWriter(tableDefinition, true);
        mExistenceWriter = new ExistenceWriter(tableDefinition, true);
        mWhereQueryWriter = new WhereQueryWriter(tableDefinition, true);
        mToModelWriter = new ToModelWriter(tableDefinition);
        mLoadCursorWriter = new LoadCursorWriter(tableDefinition, true);
        mDeleteWriter = new DeleteWriter(tableDefinition, true);
    }


    @Override
    public void write(JavaWriter javaWriter) throws IOException {
        javaWriter.emitPackage(packageName);
        javaWriter.emitImports(
                Classes.FLOW_MANAGER,
                Classes.CONDITION_QUERY_BUILDER,
                Classes.MODEL_CONTAINER,
                Classes.MODEL_CONTAINER_UTILS,
                Classes.CONTAINER_ADAPTER,
                Classes.MODEL,
                Classes.CONTENT_VALUES,
                Classes.CURSOR,
                Classes.SQL_UTILS,
                Classes.SELECT,
                Classes.DELETE,
                Classes.CONDITION,
                Classes.TRANSACTION_MANAGER,
                Classes.PROCESS_MODEL_INFO,
                Classes.DBTRANSACTION_INFO
        );
        javaWriter.beginType(sourceFileName, "class", Sets.newHashSet(Modifier.PUBLIC, Modifier.FINAL), "ContainerAdapter<" + classElement.getSimpleName() + ">");
        InternalAdapterHelper.writeGetModelClass(javaWriter, getModelClassQualifiedName());
        InternalAdapterHelper.writeGetTableName(javaWriter, classElement.getSimpleName().toString() + TableDefinition.DBFLOW_TABLE_TAG);

        mContentValuesWriter.write(javaWriter);
        mExistenceWriter.write(javaWriter);
        mWhereQueryWriter.write(javaWriter);
        mToModelWriter.write(javaWriter);
        mLoadCursorWriter.write(javaWriter);
        mDeleteWriter.write(javaWriter);

        javaWriter.endType();
        javaWriter.close();
    }

    public String getModelClassQualifiedName() {
        return classElement.getQualifiedName().toString();
    }

    public String getFQCN() {
        return packageName+ "." + sourceFileName;
    }

}
