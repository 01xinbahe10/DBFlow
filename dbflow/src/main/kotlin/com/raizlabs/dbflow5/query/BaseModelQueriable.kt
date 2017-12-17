package com.raizlabs.dbflow5.query

import com.raizlabs.dbflow5.adapter.RetrievalAdapter
import com.raizlabs.dbflow5.adapter.queriable.ListModelLoader
import com.raizlabs.dbflow5.adapter.queriable.SingleModelLoader
import com.raizlabs.dbflow5.config.FlowLog
import com.raizlabs.dbflow5.config.FlowManager
import com.raizlabs.dbflow5.config.queryModelAdapter
import com.raizlabs.dbflow5.database.DatabaseWrapper
import com.raizlabs.dbflow5.query.list.FlowCursorList
import com.raizlabs.dbflow5.query.list.FlowQueryList
import com.raizlabs.dbflow5.runtime.NotifyDistributor
import com.raizlabs.dbflow5.sql.Query

/**
 * Description: Provides a base implementation of [ModelQueriable] to simplify a lot of code. It provides the
 * default implementation for convenience.
 */
abstract class BaseModelQueriable<TModel : Any>
/**
 * Constructs new instance of this class and is meant for subclasses only.
 *
 * @param table the table that belongs to this query.
 */
protected constructor(table: Class<TModel>)
    : BaseQueriable<TModel>(table), ModelQueriable<TModel>, Query {

    private val retrievalAdapter: RetrievalAdapter<TModel> by lazy { FlowManager.getRetrievalAdapter(table) }

    private var cachingEnabled = true

    private val listModelLoader: ListModelLoader<TModel>
        get() = if (cachingEnabled) {
            retrievalAdapter.listModelLoader
        } else {
            retrievalAdapter.nonCacheableListModelLoader
        }

    private val singleModelLoader: SingleModelLoader<TModel>
        get() = if (cachingEnabled) {
            retrievalAdapter.singleModelLoader
        } else {
            retrievalAdapter.nonCacheableSingleModelLoader
        }

    override fun disableCaching() = apply {
        cachingEnabled = false
    }

    override fun queryResults(databaseWrapper: DatabaseWrapper): CursorResult<TModel> =
            CursorResult(retrievalAdapter.table, cursor(databaseWrapper), databaseWrapper)

    override fun queryList(databaseWrapper: DatabaseWrapper): MutableList<TModel> {
        val query = query
        FlowLog.log(FlowLog.Level.V, "Executing cursor: " + query)
        return listModelLoader.load(databaseWrapper, query)!!
    }

    override fun querySingle(databaseWrapper: DatabaseWrapper): TModel? {
        val query = query
        FlowLog.log(FlowLog.Level.V, "Executing cursor: " + query)
        return singleModelLoader.load(databaseWrapper, query)
    }

    override fun cursorList(databaseWrapper: DatabaseWrapper): FlowCursorList<TModel> =
            FlowCursorList.Builder(modelQueriable = this, databaseWrapper = databaseWrapper).build()

    override fun flowQueryList(databaseWrapper: DatabaseWrapper): FlowQueryList<TModel> =
            FlowQueryList.Builder(modelQueriable = this, databaseWrapper = databaseWrapper).build()

    override fun executeUpdateDelete(databaseWrapper: DatabaseWrapper): Long {
        val affected = databaseWrapper.compileStatement(query).executeUpdateDelete()

        // only notify for affected.
        if (affected > 0) {
            NotifyDistributor.get().notifyTableChanged(table, primaryAction)
        }
        return affected
    }

    override fun <QueryClass : Any> queryCustomList(queryModelClass: Class<QueryClass>,
                                                    databaseWrapper: DatabaseWrapper)
            : MutableList<QueryClass> {
        val query = query
        FlowLog.log(FlowLog.Level.V, "Executing cursor: " + query)
        return getListQueryModelLoader(queryModelClass).load(databaseWrapper, query)!!
    }

    override fun <QueryClass : Any> queryCustomSingle(queryModelClass: Class<QueryClass>,
                                                      databaseWrapper: DatabaseWrapper)
            : QueryClass? {
        val query = query
        FlowLog.log(FlowLog.Level.V, "Executing cursor: " + query)
        return getSingleQueryModelLoader(queryModelClass).load(databaseWrapper, query)
    }


    private fun <T : Any> getListQueryModelLoader(table: Class<T>): ListModelLoader<T> = if (cachingEnabled) {
        table.queryModelAdapter.listModelLoader
    } else {
        table.queryModelAdapter.nonCacheableListModelLoader
    }

    private fun <T : Any> getSingleQueryModelLoader(table: Class<T>): SingleModelLoader<T> = if (cachingEnabled) {
        table.queryModelAdapter.singleModelLoader
    } else {
        table.queryModelAdapter.nonCacheableSingleModelLoader
    }
}
