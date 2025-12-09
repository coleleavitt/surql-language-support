package org.surrealdb.surql.types

/**
 * Type checker for SurrealQL that determines type compatibility.
 * 
 * This handles:
 * - Assignment compatibility (can value of type A be assigned to field of type B?)
 * - Operator compatibility (can operator X be applied to types A and B?)
 * - Function argument compatibility (do arguments match parameter types?)
 * - Implicit conversions (SurrealDB's automatic type coercion)
 */
object SurqlTypeChecker {
    
    /**
     * Result of a type check operation.
     */
    sealed class TypeCheckResult {
        /** Types are compatible */
        object Compatible : TypeCheckResult()
        
        /** Types are compatible with implicit conversion */
        data class CompatibleWithConversion(val message: String) : TypeCheckResult()
        
        /** Types are incompatible */
        data class Incompatible(val expected: SurqlType, val actual: SurqlType, val message: String) : TypeCheckResult()
        
        val isCompatible: Boolean
            get() = this is Compatible || this is CompatibleWithConversion
    }
    
    /**
     * Checks if a value of [actualType] can be assigned to a target of [expectedType].
     * 
     * This is used for:
     * - Field value validation (SET field = value)
     * - Parameter passing (fn(arg))
     * - CREATE/UPDATE content validation
     */
    fun isAssignable(expectedType: SurqlType, actualType: SurqlType): TypeCheckResult {
        // Any accepts everything
        if (expectedType == SurqlType.Any) return TypeCheckResult.Compatible
        
        // Unknown types are always compatible (can't validate)
        if (expectedType == SurqlType.Unknown || actualType == SurqlType.Unknown) {
            return TypeCheckResult.Compatible
        }
        
        // Same type is always compatible
        if (expectedType == actualType) return TypeCheckResult.Compatible
        
        // Check specific type relationships
        return when (expectedType) {
            is SurqlType.Option -> checkOptionAssignment(expectedType, actualType)
            is SurqlType.Union -> checkUnionAssignment(expectedType, actualType)
            is SurqlType.Array -> checkArrayAssignment(expectedType, actualType)
            is SurqlType.Set -> checkSetAssignment(expectedType, actualType)
            is SurqlType.Object -> checkObjectAssignment(expectedType, actualType)
            is SurqlType.Record -> checkRecordAssignment(expectedType, actualType)
            is SurqlType.Literal -> checkLiteralAssignment(expectedType, actualType)
            is SurqlType.Number -> checkNumberAssignment(actualType)
            is SurqlType.Int -> checkIntAssignment(actualType)
            is SurqlType.Float -> checkFloatAssignment(actualType)
            is SurqlType.Decimal -> checkDecimalAssignment(actualType)
            is SurqlType.Str -> checkStringAssignment(actualType)
            is SurqlType.Geometry -> checkGeometryAssignment(expectedType, actualType)
            else -> checkDefaultAssignment(expectedType, actualType)
        }
    }
    
    private fun checkOptionAssignment(expected: SurqlType.Option, actual: SurqlType): TypeCheckResult {
        // Option<T> accepts T, null, and none
        if (actual == SurqlType.Null || actual == SurqlType.None) {
            return TypeCheckResult.Compatible
        }
        
        // Check if inner type is compatible
        return isAssignable(expected.innerType, actual)
    }
    
    private fun checkUnionAssignment(expected: SurqlType.Union, actual: SurqlType): TypeCheckResult {
        // Union accepts if any member type accepts
        for (memberType in expected.types) {
            if (isAssignable(memberType, actual).isCompatible) {
                return TypeCheckResult.Compatible
            }
        }
        
        // If actual is also a union, check if all its types are in expected
        if (actual is SurqlType.Union) {
            val allCompatible = actual.types.all { actualMember ->
                expected.types.any { expectedMember ->
                    isAssignable(expectedMember, actualMember).isCompatible
                }
            }
            if (allCompatible) return TypeCheckResult.Compatible
        }
        
        return TypeCheckResult.Incompatible(
            expected, actual,
            "Type '$actual' is not compatible with union type '$expected'"
        )
    }
    
