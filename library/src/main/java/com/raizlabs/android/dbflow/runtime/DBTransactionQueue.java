package com.raizlabs.android.dbflow.runtime;

import android.os.Looper;

import com.raizlabs.android.dbflow.config.FlowLog;
import com.raizlabs.android.dbflow.runtime.transaction.BaseTransaction;

import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by andrewgrosner
 * Description: will handle concurrent requests to the DB based on priority
 */
public class DBTransactionQueue extends Thread {

    /**
     * Queue of requests
     */
    private final PriorityBlockingQueue<BaseTransaction> mQueue;

    private boolean mQuit = false;

    private TransactionManager mManager;

    /**
     * Creates a queue with the specified name to ID it.
     *
     * @param name
     */
    public DBTransactionQueue(String name, TransactionManager transactionManager) {
        super(name);
        mManager = transactionManager;
        mQueue = new PriorityBlockingQueue<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Looper.prepare();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        BaseTransaction transaction;
        while (true) {
            try {
                synchronized (mQueue) {
                    transaction = mQueue.take();
                }
            } catch (InterruptedException e) {
                if (mQuit) {
                    synchronized (mQueue) {
                        mQueue.clear();
                    }
                    return;
                }
                continue;
            }

            try {
                // If the transaction is ready
                if (transaction.onReady()) {

                    // Retrieve the result of the transaction
                    final Object result = transaction.onExecute();
                    final BaseTransaction finalTransaction = transaction;

                    // Run the result on the FG
                    if (transaction.hasResult(result)) {
                        mManager.processOnRequestHandler(new Runnable() {
                            @Override
                            public void run() {
                                finalTransaction.onPostExecute(result);
                            }
                        });
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

    }

    public void add(BaseTransaction runnable) {
        if (!mQueue.contains(runnable)) {
            mQueue.add(runnable);
        }
    }

    /**
     * Cancels the specified request.
     *
     * @param runnable
     */
    public void cancel(BaseTransaction runnable) {
        synchronized (mQueue) {
            if (mQueue.contains(runnable)) {
                mQueue.remove(runnable);
            }
        }
    }

    /**
     * Cancels all requests by a specific tag
     *
     * @param tag
     */
    public void cancel(String tag) {
        synchronized (mQueue) {
            Iterator<BaseTransaction> it = mQueue.iterator();
            while (it.hasNext()) {
                BaseTransaction next = it.next();
                if (next.getName().equals(tag)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Quits this process
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }
}

