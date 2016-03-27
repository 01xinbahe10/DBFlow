# Storing Data

DBFlow provide a few mechanisms by which we store data to the database. The difference of options
should not provide confusion but rather allow flexibility in what you decide is the best way
to store information.

## Synchronous Storage

While generally saving data synchronous should be avoided, for small amounts of data
it has little effect.

```java

model.insert(); // inserts
model.update(); // updates
model.save(); // checks if exists, if true update, else insert.

```

Code like this should be avoided:

```java

for (int i = 0; i < models.size(), i++) {
  models.get(i).save();
}

```

## Synchronous Transactions

A simple database transaction can be wrapped in a call:

```java

FlowManager.getDatabase(AppDatabase.class).executeTransaction(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {
                // something here
                Player player = new Player("Andrew", "Grosner");
                player.save(databaseWrapper); // use wrapper (from BaseModel)
            }
});

```

Even though DBFlow is ridiculously fast, this should be put on a separate thread
 outside of the UI, so that your UI remains responsive on all devices.

 Instead we should move onto `Transaction` (the preferred method).

 ### Async Transactions

## Transactions

Transactions are ACID in SQLite, meaning they either occur completely or not at all.
Using transactions significantly speed up the time it takes to store. So recommendation
you should use transactions whenever you can.

Async is the preferred method. Transactions, using the `DefaultTransactionManager`,
 occur on one thread per-database (to prevent flooding from other DB in your app)
  and receive callbacks on the UI. You can override this behavior and roll your own
  or hook into an existing system, read [here](/usage2/StoringData.md#custom-transactionmanager)

 A basic transaction:

 ```java

DatabaseDefinition database = FlowManager.getDatabase(AppDatabase.class);
Transaction transaction = database.beginTransactionAsync(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {
                called.set(true);
            }
        }).build();
transaction.execute(); // execute

transaction.cancel();
 // attempt to cancel before its run. If it's already ran, this call has no effect.

 ```

 `Transaction` have callbacks to allow you to "listen" for success and errors.

 ```java


 transaction
    .success(new Transaction.Success() {
       @Override
       public void onSuccess(Transaction transaction) {
          // called post-execution on the UI thread.
       }
   })
   .error(new Transaction.Error() {
       @Override
       public void onError(Transaction transaction, Throwable error) {
          // call if any errors occur in the transaction.
       }
   });


 ```

 The `Success` callback runs post-transaction on the UI thread.
 The `Error` callback is called on the UI thread if and only if it is specified and an exception occurs,
 otherwise it is thrown in the `Transaction` as a `RuntimeException`. **Note**:
 all exceptions are caught when specifying the callback. Ensure you handle all
 errors, otherwise you might miss some problems.

### ProcessModelTransaction

`ProcessModelTransaction` allows you to easily operate on a set of `Model` in a
`Transaction` easily.

It is a convenient way to operate on them:

```java

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
        }).addAll(items).build();
Transaction transaction = database.beginTransactionAsync(processModelTransaction).build();
transaction.execute();

```

In Kotlin (with dbflow-kotlinextensions), this becomes as simple as:

```java

database<TestModel1>().beginTransactionAsync {
    ProcessModelTransaction.Builder<TestModel1>({ it.save() })
            .processListener(ProcessModelTransaction
                    .OnModelProcessListener<TestModel1> { current, total, item ->
                      // do something here on UI thread.
                    })
            .addAll(items).build()
}.build().execute()

```
You can listen to when operations complete for each model via the `OnModelProcessListener`.
These callbacks occur on the UI thread.

### Custom TransactionManager

If you prefer to roll your own thread-management system or have an existing
system you can override the default system included.


To begin you must implement a `ITransactionQueue`:

```java

public class CustomQueue implements ITransactionQueue {

    @Override
    public void add(Transaction transaction) {

    }

    @Override
    public void cancel(Transaction transaction) {

    }

    @Override
    public void startIfNotAlive() {

    }

    @Override
    public void cancel(String name) {

    }

    @Override
    public void quit() {

    }
}

```

You must provide ways to `add()`, `cancel(Transaction)`, and `startIfNotAlive()`.
The other two methods are optional, but recommended.

`startIfNotAlive()` in the `DefaultTransactionQueue` will start itself (since it's
a thread).

 Next you can override the `BaseTransactionManager` (not required, see later):

```java

public class CustomTransactionManager extends BaseTransactionManager {

   public CustomTransactionManager(DatabaseDefinition databaseDefinition) {
       super(new CustomTransactionQueue(), databaseDefinition);
   }

}

```

To register it with DBFlow, in your `FlowConfig`, you must:

```java

FlowManager.init(builder
    .addDatabaseConfig(new DatabaseConfig.Builder(AppDatabase.class)
       .transactionManagerCreator(new DatabaseConfig.TransactionManagerCreator() {
                        @Override
                        public BaseTransactionManager createManager(DatabaseDefinition databaseDefinition) {
                          // this will be called once database modules are loaded and created.
                            return new CustomTransactionManager(databaseDefinition);

                            // or you can:
                            //return new DefaultTransactionManager(new CustomTransactionQueue(), databaseDefinition);
                        }
                    })
       .build())
    .build());

```
