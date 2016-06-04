# Kotlin Support + Extensions

DBFlow supports Kotlin out of the box and is fairly easily to use and implement.

Currently, we cannot write DBFlow classes in Kotlin, due to some bugs with the generated
Java classes that DBFlow creates are not found in Kotlin files when compiling.

## Extensions

DBFlow as of `3.0.0+` contains some extensions for use in Kotlin. These
are defined in a separate dependency:

```
dependencies {
  compile "com.github.Raizlabs.DBFlow:dbflow-kotlinextensions:${dbflow_version}"
}

```

### Query Extensions

Note that these features are incubating and may change or get removed in a later version.


#### Query LINQ Syntax

Kotlin has nice support for custim `infix` operators. Using this we can convert a regular, Plain old java query into a C#-like LINQ syntax.

java:
```

List<Result> = SQLite.select()
                .from(Result.class)
                .where(Result_Table.column.eq(6))
                .and(Result_Table.column2.in("5", "6", "9")).queryList()

```

kotlin:

```
val results = (select
              from Result::class
              where (column eq 6)
              and (column2 in("5", "6", "9"))
              groupBy column).list
              // can call .result for single result
              // .hasData if it has results
              // .statement for a compiled statement
```

Enabling us to write code that is closer in syntax to SQLite!

This supported for almost any SQLite operator that this library provides including:
  1. `Select`
  2. `Insert`
  3. `Update`
  4. `Delete`

**Async Operations**:
With extensions we also support `async` operations on queries:

```kotlin

// easy async list query
(select
    from Result::class
    where (column eq 6))
.async list { transaction, list ->
    // do something here
    updateUI(list)
}

// easy single result query
(select
    from Result::class
    where (column eq 6))
.async result { transaction, model ->
    // do something here
    updateUI(model)
}

```

#### Query DSL

Select

```kotlin

var items = select {
  from<SomeTable> {
    where {
      SomeTable_Table.name.eq("something")
    }.
    and {
      SomeTable_Table.job.eq("Software Engineer")
    }
  }
}.queryList()

var another = select {
    from<TestModel1> {
        join<TestModel1, TestModel2>(INNER) {
            on { TestModel2_Table.name.withTable().eq(TestModel1_Table.name.withTable()) }
        }

        join<TestModel1, TestModel3>(LEFT_OUTER) {
            on { TestModel1_Table.name.withTable().eq(TestModel3_Table.name.withTable()) }
        }
    }
}

```

Insert

```kotlin

var query = insert<TestModel1> {
           orReplace()
           into(KotlinTestModel_Table.id to 5, KotlinTestModel_Table.name to "5")
       }

```

We added an `into` method that takes in a `Pair<IProperty<*>, *>` to allow you
to specify values a little easier when using `Insert` statement wrappers.

Delete

```kotlin

delete<TestModel1> {
    where {
        TestModel1_Table.name.eq("test")
    }
}.execute()

```

Update

```kotlin
update<TestModel1> {
    set {
        conditions(TestModel1_Table.name.`is`("yes"))
        where { TestModel1_Table.name.eq("no") }
                .and { TestModel1_Table.name.eq("maybe") }
    }
}.execute()
```

### Property Extensions

With Kotlin, we can define extension methods on pretty much any class.

With this, we added methods to easily create `IProperty` from anything to make
queries a little more streamlined. In this query, we also make use of the extension
method for `from` to streamline the query even more.

```kotlin

var query = SQLite.select()
  .from<TestModel>()
  .where(5.property.lessThan(TestModel_Table.column))
  .and(ConditionGroup.clause().and(date.property.between(TestModel_Table.start_date)
      .and(TestModel_Table.end_date)))


```

### Database Extensions

The more interesting part is the extensions here.

#### Process Models Asynchronously

In Java, we need to write something of the fashion:

```java

List<TestModel> items = SQLite.select()
    .from(TestModel.class)
    .queryList();

TransactionManager.getInstance()
  .add(new ProcessModelTransaction(ProcessModelInfo.withModels(items), null) {
    @Override
    public void processModel(TestModel model) {
        // do something.
    }
});

```

In Kotlin, we can use a combo of DSL and extension methods to:

```kotlin

var items = SQLite.select()
               .from<TestModel1>().queryList()

 // easily delete all these items.
 items.processInTransactionAsync { it, databaseWrapper -> it.delete(databaseWrapper) }

 // easily delete all these items with success
 items.processInTransactionAsync({ it, databaseWrapper -> it.delete(databaseWrapper) },
            Transaction.Success {
                // do something here
            })
// delete with all callbacks
iitems.processInTransactionAsync({ it, databaseWrapper -> it.delete(databaseWrapper) },
    Transaction.Success {
        // do something here
    },
    Transaction.Error { transaction, throwable ->

    })

```

The extension method on `Collection<T : Model>` allows you to perform this on all
collections from your Table!

If you wish to easily do them _synchronously_ then use:

```kotlin

items.processInTransaction { it, databaseWrapper -> it.delete(databaseWrapper) }

```

#### Class Extensions

If you need access to the Database, ModelAdapter, etc for a specific class you
can now:


```kotlin

database<TestModel>

tableName<TestModel>

modelAdapter<TestModel>

containerAdapter<TestModel>

```


Which under-the-hood call their corresponding `FlowManager` methods.
