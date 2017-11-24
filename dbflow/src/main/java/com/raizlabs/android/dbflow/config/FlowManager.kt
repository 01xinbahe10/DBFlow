package com.raizlabs.android.dbflow.config

import android.content.Context
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.converter.TypeConverter
import com.raizlabs.android.dbflow.quote
import com.raizlabs.android.dbflow.runtime.ModelNotifier
import com.raizlabs.android.dbflow.runtime.TableNotifierRegister
import com.raizlabs.android.dbflow.sql.migration.Migration
import com.raizlabs.android.dbflow.structure.BaseModel
import com.raizlabs.android.dbflow.structure.BaseModelView
import com.raizlabs.android.dbflow.structure.BaseQueryModel
import com.raizlabs.android.dbflow.structure.InstanceAdapter
import com.raizlabs.android.dbflow.structure.InvalidDBConfiguration
import com.raizlabs.android.dbflow.structure.Model
import com.raizlabs.android.dbflow.structure.ModelAdapter
import com.raizlabs.android.dbflow.structure.ModelViewAdapter
import com.raizlabs.android.dbflow.structure.QueryModelAdapter
import com.raizlabs.android.dbflow.structure.RetrievalAdapter
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper
import java.util.*
import kotlin.reflect.KClass

/**
 * Description: The main entry point into the generated database code. It uses reflection to look up
 * and construct the generated database holder class used in defining the structure for all databases
 * used in this application.
 */
object FlowManager {

    internal var config: FlowConfig? = null

    private var globalDatabaseHolder = GlobalDatabaseHolder()

    private val loadedModules = HashSet<Class<out DatabaseHolder>>()

    private val DEFAULT_DATABASE_HOLDER_NAME = "GeneratedDatabaseHolder"

    private val DEFAULT_DATABASE_HOLDER_PACKAGE_NAME = FlowManager::class.java.`package`.name

    private val DEFAULT_DATABASE_HOLDER_CLASSNAME =
            DEFAULT_DATABASE_HOLDER_PACKAGE_NAME + "." + DEFAULT_DATABASE_HOLDER_NAME

    /**
     * Will throw an exception if this class is not initialized yet in [.init]
     *
     * @return The shared context.
     */
    @JvmStatic
    val context: Context
        get() = config?.context ?:
                throw IllegalStateException("You must provide a valid FlowConfig instance." +
                        " We recommend calling init() in your application class.")

    private class GlobalDatabaseHolder : DatabaseHolder() {

        var isInitialized = false
            private set

        fun add(holder: DatabaseHolder) {
            databaseDefinitionMap.putAll(holder.databaseDefinitionMap)
            databaseNameMap.putAll(holder.databaseNameMap)
            typeConverters.putAll(holder.typeConverters)
            databaseClassLookupMap.putAll(holder.databaseClassLookupMap)
            isInitialized = true
        }
    }

    /**
     * Returns the table name for the specific model class
     *
     * @param table The class that implements [Model]
     * @return The table name, which can be different than the [Model] class name
     */
    @JvmStatic
    fun getTableName(table: Class<*>): String {
        return getModelAdapterOrNull(table)?.tableName
                ?: getModelViewAdapterOrNull(table)?.viewName
                ?: throwCannotFindAdapter("ModelAdapter/ModelViewAdapter", table)
    }

    /**
     * @param databaseName The name of the database. Will throw an exception if the database doesn't exist.
     * @param tableName    The name of the table in the DB.
     * @return The associated table class for the specified name.
     */
    @JvmStatic
    fun getTableClassForName(databaseName: String, tableName: String): Class<*> {
        val databaseDefinition = getDatabase(databaseName)
        return databaseDefinition.getModelClassForName(tableName)
                ?: databaseDefinition.getModelClassForName(tableName.quote())
                ?: throw IllegalArgumentException("The specified table $tableName was not found." +
                " Did you forget to add the @Table annotation and point it to $databaseName?")
    }

    /**
     * @param databaseClass The class of the database. Will throw an exception if the database doesn't exist.
     * @param tableName     The name of the table in the DB.
     * @return The associated table class for the specified name.
     */
    @JvmStatic
    fun getTableClassForName(databaseClass: Class<*>, tableName: String): Class<*> {
        val databaseDefinition = getDatabase(databaseClass)
        return databaseDefinition.getModelClassForName(tableName)
                ?: databaseDefinition.getModelClassForName(tableName.quote())
                ?: throw IllegalArgumentException("The specified table $tableName was not found." +
                " Did you forget to add the @Table annotation and point it to $databaseClass?")
    }

