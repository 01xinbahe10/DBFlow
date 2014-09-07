package com.raizlabs.android.dbflow.cache;

import android.support.v4.util.LruCache;

import com.raizlabs.android.dbflow.config.DBConfiguration;
import com.raizlabs.android.dbflow.DatabaseHelperListener;
import com.raizlabs.android.dbflow.config.FlowSQLiteOpenHelper;
import com.raizlabs.android.dbflow.structure.DBStructure;
import com.raizlabs.android.dbflow.structure.Model;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
public class ModelCache {

    private LruCache<Class<? extends Model>, Model> mModelCache;

    private DBConfiguration mDbConfiguration;

    private DBStructure mStructure;

    private FlowSQLiteOpenHelper mHelper;

    public void initialize() {
        initialize(null);
    }

    public void initialize(DatabaseHelperListener helperListener) {
        initialize(new DBConfiguration.Builder().create(), helperListener);
    }

    public void initialize(DBConfiguration dbConfiguration, DatabaseHelperListener helperListener) {
        mDbConfiguration = dbConfiguration;
        mModelCache = new LruCache<Class<? extends Model>, Model>(dbConfiguration.getCacheSize());

        mStructure = new DBStructure(dbConfiguration);

        mHelper = new FlowSQLiteOpenHelper(dbConfiguration);
        mHelper.setDatabaseListener(helperListener);
    }

    public LruCache<Class<? extends Model>, Model> getModelCache() {
        return mModelCache;
    }

    public DBConfiguration getDbConfiguration() {
        return mDbConfiguration;
    }

    public DBStructure getStructure() {
        return mStructure;
    }

    public String getTableName(Class<? extends Model> model) {
        return mStructure.getTableStructure().get(model).getTableName();
    }

    public FlowSQLiteOpenHelper getHelper() {
        return mHelper;
    }
}
