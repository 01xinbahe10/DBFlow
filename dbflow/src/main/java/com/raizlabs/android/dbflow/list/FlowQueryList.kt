package com.raizlabs.android.dbflow.list

import android.annotation.TargetApi
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.raizlabs.android.dbflow.list.FlowCursorList.OnCursorRefreshListener
import com.raizlabs.android.dbflow.runtime.FlowContentObserver
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.sql.queriable.ModelQueriable
import com.raizlabs.android.dbflow.structure.InstanceAdapter
import com.raizlabs.android.dbflow.structure.ModelAdapter
import com.raizlabs.android.dbflow.structure.database.transaction.DefaultTransactionQueue
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction

/**
 * Description: Operates very similiar to a [java.util.List] except its backed by a table cursor. All of
 * the [java.util.List] modifications default to the main thread, but it can be set to
 * run on the [DefaultTransactionQueue]. Register a [Transaction.Success]
 * on this list to know when the results complete. NOTE: any modifications to this list will be reflected
 * on the underlying table.
 */
class FlowQueryList<TModel>(
        /**
         * If true, we will make all modifications on the [DefaultTransactionQueue], else
         * we will run it on the main thread.
         */
        val transact: Boolean = false,
        private var changeInTransaction: Boolean = false,
        /**
         * Holds the table cursor
         */
        val internalCursorList: FlowCursorList<TModel>)
    : FlowContentObserver(), List<TModel>, IFlowCursorIterator<TModel> {

    private var pendingRefresh = false

    /**
     * @return a mutable list that does not reflect changes on the underlying DB.
     */
    val copy: List<TModel>
        get() = internalCursorList.all

    internal val modelAdapter: ModelAdapter<TModel>
        get() = internalCursorList.modelAdapter

    internal val instanceAdapter: InstanceAdapter<TModel>
        get() = internalCursorList.instanceAdapter

    override val count: Long
        get() = internalCursorList.count

    fun changeInTransaction() = changeInTransaction

    private val refreshRunnable = object : Runnable {
        override fun run() {
            synchronized(this) {
                pendingRefresh = false
            }
            refresh()
        }
    }

    internal constructor(builder: Builder<TModel>) : this(
            transact = builder.transact,
            changeInTransaction = builder.changeInTransaction,
            internalCursorList = FlowCursorList.Builder(builder.modelQueriable)
                    .cursor(builder.cursor)
                    .build()
    )

    /**
     * Registers the list for model change events. Internally this refreshes the underlying [FlowCursorList]. Call
     * [.beginTransaction] to bunch up calls to model changes and then [.endTransactionAndNotify] to dispatch
     * and refresh this list when completed.
     */
    fun registerForContentChanges(context: Context) {
        super.registerForContentChanges(context, internalCursorList.table)
    }

    fun addOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<TModel>) {
        internalCursorList.addOnCursorRefreshListener(onCursorRefreshListener)
    }

    fun removeOnCursorRefreshListener(onCursorRefreshListener: OnCursorRefreshListener<TModel>) {
        internalCursorList.removeOnCursorRefreshListener(onCursorRefreshListener)
    }

    override fun registerForContentChanges(context: Context, table: Class<*>) {
        throw RuntimeException(
                "This method is not to be used in the FlowQueryList. We should only ever receive"
                        + " notifications for one class here. Call registerForContentChanges(Context) instead")
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        if (!isInTransaction) {
            refreshAsync()
        } else {
            changeInTransaction = true
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onChange(selfChange: Boolean, uri: Uri) {
        super.onChange(selfChange, uri)
        if (!isInTransaction) {
            refreshAsync()
        } else {
            changeInTransaction = true
        }
    }

    val cursorList: FlowCursorList<TModel>
        get() = internalCursorList

    /**
     * @return Constructs a new [Builder] that reuses the underlying [Cursor], cache,
     * callbacks, and other properties.
     */
    fun newBuilder(): Builder<TModel> {
        return Builder(internalCursorList)
                .changeInTransaction(changeInTransaction)
                .transact(transact)
    }

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

    override fun endTransactionAndNotify() {
        if (changeInTransaction) {
            changeInTransaction = false
            refresh()
        }
        super.endTransactionAndNotify()
    }

    /**
     * Checks to see if the table contains the object only if its a [TModel]
     *
     * @param element A model class. For interface purposes, this must be an Object.
     * @return always false if its anything other than the current table. True if [com.raizlabs.android.dbflow.structure.Model.exists] passes.
     */
    override operator fun contains(element: TModel): Boolean {
        return internalCursorList.instanceAdapter.exists(element)
    }

    /**
     * If the collection is null or empty, we return false.
     *
     * @param elements The collection to check if all exist within the table.
     * @return true if all items exist in table, false if at least one fails.
     */
    override fun containsAll(elements: Collection<TModel>): Boolean {
        var contains = !elements.isEmpty()
        if (contains) {
            contains = elements.all { it in this }
        }
        return contains
    }

    override fun getItem(position: Long): TModel {
        return internalCursorList.getItem(position)
    }

    override fun cursor(): Cursor? {
        return internalCursorList.cursor()
    }

    /**
     * Returns the item from the backing [FlowCursorList]. First call
     * will load the model from the cursor, while subsequent calls will use the cache.
     *
     * @param index the row from the internal [FlowCursorList] query that we use.
     * @return A model converted from the internal [FlowCursorList]. For
     * performance improvements, ensure caching is turned on.
     */
    override operator fun get(index: Int): TModel {
        return internalCursorList.getItem(index.toLong())
    }

    override fun indexOf(element: TModel): Int {
        throw UnsupportedOperationException(
                "We cannot determine which index in the table this item exists at efficiently")
    }

    override fun isEmpty(): Boolean {
        return internalCursorList.isEmpty
    }

    /**
     * @return An iterator from [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [TModel] in the UI thread.
     */
    override fun iterator(): FlowCursorIterator<TModel> {
        return FlowCursorIterator(this)
    }

    override fun iterator(startingLocation: Int, limit: Long): FlowCursorIterator<TModel> {
        return FlowCursorIterator(this, startingLocation, limit)
    }

    override fun lastIndexOf(element: TModel): Int {
        throw UnsupportedOperationException(
                "We cannot determine which index in the table this item exists at efficiently")
    }

    /**
     * @return A list iterator from the [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [TModel] in the UI thread.
     */
    override fun listIterator(): ListIterator<TModel> {
        return FlowCursorIterator(this)
    }

    /**
     * @param location The index to start the iterator.
     * @return A list iterator from the [FlowCursorList.getAll].
     * Be careful as this method will convert all data under this table into a list of [TModel] in the UI thread.
     */
    override fun listIterator(location: Int): ListIterator<TModel> {
        return FlowCursorIterator(this, location)
    }

    override val size: Int
        get() = internalCursorList.count.toInt()

    override fun subList(fromIndex: Int, toIndex: Int): List<TModel> {
        val tableList = internalCursorList.all
        return tableList.subList(fromIndex, toIndex)
    }

    override fun close() {
        internalCursorList.close()
    }

    class Builder<TModel> {

        internal val table: Class<TModel>

        internal var transact: Boolean = false
        internal var changeInTransaction: Boolean = false
        internal var cursor: Cursor? = null
        internal var modelQueriable: ModelQueriable<TModel>

        internal constructor(cursorList: FlowCursorList<TModel>) {
            table = cursorList.table
            cursor = cursorList.cursor()
            modelQueriable = cursorList.modelQueriable
        }

        constructor(table: Class<TModel>) {
            this.table = table
            modelQueriable = SQLite.select().from(table)
        }

        constructor(modelQueriable: ModelQueriable<TModel>) {
            this.table = modelQueriable.table
            this.modelQueriable = modelQueriable
        }

        fun cursor(cursor: Cursor) = apply {
            this.cursor = cursor
        }

        fun transact(transact: Boolean) = apply {
            this.transact = transact
        }

        /**
         * If true, when an operation occurs whenever we call endTransactionAndNotify, we refresh content.
         */
        fun changeInTransaction(changeInTransaction: Boolean) = apply {
            this.changeInTransaction = changeInTransaction
        }

        fun build() = FlowQueryList(this)
    }

    companion object {

        private val REFRESH_HANDLER = Handler(Looper.myLooper())
    }


}
