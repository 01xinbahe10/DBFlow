package com.raizlabs.android.dbflow.processor.definition.column

import com.raizlabs.android.dbflow.annotation.ForeignKeyReference
import com.raizlabs.android.dbflow.processor.model.ProcessorManager
import com.raizlabs.android.dbflow.processor.utils.ElementUtility
import com.raizlabs.android.dbflow.processor.utils.ModelUtils
import com.raizlabs.android.dbflow.sql.QueryBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName

import java.util.concurrent.atomic.AtomicInteger

import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

/**
 * Description:
 */
class ForeignKeyReferenceDefinition {

    private val manager: ProcessorManager
    private val foreignKeyFieldName: String

    val columnName: String
    val foreignColumnName: String
    val columnClassName: TypeName?

    private var isReferencedFieldPrivate: Boolean = false
    private var isReferencedFieldPackagePrivate: Boolean = false

    var columnAccess: BaseColumnAccess? = null

    private val tableColumnAccess: BaseColumnAccess
    private val foreignKeyColumnDefinition: ForeignKeyColumnDefinition

    private val simpleColumnAccess: BaseColumnAccess

    constructor(manager: ProcessorManager, foreignKeyFieldName: String,
                referencedColumn: ColumnDefinition,
                tableColumnAccess: BaseColumnAccess,
                foreignKeyColumnDefinition: ForeignKeyColumnDefinition, referenceCount: Int) {
        this.manager = manager
        this.foreignKeyColumnDefinition = foreignKeyColumnDefinition
        this.tableColumnAccess = tableColumnAccess
        this.foreignKeyFieldName = foreignKeyFieldName

        if (!foreignKeyColumnDefinition.isPrimaryKey && !foreignKeyColumnDefinition.isPrimaryKeyAutoIncrement
                && !foreignKeyColumnDefinition.isRowId || referenceCount > 0) {
            columnName = foreignKeyFieldName + "_" + referencedColumn.columnName
        } else {
            columnName = foreignKeyFieldName
        }
        foreignColumnName = referencedColumn.columnName
        columnClassName = referencedColumn.elementTypeName

        if (referencedColumn.columnAccess is WrapperColumnAccess) {
            isReferencedFieldPrivate = (referencedColumn.columnAccess as WrapperColumnAccess).existingColumnAccess is PrivateColumnAccess
            isReferencedFieldPackagePrivate = (referencedColumn.columnAccess as WrapperColumnAccess).existingColumnAccess is PackagePrivateAccess
        } else {
            isReferencedFieldPrivate = referencedColumn.columnAccess is PrivateColumnAccess

            // fix here to ensure we can access it otherwise we generate helper
            val isPackagePrivate = ElementUtility.isPackagePrivate(referencedColumn.element)
            val isPackagePrivateNotInSamePackage = isPackagePrivate &&
                    !ElementUtility.isInSamePackage(manager, referencedColumn.element,
                            foreignKeyColumnDefinition.element)

            isReferencedFieldPackagePrivate = referencedColumn.columnAccess is PackagePrivateAccess || isPackagePrivateNotInSamePackage
        }
        if (isReferencedFieldPrivate) {
            columnAccess = PrivateColumnAccess(referencedColumn.column, false)
        } else if (isReferencedFieldPackagePrivate) {
            columnAccess = PackagePrivateAccess(referencedColumn.packageName,
                    foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                    ClassName.get(referencedColumn.element.enclosingElement as TypeElement).simpleName())
            PackagePrivateAccess.putElement((columnAccess as PackagePrivateAccess).helperClassName, foreignColumnName)
        } else {
            columnAccess = SimpleColumnAccess()
        }

        simpleColumnAccess = SimpleColumnAccess(columnAccess is PackagePrivateAccess)
    }

