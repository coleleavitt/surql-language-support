package org.surrealdb.surql.lexer

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.surrealdb.surql.lang.SurqlLanguage

class SurqlTokenType(debugName: String) : IElementType(debugName, SurqlLanguage)

class SurqlElementType(debugName: String) : IElementType(debugName, SurqlLanguage)

object SurqlTokenTypes {
    // Special tokens
    @JvmField val WHITE_SPACE = SurqlTokenType("WHITE_SPACE")
    @JvmField val BAD_CHARACTER = SurqlTokenType("BAD_CHARACTER")
    
    // Comments
    @JvmField val LINE_COMMENT = SurqlTokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = SurqlTokenType("BLOCK_COMMENT")
    
    // Literals
    @JvmField val NUMBER = SurqlTokenType("NUMBER")
    @JvmField val STRING = SurqlTokenType("STRING")
    @JvmField val DATETIME_STRING = SurqlTokenType("DATETIME_STRING")
    @JvmField val UUID_STRING = SurqlTokenType("UUID_STRING")
    @JvmField val RECORD_STRING = SurqlTokenType("RECORD_STRING")
    @JvmField val REGEX = SurqlTokenType("REGEX")
    @JvmField val DURATION_LITERAL = SurqlTokenType("DURATION_LITERAL")
    
    // Identifiers and parameters
    @JvmField val IDENTIFIER = SurqlTokenType("IDENTIFIER")
    @JvmField val PARAMETER = SurqlTokenType("PARAMETER")
    @JvmField val RECORD_ID = SurqlTokenType("RECORD_ID")
    
    // Punctuation
    @JvmField val SEMICOLON = SurqlTokenType("SEMICOLON")
    @JvmField val COMMA = SurqlTokenType("COMMA")
    @JvmField val DOT = SurqlTokenType("DOT")
    @JvmField val DOTDOT = SurqlTokenType("DOTDOT")
    @JvmField val DOTDOTDOT = SurqlTokenType("DOTDOTDOT")
    @JvmField val COLON = SurqlTokenType("COLON")
    @JvmField val COLONCOLON = SurqlTokenType("COLONCOLON")
    
    // Brackets
    @JvmField val LPAREN = SurqlTokenType("LPAREN")
    @JvmField val RPAREN = SurqlTokenType("RPAREN")
    @JvmField val LBRACKET = SurqlTokenType("LBRACKET")
    @JvmField val RBRACKET = SurqlTokenType("RBRACKET")
    @JvmField val LBRACE = SurqlTokenType("LBRACE")
    @JvmField val RBRACE = SurqlTokenType("RBRACE")
    
    // Operators
    @JvmField val PLUS = SurqlTokenType("PLUS")
    @JvmField val MINUS = SurqlTokenType("MINUS")
    @JvmField val STAR = SurqlTokenType("STAR")
    @JvmField val SLASH = SurqlTokenType("SLASH")
    @JvmField val PERCENT = SurqlTokenType("PERCENT")
    @JvmField val CARET = SurqlTokenType("CARET")
    @JvmField val EQ = SurqlTokenType("EQ")
    @JvmField val EQEQ = SurqlTokenType("EQEQ")
    @JvmField val NE = SurqlTokenType("NE")
    @JvmField val LT = SurqlTokenType("LT")
    @JvmField val LE = SurqlTokenType("LE")
    @JvmField val GT = SurqlTokenType("GT")
    @JvmField val GE = SurqlTokenType("GE")
    @JvmField val PLUSEQ = SurqlTokenType("PLUSEQ")
    @JvmField val MINUSEQ = SurqlTokenType("MINUSEQ")
    @JvmField val STAREQ = SurqlTokenType("STAREQ")
    @JvmField val SLASHEQ = SurqlTokenType("SLASHEQ")
    @JvmField val ARROW = SurqlTokenType("ARROW")
    @JvmField val LARROW = SurqlTokenType("LARROW")
    @JvmField val BIARROW = SurqlTokenType("BIARROW")
    @JvmField val OR = SurqlTokenType("OR")
    @JvmField val AND = SurqlTokenType("AND")
    @JvmField val QUESTION = SurqlTokenType("QUESTION")
    @JvmField val QUESTIONCOLON = SurqlTokenType("QUESTIONCOLON")
    @JvmField val QUESTIONQUESTION = SurqlTokenType("QUESTIONQUESTION")
    @JvmField val AT = SurqlTokenType("AT")
    @JvmField val TILDE = SurqlTokenType("TILDE")
    @JvmField val PIPE = SurqlTokenType("PIPE")
    @JvmField val PIPELT = SurqlTokenType("PIPELT")
    
