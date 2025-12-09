package org.surrealdb.surql.types

import com.intellij.psi.PsiElement
import org.surrealdb.surql.psi.impl.*

/**
 * Type inference engine for SurrealQL expressions.
 * 
 * This class infers the type of any expression based on:
 * - Literal types (string, number, boolean, etc.)
 * - Operator semantics (arithmetic, comparison, logical, etc.)
 * - Function return types
 * - Schema definitions (DEFINE FIELD types)
 * - Parameter types (LET, DEFINE PARAM)
 */
class SurqlTypeInference(private val context: SurqlSchemaContext) {
    
    /**
     * Infers the type of an expression.
     */
    fun inferType(element: PsiElement): SurqlType {
        return when (element) {
            // Literals
            is SurqlStringLiteralImpl -> SurqlType.Str
            is SurqlNumberLiteralImpl -> inferNumberLiteralType(element)
            is SurqlBooleanLiteralImpl -> SurqlType.Bool
            is SurqlNullLiteralImpl -> SurqlType.Null
            is SurqlNoneLiteralImpl -> SurqlType.None
            is SurqlDatetimeLiteralImpl -> SurqlType.Datetime
            is SurqlDurationLiteralImpl -> SurqlType.Duration
            is SurqlUuidLiteralImpl -> SurqlType.Uuid
            is SurqlRecordIdLiteralImpl -> inferRecordIdType(element)
            
            // Compound literals
            is SurqlArrayLiteralImpl -> inferArrayLiteralType(element)
            is SurqlObjectLiteralImpl -> inferObjectLiteralType(element)
            
            // Operators
            is SurqlBinaryExpressionImpl -> inferBinaryExpressionType(element)
            is SurqlUnaryExpressionImpl -> inferUnaryExpressionType(element)
            is SurqlTernaryExpressionImpl -> inferTernaryExpressionType(element)
            is SurqlRangeExpressionImpl -> inferRangeExpressionType(element)
            
            // Access expressions
            is SurqlFieldAccessImpl -> inferFieldAccessType(element)
            is SurqlIndexAccessImpl -> inferIndexAccessType(element)
            is SurqlGraphTraversalImpl -> inferGraphTraversalType(element)
            
            // Function calls
            is SurqlFunctionCallImpl -> inferFunctionCallType(element)
            
            // Subqueries
            is SurqlSubqueryImpl -> inferSubqueryType(element)
            
            // Cast expressions
            is SurqlCastExpressionImpl -> inferCastType(element)
            
            // References
            is SurqlParameterRefImpl -> inferParameterRefType(element)
            is SurqlIdentifierRefImpl -> inferIdentifierRefType(element)
            
            // Parenthesized expression
            is SurqlParenExpressionImpl -> element.innerExpression?.let { inferType(it) } ?: SurqlType.Unknown
            
            // Future block
            is SurqlFutureBlockImpl -> SurqlType.Future(
                element.innerExpression?.let { inferType(it) } ?: SurqlType.Any
            )
            
            // Generic expression base class - try to determine from children
            is SurqlExpressionImpl -> inferGenericExpressionType(element)
            
            else -> SurqlType.Unknown
        }
    }
    
    // ==================== Literal Type Inference ====================
    
    private fun inferNumberLiteralType(element: SurqlNumberLiteralImpl): SurqlType {
        return when {
            element.isDecimal -> SurqlType.Decimal
            element.isFloat -> SurqlType.Float
            element.isInteger -> SurqlType.Int
            else -> SurqlType.Number
        }
    }
    
    private fun inferRecordIdType(element: SurqlRecordIdLiteralImpl): SurqlType {
        val tableName = element.tableName
        return if (tableName != null) {
            SurqlType.Record(setOf(tableName))
        } else {
            SurqlType.Record()
        }
    }
    
    private fun inferArrayLiteralType(element: SurqlArrayLiteralImpl): SurqlType {
        val elements = element.elements
        if (elements.isEmpty()) {
            return SurqlType.Array(SurqlType.Any)
        }
        
        // Infer element type from array contents
        val elementTypes = elements.map { inferType(it) }.toSet()
        
        val elementType = when {
            elementTypes.size == 1 -> elementTypes.first()
            elementTypes.all { SurqlType.isNumeric(it) } -> SurqlType.Number
            else -> SurqlType.Union.of(*elementTypes.toTypedArray())
        }
        
        return SurqlType.Array(elementType, elements.size)
    }
    