    /**
     * @param table The table to lookup the database for.
     * @return the corresponding [DatabaseDefinition] for the specified model
     */
    @JvmStatic
    fun getDatabaseForTable(table: Class<*>): DatabaseDefinition {
        checkDatabaseHolder()
        return globalDatabaseHolder.getDatabaseForTable(table) ?:
                throw InvalidDBConfiguration("Model object: ${table.name} is not registered with a Database." +
                        " Did you forget an annotation?")
    }

    @JvmStatic
    fun getDatabase(databaseClass: Class<*>): DatabaseDefinition {
        checkDatabaseHolder()
        return globalDatabaseHolder.getDatabase(databaseClass) ?:
                throw InvalidDBConfiguration("Database: ${databaseClass.name} is not a registered Database. " +
                        "Did you forget the @Database annotation?")
    }

    @JvmStatic
    fun getDatabaseName(database: Class<*>): String = getDatabase(database).databaseName

    @JvmStatic
    fun getWritableDatabaseForTable(table: Class<*>): DatabaseWrapper =
            getDatabaseForTable(table).writableDatabase

    /**
     * @param databaseName The name of the database. Will throw an exception if the database doesn't exist.
     * @return the [DatabaseDefinition] for the specified database
     */
    @JvmStatic
    fun getDatabase(databaseName: String): DatabaseDefinition {
        checkDatabaseHolder()
        return globalDatabaseHolder.getDatabase(databaseName) ?:
                throw InvalidDBConfiguration("The specified database $databaseName was not found. " +
                        "Did you forget the @Database annotation?")
    }

    @JvmStatic
    fun getWritableDatabase(databaseName: String): DatabaseWrapper =
            getDatabase(databaseName).writableDatabase

    @JvmStatic
    fun getWritableDatabase(databaseClass: Class<*>): DatabaseWrapper =
            getDatabase(databaseClass).writableDatabase

    /**
     * Loading the module Database holder via reflection.
     *
     *
     * It is assumed FlowManager.init() is called by the application that uses the
     * module database. This method should only be called if you need to load databases
     * that are part of a module. Building once will give you the ability to add the class.
     */
    @JvmStatic
    fun initModule(generatedClassName: Class<out DatabaseHolder>) {
        loadDatabaseHolder(generatedClassName)
    }

    @JvmStatic
    fun getConfig(): FlowConfig = config ?:
            throw IllegalStateException("Configuration is not initialized. " +
                    "Please call init(FlowConfig) in your application class.")

