package org.surrealdb.surql.psi

import com.intellij.psi.tree.IElementType
import org.surrealdb.surql.lang.SurqlLanguage

/**
 * Element types for SurrealQL PSI tree structure.
 * 
 * These types represent semantic constructs in the language,
 * as opposed to token types which are lexical units.
 */
object SurqlElementTypes {
    // Root and file elements
    @JvmField val FILE = IElementType("SURQL_FILE", SurqlLanguage)
    
    // Statement types
    @JvmField val SELECT_STATEMENT = IElementType("SELECT_STATEMENT", SurqlLanguage)
    @JvmField val CREATE_STATEMENT = IElementType("CREATE_STATEMENT", SurqlLanguage)
    @JvmField val UPDATE_STATEMENT = IElementType("UPDATE_STATEMENT", SurqlLanguage)
    @JvmField val DELETE_STATEMENT = IElementType("DELETE_STATEMENT", SurqlLanguage)
    @JvmField val INSERT_STATEMENT = IElementType("INSERT_STATEMENT", SurqlLanguage)
    @JvmField val UPSERT_STATEMENT = IElementType("UPSERT_STATEMENT", SurqlLanguage)
    @JvmField val RELATE_STATEMENT = IElementType("RELATE_STATEMENT", SurqlLanguage)
    @JvmField val DEFINE_STATEMENT = IElementType("DEFINE_STATEMENT", SurqlLanguage)
    @JvmField val REMOVE_STATEMENT = IElementType("REMOVE_STATEMENT", SurqlLanguage)
    @JvmField val LET_STATEMENT = IElementType("LET_STATEMENT", SurqlLanguage)
    @JvmField val RETURN_STATEMENT = IElementType("RETURN_STATEMENT", SurqlLanguage)
    @JvmField val IF_STATEMENT = IElementType("IF_STATEMENT", SurqlLanguage)
    @JvmField val FOR_STATEMENT = IElementType("FOR_STATEMENT", SurqlLanguage)
    @JvmField val TRANSACTION_STATEMENT = IElementType("TRANSACTION_STATEMENT", SurqlLanguage)
    @JvmField val USE_STATEMENT = IElementType("USE_STATEMENT", SurqlLanguage)
    @JvmField val INFO_STATEMENT = IElementType("INFO_STATEMENT", SurqlLanguage)
    @JvmField val LIVE_STATEMENT = IElementType("LIVE_STATEMENT", SurqlLanguage)
    
    // Expression types
    @JvmField val EXPRESSION = IElementType("EXPRESSION", SurqlLanguage)
    @JvmField val BINARY_EXPRESSION = IElementType("BINARY_EXPRESSION", SurqlLanguage)
    @JvmField val UNARY_EXPRESSION = IElementType("UNARY_EXPRESSION", SurqlLanguage)
    @JvmField val FUNCTION_CALL = IElementType("FUNCTION_CALL", SurqlLanguage)
    @JvmField val SUBQUERY = IElementType("SUBQUERY", SurqlLanguage)
    @JvmField val OBJECT_LITERAL = IElementType("OBJECT_LITERAL", SurqlLanguage)
    @JvmField val ARRAY_LITERAL = IElementType("ARRAY_LITERAL", SurqlLanguage)
    @JvmField val FIELD_ACCESS = IElementType("FIELD_ACCESS", SurqlLanguage)
    @JvmField val INDEX_ACCESS = IElementType("INDEX_ACCESS", SurqlLanguage)
    
    // Identifier and reference types
    @JvmField val TABLE_NAME = IElementType("TABLE_NAME", SurqlLanguage)
    @JvmField val FIELD_NAME = IElementType("FIELD_NAME", SurqlLanguage)
    @JvmField val RECORD_ID_EXPR = IElementType("RECORD_ID_EXPR", SurqlLanguage)
    @JvmField val PARAMETER_REF = IElementType("PARAMETER_REF", SurqlLanguage)
    @JvmField val FUNCTION_NAME = IElementType("FUNCTION_NAME", SurqlLanguage)
    @JvmField val NAMESPACE_NAME = IElementType("NAMESPACE_NAME", SurqlLanguage)
    @JvmField val DATABASE_NAME = IElementType("DATABASE_NAME", SurqlLanguage)
    @JvmField val INDEX_NAME = IElementType("INDEX_NAME", SurqlLanguage)
    @JvmField val EVENT_NAME = IElementType("EVENT_NAME", SurqlLanguage)
    @JvmField val ANALYZER_NAME = IElementType("ANALYZER_NAME", SurqlLanguage)
    @JvmField val ACCESS_NAME = IElementType("ACCESS_NAME", SurqlLanguage)
    
