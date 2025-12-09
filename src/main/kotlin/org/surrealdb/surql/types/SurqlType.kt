package org.surrealdb.surql.types

/**
 * Represents all possible types in SurrealQL's type system.
 * 
 * SurrealDB supports a rich type system including:
 * - Primitive types (bool, int, float, decimal, string, etc.)
 * - Compound types (array, set, object, record)
 * - Special types (option, union, literal, future)
 * - Geometry types (point, line, polygon, etc.)
 */
sealed class SurqlType {
    
    /**
     * Returns a human-readable string representation of this type.
     */
    abstract fun displayName(): kotlin.String
    
    /**
     * Returns true if this type is nullable (can be null or none).
     */
    open fun isNullable(): kotlin.Boolean = false
    
    /**
     * Returns the unwrapped type if this is an Option, otherwise returns this.
     */
    open fun unwrapOption(): SurqlType = this
    
    // ==================== Primitive Types ====================
    
    /** The `any` type - matches any value */
    data object Any : SurqlType() {
        override fun displayName(): kotlin.String = "any"
        override fun toString(): kotlin.String = "any"
    }
    
    /** The `bool` type - true or false */
    data object Bool : SurqlType() {
        override fun displayName(): kotlin.String = "bool"
        override fun toString(): kotlin.String = "bool"
    }
    
    /** The `int` type - 64-bit signed integer */
    data object Int : SurqlType() {
        override fun displayName(): kotlin.String = "int"
        override fun toString(): kotlin.String = "int"
    }
    
    /** The `float` type - 64-bit floating point */
    data object Float : SurqlType() {
        override fun displayName(): kotlin.String = "float"
        override fun toString(): kotlin.String = "float"
    }
    
    /** The `decimal` type - arbitrary precision decimal */
    data object Decimal : SurqlType() {
        override fun displayName(): kotlin.String = "decimal"
        override fun toString(): kotlin.String = "decimal"
    }
    
    /** The `number` type - any numeric type (int, float, or decimal) */
    data object Number : SurqlType() {
        override fun displayName(): kotlin.String = "number"
        override fun toString(): kotlin.String = "number"
    }
    
    /** The `string` type - UTF-8 string */
    data object Str : SurqlType() {
        override fun displayName(): kotlin.String = "string"
        override fun toString(): kotlin.String = "string"
    }
    
    /** The `datetime` type - ISO 8601 datetime */
    data object Datetime : SurqlType() {
        override fun displayName(): kotlin.String = "datetime"
        override fun toString(): kotlin.String = "datetime"
    }
    
    /** The `duration` type - time duration */
    data object Duration : SurqlType() {
        override fun displayName(): kotlin.String = "duration"
        override fun toString(): kotlin.String = "duration"
    }
    
    /** The `bytes` type - binary data */
    data object Bytes : SurqlType() {
        override fun displayName(): kotlin.String = "bytes"
        override fun toString(): kotlin.String = "bytes"
    }
    
    /** The `uuid` type - UUID value */
    data object Uuid : SurqlType() {
        override fun displayName(): kotlin.String = "uuid"
        override fun toString(): kotlin.String = "uuid"
    }
    
    /** The `null` type - explicit null value */
    data object Null : SurqlType() {
        override fun displayName(): kotlin.String = "null"
        override fun isNullable(): kotlin.Boolean = true
        override fun toString(): kotlin.String = "null"
    }
    
    /** The `none` type - absence of a value */
    data object None : SurqlType() {
        override fun displayName(): kotlin.String = "none"
        override fun isNullable(): kotlin.Boolean = true
        override fun toString(): kotlin.String = "none"
    }
    
    // ==================== Compound Types ====================
    
