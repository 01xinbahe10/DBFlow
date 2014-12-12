package com.raizlabs.android.dbflow.runtime;

import java.util.ArrayList;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
public class TransactionManagerRuntime {


    private static ArrayList<TransactionManager> managers;

    /**
     * Quits all active DBManager queues
     */
    public static void quit() {
        for (TransactionManager manager : getManagers()) {
            if (manager.hasOwnQueue()) {
                manager.getQueue().quit();
                manager.disposeQueue();
            }
        }
        DBBatchSaveQueue.getSharedSaveQueue().quit();
        DBBatchSaveQueue.disposeSharedQueue();
    }

    static ArrayList<TransactionManager> getManagers() {
        if (managers == null) {
            managers = new ArrayList<TransactionManager>();
        }
        return managers;
    }

    public static void restartManagers() {
        for (TransactionManager manager : getManagers()) {
            manager.checkQueue();
        }
    }
}
