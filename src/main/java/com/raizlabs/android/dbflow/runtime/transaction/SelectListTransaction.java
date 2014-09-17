package com.raizlabs.android.dbflow.runtime.transaction;

import com.raizlabs.android.dbflow.runtime.DBTransactionInfo;
import com.raizlabs.android.dbflow.sql.Select;
import com.raizlabs.android.dbflow.sql.Where;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.List;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description: Runs a fetch on the {@link com.raizlabs.android.dbflow.runtime.DBTransactionQueue}
 */
public class SelectListTransaction<ModelClass extends Model> extends BaseResultTransaction<ModelClass, List<ModelClass>> {

    private Where<ModelClass> mWhere;

    /**
     * Creates an instance of this classs with defaulted {@link com.raizlabs.android.dbflow.sql.Select} all.
     * @param tableClass
     * @param resultReceiver
     */
    public SelectListTransaction(Class<ModelClass> tableClass, ResultReceiver<List<ModelClass>> resultReceiver) {
        this(tableClass, new Select(), resultReceiver);
    }

    /**
     * Creates this class with the specified arguments.
     * @param tableClass The class we will retrieve the models from
     * @param select The select statement we will use to retrieve them.
     * @param resultReceiver The result we get.
     */
    public SelectListTransaction(Class<ModelClass> tableClass, Select select, ResultReceiver<List<ModelClass>> resultReceiver) {
        this(select.from(tableClass).where(), resultReceiver);
    }

    /**
     * Creates this class with a {@link com.raizlabs.android.dbflow.sql.From}
     * @param where The completed Sql Statement we will use to fetch the models
     * @param resultReceiver
     */
    public SelectListTransaction(Where<ModelClass> where, ResultReceiver<List<ModelClass>> resultReceiver) {
        super(DBTransactionInfo.createFetch(), resultReceiver);
        mWhere = where;
    }

    @Override
    public boolean onReady() {
        return mWhere != null;
    }

    @Override
    public List<ModelClass> onExecute() {
        return mWhere.queryList();
    }

}