    // Keywords - Statement keywords
    @JvmField val SELECT = SurqlTokenType("SELECT")
    @JvmField val FROM = SurqlTokenType("FROM")
    @JvmField val WHERE = SurqlTokenType("WHERE")
    @JvmField val CREATE = SurqlTokenType("CREATE")
    @JvmField val UPDATE = SurqlTokenType("UPDATE")
    @JvmField val DELETE = SurqlTokenType("DELETE")
    @JvmField val INSERT = SurqlTokenType("INSERT")
    @JvmField val UPSERT = SurqlTokenType("UPSERT")
    @JvmField val RELATE = SurqlTokenType("RELATE")
    @JvmField val DEFINE = SurqlTokenType("DEFINE")
    @JvmField val REMOVE = SurqlTokenType("REMOVE")
    @JvmField val ALTER = SurqlTokenType("ALTER")
    @JvmField val INFO = SurqlTokenType("INFO")
    @JvmField val USE = SurqlTokenType("USE")
    @JvmField val LET = SurqlTokenType("LET")
    @JvmField val BEGIN = SurqlTokenType("BEGIN")
    @JvmField val COMMIT = SurqlTokenType("COMMIT")
    @JvmField val CANCEL = SurqlTokenType("CANCEL")
    @JvmField val BREAK = SurqlTokenType("BREAK")
    @JvmField val CONTINUE = SurqlTokenType("CONTINUE")
    @JvmField val RETURN = SurqlTokenType("RETURN")
    @JvmField val THROW = SurqlTokenType("THROW")
    @JvmField val IF = SurqlTokenType("IF")
    @JvmField val ELSE = SurqlTokenType("ELSE")
    @JvmField val THEN = SurqlTokenType("THEN")
    @JvmField val FOR = SurqlTokenType("FOR")
    @JvmField val IN = SurqlTokenType("IN")
    @JvmField val LIVE = SurqlTokenType("LIVE")
    @JvmField val KILL = SurqlTokenType("KILL")
    @JvmField val SHOW = SurqlTokenType("SHOW")
    @JvmField val SLEEP = SurqlTokenType("SLEEP")
    @JvmField val OPTION = SurqlTokenType("OPTION")
    @JvmField val REBUILD = SurqlTokenType("REBUILD")
    
    // Keywords - Clauses
    @JvmField val SET = SurqlTokenType("SET")
    @JvmField val UNSET = SurqlTokenType("UNSET")
    @JvmField val CONTENT = SurqlTokenType("CONTENT")
    @JvmField val MERGE = SurqlTokenType("MERGE")
    @JvmField val PATCH = SurqlTokenType("PATCH")
    @JvmField val REPLACE = SurqlTokenType("REPLACE")
    @JvmField val ONLY = SurqlTokenType("ONLY")
    @JvmField val LIMIT = SurqlTokenType("LIMIT")
    @JvmField val START = SurqlTokenType("START")
    @JvmField val ORDER = SurqlTokenType("ORDER")
    @JvmField val BY = SurqlTokenType("BY")
    @JvmField val ASC = SurqlTokenType("ASC")
    @JvmField val DESC = SurqlTokenType("DESC")
    @JvmField val GROUP = SurqlTokenType("GROUP")
    @JvmField val SPLIT = SurqlTokenType("SPLIT")
    @JvmField val FETCH = SurqlTokenType("FETCH")
    @JvmField val TIMEOUT = SurqlTokenType("TIMEOUT")
    @JvmField val PARALLEL = SurqlTokenType("PARALLEL")
    @JvmField val EXPLAIN = SurqlTokenType("EXPLAIN")
    @JvmField val TEMPFILES = SurqlTokenType("TEMPFILES")
    @JvmField val WITH = SurqlTokenType("WITH")
    @JvmField val NOINDEX = SurqlTokenType("NOINDEX")
    @JvmField val INDEX = SurqlTokenType("INDEX")
    @JvmField val VERSION = SurqlTokenType("VERSION")
    @JvmField val DIFF = SurqlTokenType("DIFF")
    @JvmField val OMIT = SurqlTokenType("OMIT")
    @JvmField val VALUE = SurqlTokenType("VALUE")
    @JvmField val ON = SurqlTokenType("ON")
    @JvmField val DUPLICATE = SurqlTokenType("DUPLICATE")
    @JvmField val KEY = SurqlTokenType("KEY")
    @JvmField val IGNORE = SurqlTokenType("IGNORE")
    @JvmField val RELATION = SurqlTokenType("RELATION")
    @JvmField val VALUES = SurqlTokenType("VALUES")
    @JvmField val INTO = SurqlTokenType("INTO")
    