    private fun inferObjectLiteralType(element: SurqlObjectLiteralImpl): SurqlType {
        val fields = mutableMapOf<String, SurqlType>()
        
        for (entry in element.entries) {
            val key = entry.keyText ?: continue
            val value = entry.value?.let { inferType(it) } ?: SurqlType.Unknown
            fields[key] = value
        }
        
        return SurqlType.Object(fields, isFlexible = true)
    }
    
    // ==================== Operator Type Inference ====================
    
    private fun inferBinaryExpressionType(element: SurqlBinaryExpressionImpl): SurqlType {
        val op = element.operatorText?.uppercase() ?: return SurqlType.Unknown
        val left = element.leftOperand?.let { inferType(it) } ?: SurqlType.Unknown
        val right = element.rightOperand?.let { inferType(it) } ?: SurqlType.Unknown
        
        return when (op) {
            // Comparison operators -> bool
            "==", "!=", "<", ">", "<=", ">=", "=", "IS", "IS NOT" -> SurqlType.Bool
            
            // Logical operators -> bool
            "&&", "||", "AND", "OR" -> SurqlType.Bool
            
            // Containment operators -> bool
            "CONTAINS", "CONTAINSALL", "CONTAINSANY", "CONTAINSNONE", "CONTAINSNOT",
            "∋", "⊇", "⊃", "⊅", "∌" -> SurqlType.Bool
            
            // In/inside operators -> bool
            "IN", "NOT IN", "∈", "∉",
            "INSIDE", "NOTINSIDE", "ALLINSIDE", "ANYINSIDE", "NONEINSIDE" -> SurqlType.Bool
            
            // Geo operators -> bool
            "OUTSIDE", "INTERSECTS" -> SurqlType.Bool
            
            // Pattern matching -> bool
            "LIKE", "NOT LIKE", "~", "!~", "?~", "*~",
            "MATCHES", "@" -> SurqlType.Bool
            
            // Arithmetic operators
            "+", "-", "*", "/", "%" -> inferArithmeticType(op, left, right)
            "^", "**" -> SurqlType.Float // Power always returns float
            
            // Range operators -> array
            "..", "..=" -> SurqlType.Array(left)
            
            // Null coalescing
            "??", "?:" -> {
                // Returns right type if left is null/none, otherwise left type
                if (left == SurqlType.Null || left == SurqlType.None) {
                    right
                } else if (left is SurqlType.Option) {
                    SurqlType.Union.of(left.innerType, right)
                } else {
                    SurqlType.Union.of(left, right)
                }
            }
            
            else -> SurqlType.Unknown
        }
    }
    
    private fun inferArithmeticType(op: String, left: SurqlType, right: SurqlType): SurqlType {
        // String concatenation
        if (op == "+" && (left == SurqlType.Str || right == SurqlType.Str)) {
            return SurqlType.Str
        }
        
        // Duration arithmetic
        if (left == SurqlType.Duration || right == SurqlType.Duration) {
            return when {
                op == "+" && left == SurqlType.Datetime -> SurqlType.Datetime
                op == "+" && right == SurqlType.Datetime -> SurqlType.Datetime
                op == "-" && left == SurqlType.Datetime && right == SurqlType.Datetime -> SurqlType.Duration
                op == "-" && left == SurqlType.Datetime -> SurqlType.Datetime
                op in listOf("+", "-") && left == SurqlType.Duration && right == SurqlType.Duration -> SurqlType.Duration
                op in listOf("*", "/") && SurqlType.isNumeric(right) -> SurqlType.Duration
                else -> SurqlType.Unknown
            }
        }
        
        // Datetime arithmetic
        if (left == SurqlType.Datetime && right == SurqlType.Datetime && op == "-") {
            return SurqlType.Duration
        }
        
        // Array operations
        if (left is SurqlType.Array) {
            return when (op) {
                "+" -> left // Append
                "-" -> left // Remove
                else -> SurqlType.Unknown
            }
        }
        
        // Set operations
        if (left is SurqlType.Set) {
            return when (op) {
                "+" -> left // Union
                "-" -> left // Difference
                else -> SurqlType.Unknown
            }
        }
        
        // Numeric type promotion
        return promoteNumericTypes(left, right)
    }
    
