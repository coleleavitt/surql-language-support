package org.surrealdb.surql.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.surrealdb.surql.lang.SurqlFileType
import org.surrealdb.surql.lang.SurqlLanguage

class SurqlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SurqlLanguage) {
    override fun getFileType(): FileType = SurqlFileType
    override fun toString(): String = "SurrealQL File"
}
