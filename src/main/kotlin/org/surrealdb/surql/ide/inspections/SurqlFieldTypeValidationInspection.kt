package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.*
import org.surrealdb.surql.types.*

/**
 * Inspection that validates field values against their declared types.
 * 
 * This inspection checks:
 * - CREATE/UPDATE/UPSERT statements with CONTENT or SET clauses
 * - Field assignments match DEFINE FIELD types
 * - SCHEMAFULL tables don't have undefined fields
 * - Required fields (non-optional) are present
 */
class SurqlFieldTypeValidationInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Field type validation"
    
    override fun getShortName(): String = "SurqlFieldTypeValidation"
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            private val schemaContext by lazy { SurqlSchemaContext.forFile(holder.file) }
            private val typeInference by lazy { SurqlTypeInference(schemaContext) }
            
            override fun visitElement(element: PsiElement) {
                val elementType = element.node.elementType
                when (elementType) {
                    SurqlElementTypes.CREATE_STATEMENT -> checkCreateStatement(element, holder)
                    SurqlElementTypes.UPDATE_STATEMENT -> checkUpdateStatement(element, holder)
                    SurqlElementTypes.INSERT_STATEMENT -> checkInsertStatement(element, holder)
                    SurqlElementTypes.UPSERT_STATEMENT -> checkUpsertStatement(element, holder)
                    SurqlElementTypes.CONTENT_CLAUSE -> checkContentClause(element, holder)
                    SurqlElementTypes.SET_CLAUSE -> checkSetClause(element, holder)
                }
            }
            
            private fun checkCreateStatement(element: PsiElement, holder: ProblemsHolder) {
                val tableName = extractTableName(element, "CREATE") ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                // Check for SCHEMAFULL tables - all fields must be defined
                if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                    tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                    checkSchemafulCompliance(element, tableName, tableSchema, holder)
                }
            }
            
            private fun checkUpdateStatement(element: PsiElement, holder: ProblemsHolder) {
                val tableName = extractTableName(element, "UPDATE") ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                    tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                    checkSchemafulCompliance(element, tableName, tableSchema, holder)
                }
            }
            
            private fun checkInsertStatement(element: PsiElement, holder: ProblemsHolder) {
                val tableName = extractTableName(element, "INSERT") ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                    tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                    checkSchemafulCompliance(element, tableName, tableSchema, holder)
                }
            }
            
            private fun checkUpsertStatement(element: PsiElement, holder: ProblemsHolder) {
                val tableName = extractTableName(element, "UPSERT") ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                    tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                    checkSchemafulCompliance(element, tableName, tableSchema, holder)
                }
            }
            
            private fun checkContentClause(element: PsiElement, holder: ProblemsHolder) {
                val tableName = findTableNameFromParentStatement(element) ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                // Find the object literal in the CONTENT clause
                val objectLiteral = findChildOfTypeAs(element, SurqlObjectLiteralImpl::class.java) ?: return
                
                checkObjectAgainstSchema(objectLiteral, tableName, tableSchema, holder)
            }
            
            private fun checkSetClause(element: PsiElement, holder: ProblemsHolder) {
                val tableName = findTableNameFromParentStatement(element) ?: return
                val tableSchema = schemaContext.getTable(tableName) ?: return
                
                // Find assignments in the SET clause
                val assignments = findChildrenOfTypeAs(element, SurqlAssignmentImpl::class.java)
                
                for (assignment in assignments) {
                    checkAssignmentAgainstSchema(assignment, tableName, tableSchema, holder)
                }
            }
            
            private fun checkSchemafulCompliance(
                statement: PsiElement,
                tableName: String,
                tableSchema: SurqlSchemaContext.TableSchema,
                holder: ProblemsHolder
            ) {
                // Find SET or CONTENT clauses and check fields
                visitChildrenRecursive(statement) { child ->
                    when (child.node.elementType) {
                        SurqlElementTypes.CONTENT_CLAUSE -> {
                            val objectLiteral = findChildOfTypeAs(child, SurqlObjectLiteralImpl::class.java)
                            if (objectLiteral != null) {
                                checkObjectAgainstSchema(objectLiteral, tableName, tableSchema, holder)
                            }
                        }
                        SurqlElementTypes.SET_CLAUSE -> {
                            val assignments = findChildrenOfTypeAs(child, SurqlAssignmentImpl::class.java)
                            for (assignment in assignments) {
                                checkAssignmentAgainstSchema(assignment, tableName, tableSchema, holder)
                            }
                        }
                    }
                }
            }
            
            private fun checkObjectAgainstSchema(
                objectLiteral: SurqlObjectLiteralImpl,
                tableName: String,
                tableSchema: SurqlSchemaContext.TableSchema,
                holder: ProblemsHolder
            ) {
                val providedFields = mutableSetOf<String>()
                
                // Check each field in the object
                for (entry in objectLiteral.entries) {
                    val fieldName = entry.keyText ?: continue
                    val valueExpr = entry.value ?: continue
                    providedFields.add(fieldName)
                    
                    val fieldSchema = tableSchema.fields[fieldName]
                    
                    // Check for undefined field in SCHEMAFULL table
                    if (fieldSchema == null) {
                        if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                            tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                            holder.registerProblem(
                                entry.key ?: entry,
                                "Field '$fieldName' is not defined in SCHEMAFULL table '$tableName'",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                DefineFieldQuickFix(tableName, fieldName)
                            )
                        }
                        continue
                    }
                    
                    // Check type compatibility
                    if (!fieldSchema.isFlexible) {
                        val valueType = typeInference.inferType(valueExpr)
                        if (valueType != SurqlType.Unknown && valueType != SurqlType.Any) {
                            val result = SurqlTypeChecker.isAssignable(fieldSchema.type, valueType)
                            
                            when (result) {
                                is SurqlTypeChecker.TypeCheckResult.Incompatible -> {
                                    holder.registerProblem(
                                        valueExpr,
                                        "Type mismatch for field '$fieldName': expected '${fieldSchema.type.displayName()}', got '${valueType.displayName()}'",
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                                    )
                                }
                                is SurqlTypeChecker.TypeCheckResult.CompatibleWithConversion -> {
                                    // Could show info about implicit conversion
                                }
                                is SurqlTypeChecker.TypeCheckResult.Compatible -> {
                                    // All good
                                }
                            }
                        }
                    }
                    
                    // Check READONLY constraint
                    if (fieldSchema.isReadonly) {
                        holder.registerProblem(
                            entry.key ?: entry,
                            "Field '$fieldName' is READONLY and cannot be set directly",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
                
                // Check for missing required fields (only for CREATE, not UPDATE)
                val parentStatement = findParentStatement(objectLiteral)
                if (parentStatement?.node?.elementType == SurqlElementTypes.CREATE_STATEMENT) {
                    for ((fieldName, fieldSchema) in tableSchema.fields) {
                        if (fieldName !in providedFields && 
                            !isOptionalField(fieldSchema) &&
                            fieldSchema.defaultValue == null &&
                            fieldSchema.valueExpr == null) {
                            holder.registerProblem(
                                objectLiteral,
                                "Missing required field '$fieldName' of type '${fieldSchema.type.displayName()}'",
                                ProblemHighlightType.WARNING
                            )
                        }
                    }
                }
            }
            
            private fun checkAssignmentAgainstSchema(
                assignment: SurqlAssignmentImpl,
                tableName: String,
                tableSchema: SurqlSchemaContext.TableSchema,
                holder: ProblemsHolder
            ) {
                val fieldName = assignment.targetName ?: return
                val valueExpr = assignment.value ?: return
                
                val fieldSchema = tableSchema.fields[fieldName]
                
                // Check for undefined field in SCHEMAFULL table
                if (fieldSchema == null) {
                    if (tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFULL ||
                        tableSchema.schemaMode == SurqlSchemaContext.SchemaMode.SCHEMAFUL) {
                        holder.registerProblem(
                            assignment.target ?: assignment,
                            "Field '$fieldName' is not defined in SCHEMAFULL table '$tableName'",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            DefineFieldQuickFix(tableName, fieldName)
                        )
                    }
                    return
                }
                
                // Check type compatibility
                if (!fieldSchema.isFlexible) {
                    val valueType = typeInference.inferType(valueExpr)
                    if (valueType != SurqlType.Unknown && valueType != SurqlType.Any) {
                        val result = SurqlTypeChecker.isAssignable(fieldSchema.type, valueType)
                        
                        when (result) {
                            is SurqlTypeChecker.TypeCheckResult.Incompatible -> {
                                holder.registerProblem(
                                    valueExpr,
                                    "Type mismatch for field '$fieldName': expected '${fieldSchema.type.displayName()}', got '${valueType.displayName()}'",
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                                )
                            }
                            is SurqlTypeChecker.TypeCheckResult.CompatibleWithConversion -> {
                                // Could show info about implicit conversion
                            }
                            is SurqlTypeChecker.TypeCheckResult.Compatible -> {
                                // All good
                            }
                        }
                    }
                }
                
                // Check READONLY constraint
                if (fieldSchema.isReadonly) {
                    holder.registerProblem(
                        assignment.target ?: assignment,
                        "Field '$fieldName' is READONLY and cannot be set directly",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
            
            private fun isOptionalField(fieldSchema: SurqlSchemaContext.FieldSchema): Boolean {
                return fieldSchema.type is SurqlType.Option ||
                       fieldSchema.type.isNullable() ||
                       fieldSchema.isFlexible
            }
            
            private fun extractTableName(element: PsiElement, keyword: String): String? {
                val text = element.text
                val pattern = when (keyword) {
                    "INSERT" -> Regex("""INSERT\s+(?:INTO\s+)?(\w+)""", RegexOption.IGNORE_CASE)
                    else -> Regex("""$keyword\s+(\w+)""", RegexOption.IGNORE_CASE)
                }
                
                val match = pattern.find(text)
                val tableName = match?.groupValues?.get(1)
                
                // Filter out keywords
                if (tableName?.lowercase() in listOf("set", "content", "merge", "patch", "into", "only", "ignore")) {
                    return null
                }
                
                return tableName
            }
            
            private fun findTableNameFromParentStatement(element: PsiElement): String? {
                var parent = element.parent
                while (parent != null) {
                    when (parent.node.elementType) {
                        SurqlElementTypes.CREATE_STATEMENT -> return extractTableName(parent, "CREATE")
                        SurqlElementTypes.UPDATE_STATEMENT -> return extractTableName(parent, "UPDATE")
                        SurqlElementTypes.INSERT_STATEMENT -> return extractTableName(parent, "INSERT")
                        SurqlElementTypes.UPSERT_STATEMENT -> return extractTableName(parent, "UPSERT")
                    }
                    parent = parent.parent
                }
                return null
            }
            
            private fun findParentStatement(element: PsiElement): PsiElement? {
                var parent = element.parent
                while (parent != null) {
                    val type = parent.node.elementType
                    if (type == SurqlElementTypes.CREATE_STATEMENT ||
                        type == SurqlElementTypes.UPDATE_STATEMENT ||
                        type == SurqlElementTypes.INSERT_STATEMENT ||
                        type == SurqlElementTypes.UPSERT_STATEMENT) {
                        return parent
                    }
                    parent = parent.parent
                }
                return null
            }
            
            private fun findChildOfType(element: PsiElement, targetClass: Class<*>): PsiElement? {
                for (child in element.children) {
                    if (targetClass.isInstance(child)) return child
                    val found = findChildOfType(child, targetClass)
                    if (found != null) return found
                }
                return null
            }
            
            @Suppress("UNCHECKED_CAST")
            private fun <T : PsiElement> findChildOfTypeAs(element: PsiElement, targetClass: Class<T>): T? {
                return findChildOfType(element, targetClass) as? T
            }
            
            private fun findChildrenOfType(element: PsiElement, targetClass: Class<*>, result: MutableList<PsiElement>) {
                for (child in element.children) {
                    if (targetClass.isInstance(child)) result.add(child)
                    findChildrenOfType(child, targetClass, result)
                }
            }
            
            @Suppress("UNCHECKED_CAST")
            private fun <T : PsiElement> findChildrenOfTypeAs(element: PsiElement, targetClass: Class<T>): List<T> {
                val result = mutableListOf<PsiElement>()
                findChildrenOfType(element, targetClass, result)
                return result as List<T>
            }
            
            private fun visitChildrenRecursive(element: PsiElement, action: (PsiElement) -> Unit) {
                for (child in element.children) {
                    action(child)
                    visitChildrenRecursive(child, action)
                }
            }
        }
    }
    
    /**
     * Quick fix to add a DEFINE FIELD statement for an undefined field.
     */
    private class DefineFieldQuickFix(
        private val tableName: String,
        private val fieldName: String
    ) : LocalQuickFix {
        
        override fun getName(): String = "Define field '$fieldName' on table '$tableName'"
        
        override fun getFamilyName(): String = "SurrealQL"
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile ?: return
            
            // Find a good insertion point (after last DEFINE or at end of file)
            val insertionPoint = findInsertionPoint(file)
            val indent = getIndentation(insertionPoint)
            
            // Create the DEFINE FIELD statement
            val defineStatement = "\n${indent}DEFINE FIELD $fieldName ON TABLE $tableName TYPE any;\n"
            
            // Insert at insertion point
            val document = file.viewProvider.document ?: return
            val offset = insertionPoint?.textRange?.endOffset ?: document.textLength
            document.insertString(offset, defineStatement)
        }
        
        private fun findInsertionPoint(file: com.intellij.psi.PsiFile): PsiElement? {
            var lastDefine: PsiElement? = null
            
            for (child in file.children) {
                if (child.node.elementType == SurqlElementTypes.DEFINE_STATEMENT) {
                    // Check if it's a DEFINE TABLE or DEFINE FIELD for this table
                    val text = child.text.uppercase()
                    if (text.contains("DEFINE TABLE $tableName") ||
                        text.contains("DEFINE FIELD") && text.contains("ON TABLE $tableName")) {
                        lastDefine = child
                    }
                }
            }
            
            return lastDefine
        }
        
        private fun getIndentation(element: PsiElement?): String {
            if (element == null) return ""
            val lineStart = element.text.takeWhile { it == ' ' || it == '\t' }
            return lineStart
        }
    }
}