    private fun promoteNumericTypes(a: SurqlType, b: SurqlType): SurqlType {
        val aNum = unwrapToNumeric(a)
        val bNum = unwrapToNumeric(b)
        
        if (aNum == null && bNum == null) return SurqlType.Unknown
        
        return when {
            aNum == SurqlType.Decimal || bNum == SurqlType.Decimal -> SurqlType.Decimal
            aNum == SurqlType.Float || bNum == SurqlType.Float -> SurqlType.Float
            aNum == SurqlType.Number || bNum == SurqlType.Number -> SurqlType.Number
            aNum == SurqlType.Int && bNum == SurqlType.Int -> SurqlType.Int
            aNum != null -> aNum
            bNum != null -> bNum
            else -> SurqlType.Number
        }
    }
    
    private fun unwrapToNumeric(type: SurqlType): SurqlType? {
        return when (type) {
            is SurqlType.Int, is SurqlType.Float, is SurqlType.Decimal, is SurqlType.Number -> type
            is SurqlType.Option -> unwrapToNumeric(type.innerType)
            else -> null
        }
    }
    
    private fun inferUnaryExpressionType(element: SurqlUnaryExpressionImpl): SurqlType {
        val op = element.operator?.text?.uppercase() ?: return SurqlType.Unknown
        val operandType = element.operand?.let { inferType(it) } ?: SurqlType.Unknown
        
        return when (op) {
            "NOT", "!" -> SurqlType.Bool
            "-", "+" -> operandType // Numeric negation/positive preserves type
            "~" -> SurqlType.Bool // Fuzzy match
            else -> SurqlType.Unknown
        }
    }
    
    private fun inferTernaryExpressionType(element: SurqlTernaryExpressionImpl): SurqlType {
        val thenType = element.thenExpression?.let { inferType(it) } ?: SurqlType.Unknown
        val elseType = element.elseExpression?.let { inferType(it) } ?: SurqlType.Unknown
        
        // Result is union of both branches
        return if (thenType == elseType) {
            thenType
        } else {
            SurqlType.Union.of(thenType, elseType)
        }
    }
    
    private fun inferRangeExpressionType(element: SurqlRangeExpressionImpl): SurqlType {
        val startType = element.start?.let { inferType(it) } ?: SurqlType.Int
        return SurqlType.Array(startType)
    }
    
    // ==================== Access Expression Type Inference ====================
    
    private fun inferFieldAccessType(element: SurqlFieldAccessImpl): SurqlType {
        val targetType = element.target?.let { inferType(it) } ?: return SurqlType.Unknown
        val fieldName = element.fieldName ?: return SurqlType.Unknown
        
        return when (targetType) {
            is SurqlType.Object -> {
                targetType.fields[fieldName] ?: if (targetType.isFlexible) SurqlType.Any else SurqlType.Unknown
            }
            is SurqlType.Record -> {
                // Look up field type from schema
                if (targetType.tables.size == 1) {
                    context.getFieldType(targetType.tables.first(), fieldName)
                } else if (targetType.tables.isNotEmpty()) {
                    // Multiple possible tables - try to find common field type
                    val fieldTypes = targetType.tables.mapNotNull { table ->
                        val type = context.getFieldType(table, fieldName)
                        if (type != SurqlType.Unknown) type else null
                    }.toSet()
                    
                    when {
                        fieldTypes.isEmpty() -> SurqlType.Unknown
                        fieldTypes.size == 1 -> fieldTypes.first()
                        else -> SurqlType.Union.of(*fieldTypes.toTypedArray())
                    }
                } else {
                    SurqlType.Unknown
                }
            }
            else -> SurqlType.Unknown
        }
    }
    
    private fun inferIndexAccessType(element: SurqlIndexAccessImpl): SurqlType {
        val targetType = element.target?.let { inferType(it) } ?: return SurqlType.Unknown
        val indexType = element.index?.let { inferType(it) }
        
        return when (targetType) {
            is SurqlType.Array -> targetType.elementType
            is SurqlType.Set -> targetType.elementType
            is SurqlType.Object -> {
                // Object with dynamic key access
                if (indexType == SurqlType.Str) {
                    if (targetType.isFlexible) SurqlType.Any else SurqlType.Unknown
                } else {
                    SurqlType.Unknown
                }
            }
            is SurqlType.Str -> SurqlType.Str // Character access
            else -> SurqlType.Unknown
        }
    }
    
