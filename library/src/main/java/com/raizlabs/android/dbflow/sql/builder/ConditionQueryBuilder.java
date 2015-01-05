package com.raizlabs.android.dbflow.sql.builder;

import android.database.DatabaseUtils;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.converter.TypeConverter;
import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: andrewgrosner
 * Description: Constructs a condition statement for a specific {@link com.raizlabs.android.dbflow.structure.Model} class.
 * This enables easy combining of conditions for SQL statements and will handle converting the model value for each column into
 * the correct database-valued-string.
 */
public class ConditionQueryBuilder<ModelClass extends Model> extends QueryBuilder<ConditionQueryBuilder<ModelClass>> {

    /**
     * The structure of the ModelClass this query pertains to
     */
    private ModelAdapter<ModelClass> mModelAdapter;

    /**
     * The parameters to build this query with
     */
    private List<Condition> mParams = new ArrayList<>();

    /**
     * Whether there is a new param, we will rebuild the query.
     */
    private boolean isChanged = false;

    /**
     * if true, all params must be empty. We will throw an exception if this is not the case.
     */
    private boolean useEmptyParams = false;

    /**
     * The separator between the conditions
     */
    private String mSeparator = Condition.Operation.AND;

    /**
     * Constructs an instance of this class
     * and {@link ModelClass}.
     *
     * @param table      The table to use
     * @param conditions The array of conditions to add to the mapping.
     */
    public ConditionQueryBuilder(Class<ModelClass> table, Condition... conditions) {
        mModelAdapter = FlowManager.getModelAdapter(table);
        putConditions(conditions);
    }

    /**
     * Appends all the conditions from the specified array
     *
     * @param conditions The array of conditions to add to the mapping.
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> putConditions(Condition... conditions) {
        if (conditions.length > 0) {
            for (Condition condition : conditions) {
                mParams.add(condition);
            }
            isChanged = true;
        }
        return this;
    }

    /**
     * Clears all conditions
     */
    public void clear() {
        mParams.clear();
    }

    /**
     * @param columnName The name of the column in the DB
     * @return the specified conditions matching the specified column name.
     * Case sensitive so use the $Table class fields.
     */
    public List<Condition> getConditionsMatchingColumName(String columnName) {
        List<Condition> matching = new ArrayList<>();
        for (Condition condition : mParams) {
            if (condition.columnName().equals(columnName)) {
                matching.add(condition);
            }
        }
        return matching;
    }

    /**
     * @param value The value of the conditions we're looking for
     * @return The specified conditions containing the value we're looking for. This should be the non-type-converted object.
     */
    public List<Condition> getConditionsMatchingValue(Object value) {
        List<Condition> matching = new ArrayList<>();
        for (Condition condition : mParams) {
            if (condition.value() == null ? value == null : condition.value().equals(value)) {
                matching.add(condition);
            }
        }
        return matching;
    }

    @Override
    public String getQuery() {
        // Empty query, we will build it now with params, or if the query has changed.
        if (isChanged || mQuery.length() == 0) {
            isChanged = false;
            mQuery = new StringBuilder();

            int count = 0;
            int paramSize = mParams.size();
            for (int i = 0; i < paramSize; i++) {
                Condition tempCondition = mParams.get(i);
                appendConditionToQuery(tempCondition);
                if (count < paramSize - 1) {
                    if (tempCondition.hasSeparator()) {
                        appendSpaceSeparated(tempCondition.separator());
                    } else {
                        appendSpaceSeparated(mSeparator);
                    }
                }
                count++;
            }
        }

        return mQuery.toString();
    }

    /**
     * Converts the given value for the column if it has a type converter. Then it turns that result into a string.
     *
     * @param value The value of the column in Model format.
     * @return
     */
    @SuppressWarnings("unchecked")
    public String convertValueToString(Object value) {
        String stringVal;
        if (!useEmptyParams && value != null) {
            TypeConverter typeConverter = FlowManager.getTypeConverterForClass(value.getClass());
            if (typeConverter != null) {
                value = typeConverter.getDBValue(value);
            }
        }

        if (value instanceof Number) {
            stringVal = String.valueOf(value);
        } else {
            stringVal = String.valueOf(value);
            if (!stringVal.equals(Condition.Operation.EMPTY_PARAM)) {
                stringVal = DatabaseUtils.sqlEscapeString(stringVal);
            }
        }

        return stringVal;
    }

    /**
     * Internal utility method for appending a condition to the query
     *
     * @param condition The value of the column we are looking for
     * @return This instance
     */
    ConditionQueryBuilder<ModelClass> appendConditionToQuery(Condition condition) {
        condition.appendConditionToQuery(this);
        return this;
    }

    /**
     * Sets the condition separator for when we build the query.
     *
     * @param separator AND, OR, etc.
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> setSeparator(String separator) {
        mSeparator = separator;
        return this;
    }

    /**
     * Sets this class to use empty params ONLY. Cannot mix empty and non-empty for query building.
     *
     * @param useEmptyParams If true, only empty parameters will be accepted.
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> setUseEmptyParams(boolean useEmptyParams) {
        this.useEmptyParams = useEmptyParams;
        return this;
    }


    /**
     * Appends a condition to this map. It will take the value and see if a {@link com.raizlabs.android.dbflow.converter.TypeConverter}
     * exists for the field. If so, we convert it to the database value. Also if the value is a string, we escape the string.
     * EX: columnName = value
     *
     * @param columnName The name of the column in the DB
     * @param value      The value of the column we are looking for
     * @return
     */
    public ConditionQueryBuilder<ModelClass> putCondition(String columnName, Object value) {
        return putCondition(columnName, Condition.Operation.EQUALS, value);
    }

