package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.SurqlDefineStatementImpl
import org.surrealdb.surql.psi.impl.SurqlForStatementImpl
import org.surrealdb.surql.psi.impl.SurqlLetStatementImpl
import org.surrealdb.surql.psi.impl.SurqlParameterRefImpl

/**
 * Inspection that detects references to undefined parameters.
 * 
 * This inspection tracks parameters defined via:
 * - LET statements
 * - DEFINE PARAM statements
 * - FOR loop iterators
 * - Function parameters
 * 
 * And warns when a parameter is used but not defined.
 */
class SurqlUndefinedParameterInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Undefined parameter reference"
    
    override fun getShortName(): String = "SurqlUndefinedParameter"
    
    override fun getStaticDescription(): String = """
        Reports references to parameters that are not defined in the current file.
        <p>
        Parameters can be defined via:
        <ul>
            <li>LET ${'$'}param = value</li>
            <li>DEFINE PARAM ${'$'}param VALUE value</li>
            <li>FOR ${'$'}item IN collection</li>
            <li>Function parameters in DEFINE FUNCTION</li>
        </ul>
        </p>
        <p>
        System parameters like ${'$'}this, ${'$'}parent, ${'$'}value, ${'$'}auth, etc. are automatically recognized.
        </p>
    """.trimIndent()
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            override fun visitElement(element: PsiElement) {
                if (element is SurqlParameterRefImpl) {
                    checkParameterReference(element)
                }
            }
            
            private fun checkParameterReference(element: SurqlParameterRefImpl) {
                val paramName = element.parameterName
                
                // Skip system parameters
                if (paramName in SYSTEM_PARAMETERS) return
                
                // Collect all defined parameters
                val definedParams = collectDefinedParameters(element)
                
                if (paramName !in definedParams) {
                    holder.registerProblem(
                        element,
                        "Parameter '\$$paramName' may not be defined",
                        ProblemHighlightType.WEAK_WARNING,
                        CreateLetStatementQuickFix(paramName),
                        CreateDefineParamQuickFix(paramName)
                    )
                }
            }
            
            private fun collectDefinedParameters(element: PsiElement): Set<String> {
                val params = mutableSetOf<String>()
                val file = element.containingFile
                
                // Collect from LET statements
                val letStatements = PsiTreeUtil.findChildrenOfType(file, SurqlLetStatementImpl::class.java)
                for (let in letStatements) {
                    let.parameterName?.removePrefix("$")?.let { params.add(it) }
                }
                
                // Collect from DEFINE PARAM statements (only top-level DEFINE_STATEMENT)
                val defineStatements = PsiTreeUtil.findChildrenOfType(file, SurqlDefineStatementImpl::class.java)
                    .filter { it.node.elementType == SurqlElementTypes.DEFINE_STATEMENT }
                for (define in defineStatements) {
                    if (define.defineType == SurqlDefineStatementImpl.DefineType.PARAM) {
                        define.definedName?.removePrefix("$")?.let { params.add(it) }
                    }
                }
                
                // Collect from FOR loop iterators
                val forStatements = PsiTreeUtil.findChildrenOfType(file, SurqlForStatementImpl::class.java)
                for (forStmt in forStatements) {
                    forStmt.iteratorName?.removePrefix("$")?.let { params.add(it) }
                }
                
                // TODO: Collect function parameters from DEFINE FUNCTION
                
                return params
            }
        }
    }
    
    /**
     * Quick fix that creates a LET statement for the undefined parameter.
     */
    private class CreateLetStatementQuickFix(private val paramName: String) : LocalQuickFix {
        
        override fun getName(): String = "Create LET \$$paramName statement"
        
        override fun getFamilyName(): String = "Create parameter definition"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile
            
            // Find the statement containing this parameter reference
            var current = element.parent
            while (current != null && current.parent != file) {
                current = current.parent
            }
            
            val insertOffset = current?.textRange?.startOffset ?: 0
            val letStatement = "LET \$$paramName = null;\n"
            
            val document = file.viewProvider.document ?: return
            document.insertString(insertOffset, letStatement)
        }
    }
    
    /**
     * Quick fix that creates a DEFINE PARAM statement for the undefined parameter.
     */
    private class CreateDefineParamQuickFix(private val paramName: String) : LocalQuickFix {
        
        override fun getName(): String = "Create DEFINE PARAM \$$paramName statement"
        
        override fun getFamilyName(): String = "Create global parameter definition"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile
            
            val defineStatement = "DEFINE PARAM \$$paramName VALUE null;\n\n"
            
            val document = file.viewProvider.document ?: return
            document.insertString(0, defineStatement)
        }
    }
    
    companion object {
        // System parameters that don't need to be defined
        private val SYSTEM_PARAMETERS = setOf(
            "this", "parent", "value", "input", "before", "after",
            "event", "auth", "session", "scope", "token", "access",
            "origin", "id", "in", "out"
        )
    }
}
