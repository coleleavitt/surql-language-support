package org.surrealdb.surql.ide

import com.intellij.lang.Commenter

/**
 * Provides comment/uncomment functionality for SurrealQL.
 *
 * SurrealQL supports multiple comment styles:
 * - Line comments: --, //, #
 * - Block comments: slash-star ... star-slash
 *
 * We use -- as the primary line comment style as it's the most SQL-like.
 */
class SurqlCommenter : Commenter {

    override fun getLineCommentPrefix(): String = "-- "

    override fun getBlockCommentPrefix(): String = "/* "

    override fun getBlockCommentSuffix(): String = " */"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