    // Keywords - Definition targets
    @JvmField val NAMESPACE = SurqlTokenType("NAMESPACE")
    @JvmField val NS = SurqlTokenType("NS")
    @JvmField val DATABASE = SurqlTokenType("DATABASE")
    @JvmField val DB = SurqlTokenType("DB")
    @JvmField val TABLE = SurqlTokenType("TABLE")
    @JvmField val FIELD = SurqlTokenType("FIELD")
    @JvmField val EVENT = SurqlTokenType("EVENT")
    @JvmField val FUNCTION = SurqlTokenType("FUNCTION")
    @JvmField val PARAM = SurqlTokenType("PARAM")
    @JvmField val ANALYZER = SurqlTokenType("ANALYZER")
    @JvmField val ACCESS = SurqlTokenType("ACCESS")
    @JvmField val USER = SurqlTokenType("USER")
    @JvmField val TOKEN = SurqlTokenType("TOKEN")
    @JvmField val SCOPE = SurqlTokenType("SCOPE")
    @JvmField val MODEL = SurqlTokenType("MODEL")
    @JvmField val CONFIG = SurqlTokenType("CONFIG")
    @JvmField val API = SurqlTokenType("API")
    
    // Keywords - Table options
    @JvmField val SCHEMAFULL = SurqlTokenType("SCHEMAFULL")
    @JvmField val SCHEMALESS = SurqlTokenType("SCHEMALESS")
    @JvmField val DROP = SurqlTokenType("DROP")
    @JvmField val CHANGEFEED = SurqlTokenType("CHANGEFEED")
    @JvmField val PERMISSIONS = SurqlTokenType("PERMISSIONS")
    @JvmField val FULL = SurqlTokenType("FULL")
    @JvmField val NONE = SurqlTokenType("NONE")
    @JvmField val COMMENT = SurqlTokenType("COMMENT")
    @JvmField val OVERWRITE = SurqlTokenType("OVERWRITE")
    @JvmField val IF_KW = SurqlTokenType("IF_KW")
    @JvmField val NOT = SurqlTokenType("NOT")
    @JvmField val EXISTS = SurqlTokenType("EXISTS")
    @JvmField val AS = SurqlTokenType("AS")
    @JvmField val NORMAL = SurqlTokenType("NORMAL")
    @JvmField val INCLUDE = SurqlTokenType("INCLUDE")
    @JvmField val ORIGINAL = SurqlTokenType("ORIGINAL")
    
    // Keywords - Field options
    @JvmField val TYPE = SurqlTokenType("TYPE")
    @JvmField val FLEXIBLE = SurqlTokenType("FLEXIBLE")
    @JvmField val READONLY = SurqlTokenType("READONLY")
    @JvmField val DEFAULT = SurqlTokenType("DEFAULT")
    @JvmField val ASSERT = SurqlTokenType("ASSERT")
    
    // Keywords - Index options
    @JvmField val UNIQUE = SurqlTokenType("UNIQUE")
    @JvmField val SEARCH = SurqlTokenType("SEARCH")
    @JvmField val ANALYZER_KW = SurqlTokenType("ANALYZER_KW")
    @JvmField val BM25 = SurqlTokenType("BM25")
    @JvmField val HIGHLIGHTS = SurqlTokenType("HIGHLIGHTS")
    @JvmField val MTREE = SurqlTokenType("MTREE")
    @JvmField val HNSW = SurqlTokenType("HNSW")
    @JvmField val DIMENSION = SurqlTokenType("DIMENSION")
    @JvmField val DIST = SurqlTokenType("DIST")
    @JvmField val CONCURRENTLY = SurqlTokenType("CONCURRENTLY")
    @JvmField val CAPACITY = SurqlTokenType("CAPACITY")
    @JvmField val EFC = SurqlTokenType("EFC")
    @JvmField val M = SurqlTokenType("M")
    @JvmField val FIELDS = SurqlTokenType("FIELDS")
    @JvmField val COLUMNS = SurqlTokenType("COLUMNS")
    
