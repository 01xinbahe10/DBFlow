package com.raizlabs.dbflow5.query.list

import android.database.Cursor
import android.widget.ListView
import com.raizlabs.dbflow5.adapter.InstanceAdapter
import com.raizlabs.dbflow5.config.FlowLog
import com.raizlabs.dbflow5.config.FlowManager
import com.raizlabs.dbflow5.database.FlowCursor
import com.raizlabs.dbflow5.query.ModelQueriable

/**
 * Description: A non-modifiable, cursor-backed list that you can use in [ListView] or other data sources.
 */
class FlowCursorList<T : Any> private constructor(builder: Builder<T>) : IFlowCursorIterator<T> {

    /**
     * Interface for callbacks when cursor gets refreshed.
     */
    interface OnCursorRefreshListener<TModel : Any> {

        /**
         * Callback when cursor refreshes.
         *
         * @param cursorList The object that changed.
         */
        fun onCursorRefreshed(cursorList: FlowCursorList<TModel>)
    }

    val table: Class<T>
    val modelQueriable: ModelQueriable<T>
    private var cursor: FlowCursor? = null
    private val cursorFunc: () -> FlowCursor

    internal val instanceAdapter: InstanceAdapter<T>

    private val cursorRefreshListenerSet = hashSetOf<OnCursorRefreshListener<T>>()

    /**
     * @return the full, converted [T] list from the database on this list. For large
     * data sets that require a large conversion, consider calling this on a BG thread.
     */
    val all: List<T>
        get() {
            unpackCursor()
            throwIfCursorClosed()
            warnEmptyCursor()
            return cursor?.let { cursor ->
                instanceAdapter.listModelLoader.convertToData(cursor, FlowManager.getDatabaseForTable(table))
            } ?: listOf()
        }

    /**
     * @return the count of rows on this database query list.
     */
    val isEmpty: Boolean
        get() {
            throwIfCursorClosed()
            warnEmptyCursor()
            return count == 0L
        }

    init {
        table = builder.modelClass
        this.modelQueriable = builder.modelQueriable
        cursorFunc = { builder.cursor ?: modelQueriable.query() ?: throw IllegalStateException("The query must evaluate to a cursor") }
        instanceAdapter = FlowManager.getInstanceAdapter(builder.modelClass)
    }

    override operator fun iterator(): FlowCursorIterator<T> = FlowCursorIterator(this)

    override fun iterator(startingLocation: Int, limit: Long): FlowCursorIterator<T> =
        FlowCursorIterator(this, startingLocation, limit)

    /**
     * Register listener for when cursor refreshes.
     */
    fun addOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<T>) {
        synchronized(cursorRefreshListenerSet) {
            cursorRefreshListenerSet.add(onCursorRefreshListener)
        }
    }

    fun removeOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<T>) {
        synchronized(cursorRefreshListenerSet) {
            cursorRefreshListenerSet.remove(onCursorRefreshListener)
        }
    }

    /**
     * Refreshes the data backing this list, and destroys the Model cache.
     */
    @Synchronized
    fun refresh() {
        val cursor = unpackCursor()
        cursor.close()
        this.cursor = modelQueriable.query()
        synchronized(cursorRefreshListenerSet) {
            cursorRefreshListenerSet.forEach { listener -> listener.onCursorRefreshed(this) }
        }
    }

    /**
     * Returns a model at the specified position. If we are using the cache and it does not contain a model
     * at that position, we move the cursor to the specified position and construct the [T].
     *
     * @param position The row number in the [android.database.Cursor] to look at
     * @return The [T] converted from the cursor
     */
    override fun get(position: Long): T {
        throwIfCursorClosed()

        val cursor = unpackCursor()
        return if (cursor.moveToPosition(position.toInt())) {
            instanceAdapter.singleModelLoader.convertToData(
                FlowCursor.from(cursor), false,
                FlowManager.getDatabaseForTable(table))
                ?: throw IndexOutOfBoundsException("Invalid item at position $position. Check your cursor data.")
        } else {
            throw IndexOutOfBoundsException("Invalid item at position $position. Check your cursor data.")
        }
    }

    /**
     * @return the count of the rows in the [android.database.Cursor] backed by this list.
     */
    override val count: Long
        get() {
            unpackCursor()
            throwIfCursorClosed()
            warnEmptyCursor()
            return (cursor?.count ?: 0).toLong()
        }

    /**
     * Closes the cursor backed by this list
     */
    override fun close() {
        warnEmptyCursor()
        cursor?.close()
        cursor = null
    }

    override fun cursor(): Cursor? {
        unpackCursor()
        throwIfCursorClosed()
        warnEmptyCursor()
        return cursor
    }

    private fun unpackCursor(): Cursor {
        if (cursor == null) {
            cursor = cursorFunc()
        }
        return cursor!!
    }

    private fun throwIfCursorClosed() {
        if (cursor?.isClosed == true) {
            throw IllegalStateException("Cursor has been closed for FlowCursorList")
        }
    }

    private fun warnEmptyCursor() {
        if (cursor == null) {
            FlowLog.log(FlowLog.Level.W, "Cursor was null for FlowCursorList")
        }
    }

    /**
     * @return A new [Builder] that contains the same cache, query statement, and other
     * underlying data, but allows for modification.
     */
    fun newBuilder(): Builder<T> = Builder(modelQueriable).cursor(cursor())

    /**
     * Provides easy way to construct a [FlowCursorList].
     *
     * @param [T]
     */
    class Builder<T : Any>(internal var modelQueriable: ModelQueriable<T>) {

        internal val modelClass: Class<T> = modelQueriable.table
        internal var cursor: FlowCursor? = null

        fun cursor(cursor: Cursor?) = apply {
            cursor?.let { this.cursor = FlowCursor.from(cursor) }
        }

        fun build() = FlowCursorList(this)

    }

}