    /**
     * Appends a condition to this map. It will take the value and see if a {@link com.raizlabs.android.dbflow.converter.TypeConverter}
     * exists for the field. If so, we convert it to the database value. Also if the value is a string, we escape the string.
     *
     * @param columnName The name of the column in the DB
     * @param operator   The operator to use "=", "<", etc.
     * @param value      The value of the column we are looking for
     * @return
     */
    public ConditionQueryBuilder<ModelClass> putCondition(String columnName, String operator, Object value) {
        if (useEmptyParams && !Condition.Operation.EMPTY_PARAM.equals(value)) {
            throw new IllegalStateException("The " + ConditionQueryBuilder.class.getSimpleName() + " is " +
                    "operating in empty param mode. All params must be empty");
        }
        return putCondition(Condition.column(columnName).operation(operator).value(value));

    }

    /**
     * Appends a condition to this map. It will take the value and see if a {@link com.raizlabs.android.dbflow.converter.TypeConverter}
     * exists for the field. If so, we convert it to the database valu  e. Also if the value is a string, we escape the string.
     *
     * @param condition The condition to append
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> putCondition(Condition condition) {
        mParams.add(condition);
        isChanged = true;
        return this;
    }

    /**
     * This will append all the primary key names with empty params from the underlying {@link com.raizlabs.android.dbflow.structure.ModelAdapter}.
     * Ex: name = ?, columnName = ?
     *
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> emptyPrimaryConditions() {
        return append(mModelAdapter.getPrimaryModelWhere());
    }

    /**
     * Appends all the parameters from the specified map
     *
     * @param params The mapping between column names and the string-represented value
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> putConditions(List<Condition> params) {
        if (params != null && !params.isEmpty()) {
            mParams.addAll(params);
            isChanged = true;
        }
        return this;
    }

    /**
     * Appends all the parameters from the specified map. The keys are ignored and now just take the values.
     * This is for backportability.
     * @param params
     * @deprecated {@link #putConditions(java.util.List)}
     * @return
     */
    @Deprecated
    public ConditionQueryBuilder<ModelClass> putConditionMap(Map<String, Condition> params) {
        if (params != null && !params.isEmpty()) {
            mParams.addAll(params.values());
            isChanged = true;
        }
        return this;
    }

    /**
     * Appends an empty condition to this map that will be represented with a "?". All params must either be empty or not.
     *
     * @param columnName The name of the column in the DB
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> emptyCondition(String columnName) {
        useEmptyParams = true;
        return putCondition(columnName, Condition.Operation.EMPTY_PARAM);
    }

    /**
     * Returns the raw query without converting the values of {@link com.raizlabs.android.dbflow.sql.builder.Condition}.
     *
     * @return
     */
    public String getRawQuery() {
        QueryBuilder rawQuery = new QueryBuilder();

        int count = 0;
        int paramSize = mParams.size();
        for (int i = 0; i < paramSize; i++) {
            Condition condition = mParams.get(i);
            condition.appendConditionToRawQuery(rawQuery);
            if (count < paramSize - 1) {
                if (condition.hasSeparator()) {
                    rawQuery.appendSpaceSeparated(condition.separator());
                } else {
                    rawQuery.appendSpaceSeparated(mSeparator);
                }
            }
            count++;
        }

        return rawQuery.toString();
    }

    /**
     * Replaces empty parameter values such as "columnName = ?" with the array of values passed in. It must
     * match the count of columns that are in this where query.
     *
     * @param values The values of the fields we wish to replace. Must match the length of the empty params and must be in empty param mode.
     * @return The query with the parameters filled in.
     */
    public ConditionQueryBuilder<ModelClass> replaceEmptyParams(Object... values) {
        if (!useEmptyParams) {
            throw new IllegalStateException("The " + ConditionQueryBuilder.class.getSimpleName() + " is " +
                    "not operating in empty param mode.");
        }
        if (mParams.size() != values.length) {
            throw new IllegalArgumentException("The count of values MUST match the number of columns they correspond to for " +
                    mModelAdapter.getTableName());
        }

        ConditionQueryBuilder<ModelClass> conditionQueryBuilder =
                new ConditionQueryBuilder<ModelClass>(mModelAdapter.getModelClass());
        for (int i = 0; i < values.length; i++) {
            conditionQueryBuilder.putCondition(mParams.get(i).columnName(), values[i]);
        }

        return conditionQueryBuilder;
    }

    /**
     * @return the {@link ModelClass} that this query belongs to
     */
    public Class<ModelClass> getTableClass() {
        return getModelAdapter().getModelClass();
    }

    /**
     * @return the table structure that this {@link ConditionQueryBuilder} uses.
     */
    public ModelAdapter<ModelClass> getModelAdapter() {
        return mModelAdapter;
    }

    /**
     * Sets the previous condition to use the OR separator.
     * @param condition The condition to "OR"
     * @return This instance
     */
    public ConditionQueryBuilder<ModelClass> or(Condition condition) {
        setPreviousSeparator(Condition.Operation.OR);
        putCondition(condition);
        return this;
    }

    /**
     * Sets the previous condition to use the LIKE separator
     * @param condition The condition to "LIKE"
     * @return
     */
    public ConditionQueryBuilder<ModelClass> like(Condition condition) {
        setPreviousSeparator(Condition.Operation.LIKE);
        putCondition(condition);
        return this;
    }

    public ConditionQueryBuilder<ModelClass> glob(Condition condition) {
        setPreviousSeparator(Condition.Operation.GLOB);
        putCondition(condition);
        return this;
    }

    protected void setPreviousSeparator(String separator) {
        if (mParams.size() > 0) {
            // set previous to use OR separator
            mParams.get(mParams.size()).separator(separator);
        }
    }
}
