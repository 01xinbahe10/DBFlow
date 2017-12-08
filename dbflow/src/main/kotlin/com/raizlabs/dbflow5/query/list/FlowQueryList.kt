package com.raizlabs.dbflow5.query.list

import android.os.Handler
import android.os.Looper
import com.raizlabs.dbflow5.adapter.RetrievalAdapter
import com.raizlabs.dbflow5.database.FlowCursor
import com.raizlabs.dbflow5.query.ModelQueriable
import com.raizlabs.dbflow5.query.list.FlowCursorList.OnCursorRefreshListener

/**
 * Description: A query-backed immutable [List]. Represents the results of a query without loading
 * the full query out into an actual [List]. Avoid keeping this class around without calling [close] as
 * it leaves a [FlowCursor] object active.
 */
class FlowQueryList<T : Any>(
        /**
         * Holds the table cursor
         */
        val internalCursorList: FlowCursorList<T>)
    : List<T>, IFlowCursorIterator<T> {

    private var pendingRefresh = false

    /**
     * @return a mutable list that does not reflect changes on the underlying DB.
     */
    val copy: List<T>
        get() = internalCursorList.all

    internal val retrievalAdapter: RetrievalAdapter<T>
        get() = internalCursorList.instanceAdapter

    override val count: Long
        get() = internalCursorList.count

    override val cursor: FlowCursor?
        get() = internalCursorList.cursor

    override val size: Int
        get() = internalCursorList.count.toInt()

    private val refreshRunnable = object : Runnable {
        override fun run() {
            synchronized(this) {
                pendingRefresh = false
            }
            refresh()
        }
    }

    internal constructor(builder: Builder<T>) : this(
            internalCursorList = FlowCursorList.Builder(builder.modelQueriable)
                    .cursor(builder.cursor)
                    .build()
    )

    fun addOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<T>) {
        internalCursorList.addOnCursorRefreshListener(onCursorRefreshListener)
    }

    fun removeOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<T>) {
        internalCursorList.removeOnCursorRefreshListener(onCursorRefreshListener)
    }

    val cursorList: FlowCursorList<T>
        get() = internalCursorList

    /**
     * @return Constructs a new [Builder] that reuses the underlying [FlowCursor], cache,
     * callbacks, and other properties.
     */
    fun newBuilder(): Builder<T> = Builder(internalCursorList)

    /**
     * Refreshes the content backing this list.
     */
    fun refresh() {
        internalCursorList.refresh()
    }

    /**
     * Will refresh content at a slightly later time, and multiple subsequent calls to this method within
     * a short period of time will be combined into one call.
     */
    fun refreshAsync() {
        synchronized(this) {
            if (pendingRefresh) {
                return
            }
            pendingRefresh = true
        }
        REFRESH_HANDLER.post(refreshRunnable)
    }


    /**
     * Checks to see if the table contains the object only if its a [T]
     *
     * @param element A model class. For interface purposes, this must be an Object.
     * @return always false if its anything other than the current table. True if [com.raizlabs.android.dbflow.structure.Model.exists] passes.
     */
    override operator fun contains(element: T): Boolean {
        return internalCursorList.instanceAdapter.exists(element)
    }

    /**
     * If the collection is null or empty, we return false.
     *
     * @param elements The collection to check if all exist within the table.
     * @return true if all items exist in table, false if at least one fails.
     */
    override fun containsAll(elements: Collection<T>): Boolean {
        var contains = !elements.isEmpty()
        if (contains) {
            contains = elements.all { it in this }
        }
        return contains
    }

    override fun get(position: Long): T = internalCursorList[position]


    /**
     * Returns the item from the backing [FlowCursorList]. First call
     * will load the model from the cursor, while subsequent calls will use the cache.
     *
     * @param index the row from the internal [FlowCursorList] query that we use.
     * @return A model converted from the internal [FlowCursorList]. For
     * performance improvements, ensure caching is turned on.
     */
    override operator fun get(index: Int): T = internalCursorList[index.toLong()]

    override fun indexOf(element: T): Int {
        throw UnsupportedOperationException(
                "We cannot determine which index in the table this item exists at efficiently")
    }

    override fun isEmpty(): Boolean {
        return internalCursorList.isEmpty
    }

    /**
     * @return An iterator from [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [T] in the UI thread.
     */
    override fun iterator(): FlowCursorIterator<T> {
        return FlowCursorIterator(this)
    }

    override fun iterator(startingLocation: Int, limit: Long): FlowCursorIterator<T> {
        return FlowCursorIterator(this, startingLocation, limit)
    }

    override fun lastIndexOf(element: T): Int {
        throw UnsupportedOperationException(
                "We cannot determine which index in the table this item exists at efficiently")
    }

    /**
     * @return A list iterator from the [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [T] in the UI thread.
     */
    override fun listIterator(): ListIterator<T> {
        return FlowCursorIterator(this)
    }

    /**
     * @param location The index to start the iterator.
     * @return A list iterator from the [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [T] in the UI thread.
     */
    override fun listIterator(location: Int): ListIterator<T> {
        return FlowCursorIterator(this, location)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        val tableList = internalCursorList.all
        return tableList.subList(fromIndex, toIndex)
    }

    override fun close() {
        internalCursorList.close()
    }

    class Builder<T : Any> {

        internal val table: Class<T>

        internal var cursor: FlowCursor? = null
        internal var modelQueriable: ModelQueriable<T>

        internal constructor(cursorList: FlowCursorList<T>) {
            table = cursorList.table
            cursor = cursorList.cursor
            modelQueriable = cursorList.modelQueriable
        }

        constructor(modelQueriable: ModelQueriable<T>) {
            this.table = modelQueriable.table
            this.modelQueriable = modelQueriable
        }

        fun cursor(cursor: FlowCursor) = apply {
            this.cursor = cursor
        }

        fun build() = FlowQueryList(this)
    }

    companion object {

        private val REFRESH_HANDLER = Handler(Looper.myLooper())
    }


}
