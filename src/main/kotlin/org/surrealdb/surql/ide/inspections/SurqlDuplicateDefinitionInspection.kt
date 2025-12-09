package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.SurqlDefineStatementImpl
import org.surrealdb.surql.psi.impl.SurqlLetStatementImpl

/**
 * Inspection that detects duplicate definitions of tables, fields, indexes, etc.
 * 
 * Having duplicate definitions can lead to confusion and unexpected behavior.
 */
class SurqlDuplicateDefinitionInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Duplicate definition"
    
    override fun getShortName(): String = "SurqlDuplicateDefinition"
    
    override fun getStaticDescription(): String = """
        Reports duplicate definitions of the same schema element.
        <p>
        Duplicate definitions may cause:
        <ul>
            <li>Confusion about which definition is in effect</li>
            <li>Unexpected overwrites of schema elements</li>
            <li>Maintenance difficulties</li>
        </ul>
        </p>
        <p>
        Note: Using OVERWRITE keyword is intentional and won't be flagged.
        </p>
    """.trimIndent()
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            private var processedFile = false
            private val definedElements = mutableMapOf<String, MutableList<PsiElement>>()
            private val definedParams = mutableMapOf<String, MutableList<PsiElement>>()
            
            override fun visitElement(element: PsiElement) {
                // Process the file once to collect all definitions
                if (!processedFile && element.containingFile != null) {
                    processFile(element)
                    processedFile = true
                }
            }
            
            private fun processFile(anyElement: PsiElement) {
                val file = anyElement.containingFile
                
                // Collect DEFINE statements - only top-level DEFINE_STATEMENT, not nested definition elements
                // (TABLE_DEFINITION, FIELD_DEFINITION, etc. are nested inside DEFINE_STATEMENT and would cause double-counting)
                val defineStatements = PsiTreeUtil.findChildrenOfType(file, SurqlDefineStatementImpl::class.java)
                    .filter { it.node.elementType == SurqlElementTypes.DEFINE_STATEMENT }
                for (define in defineStatements) {
                    val name = define.definedName?.lowercase() ?: continue
                    val type = define.defineType
                    val hasOverwrite = define.text.uppercase().contains("OVERWRITE")
                    
                    if (hasOverwrite) continue // Intentional overwrite
                    
                    val key = "${type.name}:$name"
                    definedElements.getOrPut(key) { mutableListOf() }.add(define)
                }
                
                // Collect LET statements
                val letStatements = PsiTreeUtil.findChildrenOfType(file, SurqlLetStatementImpl::class.java)
                for (let in letStatements) {
                    val name = let.parameterName?.lowercase() ?: continue
                    definedParams.getOrPut(name) { mutableListOf() }.add(let)
                }
                
                // Report duplicates
                reportDuplicateDefinitions()
                reportDuplicateParams()
            }
            
            private fun reportDuplicateDefinitions() {
                for ((key, elements) in definedElements) {
                    if (elements.size > 1) {
                        val (typeName, name) = key.split(":", limit = 2)
                        
                        // Report on all but the first occurrence
                        for (i in 1 until elements.size) {
                            val element = elements[i]
                            holder.registerProblem(
                                element,
                                "Duplicate ${typeName.lowercase()} definition: '$name' is already defined",
                                ProblemHighlightType.WARNING
                            )
                        }
                    }
                }
            }
            
            private fun reportDuplicateParams() {
                for ((name, elements) in definedParams) {
                    if (elements.size > 1) {
                        // Report on all but the first occurrence
                        for (i in 1 until elements.size) {
                            val element = elements[i]
                            holder.registerProblem(
                                element,
                                "Duplicate parameter definition: '\$$name' is already defined",
                                ProblemHighlightType.WARNING
                            )
                        }
                    }
                }
            }
        }
    }
}
