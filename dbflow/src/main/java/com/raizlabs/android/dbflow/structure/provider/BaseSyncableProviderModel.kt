package com.raizlabs.android.dbflow.structure.provider

import android.content.ContentProvider

import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.OperatorGroup
import com.raizlabs.android.dbflow.structure.BaseModel
import com.raizlabs.android.dbflow.structure.Model
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper
import com.raizlabs.android.dbflow.structure.database.FlowCursor

/**
 * Description: Provides a base implementation of a [Model] backed
 * by a content provider. All operations sync with the content provider in this app from a [ContentProvider]
 */
abstract class BaseSyncableProviderModel : BaseModel(), ModelProvider {

    override fun insert(wrapper: DatabaseWrapper): Long {
        val rowId = wrapper.insertModel()
        ContentUtils.insert(insertUri, wrapper)
        return rowId
    }

    override fun save(wrapper: DatabaseWrapper): Boolean {
        return if (exists(wrapper)) {
            wrapper.saveModel() && ContentUtils.update(updateUri, wrapper) > 0
        } else {
            wrapper.saveModel() && ContentUtils.insert(insertUri, wrapper) != null
        }
    }

    override fun delete(wrapper: DatabaseWrapper): Boolean = wrapper.deleteModel() && ContentUtils.delete(deleteUri, wrapper) > 0

    override fun update(wrapper: DatabaseWrapper): Boolean = wrapper.updateModel() && ContentUtils.update(updateUri, wrapper) > 0

    override fun load(whereOperatorGroup: OperatorGroup,
                      orderBy: String?,
                      wrapper: DatabaseWrapper,
                      vararg columns: String?) {
        val cursor = ContentUtils.query(FlowManager.context.contentResolver,
                queryUri, whereOperatorGroup, orderBy, *columns)
        cursor?.let {
            val flowCursor = FlowCursor.from(cursor)
            if (flowCursor.moveToFirst()) {
                modelAdapter.loadFromCursor(flowCursor, this, wrapper)
                flowCursor.close()
            }
        }
    }

    override fun load(wrapper: DatabaseWrapper) {
        load(modelAdapter.getPrimaryConditionClause(this), "", wrapper)
    }
}