    private fun checkArrayAssignment(expected: SurqlType.Array, actual: SurqlType): TypeCheckResult {
        if (actual !is SurqlType.Array) {
            return TypeCheckResult.Incompatible(expected, actual, "Expected array, got '$actual'")
        }
        
        // Check element type compatibility
        val elementResult = isAssignable(expected.elementType, actual.elementType)
        if (!elementResult.isCompatible) {
            return TypeCheckResult.Incompatible(
                expected, actual,
                "Array element types are incompatible: expected '${expected.elementType}', got '${actual.elementType}'"
            )
        }
        
        // Check size constraint if present
        if (expected.maxSize != null && actual.maxSize != null && actual.maxSize > expected.maxSize) {
            return TypeCheckResult.Incompatible(
                expected, actual,
                "Array size ${actual.maxSize} exceeds maximum ${expected.maxSize}"
            )
        }
        
        return TypeCheckResult.Compatible
    }
    
    private fun checkSetAssignment(expected: SurqlType.Set, actual: SurqlType): TypeCheckResult {
        // Set can accept array with same element type
        val actualElementType = when (actual) {
            is SurqlType.Set -> actual.elementType
            is SurqlType.Array -> actual.elementType
            else -> return TypeCheckResult.Incompatible(expected, actual, "Expected set, got '$actual'")
        }
        
        return isAssignable(expected.elementType, actualElementType)
    }
    
    private fun checkObjectAssignment(expected: SurqlType.Object, actual: SurqlType): TypeCheckResult {
        if (actual !is SurqlType.Object) {
            return TypeCheckResult.Incompatible(expected, actual, "Expected object, got '$actual'")
        }
        
        // If expected has defined fields, check they match
        for ((fieldName, expectedFieldType) in expected.fields) {
            val actualFieldType = actual.fields[fieldName]
            if (actualFieldType == null) {
                // Missing field - check if expected field is optional
                if (expectedFieldType !is SurqlType.Option) {
                    return TypeCheckResult.Incompatible(
                        expected, actual,
                        "Missing required field '$fieldName' of type '$expectedFieldType'"
                    )
                }
            } else {
                val fieldResult = isAssignable(expectedFieldType, actualFieldType)
                if (!fieldResult.isCompatible) {
                    return TypeCheckResult.Incompatible(
                        expected, actual,
                        "Field '$fieldName' type mismatch: expected '$expectedFieldType', got '$actualFieldType'"
                    )
                }
            }
        }
        
        // If object is not flexible, check for extra fields
        if (!expected.isFlexible) {
            for (fieldName in actual.fields.keys) {
                if (fieldName !in expected.fields) {
                    return TypeCheckResult.Incompatible(
                        expected, actual,
                        "Unexpected field '$fieldName' in strict schema object"
                    )
                }
            }
        }
        
        return TypeCheckResult.Compatible
    }
    
    private fun checkRecordAssignment(expected: SurqlType.Record, actual: SurqlType): TypeCheckResult {
        if (actual !is SurqlType.Record) {
            return TypeCheckResult.Incompatible(expected, actual, "Expected record, got '$actual'")
        }
        
        // If expected specifies tables, check they match
        if (expected.tables.isNotEmpty() && actual.tables.isNotEmpty()) {
            if (!actual.tables.all { it in expected.tables }) {
                return TypeCheckResult.Incompatible(
                    expected, actual,
                    "Record table(s) '${actual.tables}' not in expected tables '${expected.tables}'"
                )
            }
        }
        
        return TypeCheckResult.Compatible
    }
    
    private fun checkLiteralAssignment(expected: SurqlType.Literal, actual: SurqlType): TypeCheckResult {
        // Can't fully check literal values at static analysis time
        // Just check the base type is compatible
        val baseType = inferLiteralBaseType(expected)
        return isAssignable(baseType, actual)
    }
    
