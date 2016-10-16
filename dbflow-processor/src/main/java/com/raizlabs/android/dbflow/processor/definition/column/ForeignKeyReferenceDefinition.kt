package com.raizlabs.android.dbflow.processor.definition.column

import com.raizlabs.android.dbflow.annotation.ForeignKeyReference
import com.raizlabs.android.dbflow.data.Blob
import com.raizlabs.android.dbflow.processor.ProcessorManager
import com.raizlabs.android.dbflow.processor.definition.TypeConverterDefinition
import com.raizlabs.android.dbflow.processor.utils.ElementUtility
import com.raizlabs.android.dbflow.processor.utils.ModelUtils
import com.raizlabs.android.dbflow.sql.QueryBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName
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

    var hasTypeConverter: Boolean = false

    internal val creationStatement: CodeBlock
        get() = DefinitionUtils.getCreationStatement(columnClassName, null, columnName).build()

    internal val primaryKeyName: String
        get() = QueryBuilder.quote(columnName)

    private val foreignKeyColumnVariable: String
        get() = ModelUtils.variable

    private var isReferencedFieldPrivate: Boolean = false
    private var isReferencedFieldPackagePrivate: Boolean = false

    var columnAccess: BaseColumnAccess? = null

    var columnAccessor: ColumnAccessor? = null
    var wrapperAccessor: ColumnAccessor? = null
    var wrapperTypeName: TypeName? = null
    var subWrapperAccessor: ColumnAccessor? = null

    var partialAccessor: PartialLoadFromCursorAccessCombiner
    var primaryReferenceField: ForeignKeyAccessField
    var contentValuesField: ForeignKeyAccessField
    var sqliteStatementField: ForeignKeyAccessField

    var isBoolean = false

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
            this.columnName = foreignKeyFieldName + "_" + referencedColumn.columnName
        } else {
            this.columnName = foreignKeyFieldName
        }
        foreignColumnName = referencedColumn.columnName
        this.columnClassName = referencedColumn.elementTypeName

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

            val isBoolean = columnClassName?.box() == TypeName.BOOLEAN.box()

            columnAccessor = PrivateScopeColumnAccessor(foreignKeyFieldName, object : GetterSetter {
                override val getterName: String = referencedColumn.column?.getterName ?: ""
                override val setterName: String = referencedColumn.column?.setterName ?: ""
            }, isBoolean, false)

        } else if (isReferencedFieldPackagePrivate) {
            columnAccess = PackagePrivateAccess(referencedColumn.packageName,
                    foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                    ClassName.get(referencedColumn.element.enclosingElement as TypeElement).simpleName())
            PackagePrivateAccess.putElement((columnAccess as PackagePrivateAccess).helperClassName, foreignColumnName)

            columnAccessor = PackagePrivateScopeColumnAccessor(foreignColumnName,
                    referencedColumn.packageName,
                    foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                    ClassName.get(referencedColumn.element.enclosingElement as TypeElement).simpleName())

            PackagePrivateAccess.putElement((columnAccessor as PackagePrivateScopeColumnAccessor).helperClassName,
                    foreignColumnName)

        } else {
            columnAccess = SimpleColumnAccess()

            columnAccessor = VisibleScopeColumnAccessor(foreignColumnName)
        }

        val typeConverterDefinition = columnClassName?.let { manager.getTypeConverterDefinition(it) }
        evaluateTypeConverter(typeConverterDefinition)

        simpleColumnAccess = SimpleColumnAccess(columnAccess is PackagePrivateAccess
                || columnAccess is TypeConverterAccess)

        partialAccessor = PartialLoadFromCursorAccessCombiner(columnName, columnName,
                columnClassName!!, foreignKeyColumnDefinition.baseTableDefinition.orderedCursorLookUp,
                columnAccessor, wrapperAccessor, wrapperTypeName)

        primaryReferenceField = ForeignKeyAccessField(columnName,
                PrimaryReferenceAccessCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

        contentValuesField = ForeignKeyAccessField(columnName,
                ContentValuesCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

        sqliteStatementField = ForeignKeyAccessField("start",
                SqliteStatementAccessCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

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

            columnAccessor = PrivateScopeColumnAccessor(foreignKeyFieldName, object : GetterSetter {
                override val getterName: String = foreignKeyReference.referencedGetterName
                override val setterName: String = foreignKeyReference.referencedSetterName
            }, isBoolean, false)
        } else if (isReferencedFieldPackagePrivate) {
            foreignKeyColumnDefinition.referencedTableClassName?.let {
                columnAccess = PackagePrivateAccess(it.packageName(),
                        foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                        it.simpleName())
                PackagePrivateAccess.putElement((columnAccess as PackagePrivateAccess).helperClassName, foreignColumnName)

                columnAccessor = PackagePrivateScopeColumnAccessor(foreignColumnName,
                        it.packageName(),
                        foreignKeyColumnDefinition.baseTableDefinition.databaseDefinition?.classSeparator,
                        it.simpleName())
            }
        } else {
            columnAccess = SimpleColumnAccess()

            columnAccessor = VisibleScopeColumnAccessor(foreignColumnName)
        }

        simpleColumnAccess = SimpleColumnAccess(columnAccess is PackagePrivateAccess)

        val typeConverterDefinition = columnClassName?.let { manager.getTypeConverterDefinition(it) }
        evaluateTypeConverter(typeConverterDefinition)

        partialAccessor = PartialLoadFromCursorAccessCombiner(columnName, columnName,
                columnClassName!!, foreignKeyColumnDefinition.baseTableDefinition.orderedCursorLookUp,
                columnAccessor, wrapperAccessor, wrapperTypeName)

        primaryReferenceField = ForeignKeyAccessField(columnName,
                PrimaryReferenceAccessCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

        contentValuesField = ForeignKeyAccessField(columnName,
                ContentValuesCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

        sqliteStatementField = ForeignKeyAccessField("start",
                SqliteStatementAccessCombiner(columnAccessor!!, columnClassName, wrapperAccessor,
                        wrapperTypeName, subWrapperAccessor))

    }

    private fun evaluateTypeConverter(typeConverterDefinition: TypeConverterDefinition?) {
        // Any annotated members, otherwise we will use the scanner to find other ones
        typeConverterDefinition?.let {

            if (it.modelTypeName != columnClassName) {
                manager.logError("The specified custom TypeConverter's Model Value %1s from %1s must match the type of the column %1s. ",
                        it.modelTypeName, it.className, columnClassName)
            } else {
                hasTypeConverter = true

                val fieldName = foreignKeyColumnDefinition.baseTableDefinition
                        .addColumnForTypeConverter(foreignKeyColumnDefinition, it.className)
                if (columnClassName == TypeName.BOOLEAN.box()) {
                    isBoolean = true
                    columnAccess = BooleanColumnAccess(manager, foreignKeyColumnDefinition)
                } else {
                    columnAccess = TypeConverterAccess(manager, foreignKeyColumnDefinition, it, fieldName)
                }

                wrapperAccessor = TypeConverterScopeColumnAccessor(fieldName)
                wrapperTypeName = it.dbTypeName

                // special case of blob
                if (wrapperTypeName == ClassName.get(Blob::class.java)) {
                    subWrapperAccessor = BlobColumnAccessor()
                }
            }
        }
    }


    private fun getShortColumnAccess(isSqliteMethod: Boolean, shortAccess: CodeBlock): CodeBlock {
        columnAccess.let {
            if (it != null) {
                if (it is PackagePrivateAccess || it is TypeConverterAccess) {
                    return it.getColumnAccessString(columnClassName, foreignColumnName,
                            if (it is TypeConverterAccess) foreignColumnName else "",
                            ModelUtils.variable + "." + shortAccess, isSqliteMethod)
                } else {
                    return it.getShortAccessString(columnClassName, foreignColumnName, isSqliteMethod)
                }
            } else {
                return CodeBlock.of("")
            }
        }
    }

}
