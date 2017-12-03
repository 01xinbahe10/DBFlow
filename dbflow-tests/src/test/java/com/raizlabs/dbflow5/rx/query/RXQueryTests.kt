package com.raizlabs.dbflow5.rx.query

import android.database.Cursor
import com.raizlabs.dbflow5.dbflow.BaseUnitTest
import com.raizlabs.dbflow5.config.databaseForTable
import com.raizlabs.dbflow5.database.DatabaseStatement
import com.raizlabs.dbflow5.dbflow.models.SimpleModel
import com.raizlabs.dbflow5.dbflow.models.SimpleModel_Table.name
import com.raizlabs.dbflow5.query.insert
import com.raizlabs.dbflow5.query.property.Property
import com.raizlabs.dbflow5.query.selectCountOf
import com.raizlabs.dbflow5.structure.save
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RXQueryTests : BaseUnitTest() {

    @Test
    fun testCanQuery() {
        databaseForTable<SimpleModel> {
            SimpleModel("Name").save()

            var cursor: Cursor? = null
            (select from SimpleModel::class).rx()
                    .query()
                    .subscribe {
                        cursor = it
                    }

            assertEquals(1, cursor!!.count)
            cursor!!.close()
        }
    }

    @Test
    fun testCanCompileStatement() {
        databaseForTable<SimpleModel> {
            var databaseStatement: DatabaseStatement? = null
            (insert<SimpleModel>().columnValues(name.`is`("name")))
                    .rxBaseQueriable().compileStatement()
                    .subscribe {
                        databaseStatement = it
                    }
            assertNotNull(databaseStatement)
            databaseStatement!!.close()
        }
    }

    @Test
    fun testCountMethod() {
        databaseForTable<SimpleModel> {
            SimpleModel("name").save()
            SimpleModel("name2").save()
            var count = 0L
            (selectCountOf(Property.ALL_PROPERTY) from SimpleModel::class).rx()
                    .longValue().subscribe {
                count = it
            }

            assertEquals(2, count)
        }
    }

    @Test
    fun testInsertMethod() {
        databaseForTable<SimpleModel> {
            var count = 0L
            (insert<SimpleModel>().columnValues(name.eq("name")))
                    .rxBaseQueriable()
                    .executeInsert()
                    .subscribe {
                        count = it
                    }

            assertEquals(1, count)
        }
    }

    @Test
    fun testExecuteUpdateDelete() {

    }
}