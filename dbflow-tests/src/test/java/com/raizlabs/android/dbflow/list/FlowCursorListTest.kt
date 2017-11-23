package com.raizlabs.android.dbflow.list

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.raizlabs.android.dbflow.BaseUnitTest
import com.raizlabs.android.dbflow.models.SimpleModel
import com.raizlabs.android.dbflow.sql.language.from
import com.raizlabs.android.dbflow.sql.language.select
import com.raizlabs.android.dbflow.sql.queriable.cursor
import com.raizlabs.android.dbflow.structure.save
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Description:
 */
class FlowCursorListTest : BaseUnitTest() {

    @Test
    fun validateCursorPassed() {
        val cursor = (select from SimpleModel::class).cursor
        val list = FlowCursorList.Builder(SimpleModel::class.java)
                .cursor(cursor)
                .build()

        assertEquals(cursor, list.cursor())
    }

    @Test
    fun validateModelQueriable() {
        val modelQueriable = (select from SimpleModel::class)
        val list = FlowCursorList.Builder(modelQueriable)
                .build()

        assertEquals(modelQueriable, list.modelQueriable)
    }

    @Test
    fun validateSpecialModelCache() {
        (0..9).forEach {
            SimpleModel("$it").save()
        }

        val list = (select from SimpleModel::class).cursorList()
        assertEquals(10, list.count)
        val firsItem = list[0]
        assertEquals(firsItem, firsItem)
        assertEquals(list[2], list[2])

        assertNotEquals(firsItem, list[0])
    }

    @Test
    fun validateGetAll() {
        (0..9).forEach {
            SimpleModel("$it").save()
        }

        val list = (select from SimpleModel::class).cursorList()
        val all = list.all
        assertEquals(list.count, all.size.toLong())
        all.indices.forEach {
            assertEquals(all[it], list[it])
        }
    }

    @Test
    fun validateCursorChange() {
        (0..9).forEach {
            SimpleModel("$it").save()
        }

        val list = (select from SimpleModel::class).cursorList()

        val listener = mock<FlowCursorList.OnCursorRefreshListener<SimpleModel>>()
        list.addOnCursorRefreshListener(listener)
        assertEquals(10, list.count)
        SimpleModel("10").save()
        list.refresh()
        assertEquals(11, list.count)

        verify(listener).onCursorRefreshed(list)

        list.removeOnCursorRefreshListener(listener)

        list.refresh()
        verify(listener, times(1)).onCursorRefreshed(list)
    }
}

