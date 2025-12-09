package org.surrealdb.surql.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class SurqlLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentPosition: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentToken: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentPosition = startOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentToken

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = currentPosition
        
        if (currentPosition >= endOffset) {
            currentToken = null
            return
        }

        val c = buffer[currentPosition]

        currentToken = when {
            c.isWhitespace() -> readWhitespace()
            c == '-' && peek(1) == '-' -> readLineComment()
            c == '/' && peek(1) == '/' -> readLineComment()
            c == '#' -> readLineComment()
            c == '/' && peek(1) == '*' -> readBlockComment()
            c == '"' -> readDoubleQuotedString()
            c == '\'' -> readSingleQuotedString()
            c == '`' -> readBacktickIdentifier()
            c == '$' -> readParameter()
            c == 'd' && (peek(1) == '"' || peek(1) == '\'') -> readPrefixedString(SurqlTokenTypes.DATETIME_STRING)
            c == 'u' && (peek(1) == '"' || peek(1) == '\'') -> readPrefixedString(SurqlTokenTypes.UUID_STRING)
            c == 'r' && (peek(1) == '"' || peek(1) == '\'') -> readPrefixedString(SurqlTokenTypes.RECORD_STRING)
            c.isDigit() -> readNumber()
            c.isLetter() || c == '_' -> readIdentifierOrKeyword()
            else -> readOperatorOrPunctuation()
        }

        tokenEnd = currentPosition
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun peek(offset: Int = 0): Char? {
        val pos = currentPosition + offset
        return if (pos < endOffset) buffer[pos] else null
    }

    private fun readWhitespace(): IElementType {
        while (currentPosition < endOffset && buffer[currentPosition].isWhitespace()) {
            currentPosition++
        }
        return SurqlTokenTypes.WHITE_SPACE
    }

    private fun readLineComment(): IElementType {
        while (currentPosition < endOffset && buffer[currentPosition] != '\n') {
            currentPosition++
        }
        return SurqlTokenTypes.LINE_COMMENT
    }

    private fun readBlockComment(): IElementType {
        currentPosition += 2 // skip /*
        while (currentPosition < endOffset - 1) {
            if (buffer[currentPosition] == '*' && buffer[currentPosition + 1] == '/') {
                currentPosition += 2
                break
            }
            currentPosition++
        }
        if (currentPosition >= endOffset - 1) {
            currentPosition = endOffset
        }
        return SurqlTokenTypes.BLOCK_COMMENT
    }

    private fun readDoubleQuotedString(): IElementType {
        currentPosition++ // skip opening "
        while (currentPosition < endOffset) {
            val c = buffer[currentPosition]
            if (c == '"') {
                currentPosition++
                break
            }
            if (c == '\\' && currentPosition + 1 < endOffset) {
                currentPosition += 2 // skip escaped char
            } else {
                currentPosition++
            }
        }
        return SurqlTokenTypes.STRING
    }

    private fun readSingleQuotedString(): IElementType {
        currentPosition++ // skip opening '
        while (currentPosition < endOffset) {
            val c = buffer[currentPosition]
            if (c == '\'') {
                currentPosition++
                break
            }
            if (c == '\\' && currentPosition + 1 < endOffset) {
                currentPosition += 2 // skip escaped char
            } else {
                currentPosition++
            }
        }
        return SurqlTokenTypes.STRING
    }

    private fun readBacktickIdentifier(): IElementType {
        currentPosition++ // skip opening `
        while (currentPosition < endOffset && buffer[currentPosition] != '`') {
            currentPosition++
        }
        if (currentPosition < endOffset) {
            currentPosition++ // skip closing `
        }
        return SurqlTokenTypes.IDENTIFIER
    }

    private fun readParameter(): IElementType {
        currentPosition++ // skip $
        while (currentPosition < endOffset && (buffer[currentPosition].isLetterOrDigit() || buffer[currentPosition] == '_')) {
            currentPosition++
        }
        return SurqlTokenTypes.PARAMETER
    }

    private fun readPrefixedString(tokenType: IElementType): IElementType {
        currentPosition++ // skip prefix (d, u, r)
        val quote = buffer[currentPosition]
        currentPosition++ // skip opening quote
        while (currentPosition < endOffset) {
            val c = buffer[currentPosition]
            if (c == quote) {
                currentPosition++
                break
            }
            if (c == '\\' && currentPosition + 1 < endOffset) {
                currentPosition += 2
            } else {
                currentPosition++
            }
        }
        return tokenType
    }

    private fun readNumber(): IElementType {
        // Handle hex, octal, binary
        if (buffer[currentPosition] == '0' && currentPosition + 1 < endOffset) {
            when (buffer[currentPosition + 1].lowercaseChar()) {
                'x' -> {
                    currentPosition += 2
                    while (currentPosition < endOffset && buffer[currentPosition].isHexDigit()) {
                        currentPosition++
                    }
                    return SurqlTokenTypes.NUMBER
                }
                'o' -> {
                    currentPosition += 2
                    while (currentPosition < endOffset && buffer[currentPosition] in '0'..'7') {
                        currentPosition++
                    }
                    return SurqlTokenTypes.NUMBER
                }
                'b' -> {
                    currentPosition += 2
                    while (currentPosition < endOffset && buffer[currentPosition] in '0'..'1') {
                        currentPosition++
                    }
                    return SurqlTokenTypes.NUMBER
                }
            }
        }

        // Regular number with optional decimal and exponent
        while (currentPosition < endOffset && (buffer[currentPosition].isDigit() || buffer[currentPosition] == '_')) {
            currentPosition++
        }
        
        // Check for duration literal (e.g., 1y, 1w, 1d, 1h, 1m, 1s, 1ms, 1us, 1ns)
        // Duration can have multiple parts: 1h30m, 1d12h, etc.
        if (currentPosition < endOffset && isDurationSuffix(currentPosition)) {
            return readDurationLiteral()
        }
        
        // Decimal part
        if (currentPosition < endOffset && buffer[currentPosition] == '.' && 
            currentPosition + 1 < endOffset && buffer[currentPosition + 1].isDigit()) {
            currentPosition++ // skip .
            while (currentPosition < endOffset && (buffer[currentPosition].isDigit() || buffer[currentPosition] == '_')) {
                currentPosition++
            }
        }
        
        // Exponent
        if (currentPosition < endOffset && (buffer[currentPosition] == 'e' || buffer[currentPosition] == 'E')) {
            currentPosition++
            if (currentPosition < endOffset && (buffer[currentPosition] == '+' || buffer[currentPosition] == '-')) {
                currentPosition++
            }
            while (currentPosition < endOffset && buffer[currentPosition].isDigit()) {
                currentPosition++
            }
        }
        
        // Type suffix (dec, f)
        if (currentPosition < endOffset) {
            val remaining = buffer.substring(currentPosition, minOf(currentPosition + 3, endOffset))
            if (remaining.startsWith("dec", ignoreCase = true)) {
                currentPosition += 3
            } else if (buffer[currentPosition] == 'f' || buffer[currentPosition] == 'F') {
                currentPosition++
            }
        }

        return SurqlTokenTypes.NUMBER
    }
    
    /**
     * Check if the current position starts a duration suffix.
     */
    private fun isDurationSuffix(pos: Int): Boolean {
        if (pos >= endOffset) return false
        val c = buffer[pos].lowercaseChar()
        
        // Single-char duration suffixes: y, w, d, h, m, s
        // Two-char: ms, us, ns, µs
        return when (c) {
            'y', 'w', 'd', 'h', 's' -> true
            'm' -> {
                // Could be 'm' (minutes) or 'ms' (milliseconds)
                true
            }
            'u', 'µ' -> {
                // us or µs (microseconds)
                pos + 1 < endOffset && buffer[pos + 1].lowercaseChar() == 's'
            }
            'n' -> {
                // ns (nanoseconds)
                pos + 1 < endOffset && buffer[pos + 1].lowercaseChar() == 's'
            }
            else -> false
        }
    }
    
    /**
     * Read a duration literal like 1h30m, 7d, 100ms, etc.
     */
    private fun readDurationLiteral(): IElementType {
        // Continue reading duration parts: number + suffix
        while (currentPosition < endOffset) {
            // Read duration suffix
            val c = buffer[currentPosition].lowercaseChar()
            when (c) {
                'y', 'w', 'd', 'h' -> {
                    currentPosition++
                }
                'm' -> {
                    currentPosition++
                    // Check for 'ms' (milliseconds)
                    if (currentPosition < endOffset && buffer[currentPosition].lowercaseChar() == 's') {
                        currentPosition++
                    }
                }
                's' -> {
                    currentPosition++
                }
                'u', 'µ' -> {
                    currentPosition++
                    if (currentPosition < endOffset && buffer[currentPosition].lowercaseChar() == 's') {
                        currentPosition++
                    }
                }
                'n' -> {
                    currentPosition++
                    if (currentPosition < endOffset && buffer[currentPosition].lowercaseChar() == 's') {
                        currentPosition++
                    }
                }
                else -> break
            }
            
            // Check if there's another number+suffix part
            if (currentPosition < endOffset && buffer[currentPosition].isDigit()) {
                // Read the number
                while (currentPosition < endOffset && buffer[currentPosition].isDigit()) {
                    currentPosition++
                }
                // Continue to read the suffix in next iteration
                if (currentPosition >= endOffset || !isDurationSuffix(currentPosition)) {
                    break
                }
            } else {
                break
            }
        }
        
        return SurqlTokenTypes.DURATION_LITERAL
    }

    private fun readIdentifierOrKeyword(): IElementType {
        val start = currentPosition
        while (currentPosition < endOffset && (buffer[currentPosition].isLetterOrDigit() || buffer[currentPosition] == '_')) {
            currentPosition++
        }
        
        // Check for record ID (identifier:identifier or identifier:⟨...⟩)
        if (currentPosition < endOffset && buffer[currentPosition] == ':') {
            val afterColon = currentPosition + 1
            if (afterColon < endOffset) {
                val nextChar = buffer[afterColon]
                if (nextChar.isLetterOrDigit() || nextChar == '_' || nextChar == '⟨' || nextChar == '`') {
                    currentPosition++ // consume :
                    // Read the record part
                    if (buffer[currentPosition] == '⟨') {
                        currentPosition++
                        while (currentPosition < endOffset && buffer[currentPosition] != '⟩') {
                            currentPosition++
                        }
                        if (currentPosition < endOffset) currentPosition++ // consume ⟩
                    } else if (buffer[currentPosition] == '`') {
                        currentPosition++
                        while (currentPosition < endOffset && buffer[currentPosition] != '`') {
                            currentPosition++
                        }
                        if (currentPosition < endOffset) currentPosition++ // consume `
                    } else {
                        while (currentPosition < endOffset && (buffer[currentPosition].isLetterOrDigit() || buffer[currentPosition] == '_')) {
                            currentPosition++
                        }
                    }
                    return SurqlTokenTypes.RECORD_ID
                }
            }
        }

        val word = buffer.substring(start, currentPosition).uppercase()
        return KEYWORDS[word] ?: SurqlTokenTypes.IDENTIFIER
    }

    private fun readOperatorOrPunctuation(): IElementType {
        val c = buffer[currentPosition]
        val next = peek(1)
        val next2 = peek(2)

        // Three-character operators
        if (next != null && next2 != null) {
            when {
                c == '.' && next == '.' && next2 == '.' -> {
                    currentPosition += 3
                    return SurqlTokenTypes.DOTDOTDOT
                }
                c == '<' && next == '-' && next2 == '>' -> {
                    currentPosition += 3
                    return SurqlTokenTypes.BIARROW
                }
            }
        }

        // Two-character operators
        if (next != null) {
            when {
                c == '.' && next == '.' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.DOTDOT
                }
                c == ':' && next == ':' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.COLONCOLON
                }
                c == '=' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.EQEQ
                }
                c == '!' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.NE
                }
                c == '<' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.LE
                }
                c == '>' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.GE
                }
                c == '+' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.PLUSEQ
                }
                c == '-' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.MINUSEQ
                }
                c == '*' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.STAREQ
                }
                c == '/' && next == '=' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.SLASHEQ
                }
                c == '-' && next == '>' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.ARROW
                }
                c == '<' && next == '-' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.LARROW
                }
                c == '|' && next == '|' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.OR
                }
                c == '&' && next == '&' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.AND
                }
                c == '?' && next == ':' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.QUESTIONCOLON
                }
                c == '?' && next == '?' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.QUESTIONQUESTION
                }
                c == '|' && next == '<' -> {
                    currentPosition += 2
                    return SurqlTokenTypes.PIPELT
                }
            }
        }

        // Single-character operators and punctuation
        currentPosition++
        return when (c) {
            ';' -> SurqlTokenTypes.SEMICOLON
            ',' -> SurqlTokenTypes.COMMA
            '.' -> SurqlTokenTypes.DOT
            ':' -> SurqlTokenTypes.COLON
            '(' -> SurqlTokenTypes.LPAREN
            ')' -> SurqlTokenTypes.RPAREN
            '[' -> SurqlTokenTypes.LBRACKET
            ']' -> SurqlTokenTypes.RBRACKET
            '{' -> SurqlTokenTypes.LBRACE
            '}' -> SurqlTokenTypes.RBRACE
            '+' -> SurqlTokenTypes.PLUS
            '-' -> SurqlTokenTypes.MINUS
            '*' -> SurqlTokenTypes.STAR
            '/' -> SurqlTokenTypes.SLASH
            '%' -> SurqlTokenTypes.PERCENT
            '^' -> SurqlTokenTypes.CARET
            '=' -> SurqlTokenTypes.EQ
            '<' -> SurqlTokenTypes.LT
            '>' -> SurqlTokenTypes.GT
            '?' -> SurqlTokenTypes.QUESTION
            '@' -> SurqlTokenTypes.AT
            '~' -> SurqlTokenTypes.TILDE
            '|' -> SurqlTokenTypes.PIPE
            else -> SurqlTokenTypes.BAD_CHARACTER
        }
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    companion object {
        private val KEYWORDS = mapOf(
            // Statement keywords
            "SELECT" to SurqlTokenTypes.SELECT,
            "FROM" to SurqlTokenTypes.FROM,
            "WHERE" to SurqlTokenTypes.WHERE,
            "CREATE" to SurqlTokenTypes.CREATE,
            "UPDATE" to SurqlTokenTypes.UPDATE,
            "DELETE" to SurqlTokenTypes.DELETE,
            "INSERT" to SurqlTokenTypes.INSERT,
            "UPSERT" to SurqlTokenTypes.UPSERT,
            "RELATE" to SurqlTokenTypes.RELATE,
            "DEFINE" to SurqlTokenTypes.DEFINE,
            "REMOVE" to SurqlTokenTypes.REMOVE,
            "ALTER" to SurqlTokenTypes.ALTER,
            "INFO" to SurqlTokenTypes.INFO,
            "USE" to SurqlTokenTypes.USE,
            "LET" to SurqlTokenTypes.LET,
            "BEGIN" to SurqlTokenTypes.BEGIN,
            "COMMIT" to SurqlTokenTypes.COMMIT,
            "CANCEL" to SurqlTokenTypes.CANCEL,
            "BREAK" to SurqlTokenTypes.BREAK,
            "CONTINUE" to SurqlTokenTypes.CONTINUE,
            "RETURN" to SurqlTokenTypes.RETURN,
            "THROW" to SurqlTokenTypes.THROW,
            "IF" to SurqlTokenTypes.IF,
            "ELSE" to SurqlTokenTypes.ELSE,
            "THEN" to SurqlTokenTypes.THEN,
            "FOR" to SurqlTokenTypes.FOR,
            "IN" to SurqlTokenTypes.IN,
            "LIVE" to SurqlTokenTypes.LIVE,
            "KILL" to SurqlTokenTypes.KILL,
            "SHOW" to SurqlTokenTypes.SHOW,
            "SLEEP" to SurqlTokenTypes.SLEEP,
            "OPTION" to SurqlTokenTypes.OPTION,
            "REBUILD" to SurqlTokenTypes.REBUILD,
            
            // Clauses
            "SET" to SurqlTokenTypes.SET,
            "UNSET" to SurqlTokenTypes.UNSET,
            "CONTENT" to SurqlTokenTypes.CONTENT,
            "MERGE" to SurqlTokenTypes.MERGE,
            "PATCH" to SurqlTokenTypes.PATCH,
            "REPLACE" to SurqlTokenTypes.REPLACE,
            "ONLY" to SurqlTokenTypes.ONLY,
            "LIMIT" to SurqlTokenTypes.LIMIT,
            "START" to SurqlTokenTypes.START,
            "ORDER" to SurqlTokenTypes.ORDER,
            "BY" to SurqlTokenTypes.BY,
            "ASC" to SurqlTokenTypes.ASC,
            "DESC" to SurqlTokenTypes.DESC,
            "GROUP" to SurqlTokenTypes.GROUP,
            "SPLIT" to SurqlTokenTypes.SPLIT,
            "FETCH" to SurqlTokenTypes.FETCH,
            "TIMEOUT" to SurqlTokenTypes.TIMEOUT,
            "PARALLEL" to SurqlTokenTypes.PARALLEL,
            "EXPLAIN" to SurqlTokenTypes.EXPLAIN,
            "TEMPFILES" to SurqlTokenTypes.TEMPFILES,
            "WITH" to SurqlTokenTypes.WITH,
            "NOINDEX" to SurqlTokenTypes.NOINDEX,
            "INDEX" to SurqlTokenTypes.INDEX,
            "VERSION" to SurqlTokenTypes.VERSION,
            "DIFF" to SurqlTokenTypes.DIFF,
            "OMIT" to SurqlTokenTypes.OMIT,
            "VALUE" to SurqlTokenTypes.VALUE,
            "ON" to SurqlTokenTypes.ON,
            "DUPLICATE" to SurqlTokenTypes.DUPLICATE,
            "KEY" to SurqlTokenTypes.KEY,
            "IGNORE" to SurqlTokenTypes.IGNORE,
            "RELATION" to SurqlTokenTypes.RELATION,
            "VALUES" to SurqlTokenTypes.VALUES,
            "INTO" to SurqlTokenTypes.INTO,
            
            // Definition targets
            "NAMESPACE" to SurqlTokenTypes.NAMESPACE,
            "NS" to SurqlTokenTypes.NS,
            "DATABASE" to SurqlTokenTypes.DATABASE,
            "DB" to SurqlTokenTypes.DB,
            "TABLE" to SurqlTokenTypes.TABLE,
            "FIELD" to SurqlTokenTypes.FIELD,
            "EVENT" to SurqlTokenTypes.EVENT,
            "FUNCTION" to SurqlTokenTypes.FUNCTION,
            "PARAM" to SurqlTokenTypes.PARAM,
            "ANALYZER" to SurqlTokenTypes.ANALYZER,
            "ACCESS" to SurqlTokenTypes.ACCESS,
            "USER" to SurqlTokenTypes.USER,
            "TOKEN" to SurqlTokenTypes.TOKEN,
            "SCOPE" to SurqlTokenTypes.SCOPE,
            "MODEL" to SurqlTokenTypes.MODEL,
            "CONFIG" to SurqlTokenTypes.CONFIG,
            "API" to SurqlTokenTypes.API,
            
            // Table options
            "SCHEMAFULL" to SurqlTokenTypes.SCHEMAFULL,
            "SCHEMALESS" to SurqlTokenTypes.SCHEMALESS,
            "DROP" to SurqlTokenTypes.DROP,
            "CHANGEFEED" to SurqlTokenTypes.CHANGEFEED,
            "PERMISSIONS" to SurqlTokenTypes.PERMISSIONS,
            "FULL" to SurqlTokenTypes.FULL,
            "NONE" to SurqlTokenTypes.NONE,
            "COMMENT" to SurqlTokenTypes.COMMENT,
            "OVERWRITE" to SurqlTokenTypes.OVERWRITE,
            "NOT" to SurqlTokenTypes.NOT,
            "EXISTS" to SurqlTokenTypes.EXISTS,
            "AS" to SurqlTokenTypes.AS,
            "NORMAL" to SurqlTokenTypes.NORMAL,
            "INCLUDE" to SurqlTokenTypes.INCLUDE,
            "ORIGINAL" to SurqlTokenTypes.ORIGINAL,
            
            // Field options
            "TYPE" to SurqlTokenTypes.TYPE,
            "FLEXIBLE" to SurqlTokenTypes.FLEXIBLE,
            "READONLY" to SurqlTokenTypes.READONLY,
            "DEFAULT" to SurqlTokenTypes.DEFAULT,
            "ASSERT" to SurqlTokenTypes.ASSERT,
            
            // Index options
            "UNIQUE" to SurqlTokenTypes.UNIQUE,
            "SEARCH" to SurqlTokenTypes.SEARCH,
            "BM25" to SurqlTokenTypes.BM25,
            "HIGHLIGHTS" to SurqlTokenTypes.HIGHLIGHTS,
            "MTREE" to SurqlTokenTypes.MTREE,
            "HNSW" to SurqlTokenTypes.HNSW,
            "DIMENSION" to SurqlTokenTypes.DIMENSION,
            "DIST" to SurqlTokenTypes.DIST,
            "CONCURRENTLY" to SurqlTokenTypes.CONCURRENTLY,
            "FIELDS" to SurqlTokenTypes.FIELDS,
            "COLUMNS" to SurqlTokenTypes.COLUMNS,
            "CAPACITY" to SurqlTokenTypes.CAPACITY,
            "EFC" to SurqlTokenTypes.EFC,
            "M" to SurqlTokenTypes.M,
            
            // Distance functions
            "EUCLIDEAN" to SurqlTokenTypes.EUCLIDEAN,
            "MANHATTAN" to SurqlTokenTypes.MANHATTAN,
            "COSINE" to SurqlTokenTypes.COSINE,
            "MINKOWSKI" to SurqlTokenTypes.MINKOWSKI,
            "CHEBYSHEV" to SurqlTokenTypes.CHEBYSHEV,
            "HAMMING" to SurqlTokenTypes.HAMMING,
            "JACCARD" to SurqlTokenTypes.JACCARD,
            "PEARSON" to SurqlTokenTypes.PEARSON,
            
            // Analyzer options
            "TOKENIZERS" to SurqlTokenTypes.TOKENIZERS,
            "FILTERS" to SurqlTokenTypes.FILTERS,
            "ASCII" to SurqlTokenTypes.ASCII,
            "EDGENGRAM" to SurqlTokenTypes.EDGENGRAM,
            "LOWERCASE" to SurqlTokenTypes.LOWERCASE,
            "UPPERCASE" to SurqlTokenTypes.UPPERCASE,
            "SNOWBALL" to SurqlTokenTypes.SNOWBALL,
            "BLANK" to SurqlTokenTypes.BLANK,
            "CAMEL" to SurqlTokenTypes.CAMEL,
            "CLASS" to SurqlTokenTypes.CLASS,
            "PUNCT" to SurqlTokenTypes.PUNCT,
            
            // Access options
            "JWT" to SurqlTokenTypes.JWT,
            "RECORD" to SurqlTokenTypes.RECORD,
            "ALGORITHM" to SurqlTokenTypes.ALGORITHM,
            "ISSUER" to SurqlTokenTypes.ISSUER,
            "URL" to SurqlTokenTypes.URL,
            "BEARER" to SurqlTokenTypes.BEARER,
            "SIGNUP" to SurqlTokenTypes.SIGNUP,
            "SIGNIN" to SurqlTokenTypes.SIGNIN,
            "AUTHENTICATE" to SurqlTokenTypes.AUTHENTICATE,
            "PASSWORD" to SurqlTokenTypes.PASSWORD,
            "PASSHASH" to SurqlTokenTypes.PASSHASH,
            "ROLES" to SurqlTokenTypes.ROLES,
            "DURATION" to SurqlTokenTypes.DURATION,
            "SESSION" to SurqlTokenTypes.SESSION,
            "REVOKE" to SurqlTokenTypes.REVOKE,
            "GRANT" to SurqlTokenTypes.GRANT,
            
            // Additional keywords
            "END" to SurqlTokenTypes.END,
            "WHEN" to SurqlTokenTypes.WHEN,
            "ALL" to SurqlTokenTypes.ALL,
            "AT" to SurqlTokenTypes.AT_KW,
            "BEFORE" to SurqlTokenTypes.BEFORE,
            "AFTER" to SurqlTokenTypes.AFTER,
            "ROOT" to SurqlTokenTypes.ROOT,
            "CHANGES" to SurqlTokenTypes.CHANGES,
            "SINCE" to SurqlTokenTypes.SINCE,
            
            // Word operators
            "AND" to SurqlTokenTypes.AND_KW,
            "OR" to SurqlTokenTypes.OR_KW,
            "IS" to SurqlTokenTypes.IS,
            "CONTAINS" to SurqlTokenTypes.CONTAINS,
            "CONTAINSNOT" to SurqlTokenTypes.CONTAINSNOT,
            "CONTAINSALL" to SurqlTokenTypes.CONTAINSALL,
            "CONTAINSANY" to SurqlTokenTypes.CONTAINSANY,
            "CONTAINSNONE" to SurqlTokenTypes.CONTAINSNONE,
            "INSIDE" to SurqlTokenTypes.INSIDE,
            "NOTINSIDE" to SurqlTokenTypes.NOTINSIDE,
            "ALLINSIDE" to SurqlTokenTypes.ALLINSIDE,
            "ANYINSIDE" to SurqlTokenTypes.ANYINSIDE,
            "NONEINSIDE" to SurqlTokenTypes.NONEINSIDE,
            "OUTSIDE" to SurqlTokenTypes.OUTSIDE,
            "INTERSECTS" to SurqlTokenTypes.INTERSECTS,
            "MATCHES" to SurqlTokenTypes.MATCHES,
            "LIKE" to SurqlTokenTypes.LIKE,
            "COLLATE" to SurqlTokenTypes.COLLATE,
            "NUMERIC" to SurqlTokenTypes.NUMERIC,
            "KNN" to SurqlTokenTypes.KNN,
            
            // Literals
            "TRUE" to SurqlTokenTypes.TRUE,
            "FALSE" to SurqlTokenTypes.FALSE,
            "NULL" to SurqlTokenTypes.NULL,
            
            // Transaction
            "TRANSACTION" to SurqlTokenTypes.TRANSACTION,
            
            // Types
            "BOOL" to SurqlTokenTypes.BOOL,
            "INT" to SurqlTokenTypes.INT,
            "FLOAT" to SurqlTokenTypes.FLOAT,
            "DECIMAL" to SurqlTokenTypes.DECIMAL,
            "DATETIME" to SurqlTokenTypes.DATETIME,
            "STRING" to SurqlTokenTypes.STRING_TYPE,
            "OBJECT" to SurqlTokenTypes.OBJECT,
            "ARRAY" to SurqlTokenTypes.ARRAY,
            "BYTES" to SurqlTokenTypes.BYTES,
            "UUID" to SurqlTokenTypes.UUID,
            "GEOMETRY" to SurqlTokenTypes.GEOMETRY,
            "ANY" to SurqlTokenTypes.ANY,
            "POINT" to SurqlTokenTypes.POINT,
            "LINE" to SurqlTokenTypes.LINE,
            "POLYGON" to SurqlTokenTypes.POLYGON,
            "MULTIPOINT" to SurqlTokenTypes.MULTIPOINT,
            "MULTILINE" to SurqlTokenTypes.MULTILINE,
            "MULTIPOLYGON" to SurqlTokenTypes.MULTIPOLYGON,
            "COLLECTION" to SurqlTokenTypes.COLLECTION,
            "LITERAL" to SurqlTokenTypes.LITERAL,
            "EITHER" to SurqlTokenTypes.EITHER,
            "FUTURE" to SurqlTokenTypes.FUTURE,
            "RANGE" to SurqlTokenTypes.RANGE,
            "REFS" to SurqlTokenTypes.REFS,
            "EXPRESSIONS" to SurqlTokenTypes.EXPRESSIONS,
        )
    }
}
