package com.raizlabs.android.dbflow.sql.language

import com.raizlabs.android.dbflow.appendQualifier
import com.raizlabs.android.dbflow.sql.Query

/**
 * Description: The condition that represents EXISTS in a SQL statement.
 */
class ExistenceOperator(private val innerWhere: Where<*>) : SQLOperator, Query {

    override val query: String
        get() = appendToQuery()

    override fun appendConditionToQuery(queryBuilder: StringBuilder) {
        queryBuilder.appendQualifier("EXISTS", "(" + innerWhere.query.trim({ it <= ' ' }) + ")")
    }

    override fun columnName(): String {
        throw RuntimeException("Method not valid for ExistenceOperator")
    }

    override fun separator(): String? {
        throw RuntimeException("Method not valid for ExistenceOperator")
    }

    override fun separator(separator: String): SQLOperator {
        // not used.
        throw RuntimeException("Method not valid for ExistenceOperator")
    }

    override fun hasSeparator(): Boolean = false

    override fun operation(): String = ""

    override fun value(): Any? = innerWhere

}
