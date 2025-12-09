package org.surrealdb.surql.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.*
import org.surrealdb.surql.types.*

/**
 * Inspection that checks for type errors in SurrealQL code.
 * 
 * This inspection detects:
 * - Type mismatches in binary expressions (e.g., "string" + 5 without explicit intent)
 * - Invalid operator usage (e.g., arithmetic on non-numeric types)
 * - Function argument type mismatches
 * - Invalid type casts
 */
class SurqlTypeCheckInspection : SurqlInspectionBase() {
    
    override fun getDisplayName(): String = "Type check"
    
    override fun getShortName(): String = "SurqlTypeCheck"
    
    override fun buildSurqlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            
            private val schemaContext by lazy { SurqlSchemaContext.forFile(holder.file) }
            private val typeInference by lazy { SurqlTypeInference(schemaContext) }
            
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is SurqlBinaryExpressionImpl -> checkBinaryExpression(element, holder)
                    is SurqlFunctionCallImpl -> checkFunctionCall(element, holder)
                    is SurqlCastExpressionImpl -> checkCastExpression(element, holder)
                    is SurqlAssignmentImpl -> checkAssignment(element, holder)
                }
            }
            
            private fun checkBinaryExpression(element: SurqlBinaryExpressionImpl, holder: ProblemsHolder) {
                val operator = element.operatorText ?: return
                val leftExpr = element.leftOperand ?: return
                val rightExpr = element.rightOperand ?: return
                
                val leftType = typeInference.inferType(leftExpr)
                val rightType = typeInference.inferType(rightExpr)
                
                // Skip if types are unknown
                if (leftType == SurqlType.Unknown || rightType == SurqlType.Unknown) return
                if (leftType == SurqlType.Any || rightType == SurqlType.Any) return
                
                val result = SurqlTypeChecker.checkBinaryOperator(operator, leftType, rightType)
                
                when (result) {
                    is SurqlTypeChecker.TypeCheckResult.Incompatible -> {
                        holder.registerProblem(
                            element.operator ?: element,
                            "Operator '$operator' cannot be applied to '${leftType.displayName()}' and '${rightType.displayName()}': ${result.message}",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                    is SurqlTypeChecker.TypeCheckResult.CompatibleWithConversion -> {
                        // Optionally warn about implicit conversions
                        // This could be configurable
                    }
                    is SurqlTypeChecker.TypeCheckResult.Compatible -> {
                        // All good
                    }
                }
            }
            
            private fun checkFunctionCall(element: SurqlFunctionCallImpl, holder: ProblemsHolder) {
                val funcName = element.functionName ?: return
                
                // Get function signature
                val signature = SurqlBuiltinFunctions.getSignature(funcName)
                    ?: schemaContext.getFunction("fn::$funcName")?.let { func ->
                        // Convert user-defined function to signature for checking
                        SurqlBuiltinFunctions.FunctionSignature(
                            name = func.name,
                            parameters = func.parameters.map { param ->
                                SurqlBuiltinFunctions.FunctionParameter(
                                    name = param.name,
                                    type = param.type ?: SurqlType.Any
                                )
                            },
                            returnType = func.returnType ?: SurqlType.Any
                        )
                    }
                    ?: return // Unknown function, skip
                
                // Get argument types
                val args = element.arguments?.arguments ?: emptyList()
                val argTypes = args.map { typeInference.inferType(it) }
                
                // Skip if any arg type is unknown
                if (argTypes.any { it == SurqlType.Unknown }) return
                
                val result = SurqlTypeChecker.checkFunctionArguments(signature, argTypes)
                
                when (result) {
                    is SurqlTypeChecker.TypeCheckResult.Incompatible -> {
                        holder.registerProblem(
                            element.arguments ?: element,
                            result.message,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                    is SurqlTypeChecker.TypeCheckResult.CompatibleWithConversion -> {
                        // Could warn about implicit conversions
                    }
                    is SurqlTypeChecker.TypeCheckResult.Compatible -> {
                        // All good
                    }
                }
            }
            
            private fun checkCastExpression(element: SurqlCastExpressionImpl, holder: ProblemsHolder) {
                val targetTypeExpr = element.targetType ?: return
                val sourceExpr = element.expression ?: return
                
                val targetType = schemaContext.parseTypeString(targetTypeExpr.text)
                val sourceType = typeInference.inferType(sourceExpr)
                
                // Skip if types are unknown
                if (targetType == SurqlType.Unknown || sourceType == SurqlType.Unknown) return
                if (sourceType == SurqlType.Any) return
                
                // Check for impossible casts
                if (!isCastPossible(sourceType, targetType)) {
                    holder.registerProblem(
                        element,
                        "Cast from '${sourceType.displayName()}' to '${targetType.displayName()}' will always fail",
                        ProblemHighlightType.WARNING
                    )
                }
            }
            
            private fun isCastPossible(from: SurqlType, to: SurqlType): Boolean {
                // Most casts are possible in SurrealDB with varying success
                // Only flag obviously impossible ones
                return when {
                    from == to -> true
                    to == SurqlType.Any -> true
                    to == SurqlType.Str -> true // Anything can be cast to string
                    
                    // Numeric conversions are always possible
                    SurqlType.isNumeric(from) && SurqlType.isNumeric(to) -> true
                    
                    // String to numeric is possible (may fail at runtime)
                    from == SurqlType.Str && SurqlType.isNumeric(to) -> true
                    
                    // String to bool is possible
                    from == SurqlType.Str && to == SurqlType.Bool -> true
                    
                    // Record to specific record type might work
                    from is SurqlType.Record && to is SurqlType.Record -> true
                    
                    // Array element type casts
                    from is SurqlType.Array && to is SurqlType.Array -> true
                    
                    // Most other conversions are allowed
                    else -> true
                }
            }
            
            private fun checkAssignment(element: SurqlAssignmentImpl, holder: ProblemsHolder) {
                val targetName = element.targetName ?: return
                val valueExpr = element.value ?: return
                
                // Try to find the field type from context
                val fieldType = findFieldType(element, targetName)
                if (fieldType == SurqlType.Unknown || fieldType == SurqlType.Any) return
                
                val valueType = typeInference.inferType(valueExpr)
                if (valueType == SurqlType.Unknown) return
                
                val result = SurqlTypeChecker.isAssignable(fieldType, valueType)
                
                when (result) {
                    is SurqlTypeChecker.TypeCheckResult.Incompatible -> {
                        holder.registerProblem(
                            valueExpr,
                            "Type mismatch: field '$targetName' expects '${fieldType.displayName()}', got '${valueType.displayName()}'",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                    is SurqlTypeChecker.TypeCheckResult.CompatibleWithConversion -> {
                        // Could show weak warning about implicit conversion
                    }
                    is SurqlTypeChecker.TypeCheckResult.Compatible -> {
                        // All good
                    }
                }
            }
            
            private fun findFieldType(assignment: SurqlAssignmentImpl, fieldName: String): SurqlType {
                // Try to find the table context from parent statement
                var parent = assignment.parent
                while (parent != null) {
                    when (parent.node.elementType) {
                        SurqlElementTypes.UPDATE_STATEMENT,
                        SurqlElementTypes.CREATE_STATEMENT,
                        SurqlElementTypes.INSERT_STATEMENT,
                        SurqlElementTypes.UPSERT_STATEMENT -> {
                            // Try to extract table name from statement
                            val tableName = extractTableFromStatement(parent)
                            if (tableName != null) {
                                return schemaContext.getFieldType(tableName, fieldName)
                            }
                        }
                    }
                    parent = parent.parent
                }
                return SurqlType.Unknown
            }
            
            private fun extractTableFromStatement(statement: PsiElement): String? {
                val text = statement.text
                
                // Try to match UPDATE/CREATE/INSERT table patterns
                val patterns = listOf(
                    Regex("""UPDATE\s+(\w+)""", RegexOption.IGNORE_CASE),
                    Regex("""CREATE\s+(\w+)""", RegexOption.IGNORE_CASE),
                    Regex("""INSERT\s+(?:INTO\s+)?(\w+)""", RegexOption.IGNORE_CASE),
                    Regex("""UPSERT\s+(\w+)""", RegexOption.IGNORE_CASE)
                )
                
                for (pattern in patterns) {
                    val match = pattern.find(text)
                    if (match != null) {
                        val tableName = match.groupValues[1]
                        // Exclude keywords that might be matched
                        if (tableName.lowercase() !in listOf("set", "content", "merge", "patch", "into")) {
                            return tableName
                        }
                    }
                }
                
                return null
            }
        }
    }
}
