package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.surrealdb.surql.psi.impl.SurqlLetStatementImpl
import org.surrealdb.surql.psi.impl.SurqlParameterRefImpl

/**
 * Inspection that detects parameters that are defined but never used.
 * 
 * This helps identify dead code and unused variable declarations.
 */
class SurqlUnusedParameterInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Unused parameter"
    
    override fun getShortName(): String = "SurqlUnusedParameter"
    
    override fun getStaticDescription(): String = """
        Reports parameters defined via LET statements that are never used.
        <p>
        Unused parameters may indicate:
        <ul>
            <li>Dead code that can be removed</li>
            <li>A typo in parameter names</li>
            <li>Incomplete refactoring</li>
        </ul>
        </p>
    """.trimIndent()
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            override fun visitElement(element: PsiElement) {
                if (element is SurqlLetStatementImpl) {
                    checkUnusedParameter(element)
                }
            }
            
            private fun checkUnusedParameter(letStatement: SurqlLetStatementImpl) {
                val paramName = letStatement.parameterName?.removePrefix("$") ?: return
                val file = letStatement.containingFile
                
                // Find all parameter references in the file
                val paramRefs = PsiTreeUtil.findChildrenOfType(file, SurqlParameterRefImpl::class.java)
                
                // Check if any reference uses this parameter (excluding the definition itself)
                val isUsed = paramRefs.any { ref ->
                    ref.parameterName == paramName && 
                    !PsiTreeUtil.isAncestor(letStatement, ref, true)
                }
                
                if (!isUsed) {
                    // Find the parameter token in the LET statement
                    val paramElement = findParameterElement(letStatement) ?: letStatement
                    
                    holder.registerProblem(
                        paramElement,
                        "Parameter '\$$paramName' is never used",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveLetStatementQuickFix()
                    )
                }
            }
            
            private fun findParameterElement(letStatement: SurqlLetStatementImpl): PsiElement? {
                var child = letStatement.firstChild
                while (child != null) {
                    if (child is SurqlParameterRefImpl || 
                        child.text.startsWith("$")) {
                        return child
                    }
                    child = child.nextSibling
                }
                return null
            }
        }
    }
    
    /**
     * Quick fix that removes the unused LET statement.
     */
    private class RemoveLetStatementQuickFix : LocalQuickFix {
        
        override fun getName(): String = "Remove unused LET statement"
        
        override fun getFamilyName(): String = "Remove unused code"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            
            // Find the LET statement
            var letStatement = element
            while (letStatement !is SurqlLetStatementImpl && letStatement.parent != null) {
                letStatement = letStatement.parent
            }
            
            if (letStatement is SurqlLetStatementImpl) {
                val document = letStatement.containingFile.viewProvider.document ?: return
                
                var start = letStatement.textRange.startOffset
                var end = letStatement.textRange.endOffset
                
                // Also remove trailing semicolon and newline if present
                val text = document.text
                if (end < text.length && text[end] == ';') {
                    end++
                }
                while (end < text.length && (text[end] == '\n' || text[end] == '\r')) {
                    end++
                }
                
                // Remove leading whitespace on the line if it leaves an empty line
                while (start > 0 && text[start - 1] == ' ') {
                    start--
                }
                
                document.deleteString(start, end)
            }
        }
    }
}