    constructor(manager: ProcessorManager, foreignKeyFieldName: String,
                foreignKeyReference: ForeignKeyReference, tableColumnAccess: BaseColumnAccess,
                foreignKeyColumnDefinition: ForeignKeyColumnDefinition) {
        this.manager = manager
        this.tableColumnAccess = tableColumnAccess
        this.foreignKeyColumnDefinition = foreignKeyColumnDefinition
        this.foreignKeyFieldName = foreignKeyFieldName

        columnName = foreignKeyReference.columnName
        foreignColumnName = foreignKeyReference.foreignKeyColumnName

        var columnClass: TypeMirror? = null
        try {
            foreignKeyReference.columnType
        } catch (mte: MirroredTypeException) {
            columnClass = mte.typeMirror
        }

        columnClassName = TypeName.get(columnClass!!)
        isReferencedFieldPrivate = foreignKeyReference.referencedFieldIsPrivate
        isReferencedFieldPackagePrivate = foreignKeyReference.referencedFieldIsPackagePrivate
        if (isReferencedFieldPrivate) {
            columnAccess = PrivateColumnAccess(foreignKeyReference)
        } else if (isReferencedFieldPackagePrivate) {
            foreignKeyColumnDefinition.referencedTableClassName?.let {
                columnAccess = PackagePrivateAccess(it.packageName(),
                        foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                        it.simpleName())
                PackagePrivateAccess.putElement((columnAccess as PackagePrivateAccess).helperClassName, foreignColumnName)
            }
        } else {
            columnAccess = SimpleColumnAccess()
        }

        simpleColumnAccess = SimpleColumnAccess(columnAccess is PackagePrivateAccess)
    }

    internal val creationStatement: CodeBlock
        get() = DefinitionUtils.getCreationStatement(columnClassName, null, columnName).build()

    internal val primaryKeyName: String
        get() = QueryBuilder.quote(columnName)

    internal fun getContentValuesStatement(isModelContainerAdapter: Boolean): CodeBlock {
        // fix its access here.
        var shortAccess = tableColumnAccess.getShortAccessString(foreignKeyColumnDefinition.elementClassName, foreignKeyFieldName, false)
        shortAccess = foreignKeyColumnDefinition.getForeignKeyReferenceAccess(shortAccess)

        val columnShortAccess = getShortColumnAccess(false, shortAccess)

        val combined: CodeBlock
        if (columnAccess !is PackagePrivateAccess) {
            combined = CodeBlock.of("\$L\$L\$L", shortAccess, if (isModelContainerAdapter) "" else ".",
                    columnShortAccess)
        } else {
            combined = columnShortAccess
        }
        return DefinitionUtils.getContentValuesStatement(columnShortAccess.toString(),
                combined.toString(),
                columnName, columnClassName, simpleColumnAccess,
                foreignKeyColumnVariable, null).build()
    }

    val primaryReferenceString: CodeBlock
        get() {
            var shortAccess = tableColumnAccess.getShortAccessString(foreignKeyColumnDefinition.elementClassName, foreignKeyFieldName, false)
            shortAccess = foreignKeyColumnDefinition.getForeignKeyReferenceAccess(shortAccess)
            val columnShortAccess = getShortColumnAccess(false, shortAccess)
            val combined: CodeBlock
            if (columnAccess !is PackagePrivateAccess) {
                combined = CodeBlock.of("\$L.\$L.\$L", ModelUtils.getVariable(),
                        shortAccess, columnShortAccess)
            } else {
                combined = columnShortAccess
            }
            return combined
        }

    internal fun getSQLiteStatementMethod(index: AtomicInteger): CodeBlock {
        var shortAccess = tableColumnAccess.getShortAccessString(foreignKeyColumnDefinition.elementClassName, foreignKeyFieldName, true)
        shortAccess = foreignKeyColumnDefinition.getForeignKeyReferenceAccess(shortAccess)

        val columnShortAccess = getShortColumnAccess(true, shortAccess)
        val combined = shortAccess.toBuilder().add(".").add(columnShortAccess).build()
        return DefinitionUtils.getSQLiteStatementMethod(
                index, columnShortAccess.toString(), combined.toString(),
                columnClassName, simpleColumnAccess,
                foreignKeyColumnVariable, false, null).build()
    }

    internal fun getForeignKeyContainerMethod(referenceFieldName: String, loadFromCursorBlock: CodeBlock): CodeBlock {

        val codeBlock = columnAccess?.setColumnAccessString(columnClassName, foreignColumnName, foreignColumnName,
                referenceFieldName, loadFromCursorBlock)
        val codeBuilder = CodeBlock.builder()
        codeBuilder.addStatement("\$L", codeBlock)
        return codeBuilder.build()
    }

    private val foreignKeyColumnVariable: String
        get() = ModelUtils.getVariable()

    private fun getShortColumnAccess(isSqliteMethod: Boolean, shortAccess: CodeBlock): CodeBlock {
        columnAccess.let {
            if (it != null) {
                if (it is PackagePrivateAccess) {
                    return it.getColumnAccessString(columnClassName, foreignColumnName, "",
                            ModelUtils.getVariable() + "." + shortAccess, isSqliteMethod)
                } else {
                    return it.getShortAccessString(columnClassName, foreignColumnName, isSqliteMethod)
                }
            } else {
                return CodeBlock.of("")
            }
        }
    }

}
