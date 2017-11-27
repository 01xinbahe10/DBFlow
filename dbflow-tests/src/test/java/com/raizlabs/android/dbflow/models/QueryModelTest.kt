package com.raizlabs.android.dbflow.models

import com.raizlabs.android.dbflow.BaseUnitTest
import com.raizlabs.android.dbflow.TestDatabase
import com.raizlabs.android.dbflow.config.database
import com.raizlabs.android.dbflow.models.Author_Table.id
import com.raizlabs.android.dbflow.models.Blog_Table.author_id
import com.raizlabs.android.dbflow.models.Blog_Table.name
import com.raizlabs.android.dbflow.query.innerJoin
import com.raizlabs.android.dbflow.query.on
import com.raizlabs.android.dbflow.query.select
import com.raizlabs.android.dbflow.structure.exists
import com.raizlabs.android.dbflow.structure.save
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Description: Tests to ensure we can load a Query model from the DB
 */
class QueryModelTest : BaseUnitTest() {

    @Test
    fun testCanLoadAuthorBlogs() = database(TestDatabase::class) {
        val author = Author(0, "Andrew", "Grosner")
        author.save()
        val blog = Blog(0, "My First Blog", author)
        blog.save()

        assert(author.exists())
        assert(blog.exists())

        val result = (select(name.withTable().`as`("blogName"), id.withTable().`as`("authorId"),
                Blog_Table.id.withTable().`as`("blogId")) from Blog::class innerJoin
                Author::class on (author_id.withTable() eq id.withTable()))
                .queryCustomSingle(AuthorNameQuery::class.java)!!
        assertEquals(author.id, result.authorId)
        assertEquals(blog.id, result.blogId)
    }
}
