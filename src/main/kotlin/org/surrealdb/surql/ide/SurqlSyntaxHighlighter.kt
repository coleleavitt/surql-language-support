package org.surrealdb.surql.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.surrealdb.surql.lexer.SurqlLexer
import org.surrealdb.surql.lexer.SurqlTokenTypes

class SurqlSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = SurqlLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when {
            // Comments
            tokenType == SurqlTokenTypes.LINE_COMMENT ||
            tokenType == SurqlTokenTypes.BLOCK_COMMENT -> COMMENT_KEYS

            // Strings
            tokenType == SurqlTokenTypes.STRING -> STRING_KEYS
            tokenType == SurqlTokenTypes.DATETIME_STRING -> DATETIME_KEYS
            tokenType == SurqlTokenTypes.UUID_STRING -> UUID_KEYS
            tokenType == SurqlTokenTypes.RECORD_STRING -> RECORD_STRING_KEYS

            // Numbers
            tokenType == SurqlTokenTypes.NUMBER -> NUMBER_KEYS
            
            // Duration literals
            tokenType == SurqlTokenTypes.DURATION_LITERAL -> DURATION_KEYS

            // Parameters
            tokenType == SurqlTokenTypes.PARAMETER -> PARAMETER_KEYS

            // Record IDs
            tokenType == SurqlTokenTypes.RECORD_ID -> RECORD_ID_KEYS

            // Identifiers
            tokenType == SurqlTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS

            // Operators (symbolic)
            tokenType in SurqlTokenTypes.OPERATORS -> OPERATOR_KEYS

            // Brackets
            tokenType == SurqlTokenTypes.LPAREN || 
            tokenType == SurqlTokenTypes.RPAREN -> PARENTHESES_KEYS
            tokenType == SurqlTokenTypes.LBRACKET || 
            tokenType == SurqlTokenTypes.RBRACKET -> BRACKETS_KEYS
            tokenType == SurqlTokenTypes.LBRACE || 
            tokenType == SurqlTokenTypes.RBRACE -> BRACES_KEYS

            // Punctuation
            tokenType == SurqlTokenTypes.SEMICOLON -> SEMICOLON_KEYS
            tokenType == SurqlTokenTypes.COMMA -> COMMA_KEYS
            tokenType == SurqlTokenTypes.DOT || 
            tokenType == SurqlTokenTypes.DOTDOT || 
            tokenType == SurqlTokenTypes.DOTDOTDOT -> DOT_KEYS
            tokenType == SurqlTokenTypes.COLON || 
            tokenType == SurqlTokenTypes.COLONCOLON -> COLON_KEYS

            // ==================================================
            // SEMANTIC KEYWORD CATEGORIES - Different colors!
            // ==================================================
            
            // Statement keywords (SELECT, CREATE, UPDATE, etc.) - Bold primary keywords
            tokenType in SurqlTokenTypes.STATEMENT_KEYWORDS -> STATEMENT_KEYWORD_KEYS
            
            // Transaction keywords (BEGIN, COMMIT, CANCEL) - Same as statements
            tokenType in SurqlTokenTypes.TRANSACTION_KEYWORDS -> TRANSACTION_KEYWORD_KEYS
            
            // Control flow keywords (IF, ELSE, FOR, etc.)
            tokenType in SurqlTokenTypes.CONTROL_FLOW_KEYWORDS -> CONTROL_FLOW_KEYWORD_KEYS
            
            // Definition target keywords (TABLE, FIELD, INDEX, etc.)
            tokenType in SurqlTokenTypes.DEFINITION_TARGET_KEYWORDS -> DEFINITION_TARGET_KEYWORD_KEYS
            
            // Type keywords (BOOL, INT, STRING, etc.)
            tokenType in SurqlTokenTypes.TYPE_KEYWORDS -> TYPE_KEYWORD_KEYS
            
            // Literal keywords (TRUE, FALSE, NULL)
            tokenType in SurqlTokenTypes.LITERAL_KEYWORDS -> LITERAL_KEYWORD_KEYS
            
            // Word operator keywords (AND, OR, CONTAINS, etc.)
            tokenType in SurqlTokenTypes.WORD_OPERATOR_KEYWORDS -> WORD_OPERATOR_KEYWORD_KEYS
            
