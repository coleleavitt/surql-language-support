package org.surrealdb.surql.types

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.*

/**
 * Manages schema context extracted from SurrealQL files.
 * 
 * This class extracts and caches information about:
 * - Table definitions (DEFINE TABLE)
 * - Field definitions (DEFINE FIELD) with their types
 * - User-defined functions (DEFINE FUNCTION)
 * - Parameters (DEFINE PARAM and LET statements)
 * - Access definitions (DEFINE ACCESS)
 * 
 * The context is cached and invalidated when the file changes.
 */
class SurqlSchemaContext private constructor(private val file: PsiFile) {
    
    /**
     * Represents a table definition.
     */
    data class TableSchema(
        val name: String,
        val element: PsiElement,
        val schemaMode: SchemaMode = SchemaMode.SCHEMALESS,
        val fields: MutableMap<String, FieldSchema> = mutableMapOf(),
        val indexes: MutableMap<String, IndexSchema> = mutableMapOf(),
        val events: MutableMap<String, EventSchema> = mutableMapOf()
    )
    
    /**
     * Schema mode for tables.
     */
    enum class SchemaMode {
        SCHEMALESS,   // Default, flexible schema
        SCHEMAFULL,   // Strict schema, only defined fields allowed
        SCHEMAFUL     // Alias for SCHEMAFULL
    }
    
    /**
     * Represents a field definition.
     */
    data class FieldSchema(
        val name: String,
        val fullPath: String, // e.g., "user.address.city"
        val element: PsiElement,
        val type: SurqlType,
        val isFlexible: Boolean = false,
        val isReadonly: Boolean = false,
        val defaultValue: PsiElement? = null,
        val assertExpr: PsiElement? = null,
        val valueExpr: PsiElement? = null
    )
    
    /**
     * Represents an index definition.
     */
    data class IndexSchema(
        val name: String,
        val element: PsiElement,
        val fields: List<String>,
        val isUnique: Boolean = false,
        val indexType: IndexType = IndexType.STANDARD
    )
    
    enum class IndexType {
        STANDARD,
        UNIQUE,
        SEARCH,
        MTREE,
        HNSW
    }
    
    /**
     * Represents an event definition.
     */
    data class EventSchema(
        val name: String,
        val element: PsiElement,
        val whenExpr: PsiElement?,
        val thenExpr: PsiElement?
    )
    
    /**
     * Represents a user-defined function.
     */
    data class FunctionSchema(
        val name: String,
        val element: PsiElement,
        val parameters: List<ParameterSchema>,
        val returnType: SurqlType?,
        val body: PsiElement?
    )
    
    /**
     * Represents a parameter (from LET or DEFINE PARAM).
     */
    data class ParameterSchema(
        val name: String,
        val element: PsiElement,
        val type: SurqlType?,
        val valueExpr: PsiElement?,
        val scope: ParameterScope
    )
    
    enum class ParameterScope {
        GLOBAL,   // DEFINE PARAM - available everywhere
        LOCAL     // LET - scoped to containing statement/block
    }
    
    // Schema storage
    private val tables = mutableMapOf<String, TableSchema>()
    private val functions = mutableMapOf<String, FunctionSchema>()
    private val globalParams = mutableMapOf<String, ParameterSchema>()
    
    // Local parameters by containing statement
    private val localParams = mutableMapOf<PsiElement, MutableMap<String, ParameterSchema>>()
    
    init {
        buildSchema()
    }
    
