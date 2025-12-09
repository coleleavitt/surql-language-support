package org.surrealdb.surql.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.psi.SurqlElementTypes

/**
 * A full-featured parser for SurrealQL that builds a complete AST.
 * Supports all major SurrealQL constructs including statements, expressions,
 * clauses, and definitions.
 */
class SurqlParser : PsiParser {
    
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        
        while (!builder.eof()) {
            parseStatement(builder)
        }
        
        rootMarker.done(root)
        return builder.treeBuilt
    }
    
    // ============================================================================
    // Statement Parsing
    // ============================================================================
    
    private fun parseStatement(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        if (builder.eof()) return
        
        val tokenType = builder.tokenType
        
        when (tokenType) {
            SurqlTokenTypes.SELECT -> parseSelectStatement(builder)
            SurqlTokenTypes.CREATE -> parseCreateStatement(builder)
            SurqlTokenTypes.UPDATE -> parseUpdateStatement(builder)
            SurqlTokenTypes.DELETE -> parseDeleteStatement(builder)
            SurqlTokenTypes.INSERT -> parseInsertStatement(builder)
            SurqlTokenTypes.UPSERT -> parseUpsertStatement(builder)
            SurqlTokenTypes.RELATE -> parseRelateStatement(builder)
            SurqlTokenTypes.DEFINE -> parseDefineStatement(builder)
            SurqlTokenTypes.REMOVE -> parseRemoveStatement(builder)
            SurqlTokenTypes.ALTER -> parseAlterStatement(builder)
            SurqlTokenTypes.LET -> parseLetStatement(builder)
            SurqlTokenTypes.IF -> parseIfStatement(builder)
            SurqlTokenTypes.FOR -> parseForStatement(builder)
            SurqlTokenTypes.RETURN -> parseReturnStatement(builder)
            SurqlTokenTypes.THROW -> parseThrowStatement(builder)
            SurqlTokenTypes.BEGIN -> parseTransactionStatement(builder)
            SurqlTokenTypes.COMMIT -> parseCommitStatement(builder)
            SurqlTokenTypes.CANCEL -> parseCancelStatement(builder)
            SurqlTokenTypes.USE -> parseUseStatement(builder)
            SurqlTokenTypes.INFO -> parseInfoStatement(builder)
            SurqlTokenTypes.LIVE -> parseLiveStatement(builder)
            SurqlTokenTypes.KILL -> parseKillStatement(builder)
            SurqlTokenTypes.BREAK -> parseBreakStatement(builder)
            SurqlTokenTypes.CONTINUE -> parseContinueStatement(builder)
            SurqlTokenTypes.SLEEP -> parseSleepStatement(builder)
            SurqlTokenTypes.SHOW -> parseShowStatement(builder)
            SurqlTokenTypes.REBUILD -> parseRebuildStatement(builder)
            SurqlTokenTypes.OPTION -> parseOptionStatement(builder)
            SurqlTokenTypes.SEMICOLON -> {
                // Skip standalone semicolons
                builder.advanceLexer()
            }
            else -> {
                // Try to parse as expression statement or skip bad token
                if (isExpressionStart(tokenType)) {
                    parseExpressionStatement(builder)
                } else {
                    // Mark as error and advance
                    val errorMarker = builder.mark()
                    builder.advanceLexer()
                    errorMarker.error("Unexpected token")
                }
            }
        }
        
        // Consume optional semicolon
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.SEMICOLON) {
            builder.advanceLexer()
        }
    }
    
    // ============================================================================
    // SELECT Statement
    // ============================================================================
    
    private fun parseSelectStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // SELECT
        expect(builder, SurqlTokenTypes.SELECT)
        
        // Optional VALUE keyword
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.VALUE) {
            builder.advanceLexer()
        }
        
        // Select fields (or *)
        parseSelectFields(builder)
        
        // FROM clause (optional for some queries)
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.FROM) {
            parseFromClause(builder)
        }
        
        // Optional clauses
        parseOptionalClauses(builder, QUERY_CLAUSES)
        
        marker.done(SurqlElementTypes.SELECT_STATEMENT)
    }
    
    private fun parseSelectFields(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        // Handle *
        if (builder.tokenType == SurqlTokenTypes.STAR) {
            builder.advanceLexer()
            return
        }
        
        // Parse field list
        parseExpressionList(builder)
    }
    
    private fun parseFromClause(builder: PsiBuilder) {
        // FROM
        expect(builder, SurqlTokenTypes.FROM)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // Table references
        parseTableReferenceList(builder)
    }
    
    // ============================================================================
    // CREATE Statement
    // ============================================================================
    
    private fun parseCreateStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // CREATE
        expect(builder, SurqlTokenTypes.CREATE)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // Target (table or record ID)
        parseTableReference(builder)
        
        // Optional content/set clauses
        parseOptionalClauses(builder, MUTATION_CLAUSES)
        
        marker.done(SurqlElementTypes.CREATE_STATEMENT)
    }
    
    // ============================================================================
    // UPDATE Statement
    // ============================================================================
    
    private fun parseUpdateStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // UPDATE
        expect(builder, SurqlTokenTypes.UPDATE)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // Target
        parseTableReference(builder)
        
        // Optional clauses
        parseOptionalClauses(builder, MUTATION_CLAUSES)
        
        marker.done(SurqlElementTypes.UPDATE_STATEMENT)
    }
    
    // ============================================================================
    // DELETE Statement
    // ============================================================================
    
    private fun parseDeleteStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // DELETE
        expect(builder, SurqlTokenTypes.DELETE)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // Optional FROM
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.FROM) {
            builder.advanceLexer()
        }
        
        // Target
        parseTableReference(builder)
        
        // Optional clauses
        parseOptionalClauses(builder, DELETE_CLAUSES)
        
        marker.done(SurqlElementTypes.DELETE_STATEMENT)
    }
    
    // ============================================================================
    // INSERT Statement
    // ============================================================================
    
    private fun parseInsertStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // INSERT
        expect(builder, SurqlTokenTypes.INSERT)
        
        // Optional RELATION
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RELATION) {
            builder.advanceLexer()
        }
        
        // Optional IGNORE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IGNORE) {
            builder.advanceLexer()
        }
        
        // Optional INTO table - only if INTO keyword is present
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.INTO) {
            builder.advanceLexer()
            // Target table or parameter
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.PARAMETER) {
                builder.advanceLexer()
            } else if (builder.tokenType == SurqlTokenTypes.IDENTIFIER) {
                parseTableReference(builder)
            }
        }
        
        // Values or content - INSERT can have data directly without INTO
        // Formats: INSERT [...], INSERT {...}, INSERT (fields) VALUES (...)
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.VALUES -> {
                builder.advanceLexer()
                parseValuesList(builder)
            }
            SurqlTokenTypes.LBRACKET, SurqlTokenTypes.LBRACE -> {
                // Direct data: INSERT [...] or INSERT {...}
                parseExpression(builder)
            }
            SurqlTokenTypes.LPAREN -> {
                // Could be field list: INSERT (a, b) VALUES (1, 2)
                parseParenthesizedList(builder)
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.VALUES) {
                    builder.advanceLexer()
                    parseValuesList(builder)
                }
            }
            else -> {
                // Try to parse as expression (e.g., a parameter or subquery)
                if (isExpressionStart(builder.tokenType)) {
                    parseExpression(builder)
                }
            }
        }
        
        // Optional ON DUPLICATE KEY UPDATE
        parseOptionalClauses(builder, INSERT_CLAUSES)
        
        marker.done(SurqlElementTypes.INSERT_STATEMENT)
    }
    
    // ============================================================================
    // UPSERT Statement
    // ============================================================================
    
    private fun parseUpsertStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // UPSERT
        expect(builder, SurqlTokenTypes.UPSERT)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // Target
        parseTableReference(builder)
        
        // Optional clauses
        parseOptionalClauses(builder, MUTATION_CLAUSES)
        
        marker.done(SurqlElementTypes.UPSERT_STATEMENT)
    }
    
    // ============================================================================
    // RELATE Statement
    // ============================================================================
    
    private fun parseRelateStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // RELATE
        expect(builder, SurqlTokenTypes.RELATE)
        
        // Optional ONLY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ONLY) {
            builder.advanceLexer()
        }
        
        // From record(s)
        parseExpression(builder)
        
        // Arrow and edge table
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ARROW) {
            builder.advanceLexer()
            parseTableReference(builder)
            
            // Arrow and to record(s)
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.ARROW) {
                builder.advanceLexer()
                parseExpression(builder)
            }
        }
        
        // Optional clauses
        parseOptionalClauses(builder, MUTATION_CLAUSES)
        
        marker.done(SurqlElementTypes.RELATE_STATEMENT)
    }
    
    // ============================================================================
    // DEFINE Statement
    // ============================================================================
    
    private fun parseDefineStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // DEFINE
        expect(builder, SurqlTokenTypes.DEFINE)
        
        skipWhitespaceAndComments(builder)
        
        when (builder.tokenType) {
            SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS -> parseDefineNamespace(builder)
            SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB -> parseDefineDatabase(builder)
            SurqlTokenTypes.TABLE -> parseDefineTable(builder)
            SurqlTokenTypes.FIELD -> parseDefineField(builder)
            SurqlTokenTypes.INDEX -> parseDefineIndex(builder)
            SurqlTokenTypes.EVENT -> parseDefineEvent(builder)
            SurqlTokenTypes.FUNCTION -> parseDefineFunction(builder)
            SurqlTokenTypes.PARAM -> parseDefineParam(builder)
            SurqlTokenTypes.ANALYZER -> parseDefineAnalyzer(builder)
            SurqlTokenTypes.ACCESS -> parseDefineAccess(builder)
            SurqlTokenTypes.USER -> parseDefineUser(builder)
            SurqlTokenTypes.TOKEN -> parseDefineToken(builder)
            SurqlTokenTypes.SCOPE -> parseDefineScope(builder)
            SurqlTokenTypes.MODEL -> parseDefineModel(builder)
            SurqlTokenTypes.CONFIG -> parseDefineConfig(builder)
            else -> {
                // Unknown definition type - consume until semicolon
                consumeUntilStatementEnd(builder)
            }
        }
        
        marker.done(SurqlElementTypes.DEFINE_STATEMENT)
    }
    
    private fun parseDefineNamespace(builder: PsiBuilder) {
        builder.advanceLexer() // NAMESPACE/NS
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // namespace name
        parseOptionalComment(builder)
    }
    
    private fun parseDefineDatabase(builder: PsiBuilder) {
        builder.advanceLexer() // DATABASE/DB
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // database name
        parseOptionalComment(builder)
    }
    
    private fun parseDefineTable(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // TABLE
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseTableName(builder) // table name
        
        // Table options
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.DROP -> builder.advanceLexer()
                SurqlTokenTypes.SCHEMAFULL, SurqlTokenTypes.SCHEMALESS -> builder.advanceLexer()
                SurqlTokenTypes.TYPE -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    when (builder.tokenType) {
                        SurqlTokenTypes.ANY, SurqlTokenTypes.NORMAL, 
                        SurqlTokenTypes.RELATION -> builder.advanceLexer()
                        else -> {}
                    }
                }
                SurqlTokenTypes.CHANGEFEED -> {
                    builder.advanceLexer()
                    parseExpression(builder) // duration
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.INCLUDE) {
                        builder.advanceLexer()
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.ORIGINAL) {
                            builder.advanceLexer()
                        }
                    }
                }
                SurqlTokenTypes.AS -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.SELECT) {
                        parseSelectStatement(builder)
                    } else {
                        parseExpression(builder)
                    }
                }
                SurqlTokenTypes.PERMISSIONS -> parsePermissionsClause(builder)
                SurqlTokenTypes.COMMENT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        defMarker.done(SurqlElementTypes.TABLE_DEFINITION)
    }
    
    private fun parseDefineField(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // FIELD
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseFieldPath(builder) // field name (can be nested like foo.bar)
        
        // ON TABLE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.TABLE) {
                builder.advanceLexer()
            }
            parseTableName(builder)
        }
        
        // Field options
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.FLEXIBLE -> builder.advanceLexer()
                SurqlTokenTypes.READONLY -> builder.advanceLexer()
                SurqlTokenTypes.TYPE -> {
                    builder.advanceLexer()
                    parseTypeExpression(builder)
                }
                SurqlTokenTypes.DEFAULT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.VALUE -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.ASSERT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.PERMISSIONS -> parsePermissionsClause(builder)
                SurqlTokenTypes.COMMENT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        defMarker.done(SurqlElementTypes.FIELD_DEFINITION)
    }
    
    private fun parseDefineIndex(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // INDEX
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // index name
        
        // ON TABLE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.TABLE) {
                builder.advanceLexer()
            }
            parseTableName(builder)
        }
        
        // FIELDS or COLUMNS
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.FIELDS || builder.tokenType == SurqlTokenTypes.COLUMNS) {
            builder.advanceLexer()
            parseFieldList(builder)
        }
        
        // Index options
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.UNIQUE -> builder.advanceLexer()
                SurqlTokenTypes.SEARCH -> {
                    builder.advanceLexer()
                    parseSearchIndexOptions(builder)
                }
                SurqlTokenTypes.MTREE -> {
                    builder.advanceLexer()
                    parseMTreeOptions(builder)
                }
                SurqlTokenTypes.HNSW -> {
                    builder.advanceLexer()
                    parseHnswOptions(builder)
                }
                SurqlTokenTypes.CONCURRENTLY -> builder.advanceLexer()
                SurqlTokenTypes.COMMENT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        defMarker.done(SurqlElementTypes.INDEX_DEFINITION)
    }
    
    private fun parseDefineEvent(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // EVENT
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // event name
        
        // ON TABLE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.TABLE) {
                builder.advanceLexer()
            }
            parseTableName(builder)
        }
        
        // WHEN
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.WHEN) {
            builder.advanceLexer()
            parseExpression(builder)
        }
        
        // THEN
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.THEN) {
            builder.advanceLexer()
            // Can be a block or expression
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.LBRACE) {
                parseBlock(builder)
            } else {
                parseExpression(builder)
            }
        }
        
        parseOptionalComment(builder)
        defMarker.done(SurqlElementTypes.EVENT_DEFINITION)
    }
    
    private fun parseDefineFunction(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // FUNCTION
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        
        // Function name (fn::name or just name)
        parseFunctionName(builder)
        
        // Parameters
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LPAREN) {
            parseParameterList(builder)
        }
        
        // Function body
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LBRACE) {
            parseBlock(builder)
        }
        
        parseOptionalComment(builder)
        parsePermissionsClauseIfPresent(builder)
        
        defMarker.done(SurqlElementTypes.FUNCTION_DEFINITION)
    }
    
    private fun parseDefineParam(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // PARAM
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        
        // Parameter name ($name)
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.PARAMETER) {
            builder.advanceLexer()
        } else {
            parseName(builder)
        }
        
        // VALUE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.VALUE) {
            builder.advanceLexer()
            parseExpression(builder)
        }
        
        parseOptionalComment(builder)
        parsePermissionsClauseIfPresent(builder)
        
        defMarker.done(SurqlElementTypes.PARAM_DEFINITION)
    }
    
    private fun parseDefineAnalyzer(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // ANALYZER
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // analyzer name
        
        // Analyzer options
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.TOKENIZERS -> {
                    builder.advanceLexer()
                    parseTokenizerList(builder)
                }
                SurqlTokenTypes.FILTERS -> {
                    builder.advanceLexer()
                    parseFilterList(builder)
                }
                SurqlTokenTypes.COMMENT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        defMarker.done(SurqlElementTypes.ANALYZER_DEFINITION)
    }
    
    private fun parseDefineAccess(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // ACCESS
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // access name
        
        // ON (NAMESPACE/DATABASE/ROOT)
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS,
                SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB,
                SurqlTokenTypes.ROOT -> builder.advanceLexer()
                else -> {}
            }
        }
        
        // TYPE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.TYPE) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.JWT -> parseJwtAccess(builder)
                SurqlTokenTypes.RECORD -> parseRecordAccess(builder)
                SurqlTokenTypes.BEARER -> parseBearerAccess(builder)
                else -> builder.advanceLexer()
            }
        }
        
        parseOptionalComment(builder)
        defMarker.done(SurqlElementTypes.ACCESS_DEFINITION)
    }
    
    private fun parseDefineUser(builder: PsiBuilder) {
        val defMarker = builder.mark()
        builder.advanceLexer() // USER
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder) // user name
        
        // ON (NAMESPACE/DATABASE/ROOT)
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS,
                SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB,
                SurqlTokenTypes.ROOT -> builder.advanceLexer()
                else -> {}
            }
        }
        
        // User options
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.PASSWORD -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.PASSHASH -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.ROLES -> {
                    builder.advanceLexer()
                    parseIdentifierList(builder)
                }
                SurqlTokenTypes.COMMENT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        defMarker.done(SurqlElementTypes.USER_DEFINITION)
    }
    
    private fun parseDefineToken(builder: PsiBuilder) {
        builder.advanceLexer() // TOKEN
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder)
        consumeUntilStatementEnd(builder)
    }
    
    private fun parseDefineScope(builder: PsiBuilder) {
        builder.advanceLexer() // SCOPE
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder)
        consumeUntilStatementEnd(builder)
    }
    
    private fun parseDefineModel(builder: PsiBuilder) {
        builder.advanceLexer() // MODEL
        parseOptionalOverwrite(builder)
        parseOptionalIfNotExists(builder)
        parseName(builder)
        consumeUntilStatementEnd(builder)
    }
    
    private fun parseDefineConfig(builder: PsiBuilder) {
        builder.advanceLexer() // CONFIG
        consumeUntilStatementEnd(builder)
    }
    
    // ============================================================================
    // REMOVE Statement
    // ============================================================================
    
    private fun parseRemoveStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // REMOVE
        expect(builder, SurqlTokenTypes.REMOVE)
        
        skipWhitespaceAndComments(builder)
        
        // Definition type
        when (builder.tokenType) {
            SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS,
            SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB,
            SurqlTokenTypes.TABLE, SurqlTokenTypes.FIELD,
            SurqlTokenTypes.INDEX, SurqlTokenTypes.EVENT,
            SurqlTokenTypes.FUNCTION, SurqlTokenTypes.PARAM,
            SurqlTokenTypes.ANALYZER, SurqlTokenTypes.ACCESS,
            SurqlTokenTypes.USER, SurqlTokenTypes.TOKEN,
            SurqlTokenTypes.SCOPE, SurqlTokenTypes.MODEL -> {
                builder.advanceLexer()
                
                // Optional IF EXISTS
                parseOptionalIfExists(builder)
                
                // Name
                parseName(builder)
                
                // Optional ON clause
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.ON) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.TABLE) {
                        builder.advanceLexer()
                    }
                    parseName(builder)
                }
            }
            else -> consumeUntilStatementEnd(builder)
        }
        
        marker.done(SurqlElementTypes.REMOVE_STATEMENT)
    }
    
    // ============================================================================
    // ALTER Statement
    // ============================================================================
    
    private fun parseAlterStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.ALTER)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.TABLE) {
            builder.advanceLexer()
            parseOptionalIfExists(builder)
            parseTableName(builder)
            
            // ALTER options (similar to DEFINE TABLE)
            while (!builder.eof() && !isStatementEnd(builder)) {
                skipWhitespaceAndComments(builder)
                when (builder.tokenType) {
                    SurqlTokenTypes.DROP -> builder.advanceLexer()
                    SurqlTokenTypes.SCHEMAFULL, SurqlTokenTypes.SCHEMALESS -> builder.advanceLexer()
                    SurqlTokenTypes.PERMISSIONS -> parsePermissionsClause(builder)
                    SurqlTokenTypes.COMMENT -> {
                        builder.advanceLexer()
                        parseExpression(builder)
                    }
                    else -> break
                }
            }
        } else {
            consumeUntilStatementEnd(builder)
        }
        
        marker.done(SurqlElementTypes.DEFINE_STATEMENT) // Reuse DEFINE for ALTER
    }
    
    // ============================================================================
    // Control Flow Statements
    // ============================================================================
    
    private fun parseLetStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.LET)
        
        // Variable name
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.PARAMETER) {
            builder.advanceLexer()
        } else {
            parseName(builder)
        }
        
        // = value
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.EQ) {
            builder.advanceLexer()
            parseExpression(builder)
        }
        
        marker.done(SurqlElementTypes.LET_STATEMENT)
    }
    
    private fun parseIfStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.IF)
        
        // Condition
        parseExpression(builder)
        
        // THEN or block
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.THEN) {
            builder.advanceLexer()
        }
        
        // Then branch
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LBRACE) {
            parseBlock(builder)
        } else {
            parseExpression(builder)
        }
        
        // Optional ELSE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ELSE) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            
            if (builder.tokenType == SurqlTokenTypes.IF) {
                // ELSE IF
                parseIfStatement(builder)
            } else if (builder.tokenType == SurqlTokenTypes.LBRACE) {
                parseBlock(builder)
            } else {
                parseExpression(builder)
            }
        }
        
        // Optional END
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.END) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.IF_STATEMENT)
    }
    
    private fun parseForStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.FOR)
        
        // Variable
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.PARAMETER) {
            builder.advanceLexer()
        } else {
            parseName(builder)
        }
        
        // IN
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IN) {
            builder.advanceLexer()
        }
        
        // Iterable
        parseExpression(builder)
        
        // Body
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LBRACE) {
            parseBlock(builder)
        } else {
            parseExpression(builder)
        }
        
        marker.done(SurqlElementTypes.FOR_STATEMENT)
    }
    
    private fun parseReturnStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.RETURN)
        
        // Optional return value
        skipWhitespaceAndComments(builder)
        if (!isStatementEnd(builder)) {
            parseExpression(builder)
        }
        
        marker.done(SurqlElementTypes.RETURN_STATEMENT)
    }
    
    private fun parseThrowStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.THROW)
        parseExpression(builder)
        
        marker.done(SurqlElementTypes.RETURN_STATEMENT) // Reuse for simplicity
    }
    
    private fun parseBreakStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.BREAK)
        marker.done(SurqlElementTypes.RETURN_STATEMENT)
    }
    
    private fun parseContinueStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.CONTINUE)
        marker.done(SurqlElementTypes.RETURN_STATEMENT)
    }
    
    // ============================================================================
    // Transaction Statements
    // ============================================================================
    
    private fun parseTransactionStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.BEGIN)
        
        // Optional TRANSACTION
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.TRANSACTION) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.TRANSACTION_STATEMENT)
    }
    
    private fun parseCommitStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.COMMIT)
        
        // Optional TRANSACTION
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.TRANSACTION) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.TRANSACTION_STATEMENT)
    }
    
    private fun parseCancelStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.CANCEL)
        
        // Optional TRANSACTION
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.TRANSACTION) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.TRANSACTION_STATEMENT)
    }
    
    // ============================================================================
    // Other Statements
    // ============================================================================
    
    private fun parseUseStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.USE)
        
        // NS/DB
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS -> {
                    builder.advanceLexer()
                    parseName(builder)
                }
                SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB -> {
                    builder.advanceLexer()
                    parseName(builder)
                }
                else -> break
            }
        }
        
        marker.done(SurqlElementTypes.USE_STATEMENT)
    }
    
    private fun parseInfoStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.INFO)
        
        // FOR
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.FOR) {
            builder.advanceLexer()
        }
        
        // Target
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.ROOT,
            SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS,
            SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB -> builder.advanceLexer()
            SurqlTokenTypes.TABLE -> {
                builder.advanceLexer()
                parseTableName(builder)
            }
            SurqlTokenTypes.USER -> {
                builder.advanceLexer()
                parseName(builder)
                // ON clause
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.ON) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    builder.advanceLexer() // ROOT/NS/DB
                }
            }
            else -> {}
        }
        
        marker.done(SurqlElementTypes.INFO_STATEMENT)
    }
    
    private fun parseLiveStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.LIVE)
        
        // SELECT
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.SELECT) {
            parseSelectStatement(builder)
        }
        
        marker.done(SurqlElementTypes.LIVE_STATEMENT)
    }
    
    private fun parseKillStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        
        expect(builder, SurqlTokenTypes.KILL)
        parseExpression(builder) // UUID
        
        marker.done(SurqlElementTypes.LIVE_STATEMENT)
    }
    
    private fun parseSleepStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.SLEEP)
        parseExpression(builder) // duration
        marker.done(SurqlElementTypes.RETURN_STATEMENT)
    }
    
    private fun parseShowStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.SHOW)
        
        // CHANGES FOR TABLE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.CHANGES) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.FOR) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.TABLE) {
                    builder.advanceLexer()
                    parseTableName(builder)
                }
            }
        }
        
        // Optional SINCE/LIMIT
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.SINCE -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.LIMIT -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
        
        marker.done(SurqlElementTypes.INFO_STATEMENT)
    }
    
    private fun parseRebuildStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.REBUILD)
        
        // INDEX
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.INDEX) {
            builder.advanceLexer()
            parseOptionalIfExists(builder)
            parseName(builder)
            
            // ON TABLE
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.ON) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.TABLE) {
                    builder.advanceLexer()
                }
                parseTableName(builder)
            }
        }
        
        marker.done(SurqlElementTypes.INFO_STATEMENT)
    }
    
    private fun parseOptionStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.OPTION)
        
        // Option name
        parseName(builder)
        
        // Optional = value
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.EQ) {
            builder.advanceLexer()
            parseExpression(builder)
        }
        
        marker.done(SurqlElementTypes.USE_STATEMENT)
    }
    
    private fun parseExpressionStatement(builder: PsiBuilder) {
        val marker = builder.mark()
        parseExpression(builder)
        marker.done(SurqlElementTypes.RETURN_STATEMENT)
    }
    
    // ============================================================================
    // Expression Parsing
    // ============================================================================
    
    private fun parseExpression(builder: PsiBuilder) {
        parseTernaryExpression(builder)
    }
    
    private fun parseTernaryExpression(builder: PsiBuilder) {
        val marker = builder.mark()
        parseOrExpression(builder)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.QUESTION) {
            builder.advanceLexer()
            parseExpression(builder)
            
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COLON) {
                builder.advanceLexer()
                parseExpression(builder)
            }
            marker.done(SurqlElementTypes.EXPRESSION)
        } else if (builder.tokenType == SurqlTokenTypes.QUESTIONQUESTION) {
            // Null coalescing
            builder.advanceLexer()
            parseExpression(builder)
            marker.done(SurqlElementTypes.BINARY_EXPRESSION)
        } else if (builder.tokenType == SurqlTokenTypes.QUESTIONCOLON) {
            // Elvis operator
            builder.advanceLexer()
            parseExpression(builder)
            marker.done(SurqlElementTypes.BINARY_EXPRESSION)
        } else {
            marker.drop()
        }
    }
    
    private fun parseOrExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseAndExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.OR || 
                builder.tokenType == SurqlTokenTypes.OR_KW) {
                builder.advanceLexer()
                parseAndExpression(builder)
                marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                marker = marker.precede()
            } else {
                break
            }
        }
        marker.drop()
    }
    
    private fun parseAndExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseNotExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.AND || 
                builder.tokenType == SurqlTokenTypes.AND_KW) {
                builder.advanceLexer()
                parseNotExpression(builder)
                marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                marker = marker.precede()
            } else {
                break
            }
        }
        marker.drop()
    }
    
    private fun parseNotExpression(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.NOT) {
            val marker = builder.mark()
            builder.advanceLexer()
            parseNotExpression(builder)
            marker.done(SurqlElementTypes.UNARY_EXPRESSION)
        } else {
            parseComparisonExpression(builder)
        }
    }
    
    private fun parseComparisonExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseContainsExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.EQEQ, SurqlTokenTypes.NE,
                SurqlTokenTypes.LT, SurqlTokenTypes.LE,
                SurqlTokenTypes.GT, SurqlTokenTypes.GE,
                SurqlTokenTypes.IS -> {
                    builder.advanceLexer()
                    
                    // Handle IS NOT
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.NOT) {
                        builder.advanceLexer()
                    }
                    
                    parseContainsExpression(builder)
                    marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                    marker = marker.precede()
                }
                else -> break
            }
        }
        marker.drop()
    }
    
    private fun parseContainsExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseRangeExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.CONTAINS, SurqlTokenTypes.CONTAINSNOT,
                SurqlTokenTypes.CONTAINSALL, SurqlTokenTypes.CONTAINSANY,
                SurqlTokenTypes.CONTAINSNONE, SurqlTokenTypes.IN,
                SurqlTokenTypes.INSIDE, SurqlTokenTypes.NOTINSIDE,
                SurqlTokenTypes.ALLINSIDE, SurqlTokenTypes.ANYINSIDE,
                SurqlTokenTypes.NONEINSIDE, SurqlTokenTypes.OUTSIDE,
                SurqlTokenTypes.INTERSECTS, SurqlTokenTypes.MATCHES,
                SurqlTokenTypes.LIKE, SurqlTokenTypes.KNN -> {
                    builder.advanceLexer()
                    parseRangeExpression(builder)
                    marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                    marker = marker.precede()
                }
                else -> break
            }
        }
        marker.drop()
    }
    
    private fun parseRangeExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseAdditiveExpression(builder)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.DOTDOT || 
            builder.tokenType == SurqlTokenTypes.DOTDOTDOT) {
            builder.advanceLexer()
            parseAdditiveExpression(builder)
            marker.done(SurqlElementTypes.BINARY_EXPRESSION)
        } else {
            marker.drop()
        }
    }
    
    private fun parseAdditiveExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseMultiplicativeExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.PLUS, SurqlTokenTypes.MINUS -> {
                    builder.advanceLexer()
                    parseMultiplicativeExpression(builder)
                    marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                    marker = marker.precede()
                }
                else -> break
            }
        }
        marker.drop()
    }
    
    private fun parseMultiplicativeExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parsePowerExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.STAR, SurqlTokenTypes.SLASH, SurqlTokenTypes.PERCENT -> {
                    builder.advanceLexer()
                    parsePowerExpression(builder)
                    marker.done(SurqlElementTypes.BINARY_EXPRESSION)
                    marker = marker.precede()
                }
                else -> break
            }
        }
        marker.drop()
    }
    
    private fun parsePowerExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parseUnaryExpression(builder)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.CARET) {
            builder.advanceLexer()
            parsePowerExpression(builder) // Right associative
            marker.done(SurqlElementTypes.BINARY_EXPRESSION)
        } else {
            marker.drop()
        }
    }
    
    private fun parseUnaryExpression(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.MINUS, SurqlTokenTypes.PLUS, SurqlTokenTypes.TILDE -> {
                val marker = builder.mark()
                builder.advanceLexer()
                parseUnaryExpression(builder)
                marker.done(SurqlElementTypes.UNARY_EXPRESSION)
            }
            SurqlTokenTypes.LT -> {
                // Could be cast: <type>expr
                if (isCastExpression(builder)) {
                    parseCastExpression(builder)
                } else {
                    parsePostfixExpression(builder)
                }
            }
            else -> parsePostfixExpression(builder)
        }
    }
    
    private fun isCastExpression(builder: PsiBuilder): Boolean {
        // Look ahead to check if this is <type>
        val marker = builder.mark()
        builder.advanceLexer() // <
        skipWhitespaceAndComments(builder)
        val isType = builder.tokenType in SurqlTokenTypes.TYPE_KEYWORDS ||
                     builder.tokenType == SurqlTokenTypes.IDENTIFIER ||
                     builder.tokenType == SurqlTokenTypes.FUTURE
        marker.rollbackTo()
        return isType
    }
    
    private fun parseCastExpression(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LT)
        parseTypeExpression(builder)
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.GT) {
            builder.advanceLexer()
        }
        parseUnaryExpression(builder)
        marker.done(SurqlElementTypes.EXPRESSION)
    }
    
    private fun parsePostfixExpression(builder: PsiBuilder) {
        var marker = builder.mark()
        parsePrimaryExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.DOT -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    // Field access or method call
                    if (builder.tokenType == SurqlTokenTypes.STAR) {
                        builder.advanceLexer() // .* wildcard
                    } else {
                        parseName(builder)
                        // Check for function call
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                            parseArgumentList(builder)
                        }
                    }
                    marker.done(SurqlElementTypes.FIELD_ACCESS)
                    marker = marker.precede()
                }
                SurqlTokenTypes.LBRACKET -> {
                    parseIndexAccess(builder)
                    marker.done(SurqlElementTypes.INDEX_ACCESS)
                    marker = marker.precede()
                }
                SurqlTokenTypes.ARROW, SurqlTokenTypes.LARROW, SurqlTokenTypes.BIARROW -> {
                    builder.advanceLexer()
                    parseTableReference(builder)
                    marker.done(SurqlElementTypes.GRAPH_PATH)
                    marker = marker.precede()
                }
                SurqlTokenTypes.LPAREN -> {
                    // Function call on result
                    parseArgumentList(builder)
                    marker.done(SurqlElementTypes.FUNCTION_CALL)
                    marker = marker.precede()
                }
                else -> break
            }
        }
        marker.drop()
    }
    
    private fun parsePrimaryExpression(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        when (builder.tokenType) {
            // Literals
            SurqlTokenTypes.NUMBER -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.NUMBER_LITERAL)
            }
            SurqlTokenTypes.STRING -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.STRING_LITERAL)
            }
            SurqlTokenTypes.TRUE, SurqlTokenTypes.FALSE -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.BOOLEAN_LITERAL)
            }
            SurqlTokenTypes.NULL -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.NULL_LITERAL)
            }
            SurqlTokenTypes.NONE -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.NULL_LITERAL)
            }
            SurqlTokenTypes.DATETIME_STRING -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.DATETIME_LITERAL)
            }
            SurqlTokenTypes.UUID_STRING -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.UUID_LITERAL)
            }
            SurqlTokenTypes.RECORD_ID, SurqlTokenTypes.RECORD_STRING -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.RECORD_ID_EXPR)
            }
            SurqlTokenTypes.REGEX -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.STRING_LITERAL)
            }
            
            // Parameter
            SurqlTokenTypes.PARAMETER -> {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(SurqlElementTypes.PARAMETER_REF)
            }
            
            // Parenthesized expression or subquery
            SurqlTokenTypes.LPAREN -> {
                parseParenthesizedOrSubquery(builder)
            }
            
            // Object literal
            SurqlTokenTypes.LBRACE -> {
                parseObjectLiteral(builder)
            }
            
            // Array literal
            SurqlTokenTypes.LBRACKET -> {
                parseArrayLiteral(builder)
            }
            
            // Future block
            SurqlTokenTypes.FUTURE -> {
                val marker = builder.mark()
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LBRACE) {
                    parseBlock(builder)
                }
                marker.done(SurqlElementTypes.EXPRESSION)
            }
            
            // Identifier or function call
            SurqlTokenTypes.IDENTIFIER -> {
                parseIdentifierOrFunctionCall(builder)
            }
            
            // Type keywords that can also be identifiers
            else -> {
                if (builder.tokenType in SurqlTokenTypes.TYPE_KEYWORDS ||
                    builder.tokenType in SurqlTokenTypes.STATEMENT_KEYWORDS) {
                    // Some keywords can be used as identifiers in certain contexts
                    val marker = builder.mark()
                    builder.advanceLexer()
                    
                    // Check if it's a function call
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                        parseArgumentList(builder)
                        marker.done(SurqlElementTypes.FUNCTION_CALL)
                    } else if (builder.tokenType == SurqlTokenTypes.COLONCOLON) {
                        // Namespaced function
                        marker.rollbackTo()
                        parseIdentifierOrFunctionCall(builder)
                    } else {
                        marker.done(SurqlElementTypes.EXPRESSION)
                    }
                } else {
                    // Unknown token - mark as error but continue
                    val marker = builder.mark()
                    builder.advanceLexer()
                    marker.error("Expected expression")
                }
            }
        }
    }
    
    private fun parseIdentifierOrFunctionCall(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // Parse potentially namespaced identifier
        builder.advanceLexer() // first identifier
        
        // Check for namespace separator
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COLONCOLON) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.IDENTIFIER ||
                    builder.tokenType in SurqlTokenTypes.KEYWORDS) {
                    builder.advanceLexer()
                } else {
                    break
                }
            } else {
                break
            }
        }
        
        // Check if it's a function call
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LPAREN) {
            parseArgumentList(builder)
            marker.done(SurqlElementTypes.FUNCTION_CALL)
        } else if (builder.tokenType == SurqlTokenTypes.COLON) {
            // Record ID: table:id
            builder.advanceLexer()
            parseRecordIdValue(builder)
            marker.done(SurqlElementTypes.RECORD_ID_EXPR)
        } else {
            marker.done(SurqlElementTypes.EXPRESSION)
        }
    }
    
    private fun parseRecordIdValue(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.IDENTIFIER, SurqlTokenTypes.NUMBER,
            SurqlTokenTypes.STRING -> builder.advanceLexer()
            SurqlTokenTypes.LBRACKET -> {
                // Complex ID: table:[...]
                builder.advanceLexer()
                parseExpression(builder)
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.RBRACKET) {
                    builder.advanceLexer()
                }
            }
            SurqlTokenTypes.LBRACE -> {
                // Object ID: table:{...}
                parseObjectLiteral(builder)
            }
            SurqlTokenTypes.LPAREN -> {
                // Tuple ID: table:(...)
                parseParenthesizedOrSubquery(builder)
            }
            else -> {}
        }
    }
    
    private fun parseParenthesizedOrSubquery(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LPAREN)
        
        skipWhitespaceAndComments(builder)
        
        // Check if it's a subquery
        if (builder.tokenType == SurqlTokenTypes.SELECT ||
            builder.tokenType == SurqlTokenTypes.CREATE ||
            builder.tokenType == SurqlTokenTypes.UPDATE ||
            builder.tokenType == SurqlTokenTypes.DELETE ||
            builder.tokenType == SurqlTokenTypes.INSERT ||
            builder.tokenType == SurqlTokenTypes.RELATE) {
            parseStatement(builder)
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.RPAREN) {
                builder.advanceLexer()
            }
            marker.done(SurqlElementTypes.SUBQUERY)
        } else {
            // Regular parenthesized expression
            parseExpression(builder)
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.RPAREN) {
                builder.advanceLexer()
            }
            marker.done(SurqlElementTypes.EXPRESSION)
        }
    }
    
    private fun parseObjectLiteral(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LBRACE)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType != SurqlTokenTypes.RBRACE) {
            parseObjectEntry(builder)
            
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.RBRACE) break
                    parseObjectEntry(builder)
                } else {
                    break
                }
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RBRACE) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.OBJECT_LITERAL)
    }
    
    private fun parseObjectEntry(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // Key
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.IDENTIFIER -> builder.advanceLexer()
            SurqlTokenTypes.STRING -> builder.advanceLexer()
            else -> {
                if (builder.tokenType in SurqlTokenTypes.KEYWORDS) {
                    builder.advanceLexer() // Keywords can be object keys
                }
            }
        }
        
        // : or =
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.COLON || 
            builder.tokenType == SurqlTokenTypes.EQ) {
            builder.advanceLexer()
            parseExpression(builder)
        }
        
        marker.done(SurqlElementTypes.OBJECT_ENTRY)
    }
    
    private fun parseArrayLiteral(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LBRACKET)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType != SurqlTokenTypes.RBRACKET) {
            parseExpression(builder)
            
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.RBRACKET) break
                    parseExpression(builder)
                } else {
                    break
                }
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RBRACKET) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.ARRAY_LITERAL)
    }
    
    private fun parseIndexAccess(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.LBRACKET)
        
        skipWhitespaceAndComments(builder)
        
        // Could be: [index], [WHERE ...], [$], [*], [?condition]
        when (builder.tokenType) {
            SurqlTokenTypes.WHERE -> {
                builder.advanceLexer()
                parseExpression(builder)
            }
            SurqlTokenTypes.STAR -> {
                builder.advanceLexer()
            }
            SurqlTokenTypes.QUESTION -> {
                builder.advanceLexer()
                parseExpression(builder)
            }
            SurqlTokenTypes.PARAMETER -> {
                if (builder.tokenText == "$") {
                    builder.advanceLexer()
                } else {
                    parseExpression(builder)
                }
            }
            else -> {
                parseExpression(builder)
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RBRACKET) {
            builder.advanceLexer()
        }
    }
    
    private fun parseArgumentList(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LPAREN)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType != SurqlTokenTypes.RPAREN) {
            parseExpression(builder)
            
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.RPAREN) break
                    parseExpression(builder)
                } else {
                    break
                }
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RPAREN) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.ARGUMENT_LIST)
    }
    
    // ============================================================================
    // Type Expression Parsing
    // ============================================================================
    
    private fun parseTypeExpression(builder: PsiBuilder) {
        val marker = builder.mark()
        parseTypeUnion(builder)
        marker.done(SurqlElementTypes.TYPE_EXPRESSION)
    }
    
    private fun parseTypeUnion(builder: PsiBuilder) {
        parseTypePrimary(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.PIPE) {
                builder.advanceLexer()
                parseTypePrimary(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseTypePrimary(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        when (builder.tokenType) {
            // Simple types
            SurqlTokenTypes.BOOL, SurqlTokenTypes.INT, SurqlTokenTypes.FLOAT,
            SurqlTokenTypes.DECIMAL, SurqlTokenTypes.DATETIME, SurqlTokenTypes.STRING_TYPE,
            SurqlTokenTypes.OBJECT, SurqlTokenTypes.BYTES, SurqlTokenTypes.UUID,
            SurqlTokenTypes.ANY, SurqlTokenTypes.NULL, SurqlTokenTypes.NONE -> {
                builder.advanceLexer()
            }
            
            // Generic types
            SurqlTokenTypes.ARRAY, SurqlTokenTypes.SET_TYPE, SurqlTokenTypes.OPTION_TYPE -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    parseTypeExpression(builder)
                    skipWhitespaceAndComments(builder)
                    
                    // Optional size for array
                    if (builder.tokenType == SurqlTokenTypes.COMMA) {
                        builder.advanceLexer()
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.NUMBER) {
                            builder.advanceLexer()
                        }
                    }
                    
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            // Record type
            SurqlTokenTypes.RECORD -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    // Table names
                    parseName(builder)
                    while (true) {
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.PIPE) {
                            builder.advanceLexer()
                            parseName(builder)
                        } else {
                            break
                        }
                    }
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            // Geometry types
            SurqlTokenTypes.GEOMETRY -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    // Geometry subtypes
                    parseName(builder)
                    while (true) {
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.PIPE) {
                            builder.advanceLexer()
                            parseName(builder)
                        } else {
                            break
                        }
                    }
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            SurqlTokenTypes.POINT, SurqlTokenTypes.LINE, SurqlTokenTypes.POLYGON,
            SurqlTokenTypes.MULTIPOINT, SurqlTokenTypes.MULTILINE, 
            SurqlTokenTypes.MULTIPOLYGON, SurqlTokenTypes.COLLECTION -> {
                builder.advanceLexer()
            }
            
            // Either type
            SurqlTokenTypes.EITHER -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    parseTypeExpression(builder)
                    while (true) {
                        skipWhitespaceAndComments(builder)
                        if (builder.tokenType == SurqlTokenTypes.COMMA) {
                            builder.advanceLexer()
                            parseTypeExpression(builder)
                        } else {
                            break
                        }
                    }
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            // Literal type
            SurqlTokenTypes.LITERAL -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    parseExpression(builder) // literal value
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            // Range type
            SurqlTokenTypes.RANGE -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LT) {
                    builder.advanceLexer()
                    parseTypeExpression(builder)
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.GT) {
                        builder.advanceLexer()
                    }
                }
            }
            
            // Future type
            SurqlTokenTypes.FUTURE -> {
                builder.advanceLexer()
            }
            
            // Identifier (custom type or table name for record)
            SurqlTokenTypes.IDENTIFIER -> {
                builder.advanceLexer()
            }
            
            else -> {
                // Unknown type
                if (builder.tokenType != null) {
                    builder.advanceLexer()
                }
            }
        }
    }
    
    // ============================================================================
    // Clause Parsing
    // ============================================================================
    
    private fun parseOptionalClauses(builder: PsiBuilder, allowedClauses: TokenSet) {
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            
            val tokenType = builder.tokenType ?: break
            
            if (tokenType !in allowedClauses) break
            
            when (tokenType) {
                SurqlTokenTypes.WHERE -> parseWhereClause(builder)
                SurqlTokenTypes.ORDER -> parseOrderClause(builder)
                SurqlTokenTypes.LIMIT -> parseLimitClause(builder)
                SurqlTokenTypes.START -> parseStartClause(builder)
                SurqlTokenTypes.GROUP -> parseGroupClause(builder)
                SurqlTokenTypes.SPLIT -> parseSplitClause(builder)
                SurqlTokenTypes.FETCH -> parseFetchClause(builder)
                SurqlTokenTypes.TIMEOUT -> parseTimeoutClause(builder)
                SurqlTokenTypes.PARALLEL -> builder.advanceLexer()
                SurqlTokenTypes.EXPLAIN -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.FULL) {
                        builder.advanceLexer()
                    }
                }
                SurqlTokenTypes.SET -> parseSetClause(builder)
                SurqlTokenTypes.UNSET -> parseUnsetClause(builder)
                SurqlTokenTypes.CONTENT -> parseContentClause(builder)
                SurqlTokenTypes.MERGE -> parseMergeClause(builder)
                SurqlTokenTypes.PATCH -> parsePatchClause(builder)
                SurqlTokenTypes.REPLACE -> parseReplaceClause(builder)
                SurqlTokenTypes.RETURN -> parseReturnClause(builder)
                SurqlTokenTypes.VERSION -> parseVersionClause(builder)
                SurqlTokenTypes.WITH -> parseWithClause(builder)
                SurqlTokenTypes.OMIT -> parseOmitClause(builder)
                SurqlTokenTypes.ON -> parseOnDuplicateClause(builder)
                else -> break
            }
        }
    }
    
    private fun parseWhereClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.WHERE)
        parseExpression(builder)
        marker.done(SurqlElementTypes.WHERE_CLAUSE)
    }
    
    private fun parseOrderClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.ORDER)
        
        // Optional BY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.BY) {
            builder.advanceLexer()
        }
        
        // Order fields
        parseOrderField(builder)
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseOrderField(builder)
            } else {
                break
            }
        }
        
        marker.done(SurqlElementTypes.ORDER_CLAUSE)
    }
    
    private fun parseOrderField(builder: PsiBuilder) {
        parseExpression(builder)
        
        // Optional ASC/DESC
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.ASC, SurqlTokenTypes.DESC -> builder.advanceLexer()
            else -> {}
        }
        
        // Optional COLLATE
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.COLLATE) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.NUMERIC) {
                builder.advanceLexer()
            }
        }
        
        // Optional NUMERIC
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.NUMERIC) {
            builder.advanceLexer()
        }
    }
    
    private fun parseLimitClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LIMIT)
        
        // Optional BY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.BY) {
            builder.advanceLexer()
        }
        
        parseExpression(builder)
        marker.done(SurqlElementTypes.LIMIT_CLAUSE)
    }
    
    private fun parseStartClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.START)
        
        // Optional AT
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.AT_KW) {
            builder.advanceLexer()
        }
        
        parseExpression(builder)
        marker.done(SurqlElementTypes.LIMIT_CLAUSE)
    }
    
    private fun parseGroupClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.GROUP)
        
        // Optional BY
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.BY) {
            builder.advanceLexer()
        }
        
        // ALL or field list
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ALL) {
            builder.advanceLexer()
        } else {
            parseFieldList(builder)
        }
        
        marker.done(SurqlElementTypes.GROUP_CLAUSE)
    }
    
    private fun parseSplitClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.SPLIT)
        
        // Optional ON
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.ON) {
            builder.advanceLexer()
        }
        
        parseFieldList(builder)
        marker.done(SurqlElementTypes.SPLIT_CLAUSE)
    }
    
    private fun parseFetchClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.FETCH)
        parseFieldList(builder)
        marker.done(SurqlElementTypes.FETCH_CLAUSE)
    }
    
    private fun parseTimeoutClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.TIMEOUT)
        parseExpression(builder) // duration
        marker.done(SurqlElementTypes.TIMEOUT_CLAUSE)
    }
    
    private fun parseSetClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.SET)
        parseAssignmentList(builder)
        marker.done(SurqlElementTypes.SET_CLAUSE)
    }
    
    private fun parseUnsetClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.UNSET)
        parseFieldList(builder)
        marker.done(SurqlElementTypes.SET_CLAUSE)
    }
    
    private fun parseContentClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.CONTENT)
        parseExpression(builder)
        marker.done(SurqlElementTypes.CONTENT_CLAUSE)
    }
    
    private fun parseMergeClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.MERGE)
        parseExpression(builder)
        marker.done(SurqlElementTypes.MERGE_CLAUSE)
    }
    
    private fun parsePatchClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.PATCH)
        parseExpression(builder)
        marker.done(SurqlElementTypes.MERGE_CLAUSE)
    }
    
    private fun parseReplaceClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.REPLACE)
        parseExpression(builder)
        marker.done(SurqlElementTypes.CONTENT_CLAUSE)
    }
    
    private fun parseReturnClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.RETURN)
        
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.NONE, SurqlTokenTypes.NULL,
            SurqlTokenTypes.BEFORE, SurqlTokenTypes.AFTER,
            SurqlTokenTypes.DIFF -> builder.advanceLexer()
            else -> {
                // Could be field list or expression
                if (isExpressionStart(builder.tokenType)) {
                    parseExpressionList(builder)
                }
            }
        }
        
        marker.done(SurqlElementTypes.CONTENT_CLAUSE)
    }
    
    private fun parseVersionClause(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.VERSION)
        parseExpression(builder) // datetime
    }
    
    private fun parseWithClause(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.WITH)
        
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.NOINDEX -> builder.advanceLexer()
            SurqlTokenTypes.INDEX -> {
                builder.advanceLexer()
                parseIdentifierList(builder)
            }
            else -> {}
        }
    }
    
    private fun parseOmitClause(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.OMIT)
        parseFieldList(builder)
    }
    
    private fun parseOnDuplicateClause(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.ON)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.DUPLICATE) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.KEY) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.UPDATE) {
                    builder.advanceLexer()
                    parseAssignmentList(builder)
                }
            }
        }
    }
    
    private fun parsePermissionsClause(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.PERMISSIONS)
        
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.NONE, SurqlTokenTypes.FULL -> builder.advanceLexer()
            SurqlTokenTypes.FOR -> {
                // Parse permission rules
                while (builder.tokenType == SurqlTokenTypes.FOR) {
                    builder.advanceLexer()
                    
                    // Action: select, create, update, delete
                    skipWhitespaceAndComments(builder)
                    when (builder.tokenType) {
                        SurqlTokenTypes.SELECT, SurqlTokenTypes.CREATE,
                        SurqlTokenTypes.UPDATE, SurqlTokenTypes.DELETE -> {
                            builder.advanceLexer()
                            
                            // Additional actions separated by comma
                            while (true) {
                                skipWhitespaceAndComments(builder)
                                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                                    builder.advanceLexer()
                                    skipWhitespaceAndComments(builder)
                                    builder.advanceLexer()
                                } else {
                                    break
                                }
                            }
                        }
                        else -> {}
                    }
                    
                    // Permission value
                    skipWhitespaceAndComments(builder)
                    when (builder.tokenType) {
                        SurqlTokenTypes.NONE, SurqlTokenTypes.FULL -> builder.advanceLexer()
                        SurqlTokenTypes.WHERE -> {
                            builder.advanceLexer()
                            parseExpression(builder)
                        }
                        else -> {}
                    }
                    
                    skipWhitespaceAndComments(builder)
                }
            }
            else -> {}
        }
        
        marker.done(SurqlElementTypes.PERMISSIONS_CLAUSE)
    }
    
    private fun parsePermissionsClauseIfPresent(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.PERMISSIONS) {
            parsePermissionsClause(builder)
        }
    }
    
    // ============================================================================
    // Helper Parsing Methods
    // ============================================================================
    
    private fun parseTableReference(builder: PsiBuilder) {
        val marker = builder.mark()
        
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.IDENTIFIER -> {
                builder.advanceLexer()
                
                // Check for record ID
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COLON) {
                    builder.advanceLexer()
                    parseRecordIdValue(builder)
                    marker.done(SurqlElementTypes.RECORD_ID_EXPR)
                    return
                }
            }
            SurqlTokenTypes.RECORD_ID, SurqlTokenTypes.RECORD_STRING -> {
                builder.advanceLexer()
            }
            SurqlTokenTypes.PARAMETER -> {
                builder.advanceLexer()
            }
            SurqlTokenTypes.LPAREN -> {
                parseParenthesizedOrSubquery(builder)
                marker.drop()
                return
            }
            SurqlTokenTypes.LBRACKET -> {
                parseArrayLiteral(builder)
                marker.drop()
                return
            }
            else -> {
                // Try to parse as expression
                if (isExpressionStart(builder.tokenType)) {
                    parseExpression(builder)
                    marker.drop()
                    return
                }
            }
        }
        
        marker.done(SurqlElementTypes.TABLE_NAME)
    }
    
    private fun parseTableReferenceList(builder: PsiBuilder) {
        parseTableReference(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseTableReference(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseTableName(builder: PsiBuilder) {
        val marker = builder.mark()
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IDENTIFIER) {
            builder.advanceLexer()
        } else if (builder.tokenType in SurqlTokenTypes.KEYWORDS) {
            // Some keywords can be table names
            builder.advanceLexer()
        }
        marker.done(SurqlElementTypes.TABLE_NAME)
    }
    
    private fun parseName(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IDENTIFIER) {
            builder.advanceLexer()
        } else if (builder.tokenType in SurqlTokenTypes.KEYWORDS) {
            builder.advanceLexer()
        } else if (builder.tokenType == SurqlTokenTypes.STRING) {
            builder.advanceLexer()
        }
    }
    
    private fun parseFunctionName(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        // Could be fn::name or just identifier
        if (builder.tokenType == SurqlTokenTypes.IDENTIFIER) {
            builder.advanceLexer()
            
            // Check for namespace
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COLONCOLON) {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.IDENTIFIER ||
                        builder.tokenType in SurqlTokenTypes.KEYWORDS) {
                        builder.advanceLexer()
                    }
                } else {
                    break
                }
            }
        }
    }
    
    private fun parseFieldPath(builder: PsiBuilder) {
        parseName(builder)
        
        // Handle nested fields: foo.bar.baz
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.DOT) {
                builder.advanceLexer()
                parseName(builder)
            } else if (builder.tokenType == SurqlTokenTypes.LBRACKET) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.STAR) {
                    builder.advanceLexer()
                } else {
                    parseExpression(builder)
                }
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.RBRACKET) {
                    builder.advanceLexer()
                }
            } else {
                break
            }
        }
    }
    
    private fun parseFieldList(builder: PsiBuilder) {
        parseFieldPath(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseFieldPath(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseIdentifierList(builder: PsiBuilder) {
        parseName(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseName(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseExpressionList(builder: PsiBuilder) {
        parseAliasedExpression(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (isStatementEnd(builder)) break
                parseAliasedExpression(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseAliasedExpression(builder: PsiBuilder) {
        parseExpression(builder)
        
        // Optional AS alias
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.AS) {
            builder.advanceLexer()
            parseName(builder)
        }
    }
    
    private fun parseAssignmentList(builder: PsiBuilder) {
        parseAssignment(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseAssignment(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseAssignment(builder: PsiBuilder) {
        val marker = builder.mark()
        
        // Field path
        parseFieldPath(builder)
        
        // Operator (=, +=, -=, etc.)
        skipWhitespaceAndComments(builder)
        when (builder.tokenType) {
            SurqlTokenTypes.EQ, SurqlTokenTypes.PLUSEQ,
            SurqlTokenTypes.MINUSEQ, SurqlTokenTypes.STAREQ,
            SurqlTokenTypes.SLASHEQ -> {
                builder.advanceLexer()
                parseExpression(builder)
            }
            else -> {}
        }
        
        marker.done(SurqlElementTypes.ASSIGNMENT)
    }
    
    private fun parseBlock(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.LBRACE)
        
        while (!builder.eof()) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.RBRACE) {
                break
            }
            parseStatement(builder)
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RBRACE) {
            builder.advanceLexer()
        }
    }
    
    private fun parseParameterList(builder: PsiBuilder) {
        val marker = builder.mark()
        expect(builder, SurqlTokenTypes.LPAREN)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType != SurqlTokenTypes.RPAREN) {
            parseParameterDeclaration(builder)
            
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                    builder.advanceLexer()
                    parseParameterDeclaration(builder)
                } else {
                    break
                }
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RPAREN) {
            builder.advanceLexer()
        }
        
        marker.done(SurqlElementTypes.PARAMETER_LIST)
    }
    
    private fun parseParameterDeclaration(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        // Parameter name
        if (builder.tokenType == SurqlTokenTypes.PARAMETER) {
            builder.advanceLexer()
        } else {
            parseName(builder)
        }
        
        // Optional type annotation
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.COLON) {
            builder.advanceLexer()
            parseTypeExpression(builder)
        }
    }
    
    private fun parseParenthesizedList(builder: PsiBuilder) {
        expect(builder, SurqlTokenTypes.LPAREN)
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType != SurqlTokenTypes.RPAREN) {
            parseName(builder)
            
            while (true) {
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.COMMA) {
                    builder.advanceLexer()
                    parseName(builder)
                } else {
                    break
                }
            }
        }
        
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.RPAREN) {
            builder.advanceLexer()
        }
    }
    
    private fun parseValuesList(builder: PsiBuilder) {
        parseValuesRow(builder)
        
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                    parseValuesRow(builder)
                } else {
                    break
                }
            } else {
                break
            }
        }
    }
    
    private fun parseValuesRow(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.LPAREN) {
            builder.advanceLexer()
            
            skipWhitespaceAndComments(builder)
            if (builder.tokenType != SurqlTokenTypes.RPAREN) {
                parseExpression(builder)
                
                while (true) {
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.COMMA) {
                        builder.advanceLexer()
                        parseExpression(builder)
                    } else {
                        break
                    }
                }
            }
            
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.RPAREN) {
                builder.advanceLexer()
            }
        }
    }
    
    private fun parseTokenizerList(builder: PsiBuilder) {
        parseName(builder)
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseName(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseFilterList(builder: PsiBuilder) {
        parseFilter(builder)
        while (true) {
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.COMMA) {
                builder.advanceLexer()
                parseFilter(builder)
            } else {
                break
            }
        }
    }
    
    private fun parseFilter(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        
        // Filter name
        when (builder.tokenType) {
            SurqlTokenTypes.ASCII, SurqlTokenTypes.EDGENGRAM,
            SurqlTokenTypes.LOWERCASE, SurqlTokenTypes.UPPERCASE,
            SurqlTokenTypes.SNOWBALL -> {
                builder.advanceLexer()
                
                // Optional parameters
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                    parseArgumentList(builder)
                }
            }
            SurqlTokenTypes.IDENTIFIER -> {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                    parseArgumentList(builder)
                }
            }
            else -> {}
        }
    }
    
    private fun parseSearchIndexOptions(builder: PsiBuilder) {
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.ANALYZER_KW, SurqlTokenTypes.ANALYZER -> {
                    builder.advanceLexer()
                    parseName(builder)
                }
                SurqlTokenTypes.BM25 -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.LPAREN) {
                        parseArgumentList(builder)
                    }
                }
                SurqlTokenTypes.HIGHLIGHTS -> builder.advanceLexer()
                else -> break
            }
        }
    }
    
    private fun parseMTreeOptions(builder: PsiBuilder) {
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.DIMENSION -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.DIST -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    builder.advanceLexer() // distance function
                }
                SurqlTokenTypes.TYPE -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    builder.advanceLexer() // type
                }
                SurqlTokenTypes.CAPACITY -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
    }
    
    private fun parseHnswOptions(builder: PsiBuilder) {
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.DIMENSION -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.DIST -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    builder.advanceLexer() // distance function
                }
                SurqlTokenTypes.TYPE -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    builder.advanceLexer() // type
                }
                SurqlTokenTypes.EFC -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.M -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                else -> break
            }
        }
    }
    
    private fun parseJwtAccess(builder: PsiBuilder) {
        builder.advanceLexer() // JWT
        consumeUntilStatementEnd(builder)
    }
    
    private fun parseRecordAccess(builder: PsiBuilder) {
        builder.advanceLexer() // RECORD
        
        while (!builder.eof() && !isStatementEnd(builder)) {
            skipWhitespaceAndComments(builder)
            when (builder.tokenType) {
                SurqlTokenTypes.SIGNUP -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.SIGNIN -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.AUTHENTICATE -> {
                    builder.advanceLexer()
                    parseExpression(builder)
                }
                SurqlTokenTypes.WITH -> {
                    builder.advanceLexer()
                    skipWhitespaceAndComments(builder)
                    if (builder.tokenType == SurqlTokenTypes.JWT) {
                        builder.advanceLexer()
                    }
                }
                SurqlTokenTypes.DURATION -> {
                    builder.advanceLexer()
                    consumeUntilStatementEnd(builder)
                    return
                }
                else -> break
            }
        }
    }
    
    private fun parseBearerAccess(builder: PsiBuilder) {
        builder.advanceLexer() // BEARER
        consumeUntilStatementEnd(builder)
    }
    
    private fun parseOptionalOverwrite(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.OVERWRITE) {
            builder.advanceLexer()
        }
    }
    
    private fun parseOptionalIfNotExists(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IF_KW || builder.tokenType == SurqlTokenTypes.IF) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.NOT) {
                builder.advanceLexer()
                skipWhitespaceAndComments(builder)
                if (builder.tokenType == SurqlTokenTypes.EXISTS) {
                    builder.advanceLexer()
                }
            }
        }
    }
    
    private fun parseOptionalIfExists(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.IF_KW || builder.tokenType == SurqlTokenTypes.IF) {
            builder.advanceLexer()
            skipWhitespaceAndComments(builder)
            if (builder.tokenType == SurqlTokenTypes.EXISTS) {
                builder.advanceLexer()
            }
        }
    }
    
    private fun parseOptionalComment(builder: PsiBuilder) {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == SurqlTokenTypes.COMMENT) {
            builder.advanceLexer()
            parseExpression(builder)
        }
    }
    
    // ============================================================================
    // Utility Methods
    // ============================================================================
    
    private fun skipWhitespaceAndComments(builder: PsiBuilder) {
        while (builder.tokenType == SurqlTokenTypes.WHITE_SPACE ||
               builder.tokenType == SurqlTokenTypes.LINE_COMMENT ||
               builder.tokenType == SurqlTokenTypes.BLOCK_COMMENT) {
            builder.advanceLexer()
        }
    }
    
    private fun expect(builder: PsiBuilder, tokenType: IElementType): Boolean {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == tokenType) {
            builder.advanceLexer()
            return true
        }
        return false
    }
    
    /**
     * Expect a token with error reporting if not found.
     * @return true if token was found, false otherwise
     */
    private fun expectWithError(builder: PsiBuilder, tokenType: IElementType, errorMessage: String): Boolean {
        skipWhitespaceAndComments(builder)
        if (builder.tokenType == tokenType) {
            builder.advanceLexer()
            return true
        }
        builder.error(errorMessage)
        return false
    }
    
    /**
     * Try to recover from an error by skipping to a synchronization point.
     * Returns true if recovery was possible, false if we hit EOF.
     */
    private fun recoverToSyncPoint(builder: PsiBuilder): Boolean {
        var depth = 0
        while (!builder.eof()) {
            when (builder.tokenType) {
                SurqlTokenTypes.LBRACE, SurqlTokenTypes.LPAREN, SurqlTokenTypes.LBRACKET -> depth++
                SurqlTokenTypes.RBRACE -> {
                    if (depth > 0) depth--
                    else return true
                }
                SurqlTokenTypes.RPAREN, SurqlTokenTypes.RBRACKET -> {
                    if (depth > 0) depth--
                }
                SurqlTokenTypes.SEMICOLON -> {
                    if (depth == 0) {
                        builder.advanceLexer()
                        return true
                    }
                }
                in STATEMENT_START_TOKENS -> {
                    if (depth == 0) return true
                }
                null -> return false
            }
            builder.advanceLexer()
        }
        return false
    }
    
    private fun isStatementEnd(builder: PsiBuilder): Boolean {
        val token = builder.tokenType
        return token == null ||
               token == SurqlTokenTypes.SEMICOLON ||
               token == SurqlTokenTypes.RBRACE ||
               token in STATEMENT_START_TOKENS
    }
    
    private fun isExpressionStart(tokenType: IElementType?): Boolean {
        if (tokenType == null) return false
        return tokenType == SurqlTokenTypes.IDENTIFIER ||
               tokenType == SurqlTokenTypes.PARAMETER ||
               tokenType == SurqlTokenTypes.NUMBER ||
               tokenType == SurqlTokenTypes.STRING ||
               tokenType == SurqlTokenTypes.TRUE ||
               tokenType == SurqlTokenTypes.FALSE ||
               tokenType == SurqlTokenTypes.NULL ||
               tokenType == SurqlTokenTypes.NONE ||
               tokenType == SurqlTokenTypes.LPAREN ||
               tokenType == SurqlTokenTypes.LBRACE ||
               tokenType == SurqlTokenTypes.LBRACKET ||
               tokenType == SurqlTokenTypes.MINUS ||
               tokenType == SurqlTokenTypes.PLUS ||
               tokenType == SurqlTokenTypes.NOT ||
               tokenType == SurqlTokenTypes.TILDE ||
               tokenType == SurqlTokenTypes.LT ||
               tokenType == SurqlTokenTypes.RECORD_ID ||
               tokenType == SurqlTokenTypes.RECORD_STRING ||
               tokenType == SurqlTokenTypes.DATETIME_STRING ||
               tokenType == SurqlTokenTypes.UUID_STRING ||
               tokenType == SurqlTokenTypes.FUTURE ||
               tokenType in SurqlTokenTypes.TYPE_KEYWORDS
    }
    
    private fun consumeUntilStatementEnd(builder: PsiBuilder) {
        var braceDepth = 0
        var parenDepth = 0
        var bracketDepth = 0
        
        while (!builder.eof()) {
            when (builder.tokenType) {
                SurqlTokenTypes.LBRACE -> braceDepth++
                SurqlTokenTypes.RBRACE -> {
                    if (braceDepth > 0) braceDepth--
                    else return
                }
                SurqlTokenTypes.LPAREN -> parenDepth++
                SurqlTokenTypes.RPAREN -> {
                    if (parenDepth > 0) parenDepth--
                }
                SurqlTokenTypes.LBRACKET -> bracketDepth++
                SurqlTokenTypes.RBRACKET -> {
                    if (bracketDepth > 0) bracketDepth--
                }
                SurqlTokenTypes.SEMICOLON -> {
                    if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                        return
                    }
                }
                in STATEMENT_START_TOKENS -> {
                    if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                        return
                    }
                }
                null -> return
                else -> {}
            }
            builder.advanceLexer()
        }
    }
    
    companion object {
        // Token sets for clause parsing
        private val QUERY_CLAUSES = TokenSet.create(
            SurqlTokenTypes.WHERE,
            SurqlTokenTypes.ORDER,
            SurqlTokenTypes.LIMIT,
            SurqlTokenTypes.START,
            SurqlTokenTypes.GROUP,
            SurqlTokenTypes.SPLIT,
            SurqlTokenTypes.FETCH,
            SurqlTokenTypes.TIMEOUT,
            SurqlTokenTypes.PARALLEL,
            SurqlTokenTypes.EXPLAIN,
            SurqlTokenTypes.WITH,
            SurqlTokenTypes.OMIT,
            SurqlTokenTypes.VERSION
        )
        
        private val MUTATION_CLAUSES = TokenSet.create(
            SurqlTokenTypes.WHERE,
            SurqlTokenTypes.SET,
            SurqlTokenTypes.UNSET,
            SurqlTokenTypes.CONTENT,
            SurqlTokenTypes.MERGE,
            SurqlTokenTypes.PATCH,
            SurqlTokenTypes.REPLACE,
            SurqlTokenTypes.RETURN,
            SurqlTokenTypes.TIMEOUT,
            SurqlTokenTypes.PARALLEL
        )
        
        private val DELETE_CLAUSES = TokenSet.create(
            SurqlTokenTypes.WHERE,
            SurqlTokenTypes.RETURN,
            SurqlTokenTypes.TIMEOUT,
            SurqlTokenTypes.PARALLEL
        )
        
        private val INSERT_CLAUSES = TokenSet.create(
            SurqlTokenTypes.ON,
            SurqlTokenTypes.RETURN,
            SurqlTokenTypes.TIMEOUT,
            SurqlTokenTypes.PARALLEL
        )
        
        private val STATEMENT_START_TOKENS = TokenSet.create(
            SurqlTokenTypes.SELECT,
            SurqlTokenTypes.CREATE,
            SurqlTokenTypes.UPDATE,
            SurqlTokenTypes.DELETE,
            SurqlTokenTypes.INSERT,
            SurqlTokenTypes.UPSERT,
            SurqlTokenTypes.RELATE,
            SurqlTokenTypes.DEFINE,
            SurqlTokenTypes.REMOVE,
            SurqlTokenTypes.ALTER,
            SurqlTokenTypes.LET,
            SurqlTokenTypes.IF,
            SurqlTokenTypes.FOR,
            SurqlTokenTypes.RETURN,
            SurqlTokenTypes.THROW,
            SurqlTokenTypes.BEGIN,
            SurqlTokenTypes.COMMIT,
            SurqlTokenTypes.CANCEL,
            SurqlTokenTypes.USE,
            SurqlTokenTypes.INFO,
            SurqlTokenTypes.LIVE,
            SurqlTokenTypes.KILL,
            SurqlTokenTypes.BREAK,
            SurqlTokenTypes.CONTINUE,
            SurqlTokenTypes.SLEEP,
            SurqlTokenTypes.SHOW,
            SurqlTokenTypes.REBUILD,
            SurqlTokenTypes.OPTION
        )
    }
}
