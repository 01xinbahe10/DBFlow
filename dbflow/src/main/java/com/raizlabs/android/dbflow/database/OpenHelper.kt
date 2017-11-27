package com.raizlabs.android.dbflow.database

/**
 * Description: Abstracts out the [DatabaseHelperDelegate] into the one used in this library.
 */
interface OpenHelper {

    val database: DatabaseWrapper

    val delegate: DatabaseHelperDelegate?

    val isDatabaseIntegrityOk: Boolean

    fun performRestoreFromBackup()

    fun backupDB()

    fun setDatabaseListener(helperListener: DatabaseHelperListener?)

    fun closeDB()
}
