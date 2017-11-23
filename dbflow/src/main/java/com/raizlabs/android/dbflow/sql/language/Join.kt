package com.raizlabs.android.dbflow.sql.language

import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.Query
import com.raizlabs.android.dbflow.sql.QueryBuilder
import com.raizlabs.android.dbflow.sql.language.property.IProperty
import com.raizlabs.android.dbflow.sql.language.property.PropertyFactory
import com.raizlabs.android.dbflow.sql.queriable.ModelQueriable
import java.util.*
import kotlin.reflect.KClass

/**
 * Description: Specifies a SQLite JOIN statement
 */
class Join<TModel : Any, TFromModel : Any> : Query {

    val table: Class<TModel>

    /**
     * The type of JOIN to use
     */
    private var type: JoinType

    /**
     * The FROM statement that prefixes this statement.
     */
    private var from: From<TFromModel>

    /**
     * The alias to name the JOIN
     */
    private var alias: NameAlias

    /**
     * The ON conditions
     */
    private var onGroup: OperatorGroup? = null

    /**
     * What columns to use.
     */
    private val using = ArrayList<IProperty<*>>()

    override// natural joins do no have on or using clauses.
    val query: String
        get() {
            val queryBuilder = QueryBuilder()

            queryBuilder.append(type.name.replace("_", " ")).appendSpace()

            queryBuilder.append("JOIN")
                    .appendSpace()
                    .append(alias!!.fullQuery)
                    .appendSpace()
            if (JoinType.NATURAL != type) {
                onGroup?.let { onGroup ->
                    queryBuilder.append("ON")
                            .appendSpace()
                            .append(onGroup.query)
                            .appendSpace()
                } ?: if (!using.isEmpty()) {
                    queryBuilder.append("USING (")
                            .appendList(using)
                            .append(")").appendSpace()
                }
            }
            return queryBuilder.query
        }

    /**
     * The specific type of JOIN that is used.
     */
    enum class JoinType {

        /**
         * an extension of the INNER JOIN. Though SQL standard defines three types of OUTER JOINs: LEFT, RIGHT,
         * and FULL but SQLite only supports the LEFT OUTER JOIN.
         *
         *
         * The OUTER JOINs have a condition that is identical to INNER JOINs, expressed using an ON, USING, or NATURAL keyword.
         * The initial results table is calculated the same way. Once the primary JOIN is calculated,
         * an OUTER join will take any unjoined rows from one or both tables, pad them out with NULLs,
         * and append them to the resulting table.
         */
        LEFT_OUTER,

        /**
         * creates a new result table by combining column values of two tables (table1 and table2) based upon the join-predicate.
         * The query compares each row of table1 with each row of table2 to find all pairs of rows which satisfy the join-predicate.
         * When the join-predicate is satisfied, column values for each matched pair of rows of A and B are combined into a result row
         */
        INNER,

        /**
         * matches every row of the first table with every row of the second table. If the input tables
         * have x and y columns, respectively, the resulting table will have x*y columns.
         * Because CROSS JOINs have the potential to generate extremely large tables,
         * care must be taken to only use them when appropriate.
         */
        CROSS,

        /**
         * a join that performs the same task as an INNER or LEFT JOIN, in which the ON or USING
         * clause refers to all columns that the tables to be joined have in common.
         */
        NATURAL
    }

    constructor(from: From<TFromModel>, table: Class<TModel>, joinType: JoinType) {
        this.from = from
        this.table = table
        type = joinType
        alias = NameAlias.Builder(FlowManager.getTableName(table)).build()
    }

    constructor(from: From<TFromModel>, joinType: JoinType,
                modelQueriable: ModelQueriable<TModel>) {
        table = modelQueriable.table
        this.from = from
        type = joinType
        alias = PropertyFactory.from(modelQueriable).nameAlias
    }

    /**
     * Specifies if the JOIN has a name it should be called.
     *
     * @param alias The name to give it
     * @return This instance
     */
    fun `as`(alias: String) = apply {
        this.alias = this.alias
                .newBuilder()
                .`as`(alias)
                .build()
    }

    /**
     * Specify the conditions that the JOIN is on
     *
     * @param onConditions The conditions it is on
     * @return The FROM that this JOIN came from
     */
    fun on(vararg onConditions: SQLOperator): From<TFromModel> {
        checkNatural()
        onGroup = OperatorGroup.nonGroupingClause().apply {
            andAll(*onConditions)
        }
        return from
    }

    /**
     * The USING statement of the JOIN
     *
     * @param columns THe columns to use
     * @return The FROM that this JOIN came from
     */
    fun using(vararg columns: IProperty<*>): From<TFromModel> {
        checkNatural()
        using.addAll(columns)
        return from
    }

    /**
     * @return End this [Join]. Used for [Join.JoinType.NATURAL]
     */
    fun end(): From<TFromModel> = from

    private fun checkNatural() {
        if (JoinType.NATURAL == type) {
            throw IllegalArgumentException("Cannot specify a clause for this join if its NATURAL." + " Specifying a clause would have no effect. Call end() to continue the query.")
        }
    }
}

infix fun <T : Any, V : Any> From<V>.innerJoin(joinTable: KClass<T>): Join<T, V> = join(joinTable.java, Join.JoinType.INNER)

infix fun <T : Any, V : Any> From<V>.crossJoin(joinTable: KClass<T>): Join<T, V> = join(joinTable.java, Join.JoinType.CROSS)

infix fun <T : Any, V : Any> From<V>.leftOuterJoin(joinTable: KClass<T>): Join<T, V> = join(joinTable.java, Join.JoinType.LEFT_OUTER)

infix fun <T : Any, V : Any> From<V>.naturalJoin(joinTable: KClass<T>): Join<T, V> = join(joinTable.java, Join.JoinType.NATURAL)

infix fun <T : Any, V : Any> Join<T, V>.on(sqlOperator: SQLOperator): From<V> = on(sqlOperator)

infix fun <T : Any, V : Any> Join<T, V>.using(property: IProperty<*>): From<V> = using(property)
