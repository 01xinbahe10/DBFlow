package com.grosner.dbflow.app;

import android.database.sqlite.SQLiteDatabase;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
@Migration(version = 3, databaseName = "App")
public class Migration2 extends BaseMigration {
    @Override
    public void migrate(SQLiteDatabase database) {

    }
}
