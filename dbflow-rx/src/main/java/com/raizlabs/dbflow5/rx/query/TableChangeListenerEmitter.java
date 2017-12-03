package com.raizlabs.dbflow5.rx.query;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.raizlabs.dbflow5.config.FlowManager;
import com.raizlabs.dbflow5.runtime.OnTableChangedListener;
import com.raizlabs.dbflow5.runtime.TableNotifierRegister;
import com.raizlabs.dbflow5.query.From;
import com.raizlabs.dbflow5.query.Join;
import com.raizlabs.dbflow5.query.Where;
import com.raizlabs.dbflow5.query.ModelQueriable;
import com.raizlabs.dbflow5.structure.ChangeAction;

import rx.Emitter;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Description: Emits when table changes occur for the related table on the {@link ModelQueriable}.
 * If the {@link ModelQueriable} relates to a {@link Join}, this can be multiple tables.
 */
public class TableChangeListenerEmitter<TModel> implements Action1<Emitter<ModelQueriable<TModel>>> {

    private final ModelQueriable<TModel> modelQueriable;

    public TableChangeListenerEmitter(ModelQueriable<TModel> modelQueriable) {
        this.modelQueriable = modelQueriable;
    }

    @Override
    public void call(Emitter<ModelQueriable<TModel>> modelQueriableEmitter) {
        modelQueriableEmitter.setSubscription(
            new FlowContentObserverSubscription(modelQueriableEmitter, modelQueriable.getTable()));
    }

    private class FlowContentObserverSubscription implements Subscription {

        private final Emitter<ModelQueriable<TModel>> modelQueriableEmitter;

        private final TableNotifierRegister register;

        private FlowContentObserverSubscription(
            Emitter<ModelQueriable<TModel>> modelQueriableEmitter, Class<TModel> table) {
            this.modelQueriableEmitter = modelQueriableEmitter;
            register = FlowManager.newRegisterForTable(table);

            From<TModel> from = null;
            if (modelQueriable instanceof From) {
                from = (From<TModel>) modelQueriable;
            } else if (modelQueriable instanceof Where
                && ((Where) modelQueriable).getWhereBase() instanceof From) {
                //noinspection unchecked
                from = (From<TModel>) ((Where) modelQueriable).getWhereBase();
            }

            // From could be part of many joins, so we register for all affected tables here.
            if (from != null) {
                java.util.Set<Class<?>> associatedTables = from.getAssociatedTables();
                for (Class<?> associated : associatedTables) {
                    register.register(associated);
                }
            } else {
                register.register(table);
            }

            register.setListener(onTableChangedListener);
        }

        @Override
        public void unsubscribe() {
            register.unregisterAll();
        }

        @Override
        public boolean isUnsubscribed() {
            return !register.isSubscribed();
        }

        private final OnTableChangedListener onTableChangedListener
            = new OnTableChangedListener() {
            @Override
            public void onTableChanged(@Nullable Class<?> table, @NonNull ChangeAction action) {
                if (modelQueriable.getTable().equals(table)) {
                    modelQueriableEmitter.onNext(modelQueriable);
                }
            }
        };
    }
}
