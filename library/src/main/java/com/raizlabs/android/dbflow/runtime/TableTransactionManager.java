package com.raizlabs.android.dbflow.runtime;

import com.raizlabs.android.dbflow.runtime.transaction.TransactionListener;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.List;

/**
 * Description: This class manages a single table, wrapping all of the relevant
 * {@link TransactionManager} operations with the {@link ModelClass}
 */
public class TableTransactionManager<ModelClass extends Model> extends TransactionManager {

    private Class<ModelClass> mTableClass;

    /**
     * Constructs a new instance. If createNewQueue is true, it will create a new looper. So only use this
     * if you need to have a second queue to have certain transactions go faster. If you create a new queue,
     * it will use up much more memory.
     *
     * @param createNewQueue Create a separate request queue from the shared one.
     * @param mTableClass    The table class this manager corresponds to
     */
    public TableTransactionManager(boolean createNewQueue, Class<ModelClass> mTableClass) {
        super(mTableClass.getSimpleName(), createNewQueue);
        this.mTableClass = mTableClass;
    }

    /**
     * Constructs a new instance of this class with the shared {@link com.raizlabs.android.dbflow.config.FlowManager} and
     * uses the shared {@link com.raizlabs.android.dbflow.runtime.DBTransactionQueue}
     *
     * @param mTableClass The table class this manager corresponds to
     */
    public TableTransactionManager(Class<ModelClass> mTableClass) {
        super(mTableClass.getSimpleName(), false);
        this.mTableClass = mTableClass;
    }

    /**
     * @param transactionListener The result of the selection will be placed here on the main thread.
     * @see #fetchFromTable(Class, com.raizlabs.android.dbflow.runtime.transaction.TransactionListener, com.raizlabs.android.dbflow.sql.builder.Condition...)
     */
    public void fetchFromTable(TransactionListener<List<ModelClass>> transactionListener, Condition... conditions) {
        super.fetchFromTable(mTableClass, transactionListener, conditions);
    }

    /**
     * @param transactionListener The result will be passed here.
     * @param ids            The list of ids given by the {@link ModelClass}
     * @see #fetchModelById(Class, com.raizlabs.android.dbflow.runtime.transaction.TransactionListener, Object...)
     */
    public void fetchModelById(TransactionListener<ModelClass> transactionListener, Object... ids) {
        super.fetchModelById(mTableClass, transactionListener, ids);
    }

    /**
     * @param transactionInfo The information on how we should approach this request.
     * @param conditions      The list of conditions to delete with
     * @see #delete(DBTransactionInfo, Class, com.raizlabs.android.dbflow.sql.builder.Condition...)
     */
    public void delete(DBTransactionInfo transactionInfo, Condition... conditions) {
        super.delete(transactionInfo, mTableClass, conditions);
    }

    /**
     * Returns the table class for this Table Transaction manager
     *
     * @return
     */
    public Class<ModelClass> getTableClass() {
        return mTableClass;
    }
}