    // Keywords - Distance functions
    @JvmField val EUCLIDEAN = SurqlTokenType("EUCLIDEAN")
    @JvmField val MANHATTAN = SurqlTokenType("MANHATTAN")
    @JvmField val COSINE = SurqlTokenType("COSINE")
    @JvmField val MINKOWSKI = SurqlTokenType("MINKOWSKI")
    @JvmField val CHEBYSHEV = SurqlTokenType("CHEBYSHEV")
    @JvmField val HAMMING = SurqlTokenType("HAMMING")
    @JvmField val JACCARD = SurqlTokenType("JACCARD")
    @JvmField val PEARSON = SurqlTokenType("PEARSON")
    
    // Keywords - Analyzer options
    @JvmField val TOKENIZERS = SurqlTokenType("TOKENIZERS")
    @JvmField val FILTERS = SurqlTokenType("FILTERS")
    @JvmField val ASCII = SurqlTokenType("ASCII")
    @JvmField val EDGENGRAM = SurqlTokenType("EDGENGRAM")
    @JvmField val LOWERCASE = SurqlTokenType("LOWERCASE")
    @JvmField val UPPERCASE = SurqlTokenType("UPPERCASE")
    @JvmField val SNOWBALL = SurqlTokenType("SNOWBALL")
    @JvmField val BLANK = SurqlTokenType("BLANK")
    @JvmField val CAMEL = SurqlTokenType("CAMEL")
    @JvmField val CLASS = SurqlTokenType("CLASS")
    @JvmField val PUNCT = SurqlTokenType("PUNCT")
    
    // Keywords - Access options
    @JvmField val JWT = SurqlTokenType("JWT")
    @JvmField val RECORD = SurqlTokenType("RECORD")
    @JvmField val ALGORITHM = SurqlTokenType("ALGORITHM")
    @JvmField val ISSUER = SurqlTokenType("ISSUER")
    @JvmField val URL = SurqlTokenType("URL")
    @JvmField val BEARER = SurqlTokenType("BEARER")
    @JvmField val SIGNUP = SurqlTokenType("SIGNUP")
    @JvmField val SIGNIN = SurqlTokenType("SIGNIN")
    @JvmField val AUTHENTICATE = SurqlTokenType("AUTHENTICATE")
    @JvmField val PASSWORD = SurqlTokenType("PASSWORD")
    @JvmField val PASSHASH = SurqlTokenType("PASSHASH")
    @JvmField val ROLES = SurqlTokenType("ROLES")
    @JvmField val DURATION = SurqlTokenType("DURATION")
    @JvmField val SESSION = SurqlTokenType("SESSION")
    @JvmField val REVOKE = SurqlTokenType("REVOKE")
    @JvmField val GRANT = SurqlTokenType("GRANT")
    
    // Keywords - Additional control flow and misc
    @JvmField val END = SurqlTokenType("END")
    @JvmField val WHEN = SurqlTokenType("WHEN")
    @JvmField val ALL = SurqlTokenType("ALL")
    @JvmField val AT_KW = SurqlTokenType("AT_KW")
    @JvmField val BEFORE = SurqlTokenType("BEFORE")
    @JvmField val AFTER = SurqlTokenType("AFTER")
    @JvmField val ROOT = SurqlTokenType("ROOT")
    @JvmField val CHANGES = SurqlTokenType("CHANGES")
    @JvmField val SINCE = SurqlTokenType("SINCE")
    