            // Clause keywords (WHERE, SET, LIMIT, etc.)
            tokenType in SurqlTokenTypes.CLAUSE_KEYWORDS -> CLAUSE_KEYWORD_KEYS
            
            // Schema option keywords
            tokenType in SurqlTokenTypes.SCHEMA_OPTION_KEYWORDS -> SCHEMA_OPTION_KEYWORD_KEYS
            
            // Distance function keywords
            tokenType in SurqlTokenTypes.DISTANCE_FUNCTION_KEYWORDS -> DISTANCE_FUNCTION_KEYWORD_KEYS
            
            // Analyzer filter keywords
            tokenType in SurqlTokenTypes.ANALYZER_FILTER_KEYWORDS -> ANALYZER_FILTER_KEYWORD_KEYS
            
            // Fallback: any other keyword
            tokenType in SurqlTokenTypes.KEYWORDS -> KEYWORD_KEYS

            // Bad character
            tokenType == SurqlTokenTypes.BAD_CHARACTER -> BAD_CHARACTER_KEYS

            else -> EMPTY_KEYS
        }
    }

    companion object {
        // ==================================================
        // BASE TEXT ATTRIBUTE KEYS
        // ==================================================
        
        // Generic keyword (fallback)
        val KEYWORD = createTextAttributesKey("SURQL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Statement keywords - Primary action verbs (SELECT, CREATE, UPDATE, etc.)
        val STATEMENT_KEYWORD = createTextAttributesKey("SURQL_STATEMENT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Transaction keywords (BEGIN, COMMIT, CANCEL, TRANSACTION)
        val TRANSACTION_KEYWORD = createTextAttributesKey("SURQL_TRANSACTION_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Control flow keywords (IF, ELSE, FOR, BREAK, CONTINUE)
        val CONTROL_FLOW_KEYWORD = createTextAttributesKey("SURQL_CONTROL_FLOW_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Clause keywords - Query modifiers (WHERE, SET, LIMIT, etc.)
        val CLAUSE_KEYWORD = createTextAttributesKey("SURQL_CLAUSE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Definition target keywords (TABLE, FIELD, INDEX, etc.)
        val DEFINITION_TARGET_KEYWORD = createTextAttributesKey("SURQL_DEFINITION_TARGET_KEYWORD", DefaultLanguageHighlighterColors.CLASS_NAME)
        
        // Type keywords (BOOL, INT, STRING, etc.)
        val TYPE_KEYWORD = createTextAttributesKey("SURQL_TYPE_KEYWORD", DefaultLanguageHighlighterColors.CLASS_NAME)
        
        // Literal keywords (TRUE, FALSE, NULL)
        val LITERAL_KEYWORD = createTextAttributesKey("SURQL_LITERAL_KEYWORD", DefaultLanguageHighlighterColors.CONSTANT)
        
        // Word operator keywords (AND, OR, CONTAINS, etc.)
        val WORD_OPERATOR_KEYWORD = createTextAttributesKey("SURQL_WORD_OPERATOR_KEYWORD", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        
        // Schema option keywords (SCHEMAFULL, PERMISSIONS, etc.)
        val SCHEMA_OPTION_KEYWORD = createTextAttributesKey("SURQL_SCHEMA_OPTION_KEYWORD", DefaultLanguageHighlighterColors.METADATA)
        
        // Distance function keywords (EUCLIDEAN, COSINE, etc.)
        val DISTANCE_FUNCTION_KEYWORD = createTextAttributesKey("SURQL_DISTANCE_FUNCTION_KEYWORD", DefaultLanguageHighlighterColors.STATIC_METHOD)
        
        // Analyzer filter keywords (ASCII, LOWERCASE, etc.)
        val ANALYZER_FILTER_KEYWORD = createTextAttributesKey("SURQL_ANALYZER_FILTER_KEYWORD", DefaultLanguageHighlighterColors.STATIC_METHOD)
        
        // ==================================================
        // OTHER TEXT ATTRIBUTE KEYS
        // ==================================================
        
        val COMMENT = createTextAttributesKey("SURQL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val STRING = createTextAttributesKey("SURQL_STRING", DefaultLanguageHighlighterColors.STRING)
        val DATETIME = createTextAttributesKey("SURQL_DATETIME", DefaultLanguageHighlighterColors.STRING)
        val UUID = createTextAttributesKey("SURQL_UUID", DefaultLanguageHighlighterColors.STRING)
        val RECORD_STRING = createTextAttributesKey("SURQL_RECORD_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = createTextAttributesKey("SURQL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val DURATION = createTextAttributesKey("SURQL_DURATION", DefaultLanguageHighlighterColors.NUMBER)
        val PARAMETER = createTextAttributesKey("SURQL_PARAMETER", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val RECORD_ID = createTextAttributesKey("SURQL_RECORD_ID", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val IDENTIFIER = createTextAttributesKey("SURQL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val OPERATOR = createTextAttributesKey("SURQL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val PARENTHESES = createTextAttributesKey("SURQL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        val BRACKETS = createTextAttributesKey("SURQL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val BRACES = createTextAttributesKey("SURQL_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val SEMICOLON = createTextAttributesKey("SURQL_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val COMMA = createTextAttributesKey("SURQL_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val DOT = createTextAttributesKey("SURQL_DOT", DefaultLanguageHighlighterColors.DOT)
        val COLON = createTextAttributesKey("SURQL_COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val BAD_CHARACTER = createTextAttributesKey("SURQL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        // Additional semantic highlighting keys (used by SurqlAnnotator)
        val FUNCTION_NAMESPACE = createTextAttributesKey("SURQL_FUNCTION_NAMESPACE", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val USER_FUNCTION = createTextAttributesKey("SURQL_USER_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val TYPE_NAME = createTextAttributesKey("SURQL_TYPE_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
        val SYSTEM_PARAMETER = createTextAttributesKey("SURQL_SYSTEM_PARAMETER", DefaultLanguageHighlighterColors.CONSTANT)
        val RECORD_TABLE = createTextAttributesKey("SURQL_RECORD_TABLE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val RECORD_ID_PART = createTextAttributesKey("SURQL_RECORD_ID_PART", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

        // ==================================================
        // KEY ARRAYS FOR HIGHLIGHTING
        // ==================================================
        
        // Keyword category key arrays
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val STATEMENT_KEYWORD_KEYS = arrayOf(STATEMENT_KEYWORD)
        private val TRANSACTION_KEYWORD_KEYS = arrayOf(TRANSACTION_KEYWORD)
        private val CONTROL_FLOW_KEYWORD_KEYS = arrayOf(CONTROL_FLOW_KEYWORD)
        private val CLAUSE_KEYWORD_KEYS = arrayOf(CLAUSE_KEYWORD)
        private val DEFINITION_TARGET_KEYWORD_KEYS = arrayOf(DEFINITION_TARGET_KEYWORD)
        private val TYPE_KEYWORD_KEYS = arrayOf(TYPE_KEYWORD)
        private val LITERAL_KEYWORD_KEYS = arrayOf(LITERAL_KEYWORD)
        private val WORD_OPERATOR_KEYWORD_KEYS = arrayOf(WORD_OPERATOR_KEYWORD)
        private val SCHEMA_OPTION_KEYWORD_KEYS = arrayOf(SCHEMA_OPTION_KEYWORD)
        private val DISTANCE_FUNCTION_KEYWORD_KEYS = arrayOf(DISTANCE_FUNCTION_KEYWORD)
        private val ANALYZER_FILTER_KEYWORD_KEYS = arrayOf(ANALYZER_FILTER_KEYWORD)
        
        // Other key arrays
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val STRING_KEYS = arrayOf(STRING)
        private val DATETIME_KEYS = arrayOf(DATETIME)
        private val UUID_KEYS = arrayOf(UUID)
        private val RECORD_STRING_KEYS = arrayOf(RECORD_STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val DURATION_KEYS = arrayOf(DURATION)
        private val PARAMETER_KEYS = arrayOf(PARAMETER)
        private val RECORD_ID_KEYS = arrayOf(RECORD_ID)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val BRACES_KEYS = arrayOf(BRACES)
        private val SEMICOLON_KEYS = arrayOf(SEMICOLON)
        private val COMMA_KEYS = arrayOf(COMMA)
        private val DOT_KEYS = arrayOf(DOT)
        private val COLON_KEYS = arrayOf(COLON)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
