package org.surrealdb.surql.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object SurqlFileType : LanguageFileType(SurqlLanguage) {
    override fun getName(): String = "SurrealQL"
    override fun getDescription(): String = "SurrealQL query file"
    override fun getDefaultExtension(): String = "surql"
    override fun getIcon(): Icon? = SurqlIcons.FILE
}