    // Keywords - Operators (word operators)
    @JvmField val AND_KW = SurqlTokenType("AND_KW")
    @JvmField val OR_KW = SurqlTokenType("OR_KW")
    @JvmField val IS = SurqlTokenType("IS")
    @JvmField val CONTAINS = SurqlTokenType("CONTAINS")
    @JvmField val CONTAINSNOT = SurqlTokenType("CONTAINSNOT")
    @JvmField val CONTAINSALL = SurqlTokenType("CONTAINSALL")
    @JvmField val CONTAINSANY = SurqlTokenType("CONTAINSANY")
    @JvmField val CONTAINSNONE = SurqlTokenType("CONTAINSNONE")
    @JvmField val INSIDE = SurqlTokenType("INSIDE")
    @JvmField val NOTINSIDE = SurqlTokenType("NOTINSIDE")
    @JvmField val ALLINSIDE = SurqlTokenType("ALLINSIDE")
    @JvmField val ANYINSIDE = SurqlTokenType("ANYINSIDE")
    @JvmField val NONEINSIDE = SurqlTokenType("NONEINSIDE")
    @JvmField val OUTSIDE = SurqlTokenType("OUTSIDE")
    @JvmField val INTERSECTS = SurqlTokenType("INTERSECTS")
    @JvmField val MATCHES = SurqlTokenType("MATCHES")
    @JvmField val LIKE = SurqlTokenType("LIKE")
    @JvmField val COLLATE = SurqlTokenType("COLLATE")
    @JvmField val NUMERIC = SurqlTokenType("NUMERIC")
    @JvmField val KNN = SurqlTokenType("KNN")
    
    // Keywords - Literals
    @JvmField val TRUE = SurqlTokenType("TRUE")
    @JvmField val FALSE = SurqlTokenType("FALSE")
    @JvmField val NULL = SurqlTokenType("NULL")
    
    // Keywords - Transaction
    @JvmField val TRANSACTION = SurqlTokenType("TRANSACTION")
    
    // Keywords - Types
    @JvmField val BOOL = SurqlTokenType("BOOL")
    @JvmField val INT = SurqlTokenType("INT")
    @JvmField val FLOAT = SurqlTokenType("FLOAT")
    @JvmField val DECIMAL = SurqlTokenType("DECIMAL")
    @JvmField val DATETIME = SurqlTokenType("DATETIME")
    @JvmField val STRING_TYPE = SurqlTokenType("STRING_TYPE")
    @JvmField val OBJECT = SurqlTokenType("OBJECT")
    @JvmField val ARRAY = SurqlTokenType("ARRAY")
    @JvmField val BYTES = SurqlTokenType("BYTES")
    @JvmField val UUID = SurqlTokenType("UUID")
    @JvmField val GEOMETRY = SurqlTokenType("GEOMETRY")
    @JvmField val ANY = SurqlTokenType("ANY")
    @JvmField val OPTION_TYPE = SurqlTokenType("OPTION_TYPE")
    @JvmField val SET_TYPE = SurqlTokenType("SET_TYPE")
    @JvmField val POINT = SurqlTokenType("POINT")
    @JvmField val LINE = SurqlTokenType("LINE")
    @JvmField val POLYGON = SurqlTokenType("POLYGON")
    @JvmField val MULTIPOINT = SurqlTokenType("MULTIPOINT")
    @JvmField val MULTILINE = SurqlTokenType("MULTILINE")
    @JvmField val MULTIPOLYGON = SurqlTokenType("MULTIPOLYGON")
    @JvmField val COLLECTION = SurqlTokenType("COLLECTION")
    @JvmField val LITERAL = SurqlTokenType("LITERAL")
    @JvmField val EITHER = SurqlTokenType("EITHER")
    @JvmField val FUTURE = SurqlTokenType("FUTURE")
    @JvmField val RANGE = SurqlTokenType("RANGE")
    @JvmField val REFS = SurqlTokenType("REFS")
    @JvmField val EXPRESSIONS = SurqlTokenType("EXPRESSIONS")
    
    // ============================================================
    // SEMANTIC KEYWORD CATEGORIES for enhanced syntax highlighting
    // ============================================================
    
    /**
     * Statement keywords - Primary action verbs that start statements
     * These are the main commands in SurrealQL
     */
    @JvmField val STATEMENT_KEYWORDS = TokenSet.create(
        SELECT, CREATE, UPDATE, DELETE, INSERT, UPSERT, RELATE,
        DEFINE, REMOVE, ALTER, INFO, USE, LET, LIVE, KILL, 
        SHOW, SLEEP, OPTION, REBUILD, RETURN, THROW
    )
    
