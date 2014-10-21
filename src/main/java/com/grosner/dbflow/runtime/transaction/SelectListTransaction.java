package com.grosner.dbflow.runtime.transaction;

import com.grosner.dbflow.runtime.DBTransactionInfo;
import com.grosner.dbflow.sql.builder.Condition;
import com.grosner.dbflow.sql.builder.ConditionQueryBuilder;
import com.grosner.dbflow.sql.language.Select;
import com.grosner.dbflow.sql.language.Where;
import com.grosner.dbflow.structure.Model;

import java.util.List;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description: Runs a fetch on the {@link com.grosner.dbflow.runtime.DBTransactionQueue}
 */
public class SelectListTransaction<ModelClass extends Model> extends BaseResultTransaction<List<ModelClass>> {

    private Where<ModelClass> mWhere;

    /**
     * Creates an instance of this class
     *
     * @param flowManager     The database manager to use
     * @param resultReceiver  The result that returns from this query
     * @param tableClass      The table to select from
     * @param whereConditions The conditions to use in the SELECT query
     */
    public SelectListTransaction(ResultReceiver<List<ModelClass>> resultReceiver,
                                 Class<ModelClass> tableClass, Condition... whereConditions) {
        this(new Select().from(tableClass).where(whereConditions), resultReceiver);
    }

    /**
     * Creates this class with a {@link com.grosner.dbflow.sql.language.From}
     *
     * @param where          The completed Sql Statement we will use to fetch the models
     * @param resultReceiver
     */
    public SelectListTransaction(Where<ModelClass> where, ResultReceiver<List<ModelClass>> resultReceiver) {
        super(DBTransactionInfo.createFetch(), resultReceiver);
        mWhere = where;
    }

    /**
     * Creates an instance of this class
     *
     * @param resultReceiver             The result that returns from this query
     * @param whereConditionQueryBuilder The query builder used to SELECT
     * @param columns                    The columns to select
     */
    public SelectListTransaction(ResultReceiver<List<ModelClass>> resultReceiver,
                                 ConditionQueryBuilder<ModelClass> whereConditionQueryBuilder, String... columns) {
        this(new Select(columns).from(whereConditionQueryBuilder.getTableClass()).where(whereConditionQueryBuilder), resultReceiver);
    }

    /**
     * Creates an instance of this class
     *
     * @param resultReceiver The result that returns from this query
     * @param table          The table to select from
     * @param columns        The columns to select
     */
    public SelectListTransaction(ResultReceiver<List<ModelClass>> resultReceiver,
                                 Class<ModelClass> table, String... columns) {
        this(new Select(columns).from(table).where(), resultReceiver);
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
