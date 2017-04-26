# Usage

DBFlow supports a number of database features that will enhance and decrease time you need to spend coding with databases.

What is covered in these docs are not all inclusive, but should give you an idea of how to operate with DBFlow on databases.

There are a few concepts to familiarize yourself with:

**SQLite Wrapper Language:** DBFlow provides a number of convenience methods, extensions, and generated helpers that produce a concise, flowable query syntax.

```
List<User> users = SQLite.select()
  .from(User.class)
  .where(name.is("Andrew Grosner"))
  .queryList();

SQLite.update(User.class)
  .set(name.eq("Andrew Grosner"))
  .where(name.eq("Andy Grosner"))
  .executeUpdateDelete()

database.beginTransactionAsync((DatabaseWrapper wrapper) -> {
  // wraps in a SQLite transaction, do something on BG thread.
});
```



