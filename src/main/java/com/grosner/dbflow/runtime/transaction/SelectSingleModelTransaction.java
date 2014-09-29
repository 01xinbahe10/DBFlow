package com.grosner.dbflow.runtime.transaction;

import com.grosner.dbflow.config.FlowManager;
import com.grosner.dbflow.runtime.DBTransactionInfo;
import com.grosner.dbflow.sql.Select;
import com.grosner.dbflow.sql.Where;
import com.grosner.dbflow.sql.builder.Condition;
import com.grosner.dbflow.sql.builder.ConditionQueryBuilder;
import com.grosner.dbflow.structure.Model;

import java.util.List;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description: Runs a fetch on the {@link com.grosner.dbflow.runtime.DBTransactionQueue}, returning only the first item.
 */
public class SelectSingleModelTransaction<ModelClass extends Model> extends BaseResultTransaction<ModelClass> {

    private Where<ModelClass> mWhere;

    /**
     * Creates an instance of this class
     *
     * @param flowManager     The database manager to use
     * @param resultReceiver  The result that returns from this query
     * @param tableClass      The table to select from
     * @param whereConditions The conditions to use in the SELECT query
     */
    public SelectSingleModelTransaction(FlowManager flowManager, Class<ModelClass> tableClass,
                                        ResultReceiver<ModelClass> resultReceiver, Condition...whereConditions) {
        this(new Select(flowManager).from(tableClass).where(whereConditions), resultReceiver);
    }

    /**
     * Creates an instance of this class
     *
     * @param flowManager                The database manager to use
     * @param resultReceiver             The result that returns from this query
     * @param whereConditionQueryBuilder The query builder used to SELECT
     * @param columns                    The columns to select
     */
    public SelectSingleModelTransaction(FlowManager flowManager, ResultReceiver<ModelClass> resultReceiver,
                                        ConditionQueryBuilder<ModelClass> whereConditionQueryBuilder, String... columns) {
        this(new Select(flowManager, columns).from(whereConditionQueryBuilder.getTableClass()).where(whereConditionQueryBuilder), resultReceiver);
    }

    /**
     * Creates this class with a {@link com.grosner.dbflow.sql.From}
     *
     * @param where           The completed Sql Statement we will use to fetch the models
     * @param resultReceiver
     */
    public SelectSingleModelTransaction(Where<ModelClass> where, ResultReceiver<ModelClass> resultReceiver) {
        super(DBTransactionInfo.createFetch(), resultReceiver);
        mWhere = where;
    }


    @Override
    public ModelClass onExecute() {
        return mWhere.querySingle();
    }
}