    /**
     * @return The database holder, creating if necessary using reflection.
     */
    @JvmStatic
    internal fun loadDatabaseHolder(holderClass: Class<out DatabaseHolder>) {
        if (loadedModules.contains(holderClass)) {
            return
        }

        try {
            // Load the database holder, and add it to the global collection.
            val dbHolder = holderClass.newInstance()
            if (dbHolder != null) {
                globalDatabaseHolder.add(dbHolder)

                // Cache the holder for future reference.
                loadedModules.add(holderClass)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw ModuleNotFoundException("Cannot load $holderClass", e)
        }

    }

    /**
     * Resets all databases and associated files.
     */
    @Synchronized
    @JvmStatic
    fun reset() {
        globalDatabaseHolder.databaseClassLookupMap.values.forEach { it.reset() }
        globalDatabaseHolder.reset()
        loadedModules.clear()
    }

    /**
     * Close all DB files and resets [FlowConfig] and the [GlobalDatabaseHolder]. Brings
     * DBFlow back to initial application state.
     */
    @Synchronized
    @JvmStatic
    fun close() {
        globalDatabaseHolder.databaseClassLookupMap.values.forEach { it.close() }
        config = null
        globalDatabaseHolder = GlobalDatabaseHolder()
        loadedModules.clear()
    }

    /**
     * Helper method to simplify the [.init]. Use [.init] to provide
     * more customization.
     *
     * @param context - should be application context, but not necessary as we retrieve it anyways.
     */
    @JvmStatic
    fun init(context: Context) {
        init(FlowConfig.Builder(context).build())
    }

    /**
     * Initializes DBFlow, loading the main application Database holder via reflection one time only.
     * This will trigger all creations, updates, and instantiation for each database defined.
     *
     * @param flowConfig The configuration instance that will help shape how DBFlow gets constructed.
     */
    @JvmStatic
    fun init(flowConfig: FlowConfig) {
        FlowManager.config = flowConfig

        @Suppress("UNCHECKED_CAST")
        try {
            val defaultHolderClass = Class.forName(DEFAULT_DATABASE_HOLDER_CLASSNAME) as Class<out DatabaseHolder>
            loadDatabaseHolder(defaultHolderClass)
        } catch (e: ModuleNotFoundException) {
            // Ignore this exception since it means the application does not have its
            // own database. The initialization happens because the application is using
            // a module that has a database.
            FlowLog.log(level = FlowLog.Level.W, message = e.message)
        } catch (e: ClassNotFoundException) {
            // warning if a library uses DBFlow with module support but the app you're using doesn't support it.
            FlowLog.log(level = FlowLog.Level.W, message = "Could not find the default GeneratedDatabaseHolder")
        }

        flowConfig.databaseHolders.forEach { loadDatabaseHolder(it) }

        if (flowConfig.openDatabasesOnInit) {
            globalDatabaseHolder.databaseDefinitions.forEach {
                // triggers open, create, migrations.
                it.writableDatabase
            }
        }
    }

    /**
     * @param objectClass A class with an associated type converter. May return null if not found.
     * @return The specific [TypeConverter] for the specified class. It defines
     * how the custom datatype is handled going into and out of the DB.
     */
    @JvmStatic
    fun getTypeConverterForClass(objectClass: Class<*>): TypeConverter<*, *>? {
        checkDatabaseHolder()
        return globalDatabaseHolder.getTypeConverterForClass(objectClass)
    }

    // region Getters

    /**
     * Release reference to context and [FlowConfig]
     */
    @JvmStatic
    @Synchronized
    fun destroy() {
        globalDatabaseHolder.databaseClassLookupMap.values.forEach { it.destroy() }
        config = null
        // Reset the global database holder.
        globalDatabaseHolder = GlobalDatabaseHolder()
        loadedModules.clear()
    }

    /**
     * @param modelClass The class that implements [Model] to find an adapter for.
     * @return The adapter associated with the class. If its not a [ModelAdapter],
     * it checks both the [ModelViewAdapter] and [QueryModelAdapter].
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Any> getInstanceAdapter(modelClass: Class<T>): InstanceAdapter<T> {
        var internalAdapter: InstanceAdapter<*>? = getModelAdapterOrNull(modelClass)
        if (internalAdapter == null) {
            internalAdapter = getModelViewAdapterOrNull(modelClass)
            if (internalAdapter == null) {
                internalAdapter = getQueryModelAdapterOrNull(modelClass)
            }
        }
        return internalAdapter as InstanceAdapter<T>? ?: throwCannotFindAdapter("InstanceAdapter", modelClass)
    }

    /**
     * @param modelClass The class that implements [Model] to find an adapter for.
     * @return The adapter associated with the class. If its not a [ModelAdapter],
     * it checks both the [ModelViewAdapter] and [QueryModelAdapter].
     */
    @JvmStatic
    fun <T : Any> getRetrievalAdapter(modelClass: Class<T>): RetrievalAdapter<T> {
        var retrievalAdapter: RetrievalAdapter<T>? = getModelAdapterOrNull(modelClass)
        if (retrievalAdapter == null) {
            retrievalAdapter = getModelViewAdapterOrNull(modelClass)
            if (retrievalAdapter == null) {
                retrievalAdapter = getQueryModelAdapterOrNull(modelClass)
            }
        }
        return retrievalAdapter ?: throwCannotFindAdapter("RetrievalAdapter", modelClass)
    }


    /**
     * @param modelClass The class of the table
     * @param [T]   The class that implements [Model]
     * @return The associated model adapter (DAO) that is generated from a [Table] class. Handles
     * interactions with the database. This method is meant for internal usage only.
     * We strongly prefer you use the built-in methods associated with [Model] and [BaseModel].
     */
    @JvmStatic
    fun <T : Any> getModelAdapter(modelClass: Class<T>): ModelAdapter<T> =
            getModelAdapterOrNull(modelClass) ?: throwCannotFindAdapter("ModelAdapter", modelClass)

    /**
     * Returns the model view adapter for a SQLite VIEW. These are only created with the [com.raizlabs.android.dbflow.annotation.ModelView] annotation.
     *
     * @param modelViewClass The class of the VIEW
     * @param [T]  The class that extends [BaseModelView]
     * @return The model view adapter for the specified model view.
     */
    @JvmStatic
    fun <T : Any> getModelViewAdapter(modelViewClass: Class<T>): ModelViewAdapter<T> =
            getModelViewAdapterOrNull(modelViewClass) ?: throwCannotFindAdapter("ModelViewAdapter", modelViewClass)

    /**
     * Returns the query model adapter for an undefined query. These are only created with the [T] annotation.
     *
     * @param queryModelClass The class of the query
     * @param [T]  The class that extends [BaseQueryModel]
     * @return The query model adapter for the specified model query.
     */
    @JvmStatic
    fun <T : Any> getQueryModelAdapter(queryModelClass: Class<T>): QueryModelAdapter<T> =
            getQueryModelAdapterOrNull(queryModelClass) ?: throwCannotFindAdapter("QueryModelAdapter", queryModelClass)

    @JvmStatic
    fun getModelNotifierForTable(table: Class<*>): ModelNotifier =
            getDatabaseForTable(table).getModelNotifier()

    @JvmStatic
    fun newRegisterForTable(table: Class<*>): TableNotifierRegister =
            getModelNotifierForTable(table).newRegister()

    private fun <T : Any> getModelAdapterOrNull(modelClass: Class<T>): ModelAdapter<T>? =
            FlowManager.getDatabaseForTable(modelClass).getModelAdapterForTable(modelClass)

    private fun <T : Any> getModelViewAdapterOrNull(modelClass: Class<T>): ModelViewAdapter<T>? =
            FlowManager.getDatabaseForTable(modelClass).getModelViewAdapterForTable(modelClass)

    private fun <T : Any> getQueryModelAdapterOrNull(modelClass: Class<T>): QueryModelAdapter<T>? =
            FlowManager.getDatabaseForTable(modelClass).getQueryModelAdapterForQueryClass(modelClass)

    /**
     * @param databaseName The name of the database. Will throw an exception if the database doesn't exist.
     * @return The map of migrations for the specified database.
     */
    @JvmStatic
    internal fun getMigrations(databaseName: String): Map<Int, List<Migration>> =
            getDatabase(databaseName).migrations

    /**
     * Checks a standard database helper for integrity using quick_check(1).
     *
     * @param databaseName The name of the database to check. Will thrown an exception if it does not exist.
     * @return true if it's integrity is OK.
     */
    @JvmStatic
    fun isDatabaseIntegrityOk(databaseName: String) = getDatabase(databaseName).helper.isDatabaseIntegrityOk

    private fun throwCannotFindAdapter(type: String, clazz: Class<*>): Nothing =
            throw IllegalArgumentException("Cannot find $type for $clazz. Ensure the class is annotated with proper annotation.")

    private fun checkDatabaseHolder() {
        if (!globalDatabaseHolder.isInitialized) {
            throw IllegalStateException("The global database holder is not initialized. Ensure you call "
                    + "FlowManager.init() before accessing the database.")
        }
    }

    // endregion

    /**
     * Exception thrown when a database holder cannot load the database holder
     * for a module.
     */
    class ModuleNotFoundException : RuntimeException {
        constructor() {}

        constructor(detailMessage: String) : super(detailMessage) {}

        constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable) {}

        constructor(throwable: Throwable) : super(throwable) {}
    }

}