    private fun inferLiteralBaseType(literal: SurqlType.Literal): SurqlType {
        val types = literal.values.map { value ->
            when (value) {
                is String -> SurqlType.Str
                is Boolean -> SurqlType.Bool
                is Int, is Long -> SurqlType.Int
                is Float, is Double -> SurqlType.Float
                else -> SurqlType.Any
            }
        }.toSet()
        
        return if (types.size == 1) types.first() else SurqlType.Union.of(*types.toTypedArray())
    }
    
    private fun checkNumberAssignment(actual: SurqlType): TypeCheckResult {
        return when (actual) {
            is SurqlType.Int, is SurqlType.Float, is SurqlType.Decimal, is SurqlType.Number -> 
                TypeCheckResult.Compatible
            else -> TypeCheckResult.Incompatible(
                SurqlType.Number, actual,
                "Expected number type, got '$actual'"
            )
        }
    }
    
    private fun checkIntAssignment(actual: SurqlType): TypeCheckResult {
        return when (actual) {
            is SurqlType.Int -> TypeCheckResult.Compatible
            is SurqlType.Float, is SurqlType.Decimal -> 
                TypeCheckResult.CompatibleWithConversion("Implicit conversion from $actual to int (may lose precision)")
            is SurqlType.Number -> TypeCheckResult.Compatible
            else -> TypeCheckResult.Incompatible(SurqlType.Int, actual, "Expected int, got '$actual'")
        }
    }
    
    private fun checkFloatAssignment(actual: SurqlType): TypeCheckResult {
        return when (actual) {
            is SurqlType.Float -> TypeCheckResult.Compatible
            is SurqlType.Int -> TypeCheckResult.CompatibleWithConversion("Implicit conversion from int to float")
            is SurqlType.Number -> TypeCheckResult.Compatible
            is SurqlType.Decimal -> 
                TypeCheckResult.CompatibleWithConversion("Implicit conversion from decimal to float (may lose precision)")
            else -> TypeCheckResult.Incompatible(SurqlType.Float, actual, "Expected float, got '$actual'")
        }
    }
    
    private fun checkDecimalAssignment(actual: SurqlType): TypeCheckResult {
        return when (actual) {
            is SurqlType.Decimal -> TypeCheckResult.Compatible
            is SurqlType.Int -> TypeCheckResult.CompatibleWithConversion("Implicit conversion from int to decimal")
            is SurqlType.Float -> 
                TypeCheckResult.CompatibleWithConversion("Implicit conversion from float to decimal")
            is SurqlType.Number -> TypeCheckResult.Compatible
            else -> TypeCheckResult.Incompatible(SurqlType.Decimal, actual, "Expected decimal, got '$actual'")
        }
    }
    
    private fun checkStringAssignment(actual: SurqlType): TypeCheckResult {
        // Many types can be converted to string
        return when (actual) {
            is SurqlType.Str -> TypeCheckResult.Compatible
            is SurqlType.Int, is SurqlType.Float, is SurqlType.Decimal, is SurqlType.Number,
            is SurqlType.Bool, is SurqlType.Uuid, is SurqlType.Datetime, is SurqlType.Duration ->
                TypeCheckResult.CompatibleWithConversion("Implicit conversion from $actual to string")
            else -> TypeCheckResult.Incompatible(SurqlType.Str, actual, "Cannot convert '$actual' to string")
        }
    }
    
    private fun checkGeometryAssignment(expected: SurqlType.Geometry, actual: SurqlType): TypeCheckResult {
        if (actual !is SurqlType.Geometry) {
            return TypeCheckResult.Incompatible(expected, actual, "Expected geometry, got '$actual'")
        }
        
        // If expected specifies a specific geometry kind
        if (expected.kind != null && actual.kind != null && expected.kind != actual.kind) {
            return TypeCheckResult.Incompatible(
                expected, actual,
                "Expected geometry<${expected.kind}>, got geometry<${actual.kind}>"
            )
        }
        
        return TypeCheckResult.Compatible
    }
    
