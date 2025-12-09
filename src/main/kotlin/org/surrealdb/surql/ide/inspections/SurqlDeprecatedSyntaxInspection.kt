package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.SurqlDefineStatementImpl

/**
 * Inspection that detects deprecated SurrealQL syntax and suggests modern alternatives.
 * 
 * This helps users migrate from older SurrealDB versions to newer ones.
 */
class SurqlDeprecatedSyntaxInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Deprecated SurrealQL syntax"
    
    override fun getShortName(): String = "SurqlDeprecatedSyntax"
    
    override fun getStaticDescription(): String = """
        Reports usage of deprecated SurrealQL syntax and suggests modern alternatives.
        <p>
        Examples of deprecated syntax:
        <ul>
            <li>SCOPE (replaced by ACCESS in SurrealDB 2.0)</li>
            <li>TOKEN (replaced by ACCESS with JWT type)</li>
            <li>DEFINE LOGIN (replaced by DEFINE USER)</li>
        </ul>
        </p>
    """.trimIndent()
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            override fun visitElement(element: PsiElement) {
                // Check for deprecated SCOPE keyword in definitions
                if (element.elementType == SurqlTokenTypes.SCOPE) {
                    checkDeprecatedScope(element)
                }
                
                // Check for deprecated TOKEN in certain contexts
                if (element.elementType == SurqlTokenTypes.TOKEN) {
                    checkDeprecatedToken(element)
                }
                
                // Check DEFINE statements for deprecated patterns (only top-level, not nested)
                if (element is SurqlDefineStatementImpl && 
                    element.node.elementType == SurqlElementTypes.DEFINE_STATEMENT) {
                    checkDefineStatement(element)
                }
            }
            
            private fun checkDeprecatedScope(element: PsiElement) {
                // SCOPE is deprecated in favor of ACCESS
                val parent = element.parent
                
                // Check if this is DEFINE SCOPE or REMOVE SCOPE
                if (parent is SurqlDefineStatementImpl || 
                    isInRemoveStatement(element)) {
                    holder.registerProblem(
                        element,
                        "SCOPE is deprecated. Use ACCESS instead (SurrealDB 2.0+)",
                        ProblemHighlightType.LIKE_DEPRECATED,
                        ReplaceScopeWithAccessQuickFix()
                    )
                }
            }
            
            private fun checkDeprecatedToken(element: PsiElement) {
                // TOKEN as a definition target is deprecated
                if (isInDefineStatement(element) || isInRemoveStatement(element)) {
                    holder.registerProblem(
                        element,
                        "DEFINE TOKEN is deprecated. Use DEFINE ACCESS with TYPE JWT instead (SurrealDB 2.0+)",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }
            
            private fun checkDefineStatement(element: SurqlDefineStatementImpl) {
                // Check for deprecated patterns in DEFINE statements
                val text = element.text.uppercase()
                
                // DEFINE LOGIN is deprecated
                if (text.contains("DEFINE LOGIN")) {
                    holder.registerProblem(
                        element,
                        "DEFINE LOGIN is deprecated. Use DEFINE USER instead",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
                
                // DEFINE TABLE with old AS SELECT syntax (pre 1.0)
                // This is informational only
                
                // Check for old PERMISSIONS syntax
                if (text.contains("PERMISSIONS WHERE") && !text.contains("FOR")) {
                    holder.registerProblem(
                        element,
                        "Consider using the new PERMISSIONS FOR syntax for more granular control",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
            
            private fun isInDefineStatement(element: PsiElement): Boolean {
                var current = element.parent
                var depth = 0
                while (current != null && depth < 5) {
                    if (current is SurqlDefineStatementImpl) return true
                    // Check for DEFINE keyword before the element
                    val prev = element.prevSibling
                    if (prev?.elementType == SurqlTokenTypes.DEFINE) return true
                    current = current.parent
                    depth++
                }
                return false
            }
            
            private fun isInRemoveStatement(element: PsiElement): Boolean {
                var current = element.parent
                while (current != null) {
                    val firstChild = current.firstChild
                    if (firstChild?.elementType == SurqlTokenTypes.REMOVE) return true
                    if (current.parent == current.containingFile) break
                    current = current.parent
                }
                return false
            }
        }
    }
    
    /**
     * Quick fix that replaces SCOPE with ACCESS.
     */
    private class ReplaceScopeWithAccessQuickFix : LocalQuickFix {
        
        override fun getName(): String = "Replace SCOPE with ACCESS"
        
        override fun getFamilyName(): String = "Update deprecated syntax"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val document = element.containingFile.viewProvider.document ?: return
            
            val start = element.textRange.startOffset
            val end = element.textRange.endOffset
            
            document.replaceString(start, end, "ACCESS")
        }
    }
}