    private fun inferGraphTraversalType(element: SurqlGraphTraversalImpl): SurqlType {
        // Graph traversal typically returns array of records
        return SurqlType.Array(SurqlType.Record())
    }
    
    // ==================== Function Call Type Inference ====================
    
    private fun inferFunctionCallType(element: SurqlFunctionCallImpl): SurqlType {
        val funcName = element.functionName ?: return SurqlType.Unknown
        
        // Check built-in functions first
        SurqlBuiltinFunctions.getSignature(funcName)?.let { sig ->
            return sig.returnType
        }
        
        // Check user-defined functions
        context.getFunction("fn::$funcName")?.let { func ->
            return func.returnType ?: SurqlType.Any
        }
        
        // Handle special aggregate functions
        return when (funcName.lowercase()) {
            "count" -> SurqlType.Int
            "sum", "avg", "mean" -> SurqlType.Number
            "min", "max" -> {
                // Type depends on argument
                val args = element.arguments?.arguments ?: return SurqlType.Any
                if (args.isNotEmpty()) inferType(args.first()) else SurqlType.Any
            }
            "group" -> SurqlType.Array(SurqlType.Any)
            "collect" -> SurqlType.Array(SurqlType.Any)
            else -> SurqlType.Unknown
        }
    }
    
    // ==================== Subquery Type Inference ====================
    
    private fun inferSubqueryType(element: SurqlSubqueryImpl): SurqlType {
        val innerStatement = element.innerStatement ?: return SurqlType.Unknown
        
        // SELECT statements return arrays of objects/records
        val stmtText = innerStatement.text.uppercase().trim()
        return when {
            stmtText.startsWith("SELECT") -> {
                // Could be more sophisticated and analyze the SELECT fields
                SurqlType.Array(SurqlType.Object())
            }
            stmtText.startsWith("CREATE") -> SurqlType.Record()
            stmtText.startsWith("UPDATE") -> SurqlType.Array(SurqlType.Record())
            stmtText.startsWith("DELETE") -> SurqlType.Array(SurqlType.Record())
            stmtText.startsWith("INSERT") -> SurqlType.Array(SurqlType.Record())
            stmtText.startsWith("RELATE") -> SurqlType.Record()
            else -> SurqlType.Unknown
        }
    }
    
    // ==================== Cast Type Inference ====================
    
    private fun inferCastType(element: SurqlCastExpressionImpl): SurqlType {
        val typeExpr = element.targetType ?: return SurqlType.Unknown
        return context.parseTypeString(typeExpr.text)
    }
    
    // ==================== Reference Type Inference ====================
    
    private fun inferParameterRefType(element: SurqlParameterRefImpl): SurqlType {
        val paramName = element.parameterName
        
        // Check system parameters
        return when (paramName.lowercase()) {
            "this" -> SurqlType.Record()
            "parent" -> SurqlType.Record()
            "value" -> SurqlType.Any
            "input" -> SurqlType.Any
            "before" -> SurqlType.Object()
            "after" -> SurqlType.Object()
            "event" -> SurqlType.Str
            "auth" -> SurqlType.Option(SurqlType.Record())
            "session" -> SurqlType.Object()
            "scope" -> SurqlType.Option(SurqlType.Str)
            "token" -> SurqlType.Option(SurqlType.Object())
            else -> context.getParameterType(paramName, element)
        }
    }
    
    private fun inferIdentifierRefType(element: SurqlIdentifierRefImpl): SurqlType {
        val name = element.identifierName
        
        // Check if it's a table reference
        if (context.getTable(name) != null) {
            return SurqlType.Record(setOf(name))
        }
        
        // Otherwise it's likely a field reference in a statement context
        return SurqlType.Unknown
    }
    
    // ==================== Generic Expression Fallback ====================
    
    private fun inferGenericExpressionType(element: SurqlExpressionImpl): SurqlType {
        // Try to infer from first child that is an expression
        val firstExprChild = element.children.filterIsInstance<SurqlExpressionImpl>().firstOrNull()
        return firstExprChild?.let { inferType(it) } ?: SurqlType.Unknown
    }
    
    companion object {
        /**
         * Convenience method to infer type of an element using its file's schema context.
         */
        fun inferType(element: PsiElement): SurqlType {
            val context = SurqlSchemaContext.forElement(element)
            return SurqlTypeInference(context).inferType(element)
        }
    }
}
