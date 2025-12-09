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
import org.surrealdb.surql.psi.impl.*

/**
 * Inspection that detects references to undefined tables.
 * 
 * This inspection tracks tables defined via DEFINE TABLE statements
 * and warns when a table is referenced but not defined in the same file.
 */
class SurqlUndefinedTableInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Undefined table reference"
    
    override fun getShortName(): String = "SurqlUndefinedTable"
    
    override fun getStaticDescription(): String = """
        Reports references to tables that are not defined in the current file.
        <p>
        This inspection helps catch typos and ensures that all referenced tables
        have corresponding DEFINE TABLE statements.
        </p>
        <p>
        Note: This inspection only checks within the current file. Tables defined
        in other files or via external means will trigger false positives.
        </p>
    """.trimIndent()
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            // Cache of defined tables in this file
            private var definedTables: Set<String>? = null
            
            private fun getDefinedTables(element: PsiElement): Set<String> {
                if (definedTables == null) {
                    val file = element.containingFile
                    // Only look at top-level DEFINE_STATEMENT, not nested definition elements
                    val defineStatements = PsiTreeUtil.findChildrenOfType(file, SurqlDefineStatementImpl::class.java)
                        .filter { it.node.elementType == SurqlElementTypes.DEFINE_STATEMENT }
                    definedTables = defineStatements
                        .filter { it.defineType == SurqlDefineStatementImpl.DefineType.TABLE }
                        .mapNotNull { it.definedName?.lowercase() }
                        .toSet()
                }
                return definedTables!!
            }
            
            override fun visitElement(element: PsiElement) {
                // Check table name elements
                if (element.node.elementType == SurqlElementTypes.TABLE_NAME) {
                    checkTableReference(element)
                }
                
                // Also check identifiers that might be table references in specific contexts
                if (element is SurqlIdentifierRefImpl) {
                    val parent = element.parent
                    // Check if this is in a FROM clause or similar table context
                    if (isTableContext(element)) {
                        checkTableReference(element)
                    }
                }
            }
            
            private fun isTableContext(element: PsiElement): Boolean {
                var current = element.parent
                var depth = 0
                while (current != null && depth < 5) {
                    when (current) {
                        is SurqlSelectStatementImpl,
                        is SurqlCreateStatementImpl,
                        is SurqlUpdateStatementImpl,
                        is SurqlDeleteStatementImpl,
                        is SurqlInsertStatementImpl,
                        is SurqlUpsertStatementImpl -> {
                            // Check if we're after FROM, INTO, or directly after the statement keyword
                            return true
                        }
                    }
                    current = current.parent
                    depth++
                }
                return false
            }
            
            private fun checkTableReference(element: PsiElement) {
                val tableName = element.text.lowercase()
                
                // Skip if it looks like a record ID (contains :)
                if (tableName.contains(':')) return
                
                // Skip system tables and common reserved names
                if (tableName in SYSTEM_TABLES) return
                
                // Skip if it's a keyword being used as identifier
                if (tableName in RESERVED_KEYWORDS) return
                
                val definedTables = getDefinedTables(element)
                
                // If no tables are defined at all, skip the check
                // (likely the file doesn't have schema definitions)
                if (definedTables.isEmpty()) return
                
                if (tableName !in definedTables) {
                    holder.registerProblem(
                        element,
                        "Table '$tableName' is not defined in this file",
                        ProblemHighlightType.WEAK_WARNING,
                        CreateDefineTableQuickFix(element.text)
                    )
                }
            }
        }
    }
    
    /**
     * Quick fix that creates a DEFINE TABLE statement for the undefined table.
     */
    private class CreateDefineTableQuickFix(private val tableName: String) : LocalQuickFix {
        
        override fun getName(): String = "Create DEFINE TABLE $tableName"
        
        override fun getFamilyName(): String = "Create table definition"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile
            
            // Find a good insertion point - before the first statement or at the beginning
            val firstChild = file.firstChild
            
            // Create the DEFINE TABLE statement text
            val defineStatement = "DEFINE TABLE $tableName SCHEMALESS;\n\n"
            
            // Insert at the beginning of the file
            val document = file.viewProvider.document ?: return
            document.insertString(0, defineStatement)
        }
    }
    
    companion object {
        // System tables that don't need to be defined
        private val SYSTEM_TABLES = setOf(
            "scope", "token", "user", "session"
        )
        
        // Reserved keywords that might appear as identifiers
        private val RESERVED_KEYWORDS = setOf(
            "true", "false", "null", "none",
            "select", "from", "where", "create", "update", "delete",
            "insert", "into", "values", "set", "content", "merge",
            "return", "before", "after", "diff", "none"
        )
    }
}
