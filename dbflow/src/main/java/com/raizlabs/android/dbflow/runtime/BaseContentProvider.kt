package com.raizlabs.android.dbflow.runtime

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

import com.raizlabs.android.dbflow.config.DatabaseDefinition
import com.raizlabs.android.dbflow.config.DatabaseHolder
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.property.IProperty
import com.raizlabs.android.dbflow.sql.language.property.Property
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper
import com.raizlabs.android.dbflow.structure.database.transaction.ITransaction

/**
 * Description: The base provider class that [com.raizlabs.android.dbflow.annotation.provider.ContentProvider]
 * extend when generated.
 */
abstract class BaseContentProvider
protected constructor(databaseHolderClass: Class<out DatabaseHolder>? = null) : ContentProvider() {

    protected open var moduleClass: Class<out DatabaseHolder>? = databaseHolderClass

    protected val database: DatabaseDefinition by lazy { FlowManager.getDatabase(databaseName) }

    protected abstract val databaseName: String

    /**
     * Converts the column into a [Property]. This exists since the property method is static and cannot
     * be referenced easily.
     */
    interface PropertyConverter {
        fun fromName(columnName: String): IProperty<*>
    }

    override fun onCreate(): Boolean {
        // If this is a module, then we need to initialize the module as part
        // of the creation process. We can assume the framework has been general
        // framework has been initialized.
        moduleClass
                ?.let { FlowManager.initModule(it) }
                ?: context?.let { FlowManager.init(it) }
        return true
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val count = intArrayOf(0)
        database.executeTransaction(object : ITransaction {
            override fun execute(databaseWrapper: DatabaseWrapper) {
                for (contentValues in values) {
                    count[0] += bulkInsert(uri, contentValues)
                }
            }
        })

        context?.contentResolver?.notifyChange(uri, null)
        return count[0]
    }

    protected abstract fun bulkInsert(uri: Uri, contentValues: ContentValues): Int

}