    private fun checkDefaultAssignment(expected: SurqlType, actual: SurqlType): TypeCheckResult {
        return if (expected == actual) {
            TypeCheckResult.Compatible
        } else {
            TypeCheckResult.Incompatible(expected, actual, "Type '$actual' is not assignable to type '$expected'")
        }
    }
    
    // ==================== Operator Type Checking ====================
    
    /**
     * Checks if a binary operator can be applied to the given operand types.
     */
    fun checkBinaryOperator(operator: String, leftType: SurqlType, rightType: SurqlType): TypeCheckResult {
        val op = operator.uppercase()
        
        return when (op) {
            // Arithmetic operators
            "+", "-", "*", "/", "%", "^", "**" -> checkArithmeticOperator(op, leftType, rightType)
            
            // Comparison operators (always valid, return bool)
            "==", "!=", "<", ">", "<=", ">=", "=", "IS", "IS NOT" -> TypeCheckResult.Compatible
            
            // Logical operators
            "&&", "||", "AND", "OR" -> checkLogicalOperator(leftType, rightType)
            
            // Containment operators (left should be collection)
            "CONTAINS", "CONTAINSALL", "CONTAINSANY", "CONTAINSNONE", "CONTAINSNOT",
            "∋", "⊇", "⊃", "⊅", "∌" -> checkContainsOperator(leftType)
            
            // In operators (right should be collection)
            "IN", "NOT IN", "∈", "∉" -> checkInOperator(rightType)
            
            // String matching
            "LIKE", "NOT LIKE", "~", "!~", "?~", "*~" -> checkStringMatchOperator(leftType, rightType)
            
            else -> TypeCheckResult.Compatible // Unknown operators pass
        }
    }
    
    private fun checkArithmeticOperator(op: String, leftType: SurqlType, rightType: SurqlType): TypeCheckResult {
        // String concatenation with +
        if (op == "+" && (leftType == SurqlType.Str || rightType == SurqlType.Str)) {
            return TypeCheckResult.Compatible
        }
        
        // Duration arithmetic
        if (leftType == SurqlType.Duration || rightType == SurqlType.Duration) {
            return when (op) {
                "+", "-" -> TypeCheckResult.Compatible
                "*", "/" -> {
                    if (SurqlType.isNumeric(rightType)) {
                        TypeCheckResult.Compatible
                    } else {
                        TypeCheckResult.Incompatible(
                            SurqlType.Number, rightType,
                            "Duration $op requires numeric right operand"
                        )
                    }
                }
                else -> TypeCheckResult.Incompatible(
                    SurqlType.Duration, leftType,
                    "Operator '$op' is not supported for duration"
                )
            }
        }
        
        // Datetime arithmetic
        if (leftType == SurqlType.Datetime) {
            return when {
                op == "+" && rightType == SurqlType.Duration -> TypeCheckResult.Compatible
                op == "-" && rightType == SurqlType.Duration -> TypeCheckResult.Compatible
                op == "-" && rightType == SurqlType.Datetime -> TypeCheckResult.Compatible
                else -> TypeCheckResult.Incompatible(
                    SurqlType.Duration, rightType,
                    "Datetime arithmetic requires duration operand"
                )
            }
        }
        
        // Array/set operations with +/-
        if (leftType is SurqlType.Array || leftType is SurqlType.Set) {
            return when (op) {
                "+", "-" -> TypeCheckResult.Compatible
                else -> TypeCheckResult.Incompatible(
                    leftType, rightType,
                    "Operator '$op' is not supported for ${leftType.displayName()}"
                )
            }
        }
        
        // Numeric operations
        if (SurqlType.isNumeric(leftType) && SurqlType.isNumeric(rightType)) {
            return TypeCheckResult.Compatible
        }
        
        return TypeCheckResult.Incompatible(
            SurqlType.Number, leftType,
            "Operator '$op' requires numeric operands"
        )
    }
    
