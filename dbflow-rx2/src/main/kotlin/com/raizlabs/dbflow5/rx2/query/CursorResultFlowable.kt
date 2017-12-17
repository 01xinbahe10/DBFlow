package com.raizlabs.dbflow5.rx2.query

import com.raizlabs.dbflow5.config.FlowLog
import com.raizlabs.dbflow5.database.DatabaseWrapper
import com.raizlabs.dbflow5.query.CursorResult
import io.reactivex.Flowable
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicLong

/**
 * Description: Wraps a [RXModelQueriable] into a [Flowable]
 * for each element represented by the query.
 */
class CursorResultFlowable<T : Any>(private val modelQueriable: RXModelQueriable<T>,
                                    private val databaseWrapper: DatabaseWrapper)
    : Flowable<T>() {

    override fun subscribeActual(subscriber: Subscriber<in T>) {
        subscriber.onSubscribe(object : Subscription {
            override fun request(n: Long) {
                modelQueriable.queryResults(databaseWrapper)
                        .subscribe(CursorResultObserver(subscriber, n))
            }

            override fun cancel() {

            }
        })
    }

    internal class CursorResultObserver<T : Any>(
            private val subscriber: Subscriber<in T>, private val count: Long)
        : SingleObserver<CursorResult<T>> {
        private val emitted: AtomicLong = AtomicLong()
        private val requested: AtomicLong = AtomicLong()
        private var disposable: Disposable? = null

        override fun onSubscribe(disposable: Disposable) {
            this.disposable = disposable
        }

        override fun onSuccess(ts: CursorResult<T>) {
            val starting = when {
                this.count == java.lang.Long.MAX_VALUE
                        && requested.compareAndSet(0, java.lang.Long.MAX_VALUE) -> 0
                else -> emitted.toInt()
            }
            var limit = this.count + starting

            while (limit > 0) {
                val iterator = ts.iterator(starting, limit)
                try {
                    var i: Long = 0
                    while (!disposable!!.isDisposed && iterator.hasNext() && i++ < limit) {
                        subscriber.onNext(iterator.next())
                    }
                    emitted.addAndGet(i)
                    // no more items
                    if (disposable?.isDisposed == false && i < limit) {
                        subscriber.onComplete()
                        break
                    }
                    limit = requested.addAndGet(-limit)
                } catch (e: Exception) {
                    FlowLog.logError(e)
                    subscriber.onError(e)
                } finally {
                    try {
                        iterator.close()
                    } catch (e: Exception) {
                        FlowLog.logError(e)
                        subscriber.onError(e)
                    }

                }
            }
        }

        override fun onError(e: Throwable) {
            subscriber.onError(e)
        }

    }
}