    /**
     * Transaction keywords - Transaction control statements
     */
    @JvmField val TRANSACTION_KEYWORDS = TokenSet.create(
        BEGIN, COMMIT, CANCEL, TRANSACTION
    )
    
    /**
     * Control flow keywords - IF, ELSE, FOR, loops, etc.
     */
    @JvmField val CONTROL_FLOW_KEYWORDS = TokenSet.create(
        IF, ELSE, THEN, FOR, IN, BREAK, CONTINUE
    )
    
    /**
     * Clause keywords - Secondary modifiers for statements
     * These follow statement keywords and modify their behavior
     */
    @JvmField val CLAUSE_KEYWORDS = TokenSet.create(
        FROM, WHERE, SET, UNSET, CONTENT, MERGE, PATCH, REPLACE,
        ONLY, LIMIT, START, ORDER, BY, ASC, DESC, GROUP, SPLIT, FETCH,
        TIMEOUT, PARALLEL, EXPLAIN, TEMPFILES, WITH, NOINDEX, VERSION,
        DIFF, OMIT, VALUE, ON, DUPLICATE, KEY, IGNORE, VALUES, INTO
    )
    
    /**
     * Definition target keywords - Schema objects that can be defined
     * Used after DEFINE/REMOVE/ALTER
     */
    @JvmField val DEFINITION_TARGET_KEYWORDS = TokenSet.create(
        NAMESPACE, NS, DATABASE, DB, TABLE, FIELD, EVENT, FUNCTION, 
        PARAM, ANALYZER, ACCESS, USER, TOKEN, SCOPE, MODEL, CONFIG, 
        API, INDEX, RELATION
    )
    
    /**
     * Schema option keywords - Options for schema definitions
     */
    @JvmField val SCHEMA_OPTION_KEYWORDS = TokenSet.create(
        SCHEMAFULL, SCHEMALESS, DROP, CHANGEFEED, PERMISSIONS,
        FULL, NONE, COMMENT, OVERWRITE, IF_KW, NOT, EXISTS, AS,
        TYPE, FLEXIBLE, READONLY, DEFAULT, ASSERT, UNIQUE, SEARCH,
        ANALYZER_KW, BM25, HIGHLIGHTS, MTREE, HNSW, DIMENSION, DIST,
        CONCURRENTLY, TOKENIZERS, FILTERS, JWT, RECORD, ALGORITHM,
        ISSUER, URL, BEARER, SIGNUP, SIGNIN, AUTHENTICATE, PASSWORD,
        PASSHASH, ROLES, DURATION, SESSION, REVOKE, GRANT
    )
    
    /**
     * Distance function keywords - Vector similarity functions
     */
    @JvmField val DISTANCE_FUNCTION_KEYWORDS = TokenSet.create(
        EUCLIDEAN, MANHATTAN, COSINE, MINKOWSKI, CHEBYSHEV, 
        HAMMING, JACCARD, PEARSON
    )
    
    /**
     * Analyzer filter keywords - Text analyzer options
     */
    @JvmField val ANALYZER_FILTER_KEYWORDS = TokenSet.create(
        ASCII, EDGENGRAM, LOWERCASE, UPPERCASE, SNOWBALL, 
        BLANK, CAMEL, CLASS, PUNCT
    )
    
    /**
     * Word operator keywords - Operators spelled as words
     */
    @JvmField val WORD_OPERATOR_KEYWORDS = TokenSet.create(
        AND_KW, OR_KW, IS, NOT, CONTAINS, CONTAINSNOT, CONTAINSALL,
        CONTAINSANY, CONTAINSNONE, INSIDE, NOTINSIDE, ALLINSIDE, 
        ANYINSIDE, NONEINSIDE, OUTSIDE, INTERSECTS, MATCHES, LIKE, 
        COLLATE, NUMERIC, KNN
    )
    
    /**
     * Literal keywords - Boolean and null literals
     */
    @JvmField val LITERAL_KEYWORDS = TokenSet.create(
        TRUE, FALSE, NULL, NONE
    )
    
    /**
     * Type keywords - Data type names
     */
    @JvmField val TYPE_KEYWORDS = TokenSet.create(
        BOOL, INT, FLOAT, DECIMAL, DATETIME, STRING_TYPE, OBJECT, 
        ARRAY, BYTES, UUID, GEOMETRY, ANY, OPTION_TYPE, SET_TYPE, 
        POINT, LINE, POLYGON, MULTIPOINT, MULTILINE, MULTIPOLYGON, 
        COLLECTION, LITERAL, EITHER, FUTURE, RANGE, REFS, EXPRESSIONS, 
        RECORD
    )
    
