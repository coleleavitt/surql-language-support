package org.surrealdb.surql.lang

import com.intellij.lang.Language

object SurqlLanguage : Language("SurrealQL") {
    override fun getDisplayName(): String = "SurrealQL"
    override fun isCaseSensitive(): Boolean = false
}
