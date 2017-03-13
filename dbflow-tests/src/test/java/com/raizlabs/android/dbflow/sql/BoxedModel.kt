package com.raizlabs.android.dbflow.sql

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.NotNull
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.TestDatabase
import com.raizlabs.android.dbflow.structure.TestModel1

/**
 * Description: Test to ensure that nullable and non-null and boxed primitive classes work as expected.
 */
@Table(database = TestDatabase::class)
class BoxedModel : TestModel1() {

    @Column
    @PrimaryKey
    @NotNull
    var id: Long? = 1L

    @Column
    @NotNull
    var integerPrimitiveFieldNotNull = 1

    @Column
    @NotNull
    var integerFieldNotNull: Int? = 1

    @Column
    var integerField: Int? = 1

    @Column
    @NotNull
    var stringFieldNotNull: String? = "1"

    @Column
    var stringField: String? = "1"

}
