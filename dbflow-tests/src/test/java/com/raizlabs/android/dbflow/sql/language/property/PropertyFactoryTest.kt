package com.raizlabs.android.dbflow.sql.language.property

import com.raizlabs.android.dbflow.BaseUnitTest
import com.raizlabs.android.dbflow.config.writableDatabaseForTable
import com.raizlabs.android.dbflow.models.SimpleModel
import com.raizlabs.android.dbflow.sql.language.select
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyFactoryTest : BaseUnitTest() {

    @Test
    fun testPrimitives() {
        assertEquals("'c'", 'c'.property.query)
        assertEquals("5", 5.property.query)
        assertEquals("5.0", 5.0.property.query)
        assertEquals("5.0", 5.0f.property.query)
        assertEquals("5", 5L.property.query)
        assertEquals("5", 5.toShort().property.query)
        assertEquals("5", 5.toByte().property.query)
        val nullable: Any? = null
        assertEquals("NULL", nullable.property.query)
        writableDatabaseForTable<SimpleModel> {
            assertEquals("(SELECT * FROM `SimpleModel`)", (select from SimpleModel::class).property.query)
        }
        assertEquals("SomethingCool", propertyString<String>("SomethingCool").query)
    }
}