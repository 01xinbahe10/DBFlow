package com.raizlabs.android.dbflow.transaction

import com.raizlabs.android.dbflow.adapter.InternalAdapter
import com.raizlabs.android.dbflow.config.modelAdapter
import com.raizlabs.android.dbflow.database.DatabaseWrapper
import com.raizlabs.android.dbflow.structure.Model
import java.util.*

/**
 * Description: Simple interface for acting on a model in a Transaction or list of [Model]
 */
private typealias ProcessModelList<TModel> = (List<TModel>, InternalAdapter<TModel>, DatabaseWrapper) -> Unit

/**
 * Description: Similiar to [ProcessModelTransaction] in that it allows you to store a [List] of
 * [Model], except that it performs it as efficiently as possible. Also due to way the class operates,
 * only one kind of [TModel] is allowed.
 */
class FastStoreModelTransaction<TModel> internal constructor(builder: Builder<TModel>) : ITransaction<List<TModel>?> {

    internal val models: List<TModel>?
    internal val processModelList: ProcessModelList<TModel>
    internal val internalAdapter: InternalAdapter<TModel>

    init {
        models = builder.models
        processModelList = builder.processModelList
        internalAdapter = builder.internalAdapter
    }

    override fun execute(databaseWrapper: DatabaseWrapper): List<TModel>? {
        if (models != null) {
            processModelList(models, internalAdapter, databaseWrapper)
        }
        return models
    }

    /**
     * Makes it easy to build a [ProcessModelTransaction].
     *
     * @param <TModel>
    </TModel> */
    class Builder<TModel> internal constructor(internal val internalAdapter: InternalAdapter<TModel>,
                                               internal val processModelList: ProcessModelList<TModel>) {
        internal var models: MutableList<TModel> = ArrayList()

        fun add(model: TModel) = apply {
            models.add(model)
        }

        /**
         * Adds all specified models to the [ArrayList].
         */
        @SafeVarargs
        fun addAll(vararg models: TModel) = apply {
            this.models.addAll(models.toList())
        }

        /**
         * Adds a [Collection] of [Model] to the existing [ArrayList].
         */
        fun addAll(models: Collection<TModel>?) = apply {
            if (models != null) {
                this.models.addAll(models)
            }
        }

        /**
         * @return A new [ProcessModelTransaction]. Subsequent calls to this method produce
         * new instances.
         */
        fun build(): FastStoreModelTransaction<TModel> = FastStoreModelTransaction(this)
    }

    companion object {

        @JvmStatic
        fun <TModel> saveBuilder(internalAdapter: InternalAdapter<TModel>): Builder<TModel> =
                Builder(internalAdapter) { tModels, adapter, wrapper -> adapter.saveAll(tModels, wrapper) }

        @JvmStatic
        fun <TModel> insertBuilder(internalAdapter: InternalAdapter<TModel>): Builder<TModel> =
                Builder(internalAdapter) { tModels, adapter, wrapper -> adapter.insertAll(tModels, wrapper) }


        @JvmStatic
        fun <TModel> updateBuilder(internalAdapter: InternalAdapter<TModel>): Builder<TModel> =
                Builder(internalAdapter) { tModels, adapter, wrapper -> adapter.updateAll(tModels, wrapper) }

        @JvmStatic
        fun <TModel> deleteBuilder(internalAdapter: InternalAdapter<TModel>): Builder<TModel> =
                Builder(internalAdapter) { tModels, adapter, wrapper -> adapter.deleteAll(tModels, wrapper) }
    }
}

inline fun <reified T : Any> Collection<T>.fastSave() = FastStoreModelTransaction.saveBuilder(modelAdapter<T>()).addAll(this)

inline fun <reified T : Any> Collection<T>.fastInsert() = FastStoreModelTransaction.insertBuilder(modelAdapter<T>()).addAll(this)

inline fun <reified T : Any> Collection<T>.fastUpdate() = FastStoreModelTransaction.updateBuilder(modelAdapter<T>()).addAll(this)

inline fun <reified T : Any> Collection<T>.fastDelete() = FastStoreModelTransaction.deleteBuilder(modelAdapter<T>()).addAll(this)