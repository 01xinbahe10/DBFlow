package com.raizlabs.android.dbflow.sql.language

import com.raizlabs.android.dbflow.BaseUnitTest
import com.raizlabs.android.dbflow.config.writableDatabaseForTable
import com.raizlabs.android.dbflow.models.SimpleModel
import com.raizlabs.android.dbflow.models.SimpleModel_Table.name
import com.raizlabs.android.dbflow.models.TwoColumnModel
import com.raizlabs.android.dbflow.models.TwoColumnModel_Table
import com.raizlabs.android.dbflow.models.TwoColumnModel_Table.id
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FromTest : BaseUnitTest() {

    @Test
    fun validateSimpleFrom() {
        writableDatabaseForTable<SimpleModel> {
            assertEquals("SELECT * FROM `SimpleModel`", (select from SimpleModel::class).query.trim())
        }
    }

    @Test
    fun validateProjectionFrom() {
        writableDatabaseForTable<SimpleModel> {
            assertEquals("SELECT `name` FROM `SimpleModel`", (select(name) from SimpleModel::class).query.trim())
        }
    }

    @Test
    fun validateMultipleProjection() {
        writableDatabaseForTable<SimpleModel> {
            assertEquals("SELECT `name`,`name`,`id` FROM `SimpleModel`",
                    (select(name, TwoColumnModel_Table.name, id) from SimpleModel::class).query.trim())
        }
    }

    @Test
    fun validateAlias() {
        writableDatabaseForTable<SimpleModel> {
            assertEquals("SELECT * FROM `SimpleModel` AS `Simple`", (select from SimpleModel::class `as` "Simple").query.trim())
        }
    }

    @Test
    fun validateJoins() {
        writableDatabaseForTable<SimpleModel> {
            val from = (select from SimpleModel::class
                    innerJoin TwoColumnModel::class
                    on name.eq(TwoColumnModel_Table.name.withTable()))
            assertEquals("SELECT * FROM `SimpleModel` INNER JOIN `TwoColumnModel` ON `name`=`TwoColumnModel`.`name`",
                    from.query.trim())
            assertTrue(from.associatedTables.isNotEmpty())
        }
    }
}