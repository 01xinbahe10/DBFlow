# RXJava 1 Support

RXJava support in DBFlow is an _incubating_ feature and likely to change over time.
Currently it supports
    1. `Insert`, `Update`, `Delete`, `Set`, `Join`, and all wrapper query mechanisms.
    2. Single + `List` model `save()`, `insert()`, `update()`, and `delete()`.
    3. Streaming a set of results from a query
    4. Observing on table changes for specific `ModelQueriable` and providing ability to query from that set repeatedly as needed.

## Getting Started

Add the separate package to your project:
```groovy

dependencies {
  compile "com.github.Raizlabs.DBFlow:dbflow-rx:${dbflow_version}"
}

```

## Wrapper Language
Using the classes is as easy as replacing all SQL wrapper calls from to `SQLite` with `RXSQLite`:

Before:
```kotlin

val list = SQLite.select()
  .from(MyTable.class)
  .queryList()

```

After:

```kotlin

RXSQLite.select()
  .from(MyTable.class)
  .queryList()
  .subscribe { list ->

  }

```

Essentially we restructured and partially reimplemented the front-end API for queries
with RX replacements.

## Model operations
To make the transition as smoothest as possible, we've provided a `BaseRXModel` which replaces `BaseModel` for convenience in the RX space.

```kotlin

class Person(@PrimaryKey var id: Int = 0, @Column var name: String? = "") : BaseRXModel

```

Operations are as easy as:
```kotlin

Person(5, "Andrew Grosner").insert()
  .subscribe { rowId ->

  }

```

## Query Stream

We can use RX to stream the result set, one at a time from the `ModelQueriable` using
the method `queryStreamResults()`:

```kotlin

RXSQLite.select()
   .from(TestModel1::class.java)
   .queryStreamResults()
   .subscribe { model ->

   }

```
