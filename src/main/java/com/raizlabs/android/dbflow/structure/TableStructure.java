package com.raizlabs.android.dbflow.structure;

import com.raizlabs.android.dbflow.ReflectionUtils;
import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.raizlabs.android.dbflow.sql.TableCreationQueryBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description: This class defines the structure of this table for reference later. It holds information
 * such as the type of {@link com.raizlabs.android.dbflow.structure.Model}, the tablename, the column names,
 * primary keys, and foreign keys.
 */
public class TableStructure<ModelType extends Model> {

    /**
     * The Model class this table connects to
     */
    private Class<ModelType> mModelType;

    /**
     * The name of the table. Can be different from the class name
     */
    private String mTableName;

    /**
     * The mapping between the fields of a {@link com.raizlabs.android.dbflow.structure.Model}
     * and the name of the columns
     */
    private Map<Field, String> mColumnNames;

    /**
     * The primary keys of this table. They must not be empty.
     */
    private List<Field> mPrimaryKeys;

    /**
     * The foreign keys of this table.
     */
    private List<Field> mForeignKeys;

    /**
     * Holds the Creation Query in this table for reference when we create it
     */
    private TableCreationQueryBuilder mCreationQuery;

    /**
     * Builds the structure of this table based on the {@link com.raizlabs.android.dbflow.structure.Model}
     * class passed in.
     * @param modelType
     */
    public TableStructure(Class<ModelType> modelType) {

        mColumnNames = new HashMap<Field, String>();

        mModelType = modelType;

        Table table = mModelType.getAnnotation(Table.class);
        if (table != null) {
            mTableName = table.name();
        } else {
            mTableName = mModelType.getSimpleName();
        }

        List<Field> fields = new ArrayList<Field>();
        fields = ReflectionUtils.getAllFields(fields, mModelType);

        // Generating creation query on the fly to be processed later
        mCreationQuery = new TableCreationQueryBuilder();
        mCreationQuery.appendCreateTableIfNotExists(mTableName);

        ArrayList<QueryBuilder> mColumnDefinitions = new ArrayList<QueryBuilder>();

        // Loop through fields and store their corresponding field names
        // Also determine if its primary key or foreign key
        for (Field field : fields) {
            TableCreationQueryBuilder tableCreationQuery = new TableCreationQueryBuilder();
            Class type = field.getType();

            String columnName;
            Column column = field.getAnnotation(Column.class);
            if (column.name() != null && !column.name().equals("")) {
                columnName = column.name();
            } else {
                columnName = field.getName();
            }

            mColumnNames.put(field, columnName);

            if (column.columnType().type() == ColumnType.PRIMARY_KEY
                    || column.columnType().type() == ColumnType.PRIMARY_KEY_AUTO_INCREMENT) {
                mPrimaryKeys.add(field);
            } else if (column.columnType().type() == ColumnType.FOREIGN_KEY) {
                mForeignKeys.add(field);
            }

            if(SQLiteType.containsClass(type)) {
                tableCreationQuery.append(columnName)
                        .appendSpace()
                        .appendType(type);
            } else if(ReflectionUtils.isSubclassOf(type, Enum.class)) {
                tableCreationQuery.append(columnName)
                        .appendSpace()
                        .appendSQLiteType(SQLiteType.TEXT);
            }

            mColumnDefinitions.add(tableCreationQuery.appendColumn(column));
        }

        if(mPrimaryKeys.isEmpty()) {
            throw new PrimaryKeyNotFoundException("Table: " + mTableName + " must define a primary key");
        }

        int count = 0;
        QueryBuilder primaryKeyQueryBuilder = new QueryBuilder().append("PRIMARY KEY(");
        for(int i  =0 ; i< mPrimaryKeys.size(); i++){
            Column primaryKey = mPrimaryKeys.get(i).getAnnotation(Column.class);
            if(primaryKey.columnType().type() != ColumnType.PRIMARY_KEY_AUTO_INCREMENT) {
                count++;
                primaryKeyQueryBuilder.append(mColumnNames.get(mPrimaryKeys.get(i)));
                if (i < mPrimaryKeys.size() - 1) {
                    primaryKeyQueryBuilder.append(", ");
                }
            }
        }

        if(count>0) {
            primaryKeyQueryBuilder.append(")");
            mColumnDefinitions.add(primaryKeyQueryBuilder);
        }

        QueryBuilder foreignKeyQueryBuilder;
        for(int i = 0; i < mForeignKeys.size(); i++){
            Field foreignKeyField = mForeignKeys.get(i);
            foreignKeyQueryBuilder = new QueryBuilder().append("FOREIGN KEY(");

            Column foreignKey = foreignKeyField.getAnnotation(Column.class);

            foreignKeyQueryBuilder.append(mColumnNames.get(foreignKeyField))
                    .append(") REFERENCES ")
                    .append(mTableName)
                    .append("(").append(foreignKey.foreignColumn()).append(")");

            mColumnDefinitions.add(foreignKeyQueryBuilder);
        }


        mCreationQuery.appendColumnDefinitions(mColumnDefinitions).append(");");

    }

    /**
     * Returns the query that we use to create this table.
     * @return
     */
    public QueryBuilder getCreationQuery() {
        return mCreationQuery;
    }

    /**
     * Returns this table name
     * @return
     */
    public String getTableName() {
        return mTableName;
    }
}
