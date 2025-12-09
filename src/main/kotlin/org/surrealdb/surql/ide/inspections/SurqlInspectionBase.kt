package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.surrealdb.surql.psi.SurqlFile

/**
 * Base class for all SurrealQL inspections.
 * Provides common functionality and ensures inspections only run on SurrealQL files.
 */
abstract class SurqlInspectionBase : LocalInspectionTool() {
    
    override fun getGroupDisplayName(): String = "SurrealQL"
    
    override fun isEnabledByDefault(): Boolean = true
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (file !is SurqlFile) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return buildSurqlVisitor(holder, isOnTheFly)
    }
    
    /**
     * Build a visitor specific to SurrealQL files.
     * Subclasses should override this method to provide their inspection logic.
     */
    protected abstract fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor
}