    // ============================================================
    // COMBINED TOKEN SETS
    // ============================================================
    
    /**
     * All keywords combined (for backward compatibility)
     */
    @JvmField val KEYWORDS = TokenSet.create(
        SELECT, FROM, WHERE, CREATE, UPDATE, DELETE, INSERT, UPSERT, RELATE,
        DEFINE, REMOVE, ALTER, INFO, USE, LET, BEGIN, COMMIT, CANCEL, BREAK,
        CONTINUE, RETURN, THROW, IF, ELSE, THEN, FOR, IN, LIVE, KILL, SHOW,
        SLEEP, OPTION, REBUILD, SET, UNSET, CONTENT, MERGE, PATCH, REPLACE,
        ONLY, LIMIT, START, ORDER, BY, ASC, DESC, GROUP, SPLIT, FETCH,
        TIMEOUT, PARALLEL, EXPLAIN, TEMPFILES, WITH, NOINDEX, INDEX, VERSION,
        DIFF, OMIT, VALUE, ON, DUPLICATE, KEY, IGNORE, RELATION, VALUES, INTO,
        NAMESPACE, NS, DATABASE, DB, TABLE, FIELD, EVENT, FUNCTION, PARAM,
        ANALYZER, ACCESS, USER, TOKEN, SCOPE, MODEL, CONFIG, API, SCHEMAFULL,
        SCHEMALESS, DROP, CHANGEFEED, PERMISSIONS, FULL, NONE, COMMENT,
        OVERWRITE, IF_KW, NOT, EXISTS, AS, TYPE, FLEXIBLE, READONLY, DEFAULT,
        ASSERT, UNIQUE, SEARCH, ANALYZER_KW, BM25, HIGHLIGHTS, MTREE, HNSW,
        DIMENSION, DIST, CONCURRENTLY, EUCLIDEAN, MANHATTAN, COSINE, MINKOWSKI,
        CHEBYSHEV, HAMMING, JACCARD, PEARSON, TOKENIZERS, FILTERS, ASCII,
        EDGENGRAM, LOWERCASE, UPPERCASE, SNOWBALL, BLANK, CAMEL, CLASS, PUNCT,
        JWT, RECORD, ALGORITHM, ISSUER, URL, BEARER, SIGNUP, SIGNIN,
        AUTHENTICATE, PASSWORD, PASSHASH, ROLES, DURATION, SESSION, REVOKE,
        GRANT, AND_KW, OR_KW, IS, CONTAINS, CONTAINSNOT, CONTAINSALL,
        CONTAINSANY, CONTAINSNONE, INSIDE, NOTINSIDE, ALLINSIDE, ANYINSIDE,
        NONEINSIDE, OUTSIDE, INTERSECTS, MATCHES, LIKE, COLLATE, NUMERIC, KNN,
        TRUE, FALSE, NULL, TRANSACTION, BOOL, INT, FLOAT, DECIMAL, DATETIME,
        STRING_TYPE, OBJECT, ARRAY, BYTES, UUID, GEOMETRY, ANY, OPTION_TYPE,
        SET_TYPE, POINT, LINE, POLYGON, MULTIPOINT, MULTILINE, MULTIPOLYGON,
        COLLECTION, LITERAL, EITHER, FUTURE, RANGE, REFS, EXPRESSIONS
    )
    
    @JvmField val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
    
    @JvmField val STRINGS = TokenSet.create(STRING, DATETIME_STRING, UUID_STRING, RECORD_STRING)
    
    @JvmField val OPERATORS = TokenSet.create(
        PLUS, MINUS, STAR, SLASH, PERCENT, CARET, EQ, EQEQ, NE, LT, LE, GT, GE,
        PLUSEQ, MINUSEQ, STAREQ, SLASHEQ, ARROW, LARROW, BIARROW, OR, AND,
        QUESTION, QUESTIONCOLON, QUESTIONQUESTION, AT, TILDE, PIPE, PIPELT
    )
    
    @JvmField val BRACKETS = TokenSet.create(LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE)
}