/**
 * Easily get access to its [DatabaseDefinition] directly.
 */
inline fun <reified T : Any> database(): DatabaseDefinition
        = FlowManager.getDatabase(T::class.java)

/**
 * Easily get access to its [DatabaseDefinition] directly.
 */
inline fun <reified T : Any, R> database(kClass: KClass<T>, f: DatabaseDefinition.() -> R): R
        = FlowManager.getDatabase(kClass.java).f()

fun <T : Any, R> writableDatabaseForTable(kClass: KClass<T>, f: DatabaseWrapper.() -> R): R
        = FlowManager.getWritableDatabaseForTable(kClass.java).f()

inline fun <reified T : Any> writableDatabaseForTable(f: DatabaseWrapper.() -> Unit): DatabaseWrapper
        = FlowManager.getWritableDatabaseForTable(T::class.java).apply(f)

inline fun <reified T : Any> writableDatabaseForTable(): DatabaseWrapper
        = FlowManager.getWritableDatabaseForTable(T::class.java)

fun <T : Any, R> writableDatabase(kClass: KClass<T>, f: DatabaseWrapper.() -> R): R
        = FlowManager.getWritableDatabase(kClass.java).f()

inline fun <reified T : Any, R> writableDatabase(f: DatabaseWrapper.() -> R): R
        = FlowManager.getWritableDatabase(T::class.java).f()

inline fun <reified T : Any> writableDatabase(): DatabaseWrapper
        = FlowManager.getWritableDatabase(T::class.java)

/**
 * Easily get access to its [DatabaseDefinition] directly.
 */
inline fun <reified T : Any> databaseForTable(): DatabaseDefinition
        = FlowManager.getDatabaseForTable(T::class.java)

/**
 * Easily get its table name.
 */
inline fun <reified T : Any> tableName(): String
        = FlowManager.getTableName(T::class.java)

/**
 * Easily get its [ModelAdapter].
 */
inline fun <reified T : Any> modelAdapter() = FlowManager.getModelAdapter(T::class.java)

/**
 * Easily get its [QueryModelAdapter].
 */
inline fun <reified T : Any> queryModelAdapter() = FlowManager.getQueryModelAdapter(T::class.java)

/**
 * Easily get its [ModelViewAdapter]
 */
inline fun <reified T : Any> modelViewAdapter() = FlowManager.getModelViewAdapter(T::class.java)