    private fun checkLogicalOperator(leftType: SurqlType, rightType: SurqlType): TypeCheckResult {
        // Logical operators work on any truthy/falsy values in SurrealDB
        // But we can warn if not boolean
        if (leftType != SurqlType.Bool && leftType != SurqlType.Any && leftType != SurqlType.Unknown) {
            return TypeCheckResult.CompatibleWithConversion(
                "Left operand of type '$leftType' will be converted to bool"
            )
        }
        if (rightType != SurqlType.Bool && rightType != SurqlType.Any && rightType != SurqlType.Unknown) {
            return TypeCheckResult.CompatibleWithConversion(
                "Right operand of type '$rightType' will be converted to bool"
            )
        }
        return TypeCheckResult.Compatible
    }
    
    private fun checkContainsOperator(leftType: SurqlType): TypeCheckResult {
        return when (leftType) {
            is SurqlType.Array, is SurqlType.Set, is SurqlType.Str,
            is SurqlType.Any, is SurqlType.Unknown -> TypeCheckResult.Compatible
            else -> TypeCheckResult.Incompatible(
                SurqlType.Array(), leftType,
                "CONTAINS operator requires array, set, or string on left side"
            )
        }
    }
    
    private fun checkInOperator(rightType: SurqlType): TypeCheckResult {
        return when (rightType) {
            is SurqlType.Array, is SurqlType.Set, is SurqlType.Object,
            is SurqlType.Any, is SurqlType.Unknown -> TypeCheckResult.Compatible
            else -> TypeCheckResult.Incompatible(
                SurqlType.Array(), rightType,
                "IN operator requires array, set, or object on right side"
            )
        }
    }
    
    private fun checkStringMatchOperator(leftType: SurqlType, rightType: SurqlType): TypeCheckResult {
        if (leftType != SurqlType.Str && leftType != SurqlType.Any && leftType != SurqlType.Unknown) {
            return TypeCheckResult.Incompatible(
                SurqlType.Str, leftType,
                "String matching operators require string on left side"
            )
        }
        if (rightType != SurqlType.Str && rightType != SurqlType.Any && rightType != SurqlType.Unknown) {
            return TypeCheckResult.Incompatible(
                SurqlType.Str, rightType,
                "String matching operators require string pattern on right side"
            )
        }
        return TypeCheckResult.Compatible
    }
    
    // ==================== Function Argument Checking ====================
    
    /**
     * Checks if function arguments match the expected parameter types.
     */
    fun checkFunctionArguments(
        signature: SurqlBuiltinFunctions.FunctionSignature,
        argumentTypes: List<SurqlType>
    ): TypeCheckResult {
        val params = signature.parameters
        
        // Check argument count
        if (argumentTypes.size < signature.minArgs) {
            return TypeCheckResult.Incompatible(
                SurqlType.Unknown, SurqlType.Unknown,
                "Function '${signature.name}' requires at least ${signature.minArgs} arguments, got ${argumentTypes.size}"
            )
        }
        
        if (!signature.isVariadic && argumentTypes.size > signature.maxArgs) {
            return TypeCheckResult.Incompatible(
                SurqlType.Unknown, SurqlType.Unknown,
                "Function '${signature.name}' accepts at most ${signature.maxArgs} arguments, got ${argumentTypes.size}"
            )
        }
        
        // Check each argument type
        for ((index, argType) in argumentTypes.withIndex()) {
            val paramIndex = if (signature.isVariadic && index >= params.size) params.lastIndex else index
            if (paramIndex < params.size) {
                val param = params[paramIndex]
                val result = isAssignable(param.type, argType)
                if (!result.isCompatible) {
                    return TypeCheckResult.Incompatible(
                        param.type, argType,
                        "Argument ${index + 1} (${param.name}): expected '${param.type}', got '$argType'"
                    )
                }
            }
        }
        
        return TypeCheckResult.Compatible
    }
}
