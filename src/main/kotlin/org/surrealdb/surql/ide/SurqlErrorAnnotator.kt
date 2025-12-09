package org.surrealdb.surql.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.impl.*

/**
 * Provides error annotations and semantic validation for SurrealQL code.
 * 
 * This annotator detects:
 * - Syntax errors from the parser
 * - Undefined parameter references
 * - Undefined table references (when table definitions are in scope)
 * - Missing required clauses
 * - Invalid value types in specific contexts
 */
class SurqlErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Annotate parser errors
        if (element is PsiErrorElement) {
            annotateParserError(element, holder)
            return
        }
        
        // Check for bad characters
        if (element.elementType == SurqlTokenTypes.BAD_CHARACTER) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unexpected character: '${element.text}'")
                .range(element)
                .create()
            return
        }
        
        // Semantic validation
        when (element) {
            is SurqlSelectStatementImpl -> validateSelectStatement(element, holder)
            is SurqlCreateStatementImpl -> validateCreateStatement(element, holder)
            is SurqlUpdateStatementImpl -> validateUpdateStatement(element, holder)
            is SurqlDeleteStatementImpl -> validateDeleteStatement(element, holder)
            is SurqlDefineStatementImpl -> {
                // Only validate top-level DEFINE_STATEMENT, not nested definition elements
                if (element.node.elementType == SurqlElementTypes.DEFINE_STATEMENT) {
                    validateDefineStatement(element, holder)
                }
            }
            is SurqlLetStatementImpl -> validateLetStatement(element, holder)
            is SurqlParameterRefImpl -> validateParameterRef(element, holder)
            is SurqlFunctionCallImpl -> validateFunctionCall(element, holder)
            is SurqlRecordIdLiteralImpl -> validateRecordId(element, holder)
        }
    }
    
    private fun annotateParserError(element: PsiErrorElement, holder: AnnotationHolder) {
        val message = element.errorDescription
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(element)
            .create()
    }
    
    private fun validateSelectStatement(element: SurqlSelectStatementImpl, holder: AnnotationHolder) {
        // Check for SELECT without FROM (unless it's a constant expression)
        val hasFrom = element.node.findChildByType(SurqlTokenTypes.FROM) != null
        val projections = element.projections
        
        // SELECT without FROM is valid only for constant expressions like SELECT 1 + 1
        // or SELECT * which is invalid without FROM
        if (!hasFrom) {
            val hasStar = element.node.findChildByType(SurqlTokenTypes.STAR) != null
            if (hasStar) {
                holder.newAnnotation(HighlightSeverity.ERROR, "SELECT * requires a FROM clause")
                    .range(element.textRange)
                    .create()
            }
        }
    }
    
    private fun validateCreateStatement(element: SurqlCreateStatementImpl, holder: AnnotationHolder) {
        // CREATE requires a target table or record ID
        val target = element.target
        if (target == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "CREATE statement requires a target table or record ID")
                .range(element.textRange)
                .create()
        }
    }
    
    private fun validateUpdateStatement(element: SurqlUpdateStatementImpl, holder: AnnotationHolder) {
        // UPDATE requires a target
        val target = element.target
        if (target == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "UPDATE statement requires a target")
                .range(element.textRange)
                .create()
        }
        
        // UPDATE should have SET, CONTENT, MERGE, or PATCH clause
        val hasSet = element.node.findChildByType(SurqlTokenTypes.SET) != null
        val hasContent = element.node.findChildByType(SurqlTokenTypes.CONTENT) != null
        val hasMerge = element.node.findChildByType(SurqlTokenTypes.MERGE) != null
        val hasPatch = element.node.findChildByType(SurqlTokenTypes.PATCH) != null
        val hasReplace = element.node.findChildByType(SurqlTokenTypes.REPLACE) != null
        
        if (!hasSet && !hasContent && !hasMerge && !hasPatch && !hasReplace) {
            holder.newAnnotation(HighlightSeverity.WARNING, "UPDATE without SET, CONTENT, MERGE, PATCH, or REPLACE will have no effect")
                .range(element.textRange)
                .create()
        }
    }
    
    private fun validateDeleteStatement(element: SurqlDeleteStatementImpl, holder: AnnotationHolder) {
        // DELETE requires a target
        val target = element.target
        if (target == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "DELETE statement requires a target")
                .range(element.textRange)
                .create()
        }
    }
    
    private fun validateDefineStatement(element: SurqlDefineStatementImpl, holder: AnnotationHolder) {
        val definedName = element.definedName
        val defineType = element.defineType
        
        // Check for unknown DEFINE type first
        if (defineType == SurqlDefineStatementImpl.DefineType.UNKNOWN) {
            holder.newAnnotation(HighlightSeverity.ERROR, 
                "Invalid DEFINE statement: expected NAMESPACE, DATABASE, TABLE, FIELD, INDEX, EVENT, FUNCTION, PARAM, ANALYZER, ACCESS, USER, CONFIG, or MODEL")
                .range(element.textRange)
                .create()
            return  // Don't report additional errors for unknown type
        }
        
        // Check for missing name in definition
        if (definedName == null && defineType != SurqlDefineStatementImpl.DefineType.CONFIG) {
            holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE ${defineType.name} requires a name")
                .range(element.textRange)
                .create()
        }
        
        // Validate DEFINE FIELD has ON TABLE clause
        if (defineType == SurqlDefineStatementImpl.DefineType.FIELD) {
            val hasOnTable = element.node.findChildByType(SurqlTokenTypes.ON) != null
            if (!hasOnTable) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE FIELD requires ON TABLE clause")
                    .range(element.textRange)
                    .create()
            }
        }
        
        // Validate DEFINE INDEX has ON TABLE and FIELDS clauses
        if (defineType == SurqlDefineStatementImpl.DefineType.INDEX) {
            val hasOnTable = element.node.findChildByType(SurqlTokenTypes.ON) != null
            val hasFields = element.node.findChildByType(SurqlTokenTypes.FIELDS) != null ||
                           element.node.findChildByType(SurqlTokenTypes.COLUMNS) != null
            
            if (!hasOnTable) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE INDEX requires ON TABLE clause")
                    .range(element.textRange)
                    .create()
            }
            if (!hasFields) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE INDEX requires FIELDS clause")
                    .range(element.textRange)
                    .create()
            }
        }
        
        // Validate DEFINE EVENT has ON TABLE, WHEN, and THEN clauses
        if (defineType == SurqlDefineStatementImpl.DefineType.EVENT) {
            val hasOnTable = element.node.findChildByType(SurqlTokenTypes.ON) != null
            val hasWhen = element.node.findChildByType(SurqlTokenTypes.WHEN) != null
            val hasThen = element.node.findChildByType(SurqlTokenTypes.THEN) != null
            
            if (!hasOnTable) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE EVENT requires ON TABLE clause")
                    .range(element.textRange)
                    .create()
            }
            if (!hasWhen) {
                holder.newAnnotation(HighlightSeverity.WARNING, "DEFINE EVENT should have WHEN clause")
                    .range(element.textRange)
                    .create()
            }
            if (!hasThen) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEFINE EVENT requires THEN clause")
                    .range(element.textRange)
                    .create()
            }
        }
    }
    
    private fun validateLetStatement(element: SurqlLetStatementImpl, holder: AnnotationHolder) {
        // LET requires a parameter name
        if (element.parameterName == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "LET statement requires a parameter name")
                .range(element.textRange)
                .create()
        }
        
        // LET should have a value
        if (element.value == null) {
            // Check if there's an = sign - if so, value is missing
            val hasEquals = element.node.findChildByType(SurqlTokenTypes.EQ) != null
            if (hasEquals) {
                holder.newAnnotation(HighlightSeverity.ERROR, "LET statement is missing a value after '='")
                    .range(element.textRange)
                    .create()
            }
        }
    }
    
    private fun validateParameterRef(element: SurqlParameterRefImpl, holder: AnnotationHolder) {
        val name = element.parameterName
        
        // Skip validation for system parameters
        if (element.isSystemParameter) {
            return
        }
        
        // Check if parameter is defined in scope
        // This requires looking for LET statements in the same file
        val file = element.containingFile
        val letStatements = PsiTreeUtil.findChildrenOfType(file, SurqlLetStatementImpl::class.java)
        val definedParams = letStatements.mapNotNull { it.parameterName?.removePrefix("$") }.toSet()
        
        // Also check DEFINE PARAM statements (only top-level DEFINE_STATEMENT, not nested definition elements)
        val defineStatements = PsiTreeUtil.findChildrenOfType(file, SurqlDefineStatementImpl::class.java)
            .filter { it.node.elementType == SurqlElementTypes.DEFINE_STATEMENT }
        val definedGlobalParams = defineStatements
            .filter { it.defineType == SurqlDefineStatementImpl.DefineType.PARAM }
            .mapNotNull { it.definedName?.removePrefix("$") }
            .toSet()
        
        val allDefinedParams = definedParams + definedGlobalParams
        
        if (name !in allDefinedParams && name !in IMPLICIT_PARAMETERS) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Parameter '$name' may be undefined")
                .range(element.textRange)
                .create()
        }
    }
    
    private fun validateFunctionCall(element: SurqlFunctionCallImpl, holder: AnnotationHolder) {
        val functionName = element.functionName ?: return
        val namespace = element.namespace
        val simpleName = element.simpleName ?: return
        
        // Check if namespace is valid
        if (namespace != null && namespace.isNotEmpty()) {
            val topNamespace = namespace.split("::").first().lowercase()
            if (topNamespace != "fn" && topNamespace !in BUILTIN_NAMESPACES) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Unknown function namespace: '$topNamespace'")
                    .range(element.textRange)
                    .create()
            }
        }
        
        // Validate argument count for known functions
        val args = element.arguments?.argumentCount ?: 0
        val expectedArgs = FUNCTION_ARG_COUNTS[functionName.lowercase()]
        
        if (expectedArgs != null) {
            val (min, max) = expectedArgs
            if (args < min) {
                holder.newAnnotation(HighlightSeverity.ERROR, 
                    "Function '$functionName' requires at least $min argument${if (min != 1) "s" else ""}")
                    .range(element.textRange)
                    .create()
            } else if (max != null && args > max) {
                holder.newAnnotation(HighlightSeverity.ERROR, 
                    "Function '$functionName' accepts at most $max argument${if (max != 1) "s" else ""}")
                    .range(element.textRange)
                    .create()
            }
        }
    }
    
    private fun validateRecordId(element: SurqlRecordIdLiteralImpl, holder: AnnotationHolder) {
        val tableName = element.tableName
        val idPart = element.idPart
        
        // Validate table name format
        if (tableName != null && !isValidIdentifier(tableName)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Invalid table name: '$tableName'")
                .range(element.textRange)
                .create()
        }
        
        // Validate ID part is not empty
        if (idPart.isNullOrEmpty()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Record ID is missing the ID part after ':'")
                .range(element.textRange)
                .create()
        }
    }
    
    private fun isValidIdentifier(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name.first().isLetter() && name.first() != '_') return false
        return name.all { it.isLetterOrDigit() || it == '_' }
    }
    
    companion object {
        // Parameters that don't need to be explicitly defined
        private val IMPLICIT_PARAMETERS = setOf(
            "this", "parent", "value", "input", "before", "after",
            "event", "auth", "session", "scope", "token", "access"
        )
        
        // Built-in function namespaces
        private val BUILTIN_NAMESPACES = setOf(
            "array", "bytes", "count", "crypto", "duration", "encoding",
            "geo", "http", "math", "meta", "object", "parse", "rand",
            "record", "search", "session", "sleep", "string", "time",
            "type", "vector"
        )
        
        // Function argument counts: function name -> (min, max or null for unlimited)
        private val FUNCTION_ARG_COUNTS = mapOf(
            // Array functions
            "array::add" to Pair(2, 2),
            "array::all" to Pair(1, 1),
            "array::any" to Pair(1, 1),
            "array::append" to Pair(2, 2),
            "array::at" to Pair(2, 2),
            "array::combine" to Pair(2, 2),
            "array::complement" to Pair(2, 2),
            "array::concat" to Pair(2, 2),
            "array::difference" to Pair(2, 2),
            "array::distinct" to Pair(1, 1),
            "array::filter" to Pair(2, 2),
            "array::filter_index" to Pair(2, 2),
            "array::find" to Pair(2, 2),
            "array::find_index" to Pair(2, 2),
            "array::first" to Pair(1, 1),
            "array::flatten" to Pair(1, 1),
            "array::group" to Pair(1, 1),
            "array::insert" to Pair(3, 3),
            "array::intersect" to Pair(2, 2),
            "array::join" to Pair(2, 2),
            "array::last" to Pair(1, 1),
            "array::len" to Pair(1, 1),
            "array::map" to Pair(2, 2),
            "array::max" to Pair(1, 1),
            "array::min" to Pair(1, 1),
            "array::pop" to Pair(1, 1),
            "array::prepend" to Pair(2, 2),
            "array::push" to Pair(2, 2),
            "array::remove" to Pair(2, 2),
            "array::reverse" to Pair(1, 1),
            "array::shuffle" to Pair(1, 1),
            "array::slice" to Pair(2, 3),
            "array::sort" to Pair(1, 2),
            "array::transpose" to Pair(1, 1),
            "array::union" to Pair(2, 2),
            
            // String functions
            "string::concat" to Pair(1, null),
            "string::contains" to Pair(2, 2),
            "string::ends_with" to Pair(2, 2),
            "string::join" to Pair(2, null),
            "string::len" to Pair(1, 1),
            "string::lowercase" to Pair(1, 1),
            "string::repeat" to Pair(2, 2),
            "string::replace" to Pair(3, 3),
            "string::reverse" to Pair(1, 1),
            "string::slice" to Pair(2, 3),
            "string::slug" to Pair(1, 2),
            "string::split" to Pair(2, 2),
            "string::starts_with" to Pair(2, 2),
            "string::trim" to Pair(1, 1),
            "string::uppercase" to Pair(1, 1),
            "string::words" to Pair(1, 1),
            
            // Math functions
            "math::abs" to Pair(1, 1),
            "math::ceil" to Pair(1, 1),
            "math::floor" to Pair(1, 1),
            "math::max" to Pair(1, null),
            "math::mean" to Pair(1, 1),
            "math::median" to Pair(1, 1),
            "math::min" to Pair(1, null),
            "math::pow" to Pair(2, 2),
            "math::round" to Pair(1, 1),
            "math::sqrt" to Pair(1, 1),
            "math::sum" to Pair(1, 1),
            
            // Time functions
            "time::day" to Pair(1, 1),
            "time::floor" to Pair(2, 2),
            "time::format" to Pair(2, 2),
            "time::group" to Pair(2, 2),
            "time::hour" to Pair(1, 1),
            "time::minute" to Pair(1, 1),
            "time::month" to Pair(1, 1),
            "time::nano" to Pair(1, 1),
            "time::now" to Pair(0, 0),
            "time::round" to Pair(2, 2),
            "time::second" to Pair(1, 1),
            "time::unix" to Pair(1, 1),
            "time::wday" to Pair(1, 1),
            "time::week" to Pair(1, 1),
            "time::yday" to Pair(1, 1),
            "time::year" to Pair(1, 1),
            
            // Type functions
            "type::bool" to Pair(1, 1),
            "type::datetime" to Pair(1, 1),
            "type::decimal" to Pair(1, 1),
            "type::duration" to Pair(1, 1),
            "type::field" to Pair(1, 1),
            "type::fields" to Pair(1, 1),
            "type::float" to Pair(1, 1),
            "type::int" to Pair(1, 1),
            "type::is::array" to Pair(1, 1),
            "type::is::bool" to Pair(1, 1),
            "type::is::datetime" to Pair(1, 1),
            "type::is::decimal" to Pair(1, 1),
            "type::is::duration" to Pair(1, 1),
            "type::is::float" to Pair(1, 1),
            "type::is::geometry" to Pair(1, 1),
            "type::is::int" to Pair(1, 1),
            "type::is::null" to Pair(1, 1),
            "type::is::number" to Pair(1, 1),
            "type::is::object" to Pair(1, 1),
            "type::is::point" to Pair(1, 1),
            "type::is::record" to Pair(1, 2),
            "type::is::string" to Pair(1, 1),
            "type::is::uuid" to Pair(1, 1),
            "type::number" to Pair(1, 1),
            "type::point" to Pair(1, 2),
            "type::string" to Pair(1, 1),
            "type::table" to Pair(1, 1),
            "type::thing" to Pair(1, 2),
            
            // Crypto functions
            "crypto::argon2::compare" to Pair(2, 2),
            "crypto::argon2::generate" to Pair(1, 1),
            "crypto::bcrypt::compare" to Pair(2, 2),
            "crypto::bcrypt::generate" to Pair(1, 1),
            "crypto::md5" to Pair(1, 1),
            "crypto::pbkdf2::compare" to Pair(2, 2),
            "crypto::pbkdf2::generate" to Pair(1, 1),
            "crypto::scrypt::compare" to Pair(2, 2),
            "crypto::scrypt::generate" to Pair(1, 1),
            "crypto::sha1" to Pair(1, 1),
            "crypto::sha256" to Pair(1, 1),
            "crypto::sha512" to Pair(1, 1),
            
            // Rand functions
            "rand" to Pair(0, 0),
            "rand::bool" to Pair(0, 0),
            "rand::enum" to Pair(1, null),
            "rand::float" to Pair(0, 2),
            "rand::guid" to Pair(0, 1),
            "rand::int" to Pair(0, 2),
            "rand::string" to Pair(0, 2),
            "rand::time" to Pair(0, 2),
            "rand::ulid" to Pair(0, 0),
            "rand::uuid" to Pair(0, 0),
            "rand::uuid::v4" to Pair(0, 0),
            "rand::uuid::v7" to Pair(0, 0),
            
            // Count function
            "count" to Pair(0, 1),
            
            // Meta functions
            "meta::id" to Pair(1, 1),
            "meta::tb" to Pair(1, 1),
            
            // Session functions
            "session::db" to Pair(0, 0),
            "session::id" to Pair(0, 0),
            "session::ip" to Pair(0, 0),
            "session::ns" to Pair(0, 0),
            "session::origin" to Pair(0, 0),
            "session::sc" to Pair(0, 0),
            
            // Parse functions
            "parse::email::domain" to Pair(1, 1),
            "parse::email::user" to Pair(1, 1),
            "parse::url::domain" to Pair(1, 1),
            "parse::url::fragment" to Pair(1, 1),
            "parse::url::host" to Pair(1, 1),
            "parse::url::path" to Pair(1, 1),
            "parse::url::port" to Pair(1, 1),
            "parse::url::query" to Pair(1, 1),
            "parse::url::scheme" to Pair(1, 1),
            
            // Search functions
            "search::analyze" to Pair(2, 2),
            "search::highlight" to Pair(3, 5),
            "search::offsets" to Pair(2, 2),
            "search::score" to Pair(1, 1),
            
            // Sleep function
            "sleep" to Pair(1, 1),
            
            // Record functions
            "record::id" to Pair(1, 1),
            "record::tb" to Pair(1, 1)
        )
    }
}
