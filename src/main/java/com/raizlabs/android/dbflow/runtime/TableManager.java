package com.raizlabs.android.dbflow.runtime;

import com.raizlabs.android.dbflow.runtime.transaction.ResultReceiver;
import com.raizlabs.android.dbflow.sql.Select;
import com.raizlabs.android.dbflow.sql.builder.AbstractWhereQueryBuilder;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.List;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description: This class manages a single table, wrapping all of the
 * {@link com.raizlabs.android.dbflow.runtime.DatabaseManager} operations with the {@link ModelClass}
 */
public class TableManager<ModelClass extends Model> extends DatabaseManager {

    private Class<ModelClass> mTableClass;

    /**
     * Constructs a new instance. If createNewQueue is true, it will create a new looper. So only use this
     * if you need to have a second queue to have certain transactions go faster. If you create a new queue,
     * it will use up much more memory.
     * @param createNewQueue Create a separate request queue from the shared one.
     * @param mTableClass The table class this manager corresponds to
     */
    public TableManager(boolean createNewQueue, Class<ModelClass> mTableClass) {
        super(mTableClass.getSimpleName(), createNewQueue);
        this.mTableClass = mTableClass;
    }

    /**
     * Constructs a new instance.
     * @param mTableClass The table class this manager corresponds to
     */
    public TableManager(Class<ModelClass> mTableClass) {
        super(mTableClass.getSimpleName(), false);
        this.mTableClass = mTableClass;
    }

    public void selectAllFromTable(ResultReceiver<List<ModelClass>> resultReceiver) {
        super.selectAllFromTable(mTableClass, resultReceiver);
    }

    public void selectFromTable(Select select, ResultReceiver<List<ModelClass>> resultReceiver) {
        super.selectFromTable(mTableClass, select, resultReceiver);
    }

    public ModelClass selectModelWithWhere(AbstractWhereQueryBuilder<ModelClass> whereQueryBuilder, String... values) {
        return super.selectModelWithWhere(mTableClass, whereQueryBuilder, values);
    }

    public ModelClass selectModelById(String... ids) {
        return super.selectModelById(mTableClass, ids);
    }

    public void selectModelWithWhere(ResultReceiver<ModelClass> resultReceiver, AbstractWhereQueryBuilder<ModelClass> whereQueryBuilder, String... ids) {
        super.selectModelWithWhere(mTableClass, resultReceiver, whereQueryBuilder, ids);
    }

    public void selectModelById(ResultReceiver<ModelClass> resultReceiver, String... ids) {
        super.selectModelById(mTableClass, resultReceiver, ids);
    }

    public void deleteTable(DBTransactionInfo transactionInfo) {
        super.deleteTable(transactionInfo, mTableClass);
    }

    public void deleteModelsWithQuery(DBTransactionInfo transctionInfo,
                                      AbstractWhereQueryBuilder<ModelClass> abstractWhereQueryBuilder) {
        super.deleteModelsWithQuery(transctionInfo, abstractWhereQueryBuilder, mTableClass);
    }
}