    // Clause types
    @JvmField val WHERE_CLAUSE = IElementType("WHERE_CLAUSE", SurqlLanguage)
    @JvmField val ORDER_CLAUSE = IElementType("ORDER_CLAUSE", SurqlLanguage)
    @JvmField val LIMIT_CLAUSE = IElementType("LIMIT_CLAUSE", SurqlLanguage)
    @JvmField val GROUP_CLAUSE = IElementType("GROUP_CLAUSE", SurqlLanguage)
    @JvmField val SPLIT_CLAUSE = IElementType("SPLIT_CLAUSE", SurqlLanguage)
    @JvmField val FETCH_CLAUSE = IElementType("FETCH_CLAUSE", SurqlLanguage)
    @JvmField val TIMEOUT_CLAUSE = IElementType("TIMEOUT_CLAUSE", SurqlLanguage)
    @JvmField val SET_CLAUSE = IElementType("SET_CLAUSE", SurqlLanguage)
    @JvmField val CONTENT_CLAUSE = IElementType("CONTENT_CLAUSE", SurqlLanguage)
    @JvmField val MERGE_CLAUSE = IElementType("MERGE_CLAUSE", SurqlLanguage)
    @JvmField val PERMISSIONS_CLAUSE = IElementType("PERMISSIONS_CLAUSE", SurqlLanguage)
    
    // Definition types
    @JvmField val TABLE_DEFINITION = IElementType("TABLE_DEFINITION", SurqlLanguage)
    @JvmField val FIELD_DEFINITION = IElementType("FIELD_DEFINITION", SurqlLanguage)
    @JvmField val INDEX_DEFINITION = IElementType("INDEX_DEFINITION", SurqlLanguage)
    @JvmField val EVENT_DEFINITION = IElementType("EVENT_DEFINITION", SurqlLanguage)
    @JvmField val FUNCTION_DEFINITION = IElementType("FUNCTION_DEFINITION", SurqlLanguage)
    @JvmField val PARAM_DEFINITION = IElementType("PARAM_DEFINITION", SurqlLanguage)
    @JvmField val ANALYZER_DEFINITION = IElementType("ANALYZER_DEFINITION", SurqlLanguage)
    @JvmField val ACCESS_DEFINITION = IElementType("ACCESS_DEFINITION", SurqlLanguage)
    @JvmField val USER_DEFINITION = IElementType("USER_DEFINITION", SurqlLanguage)
    
    // Type expressions
    @JvmField val TYPE_EXPRESSION = IElementType("TYPE_EXPRESSION", SurqlLanguage)
    @JvmField val GENERIC_TYPE = IElementType("GENERIC_TYPE", SurqlLanguage)
    @JvmField val UNION_TYPE = IElementType("UNION_TYPE", SurqlLanguage)
    
    // Literal types
    @JvmField val STRING_LITERAL = IElementType("STRING_LITERAL", SurqlLanguage)
    @JvmField val NUMBER_LITERAL = IElementType("NUMBER_LITERAL", SurqlLanguage)
    @JvmField val BOOLEAN_LITERAL = IElementType("BOOLEAN_LITERAL", SurqlLanguage)
    @JvmField val NULL_LITERAL = IElementType("NULL_LITERAL", SurqlLanguage)
    @JvmField val DATETIME_LITERAL = IElementType("DATETIME_LITERAL", SurqlLanguage)
    @JvmField val DURATION_LITERAL = IElementType("DURATION_LITERAL", SurqlLanguage)
    @JvmField val UUID_LITERAL = IElementType("UUID_LITERAL", SurqlLanguage)
    
    // Assignment
    @JvmField val ASSIGNMENT = IElementType("ASSIGNMENT", SurqlLanguage)
    @JvmField val OBJECT_ENTRY = IElementType("OBJECT_ENTRY", SurqlLanguage)
    
    // Arguments
    @JvmField val ARGUMENT_LIST = IElementType("ARGUMENT_LIST", SurqlLanguage)
    @JvmField val PARAMETER_LIST = IElementType("PARAMETER_LIST", SurqlLanguage)
    
    // Graph traversal
    @JvmField val GRAPH_PATH = IElementType("GRAPH_PATH", SurqlLanguage)
    @JvmField val EDGE_TYPE = IElementType("EDGE_TYPE", SurqlLanguage)
}