    /**
     * Builds the schema by traversing the PSI tree.
     */
    private fun buildSchema() {
        file.accept(object : com.intellij.psi.PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                when {
                    element.node.elementType == SurqlElementTypes.DEFINE_STATEMENT -> {
                        processDefineStatement(element)
                    }
                    element.node.elementType == SurqlElementTypes.LET_STATEMENT -> {
                        processLetStatement(element)
                    }
                }
                super.visitElement(element)
            }
        })
    }
    
    private fun processDefineStatement(element: PsiElement) {
        val children = element.children
        if (children.isEmpty()) return
        
        // Find the define keyword type (TABLE, FIELD, INDEX, etc.)
        val defineType = findDefineType(element)
        
        when (defineType?.uppercase()) {
            "TABLE" -> processDefineTable(element)
            "FIELD" -> processDefineField(element)
            "INDEX" -> processDefineIndex(element)
            "EVENT" -> processDefineEvent(element)
            "FUNCTION" -> processDefineFunction(element)
            "PARAM" -> processDefineParam(element)
        }
    }
    
    private fun findDefineType(element: PsiElement): String? {
        val text = element.text.uppercase()
        val defineKeywords = listOf("TABLE", "FIELD", "INDEX", "EVENT", "FUNCTION", "PARAM", "ACCESS", "ANALYZER", "USER", "NAMESPACE", "DATABASE")
        for (keyword in defineKeywords) {
            if (text.contains("DEFINE $keyword") || text.contains("DEFINE OVERWRITE $keyword")) {
                return keyword
            }
        }
        return null
    }
    
    private fun processDefineTable(element: PsiElement) {
        val text = element.text
        
        // Extract table name (after TABLE keyword)
        val tableNameMatch = Regex("""DEFINE\s+(?:OVERWRITE\s+)?TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)""", RegexOption.IGNORE_CASE)
            .find(text)
        val tableName = tableNameMatch?.groupValues?.get(1) ?: return
        
        // Determine schema mode
        val schemaMode = when {
            text.uppercase().contains("SCHEMAFULL") -> SchemaMode.SCHEMAFULL
            text.uppercase().contains("SCHEMAFUL") -> SchemaMode.SCHEMAFUL
            text.uppercase().contains("SCHEMALESS") -> SchemaMode.SCHEMALESS
            else -> SchemaMode.SCHEMALESS
        }
        
        tables[tableName] = TableSchema(
            name = tableName,
            element = element,
            schemaMode = schemaMode
        )
    }
    
    private fun processDefineField(element: PsiElement) {
        val text = element.text
        
        // Extract field path and table name
        // Pattern: DEFINE FIELD [name...] ON [TABLE] tableName
        val fieldMatch = Regex(
            """DEFINE\s+(?:OVERWRITE\s+)?FIELD\s+(?:IF\s+NOT\s+EXISTS\s+)?([^\s]+(?:\.[^\s]+)*)\s+ON\s+(?:TABLE\s+)?(\w+)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        
        val fieldPath = fieldMatch?.groupValues?.get(1) ?: return
        val tableName = fieldMatch.groupValues[2]
        
        // Extract type if present
        val type = extractFieldType(text)
        
        // Check for FLEXIBLE, READONLY
        val isFlexible = text.uppercase().contains("FLEXIBLE")
        val isReadonly = text.uppercase().contains("READONLY")
        
        // Get or create table schema
        val table = tables.getOrPut(tableName) {
            TableSchema(name = tableName, element = element)
        }
        
        // Add field to table
        table.fields[fieldPath] = FieldSchema(
            name = fieldPath.substringAfterLast('.'),
            fullPath = fieldPath,
            element = element,
            type = type,
            isFlexible = isFlexible,
            isReadonly = isReadonly
        )
    }
    
    private fun extractFieldType(defineText: String): SurqlType {
        // Look for TYPE clause
        val typeMatch = Regex("""TYPE\s+(.+?)(?:\s+(?:DEFAULT|VALUE|ASSERT|READONLY|FLEXIBLE|PERMISSIONS)|$)""", RegexOption.IGNORE_CASE)
            .find(defineText)
        
        val typeStr = typeMatch?.groupValues?.get(1)?.trim() ?: return SurqlType.Any
        return parseTypeString(typeStr)
    }
    
    /**
     * Parses a type string into a SurqlType.
     */
    fun parseTypeString(typeStr: String): SurqlType {
        val trimmed = typeStr.trim()
        
        // Handle union types (e.g., "string | int | null")
        if (trimmed.contains('|')) {
            val parts = trimmed.split('|').map { it.trim() }
            val types = parts.map { parseTypeString(it) }.toSet()
            return SurqlType.Union.of(*types.toTypedArray())
        }
        
        // Handle generic types (e.g., "array<string>", "option<int>")
        val genericMatch = Regex("""(\w+)<(.+)>""").find(trimmed)
        if (genericMatch != null) {
            val baseName = genericMatch.groupValues[1].lowercase()
            val innerStr = genericMatch.groupValues[2]
            
            return when (baseName) {
                "array" -> {
                    // Handle array<type, size>
                    val parts = splitGenericArgs(innerStr)
                    val elemType = parseTypeString(parts[0])
                    val maxSize = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    SurqlType.Array(elemType, maxSize)
                }
                "set" -> SurqlType.Set(parseTypeString(innerStr))
                "option" -> SurqlType.Option(parseTypeString(innerStr))
                "record" -> {
                    val tables = splitGenericArgs(innerStr).map { it.trim() }.toSet()
                    SurqlType.Record(tables)
                }
                "geometry" -> {
                    val kind = GeometryKind.fromName(innerStr.trim())
                    SurqlType.Geometry(kind)
                }
                "future" -> SurqlType.Future(parseTypeString(innerStr))
                "either" -> {
                    val parts = splitGenericArgs(innerStr)
                    if (parts.size == 2) {
                        SurqlType.Either(parseTypeString(parts[0]), parseTypeString(parts[1]))
                    } else {
                        SurqlType.Unknown
                    }
                }
                "range" -> SurqlType.Range(parseTypeString(innerStr))
                "literal" -> {
                    // Parse literal values
                    val values = innerStr.split('|').map { it.trim() }.map { parseLiteralValue(it) }.toSet()
                    SurqlType.Literal(values)
                }
                else -> SurqlType.Unknown
            }
        }
        
        // Handle simple types
        return SurqlType.fromName(trimmed)
    }
    
    private fun splitGenericArgs(args: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        
        for (char in args) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }
                '>' -> {
                    depth--
                    current.append(char)
                }
                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        
        return result
    }
    
    private fun parseLiteralValue(str: String): Any {
        val trimmed = str.trim()
        return when {
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> trimmed.substring(1, trimmed.length - 1)
            trimmed.startsWith("'") && trimmed.endsWith("'") -> trimmed.substring(1, trimmed.length - 1)
            trimmed == "true" -> true
            trimmed == "false" -> false
            trimmed.toIntOrNull() != null -> trimmed.toInt()
            trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
            else -> trimmed
        }
    }
    
    private fun processDefineIndex(element: PsiElement) {
        val text = element.text
        
        // Extract index name and table
        val indexMatch = Regex(
            """DEFINE\s+(?:OVERWRITE\s+)?INDEX\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)\s+ON\s+(?:TABLE\s+)?(\w+)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        
        val indexName = indexMatch?.groupValues?.get(1) ?: return
        val tableName = indexMatch.groupValues[2]
        
        // Extract fields
        val fieldsMatch = Regex("""FIELDS?\s+([^,\s]+(?:\s*,\s*[^,\s]+)*)""", RegexOption.IGNORE_CASE).find(text)
        val fields = fieldsMatch?.groupValues?.get(1)?.split(',')?.map { it.trim() } ?: emptyList()
        
        // Determine index type
        val indexType = when {
            text.uppercase().contains("UNIQUE") -> IndexType.UNIQUE
            text.uppercase().contains("SEARCH ANALYZER") -> IndexType.SEARCH
            text.uppercase().contains("MTREE") -> IndexType.MTREE
            text.uppercase().contains("HNSW") -> IndexType.HNSW
            else -> IndexType.STANDARD
        }
        
        val table = tables.getOrPut(tableName) {
            TableSchema(name = tableName, element = element)
        }
        
        table.indexes[indexName] = IndexSchema(
            name = indexName,
            element = element,
            fields = fields,
            isUnique = indexType == IndexType.UNIQUE,
            indexType = indexType
        )
    }
    
    private fun processDefineEvent(element: PsiElement) {
        val text = element.text
        
        val eventMatch = Regex(
            """DEFINE\s+(?:OVERWRITE\s+)?EVENT\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)\s+ON\s+(?:TABLE\s+)?(\w+)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        
        val eventName = eventMatch?.groupValues?.get(1) ?: return
        val tableName = eventMatch.groupValues[2]
        
        val table = tables.getOrPut(tableName) {
            TableSchema(name = tableName, element = element)
        }
        
        table.events[eventName] = EventSchema(
            name = eventName,
            element = element,
            whenExpr = null, // Would need deeper PSI analysis
            thenExpr = null
        )
    }
    
    private fun processDefineFunction(element: PsiElement) {
        val text = element.text
        
        // Extract function name
        val funcMatch = Regex(
            """DEFINE\s+(?:OVERWRITE\s+)?FUNCTION\s+(?:IF\s+NOT\s+EXISTS\s+)?fn::(\w+(?:::\w+)*)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        
        val funcName = funcMatch?.groupValues?.get(1) ?: return
        val fullName = "fn::$funcName"
        
        // Extract parameters
        val paramsMatch = Regex("""\(([^)]*)\)""").find(text)
        val paramsStr = paramsMatch?.groupValues?.get(1) ?: ""
        val parameters = parseParameters(paramsStr, element)
        
        // Extract return type if present
        val returnType = extractReturnType(text)
        
        functions[fullName] = FunctionSchema(
            name = fullName,
            element = element,
            parameters = parameters,
            returnType = returnType,
            body = null
        )
    }
    
    private fun parseParameters(paramsStr: String, context: PsiElement): List<ParameterSchema> {
        if (paramsStr.isBlank()) return emptyList()
        
        return paramsStr.split(',').mapNotNull { paramStr ->
            val trimmed = paramStr.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            
            // Parse $name: type
            val paramMatch = Regex("""\$(\w+)(?:\s*:\s*(.+))?""").find(trimmed)
            val name = paramMatch?.groupValues?.get(1) ?: return@mapNotNull null
            val typeStr = paramMatch.groupValues.getOrNull(2)
            val type = typeStr?.let { parseTypeString(it) }
            
            ParameterSchema(
                name = name,
                element = context,
                type = type,
                valueExpr = null,
                scope = ParameterScope.LOCAL
            )
        }
    }
    
    private fun extractReturnType(text: String): SurqlType? {
        // Look for -> type at the end of signature before {
        val returnMatch = Regex("""\)\s*->\s*(\w+(?:<[^>]+>)?)\s*\{""").find(text)
        return returnMatch?.groupValues?.get(1)?.let { parseTypeString(it) }
    }
    
    private fun processDefineParam(element: PsiElement) {
        val text = element.text
        
        val paramMatch = Regex(
            """DEFINE\s+(?:OVERWRITE\s+)?PARAM\s+(?:IF\s+NOT\s+EXISTS\s+)?\$(\w+)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        
        val paramName = paramMatch?.groupValues?.get(1) ?: return
        
        // Extract type if present
        val typeMatch = Regex("""VALUE\s+<([^>]+)>""", RegexOption.IGNORE_CASE).find(text)
        val type = typeMatch?.groupValues?.get(1)?.let { parseTypeString(it) }
        
        globalParams[paramName] = ParameterSchema(
            name = paramName,
            element = element,
            type = type,
            valueExpr = null,
            scope = ParameterScope.GLOBAL
        )
    }
    
    private fun processLetStatement(element: PsiElement) {
        val text = element.text
        
        val letMatch = Regex("""LET\s+\$(\w+)""", RegexOption.IGNORE_CASE).find(text)
        val paramName = letMatch?.groupValues?.get(1) ?: return
        
        // Find the containing scope (parent block or statement)
        val scope = findContainingScope(element)
        
        val scopeParams = localParams.getOrPut(scope) { mutableMapOf() }
        scopeParams[paramName] = ParameterSchema(
            name = paramName,
            element = element,
            type = null, // Type will be inferred from value
            valueExpr = findLetValue(element),
            scope = ParameterScope.LOCAL
        )
    }
    
    private fun findContainingScope(element: PsiElement): PsiElement {
        var current = element.parent
        while (current != null) {
            val elementType = current.node.elementType
            if (elementType == SurqlElementTypes.FOR_STATEMENT ||
                elementType == SurqlElementTypes.IF_STATEMENT ||
                elementType == SurqlElementTypes.TRANSACTION_STATEMENT ||
                current is PsiFile) {
                return current
            }
            current = current.parent
        }
        return file
    }
    
    private fun findLetValue(letStatement: PsiElement): PsiElement? {
        // Would need deeper PSI analysis to extract the value expression
        return null
    }
    
    // ==================== Public Query API ====================
    
    /**
     * Gets a table schema by name.
     */
    fun getTable(name: String): TableSchema? = tables[name]
    
    /**
     * Gets all table names.
     */
    fun getTableNames(): Set<String> = tables.keys
    
    /**
     * Gets a field schema by table name and field path.
     */
    fun getField(tableName: String, fieldPath: String): FieldSchema? {
        return tables[tableName]?.fields?.get(fieldPath)
    }
    
    /**
     * Gets the type of a field.
     */
    fun getFieldType(tableName: String, fieldPath: String): SurqlType {
        return getField(tableName, fieldPath)?.type ?: SurqlType.Unknown
    }
    
    /**
     * Gets all fields for a table.
     */
    fun getTableFields(tableName: String): Map<String, FieldSchema> {
        return tables[tableName]?.fields ?: emptyMap()
    }
    
    /**
     * Gets a user-defined function by name.
     */
    fun getFunction(name: String): FunctionSchema? = functions[name]
    
    /**
     * Gets all user-defined function names.
     */
    fun getFunctionNames(): Set<String> = functions.keys
    
    /**
     * Gets a parameter type by name, checking both global and local scopes.
     */
    fun getParameterType(name: String, context: PsiElement): SurqlType {
        // Check global params first
        globalParams[name]?.type?.let { return it }
        
        // Check local params in enclosing scopes
        var current: PsiElement? = context
        while (current != null) {
            localParams[current]?.get(name)?.let { param ->
                param.type?.let { return it }
                // If no explicit type, would need to infer from value
            }
            current = current.parent
        }
        
        return SurqlType.Unknown
    }
    
    /**
     * Checks if a parameter is defined in scope.
     */
    fun isParameterDefined(name: String, context: PsiElement): Boolean {
        if (globalParams.containsKey(name)) return true
        
        var current: PsiElement? = context
        while (current != null) {
            if (localParams[current]?.containsKey(name) == true) return true
            current = current.parent
        }
        
        return false
    }
    
    /**
     * Gets all global parameters.
     */
    fun getGlobalParameters(): Map<String, ParameterSchema> = globalParams.toMap()
    
    /**
     * Gets local parameters visible from a given context.
     */
    fun getVisibleLocalParameters(context: PsiElement): Map<String, ParameterSchema> {
        val result = mutableMapOf<String, ParameterSchema>()
        
        var current: PsiElement? = context
        while (current != null) {
            localParams[current]?.let { result.putAll(it) }
            current = current.parent
        }
        
        return result
    }
    
    companion object {
        private val CONTEXT_KEY = Key.create<CachedValue<SurqlSchemaContext>>("SURQL_SCHEMA_CONTEXT")
        
        /**
         * Gets the schema context for a file, using caching.
         */
        fun forFile(file: PsiFile): SurqlSchemaContext {
            return CachedValuesManager.getCachedValue(file, CONTEXT_KEY) {
                CachedValueProvider.Result.create(
                    SurqlSchemaContext(file),
                    file
                )
            }
        }
        
        /**
         * Gets the schema context for an element's containing file.
         */
        fun forElement(element: PsiElement): SurqlSchemaContext {
            return forFile(element.containingFile)
        }
    }
}