    /**
     * Array type with optional element type and max size.
     * Examples: array, array<string>, array<int, 10>
     */
    data class Array(
        val elementType: SurqlType = Any,
        val maxSize: kotlin.Int? = null
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            val base = if (elementType == Any) "array" else "array<${elementType.displayName()}>"
            return if (maxSize != null) "$base[$maxSize]" else base
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Set type - unique elements only.
     * Examples: set, set<string>
     */
    data class Set(
        val elementType: SurqlType = Any
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            return if (elementType == Any) "set" else "set<${elementType.displayName()}>"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Object type with known field types.
     * Examples: object, { name: string, age: int }
     */
    data class Object(
        val fields: Map<kotlin.String, SurqlType> = emptyMap(),
        val isFlexible: kotlin.Boolean = true // FLEXIBLE vs SCHEMAFULL
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            if (fields.isEmpty()) return "object"
            val fieldStr = fields.entries.joinToString(", ") { (k, v) -> "$k: ${v.displayName()}" }
            return "{ $fieldStr }"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Record type - reference to records in specific tables.
     * Examples: record, record<user>, record<user | admin>
     */
    data class Record(
        val tables: kotlin.collections.Set<kotlin.String> = emptySet()
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            return when {
                tables.isEmpty() -> "record"
                tables.size == 1 -> "record<${tables.first()}>"
                else -> "record<${tables.joinToString(" | ")}>"
            }
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    // ==================== Special Types ====================
    
    /**
     * Option type - value that may be null/none.
     * Examples: option<string>, option<int>
     */
    data class Option(
        val innerType: SurqlType
    ) : SurqlType() {
        override fun displayName(): kotlin.String = "option<${innerType.displayName()}>"
        override fun isNullable(): kotlin.Boolean = true
        override fun unwrapOption(): SurqlType = innerType
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Union type - one of several possible types.
     * Examples: string | int, bool | null
     */
    data class Union(
        val types: kotlin.collections.Set<SurqlType>
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            return types.joinToString(" | ") { it.displayName() }
        }
        override fun isNullable(): kotlin.Boolean = types.any { it.isNullable() }
        override fun toString(): kotlin.String = displayName()
        
        companion object {
            /**
             * Creates a union type, flattening nested unions and removing duplicates.
             */
            fun of(vararg types: SurqlType): SurqlType {
                val flattened = mutableSetOf<SurqlType>()
                for (type in types) {
                    when (type) {
                        is Union -> flattened.addAll(type.types)
                        else -> flattened.add(type)
                    }
                }
                return when {
                    flattened.isEmpty() -> Unknown
                    flattened.size == 1 -> flattened.first()
                    flattened.contains(Any) -> Any
                    else -> Union(flattened)
                }
            }
        }
    }
    
    /**
     * Literal type - specific literal values.
     * Examples: literal<"active" | "inactive">, literal<1 | 2 | 3>
     */
    data class Literal(
        val values: kotlin.collections.Set<kotlin.Any>
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            val valuesStr = values.joinToString(" | ") { 
                when (it) {
                    is kotlin.String -> "\"$it\""
                    else -> it.toString()
                }
            }
            return "literal<$valuesStr>"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Either type - success or error result.
     * Examples: either<T, E>
     */
    data class Either(
        val successType: SurqlType,
        val errorType: SurqlType
    ) : SurqlType() {
        override fun displayName(): kotlin.String = "either<${successType.displayName()}, ${errorType.displayName()}>"
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Future type - deferred computation.
     * Examples: future, future<string>
     */
    data class Future(
        val resultType: SurqlType = Any
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            return if (resultType == Any) "future" else "future<${resultType.displayName()}>"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    /**
     * Range type - numeric range.
     * Examples: range<int>, range<float>
     */
    data class Range(
        val boundType: SurqlType = Int
    ) : SurqlType() {
        override fun displayName(): kotlin.String = "range<${boundType.displayName()}>"
        override fun toString(): kotlin.String = displayName()
    }
    
    // ==================== Geometry Types ====================
    
    /**
     * Geometry type with optional specific geometry kind.
     * Examples: geometry, geometry<point>, geometry<polygon>
     */
    data class Geometry(
        val kind: GeometryKind? = null
    ) : SurqlType() {
        override fun displayName(): kotlin.String {
            return if (kind == null) "geometry" else "geometry<${kind.displayName}>"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    // ==================== Function Type ====================
    
    /**
     * Function type - for user-defined functions.
     * Includes parameter types and return type.
     */
    data class Function(
        val parameters: List<Parameter>,
        val returnType: SurqlType
    ) : SurqlType() {
        data class Parameter(
            val name: kotlin.String,
            val type: SurqlType,
            val isOptional: kotlin.Boolean = false
        )
        
        override fun displayName(): kotlin.String {
            val paramsStr = parameters.joinToString(", ") { p ->
                val opt = if (p.isOptional) "?" else ""
                "${p.name}$opt: ${p.type.displayName()}"
            }
            return "fn($paramsStr) -> ${returnType.displayName()}"
        }
        override fun toString(): kotlin.String = displayName()
    }
    
    // ==================== Special/Error Types ====================
    
    /** Unknown type - when type cannot be determined */
    data object Unknown : SurqlType() {
        override fun displayName(): kotlin.String = "unknown"
        override fun toString(): kotlin.String = "unknown"
    }
    
    /** Error type - when there's a type error */
    data class Error(
        val message: kotlin.String
    ) : SurqlType() {
        override fun displayName(): kotlin.String = "error"
        override fun toString(): kotlin.String = "error: $message"
    }
    
    /** Never type - for code paths that never return (e.g., THROW) */
    data object Never : SurqlType() {
        override fun displayName(): kotlin.String = "never"
        override fun toString(): kotlin.String = "never"
    }
    
    // ==================== Companion Utilities ====================
    
    companion object {
        // Alias for the String type to avoid kotlin.String conflict
        val String: SurqlType get() = Str
        
        /**
         * Parse a type name string into a SurqlType.
         * This is a simple parser for basic type names.
         */
        fun fromName(name: kotlin.String): SurqlType {
            return when (name.lowercase().trim()) {
                "any" -> Any
                "bool", "boolean" -> Bool
                "int", "integer" -> Int
                "float" -> Float
                "decimal" -> Decimal
                "number" -> Number
                "string" -> Str
                "datetime" -> Datetime
                "duration" -> Duration
                "bytes" -> Bytes
                "uuid" -> Uuid
                "null" -> Null
                "none" -> None
                "array" -> Array()
                "set" -> Set()
                "object" -> Object()
                "record" -> Record()
                "geometry" -> Geometry()
                "future" -> Future()
                else -> Unknown
            }
        }
        
        /**
         * Returns true if the type is a numeric type.
         */
        fun isNumeric(type: SurqlType): kotlin.Boolean {
            return when (type) {
                is Int, is Float, is Decimal, is Number -> true
                is Union -> type.types.all { isNumeric(it) }
                is Option -> isNumeric(type.innerType)
                else -> false
            }
        }
        
        /**
         * Returns true if the type is a collection type (array or set).
         */
        fun isCollection(type: SurqlType): kotlin.Boolean {
            return when (type) {
                is Array, is Set -> true
                is Option -> isCollection(type.innerType)
                else -> false
            }
        }
        
        /**
         * Returns the element type of a collection, or null if not a collection.
         */
        fun elementTypeOf(type: SurqlType): SurqlType? {
            return when (type) {
                is Array -> type.elementType
                is Set -> type.elementType
                is Option -> elementTypeOf(type.innerType)
                else -> null
            }
        }
        
        /**
         * Returns the common supertype of two types, or Unknown if none exists.
         */
        fun commonType(a: SurqlType, b: SurqlType): SurqlType {
            if (a == b) return a
            if (a == Any || b == Any) return Any
            if (a == Unknown) return b
            if (b == Unknown) return a
            
            // Numeric promotion
            if (isNumeric(a) && isNumeric(b)) {
                return when {
                    a == Decimal || b == Decimal -> Decimal
                    a == Float || b == Float -> Float
                    a == Number || b == Number -> Number
                    else -> Int
                }
            }
            
            // Null handling
            if (a == Null || a == None) return Option(b)
            if (b == Null || b == None) return Option(a)
            
            // Default to union
            return Union.of(a, b)
        }
    }
}

/**
 * Geometry subtypes supported by SurrealDB.
 */
enum class GeometryKind(val displayName: kotlin.String) {
    Point("point"),
    Line("line"),
    Polygon("polygon"),
    MultiPoint("multipoint"),
    MultiLine("multiline"),
    MultiPolygon("multipolygon"),
    Collection("collection");
    
    companion object {
        fun fromName(name: kotlin.String): GeometryKind? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}
