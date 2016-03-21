package com.raizlabs.android.dbflow.test.transaction;

import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.CursorResult;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ITransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;
import com.raizlabs.android.dbflow.test.FlowTestCase;
import com.raizlabs.android.dbflow.test.TestDatabase;
import com.raizlabs.android.dbflow.test.structure.TestModel1;
import com.raizlabs.android.dbflow.test.structure.TestModel1_Table;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Description:
 */
public class TransactionsTest extends FlowTestCase {

    DatabaseDefinition database;


    @Before
    public void beforeTests() {
        database = FlowManager.getDatabase(TestDatabase.NAME);
    }

    @Test
    public void test_basicAsyncTransactionCall() {

        final AtomicBoolean called = new AtomicBoolean(false);
        MockTransaction transaction = new MockTransaction(database.beginTransactionAsync(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {
                called.set(true);
            }
        }).build(), database);
        transaction.execute();

        assertTrue(called.get());

    }

    @Test
    public void test_basicTransaction() {
        final AtomicBoolean called = new AtomicBoolean(false);
        database.executeTransaction(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {
                called.set(true);
            }
        });
        assertTrue(called.get());
    }

    @Test
    public void test_processTransaction() {
        final int count = 10;
        List<TestModel1> testModel1List = new ArrayList<>();
        TestModel1 testModel1;
        for (int i = 0; i < count; i++) {
            testModel1 = new TestModel1();
            testModel1.setName("Name" + i);
            testModel1List.add(testModel1);
        }
        final AtomicBoolean processCalled = new AtomicBoolean(false);
        final AtomicInteger modelProcessedCount = new AtomicInteger(0);
        ProcessModelTransaction<TestModel1> processModelTransaction =
                new ProcessModelTransaction.Builder<>(new ProcessModelTransaction.ProcessModel<TestModel1>() {
                    @Override
                    public void processModel(TestModel1 model) {
                        processCalled.set(true);
                    }
                }).processListener(new ProcessModelTransaction.OnModelProcessListener<TestModel1>() {
                    @Override
                    public void onModelProcessed(long current, long total, TestModel1 modifiedModel) {
                        modelProcessedCount.incrementAndGet();
                    }
                }).addAll(testModel1List).build();
        Transaction transaction = new Transaction.Builder(processModelTransaction, database).build();
        new MockTransaction(transaction, database).execute();

        assertTrue(transaction.transaction() instanceof ProcessModelTransaction);
        assertEquals(processCalled.get(), true);
        assertEquals(modelProcessedCount.get(), count);
    }

    @Test
    public void test_queryTransaction() {

        TestModel1 testModel1 = new TestModel1();
        testModel1.setName("Thisisatest1");
        testModel1.save();

        testModel1 = new TestModel1();
        testModel1.setName("barry");
        testModel1.save();

        testModel1 = new TestModel1();
        testModel1.setName("welltest1");
        testModel1.save();

        final AtomicBoolean called = new AtomicBoolean(false);
        QueryTransaction<TestModel1> queryTransaction = new QueryTransaction.Builder<>(
                SQLite.select()
                        .from(TestModel1.class)
                        .where(TestModel1_Table.name.like("%test1"))
        ).queryResult(new QueryTransaction.QueryResultCallback<TestModel1>() {
            @Override
            public void onQueryResult(QueryTransaction transaction, CursorResult<TestModel1> tResult) {
                List<TestModel1> results = tResult.toList();
                assertEquals(results.size(), 2);
                called.set(true);
                tResult.close();
            }
        }).build();

        Transaction transaction = database.beginTransactionAsync(queryTransaction).build();
        new MockTransaction(transaction, database).execute();

        assertTrue(called.get());
    }
}
