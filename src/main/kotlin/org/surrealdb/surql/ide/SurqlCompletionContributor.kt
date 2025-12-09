package org.surrealdb.surql.ide

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.surrealdb.surql.lang.SurqlLanguage
import org.surrealdb.surql.lang.SurqlIcons

/**
 * Provides code completion for SurrealQL.
 * 
 * Completions include:
 * - Keywords (SELECT, FROM, WHERE, etc.)
 * - Built-in functions (array::*, crypto::*, etc.)
 * - Type names
 * - Common snippets
 */
class SurqlCompletionContributor : CompletionContributor() {

    init {
        // Add completion for all positions
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(SurqlLanguage),
            SurqlCompletionProvider()
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        // Ensure we capture the identifier being typed
        if (context.file.language == SurqlLanguage) {
            context.dummyIdentifier = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        }
    }
}

private class SurqlCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val prefix = result.prefixMatcher.prefix.lowercase()

        // Add keyword completions
        addKeywordCompletions(result, prefix)

        // Add function completions
        addFunctionCompletions(result, prefix)

        // Add type completions
        addTypeCompletions(result, prefix)

        // Add snippet completions
        addSnippetCompletions(result, prefix)
    }

    private fun addKeywordCompletions(result: CompletionResultSet, prefix: String) {
        KEYWORDS.forEach { keyword ->
            if (keyword.lowercase().startsWith(prefix)) {
                result.addElement(
                    LookupElementBuilder.create(keyword)
                        .withIcon(SurqlIcons.FILE)
                        .withTypeText("keyword")
                        .withBoldness(true)
                        .withCaseSensitivity(false)
                )
            }
        }
    }

    private fun addFunctionCompletions(result: CompletionResultSet, prefix: String) {
        FUNCTIONS.forEach { (name, description) ->
            if (name.lowercase().startsWith(prefix) || name.lowercase().contains("::$prefix")) {
                result.addElement(
                    LookupElementBuilder.create(name)
                        .withIcon(SurqlIcons.FILE)
                        .withTypeText("function")
                        .withTailText("()", true)
                        .withInsertHandler { ctx, _ ->
                            ctx.document.insertString(ctx.tailOffset, "()")
                            ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                        }
                )
            }
        }
    }

    private fun addTypeCompletions(result: CompletionResultSet, prefix: String) {
        TYPES.forEach { type ->
            if (type.lowercase().startsWith(prefix)) {
                result.addElement(
                    LookupElementBuilder.create(type)
                        .withIcon(SurqlIcons.FILE)
                        .withTypeText("type")
                        .withCaseSensitivity(false)
                )
            }
        }
    }

    private fun addSnippetCompletions(result: CompletionResultSet, prefix: String) {
        SNIPPETS.forEach { (trigger, snippet, description) ->
            if (trigger.lowercase().startsWith(prefix)) {
                result.addElement(
                    LookupElementBuilder.create(trigger)
                        .withIcon(SurqlIcons.FILE)
                        .withTypeText("snippet")
                        .withTailText(" $description", true)
                        .withInsertHandler { ctx, _ ->
                            val startOffset = ctx.startOffset
                            val tailOffset = ctx.tailOffset
                            ctx.document.replaceString(startOffset, tailOffset, snippet)
                            
                            // Position cursor at first placeholder or end
                            val cursorPos = snippet.indexOf('|')
                            if (cursorPos >= 0) {
                                val finalSnippet = snippet.replace("|", "")
                                ctx.document.replaceString(startOffset, startOffset + snippet.length, finalSnippet)
                                ctx.editor.caretModel.moveToOffset(startOffset + cursorPos)
                            } else {
                                ctx.editor.caretModel.moveToOffset(startOffset + snippet.length)
                            }
                        }
                )
            }
        }
    }

    companion object {
        // Statement keywords
        private val KEYWORDS = listOf(
            // DML
            "SELECT", "FROM", "WHERE", "CREATE", "UPDATE", "DELETE", "INSERT", "UPSERT", "RELATE",
            // DDL
            "DEFINE", "REMOVE", "ALTER", "INFO",
            // Clauses
            "SET", "UNSET", "CONTENT", "MERGE", "PATCH", "REPLACE", "ONLY",
            "LIMIT", "START", "ORDER", "BY", "ASC", "DESC", "GROUP", "SPLIT",
            "FETCH", "TIMEOUT", "PARALLEL", "EXPLAIN", "WITH", "INDEX", "NOINDEX",
            "VERSION", "DIFF", "OMIT", "VALUE", "VALUES",
            // Control flow
            "IF", "ELSE", "THEN", "FOR", "IN", "RETURN", "THROW", "BREAK", "CONTINUE",
            // Transaction
            "BEGIN", "COMMIT", "CANCEL", "TRANSACTION",
            // Definitions
            "NAMESPACE", "NS", "DATABASE", "DB", "TABLE", "FIELD", "EVENT",
            "FUNCTION", "PARAM", "ANALYZER", "ACCESS", "USER", "TOKEN", "SCOPE",
            "MODEL", "CONFIG", "API", "INDEX",
            // Table options
            "SCHEMAFULL", "SCHEMALESS", "DROP", "CHANGEFEED", "PERMISSIONS",
            "FULL", "NONE", "COMMENT", "OVERWRITE", "NOT", "EXISTS", "AS",
            // Field options
            "TYPE", "FLEXIBLE", "READONLY", "DEFAULT", "ASSERT",
            // Index options
            "UNIQUE", "SEARCH", "BM25", "HIGHLIGHTS", "MTREE", "HNSW",
            "DIMENSION", "DIST", "CONCURRENTLY",
            // Access options
            "JWT", "RECORD", "ALGORITHM", "ISSUER", "URL", "BEARER",
            "SIGNUP", "SIGNIN", "AUTHENTICATE", "PASSWORD", "PASSHASH",
            "ROLES", "DURATION", "SESSION", "REVOKE", "GRANT",
            // Operators
            "AND", "OR", "IS", "NOT", "IN", "CONTAINS", "CONTAINSALL",
            "CONTAINSANY", "CONTAINSNONE", "INSIDE", "OUTSIDE", "INTERSECTS",
            "MATCHES", "LIKE", "COLLATE", "NUMERIC",
            // Literals
            "TRUE", "FALSE", "NULL", "NONE",
            // Other
            "USE", "LET", "LIVE", "KILL", "SHOW", "SLEEP", "OPTION", "REBUILD",
            "ON", "DUPLICATE", "KEY", "IGNORE", "RELATION"
        )

        // Built-in functions organized by namespace
        private val FUNCTIONS = listOf(
            // Array functions
            "array::add" to "Add an item to an array",
            "array::all" to "Check if all items match",
            "array::any" to "Check if any items match",
            "array::at" to "Get item at index",
            "array::append" to "Append items to array",
            "array::boolean_and" to "Boolean AND across array",
            "array::boolean_not" to "Boolean NOT across array",
            "array::boolean_or" to "Boolean OR across array",
            "array::boolean_xor" to "Boolean XOR across array",
            "array::clump" to "Group items into chunks",
            "array::combine" to "Combine two arrays",
            "array::complement" to "Get complement of arrays",
            "array::concat" to "Concatenate arrays",
            "array::difference" to "Get difference between arrays",
            "array::distinct" to "Get distinct items",
            "array::filter_index" to "Filter by index",
            "array::find_index" to "Find item index",
            "array::first" to "Get first item",
            "array::flatten" to "Flatten nested arrays",
            "array::group" to "Group items",
            "array::insert" to "Insert item at index",
            "array::intersect" to "Get intersection of arrays",
            "array::is_empty" to "Check if array is empty",
            "array::join" to "Join array items",
            "array::last" to "Get last item",
            "array::len" to "Get array length",
            "array::logical_and" to "Logical AND across array",
            "array::logical_or" to "Logical OR across array",
            "array::logical_xor" to "Logical XOR across array",
            "array::matches" to "Check which items match",
            "array::max" to "Get maximum value",
            "array::min" to "Get minimum value",
            "array::pop" to "Remove and return last item",
            "array::prepend" to "Prepend items to array",
            "array::push" to "Push item to array",
            "array::remove" to "Remove item at index",
            "array::reverse" to "Reverse array",
            "array::shuffle" to "Shuffle array items",
            "array::slice" to "Get array slice",
            "array::sort" to "Sort array",
            "array::transpose" to "Transpose 2D array",
            "array::union" to "Get union of arrays",
            "array::windows" to "Get sliding windows",

            // Count function
            "count" to "Count items",

            // Crypto functions
            "crypto::argon2::compare" to "Compare argon2 hash",
            "crypto::argon2::generate" to "Generate argon2 hash",
            "crypto::bcrypt::compare" to "Compare bcrypt hash",
            "crypto::bcrypt::generate" to "Generate bcrypt hash",
            "crypto::md5" to "Generate MD5 hash",
            "crypto::scrypt::compare" to "Compare scrypt hash",
            "crypto::scrypt::generate" to "Generate scrypt hash",
            "crypto::sha1" to "Generate SHA-1 hash",
            "crypto::sha256" to "Generate SHA-256 hash",
            "crypto::sha512" to "Generate SHA-512 hash",

            // Duration functions
            "duration::days" to "Get days from duration",
            "duration::hours" to "Get hours from duration",
            "duration::micros" to "Get microseconds from duration",
            "duration::millis" to "Get milliseconds from duration",
            "duration::mins" to "Get minutes from duration",
            "duration::nanos" to "Get nanoseconds from duration",
            "duration::secs" to "Get seconds from duration",
            "duration::weeks" to "Get weeks from duration",
            "duration::years" to "Get years from duration",
            "duration::from::days" to "Create duration from days",
            "duration::from::hours" to "Create duration from hours",
            "duration::from::micros" to "Create duration from microseconds",
            "duration::from::millis" to "Create duration from milliseconds",
            "duration::from::mins" to "Create duration from minutes",
            "duration::from::nanos" to "Create duration from nanoseconds",
            "duration::from::secs" to "Create duration from seconds",
            "duration::from::weeks" to "Create duration from weeks",

            // Geo functions
            "geo::area" to "Calculate area",
            "geo::bearing" to "Calculate bearing",
            "geo::centroid" to "Calculate centroid",
            "geo::distance" to "Calculate distance",
            "geo::hash::decode" to "Decode geohash",
            "geo::hash::encode" to "Encode geohash",

            // HTTP functions
            "http::delete" to "HTTP DELETE request",
            "http::get" to "HTTP GET request",
            "http::head" to "HTTP HEAD request",
            "http::patch" to "HTTP PATCH request",
            "http::post" to "HTTP POST request",
            "http::put" to "HTTP PUT request",

            // Math functions
            "math::abs" to "Absolute value",
            "math::acos" to "Arc cosine",
            "math::asin" to "Arc sine",
            "math::atan" to "Arc tangent",
            "math::bottom" to "Get bottom N values",
            "math::ceil" to "Ceiling value",
            "math::cos" to "Cosine",
            "math::cot" to "Cotangent",
            "math::deg2rad" to "Degrees to radians",
            "math::e" to "Euler's number",
            "math::fixed" to "Fixed decimal places",
            "math::floor" to "Floor value",
            "math::inf" to "Infinity",
            "math::interquartile" to "Interquartile range",
            "math::ln" to "Natural logarithm",
            "math::log" to "Logarithm",
            "math::log10" to "Base-10 logarithm",
            "math::log2" to "Base-2 logarithm",
            "math::max" to "Maximum value",
            "math::mean" to "Mean average",
            "math::median" to "Median value",
            "math::min" to "Minimum value",
            "math::midhinge" to "Midhinge value",
            "math::mode" to "Mode value",
            "math::nearestrank" to "Nearest rank percentile",
            "math::neg_inf" to "Negative infinity",
            "math::percentile" to "Percentile value",
            "math::pi" to "Pi constant",
            "math::pow" to "Power function",
            "math::product" to "Product of values",
            "math::rad2deg" to "Radians to degrees",
            "math::round" to "Round value",
            "math::sign" to "Sign of value",
            "math::sin" to "Sine",
            "math::spread" to "Spread of values",
            "math::sqrt" to "Square root",
            "math::stddev" to "Standard deviation",
            "math::sum" to "Sum of values",
            "math::tan" to "Tangent",
            "math::tau" to "Tau constant",
            "math::top" to "Get top N values",
            "math::trimean" to "Trimean value",
            "math::variance" to "Variance",

            // Meta functions
            "meta::id" to "Get record ID",
            "meta::table" to "Get table name",
            "meta::tb" to "Get table name (alias)",

            // Object functions
            "object::entries" to "Get object entries",
            "object::from_entries" to "Create object from entries",
            "object::keys" to "Get object keys",
            "object::len" to "Get object length",
            "object::values" to "Get object values",

            // Parse functions
            "parse::email::host" to "Parse email host",
            "parse::email::user" to "Parse email user",
            "parse::url::domain" to "Parse URL domain",
            "parse::url::fragment" to "Parse URL fragment",
            "parse::url::host" to "Parse URL host",
            "parse::url::path" to "Parse URL path",
            "parse::url::port" to "Parse URL port",
            "parse::url::query" to "Parse URL query",
            "parse::url::scheme" to "Parse URL scheme",

            // Rand functions
            "rand" to "Random float 0-1",
            "rand::bool" to "Random boolean",
            "rand::enum" to "Random enum value",
            "rand::float" to "Random float",
            "rand::guid" to "Random GUID",
            "rand::int" to "Random integer",
            "rand::string" to "Random string",
            "rand::time" to "Random datetime",
            "rand::ulid" to "Random ULID",
            "rand::uuid" to "Random UUID",
            "rand::uuid::v4" to "Random UUID v4",
            "rand::uuid::v7" to "Random UUID v7",

            // Record functions  
            "record::exists" to "Check if record exists",
            "record::id" to "Get record ID part",
            "record::table" to "Get table from record",
            "record::tb" to "Get table from record (alias)",

            // Search functions
            "search::analyze" to "Analyze search terms",
            "search::highlight" to "Highlight search matches",
            "search::offsets" to "Get match offsets",
            "search::score" to "Get search score",

            // Session functions
            "session::db" to "Get current database",
            "session::id" to "Get session ID",
            "session::ip" to "Get client IP",
            "session::ns" to "Get current namespace",
            "session::origin" to "Get request origin",
            "session::ac" to "Get access method",
            "session::rd" to "Get record access",
            "session::token" to "Get session token",

            // Sleep function
            "sleep" to "Sleep for duration",

            // String functions
            "string::concat" to "Concatenate strings",
            "string::contains" to "Check if contains substring",
            "string::ends_with" to "Check if ends with",
            "string::html::encode" to "HTML encode",
            "string::html::sanitize" to "HTML sanitize",
            "string::is::alphanum" to "Check if alphanumeric",
            "string::is::alpha" to "Check if alphabetic",
            "string::is::ascii" to "Check if ASCII",
            "string::is::datetime" to "Check if datetime",
            "string::is::domain" to "Check if domain",
            "string::is::email" to "Check if email",
            "string::is::hexadecimal" to "Check if hexadecimal",
            "string::is::ip" to "Check if IP address",
            "string::is::ipv4" to "Check if IPv4",
            "string::is::ipv6" to "Check if IPv6",
            "string::is::latitude" to "Check if latitude",
            "string::is::longitude" to "Check if longitude",
            "string::is::numeric" to "Check if numeric",
            "string::is::semver" to "Check if semver",
            "string::is::url" to "Check if URL",
            "string::is::uuid" to "Check if UUID",
            "string::join" to "Join strings",
            "string::len" to "Get string length",
            "string::lowercase" to "Convert to lowercase",
            "string::matches" to "Check if matches regex",
            "string::repeat" to "Repeat string",
            "string::replace" to "Replace substring",
            "string::reverse" to "Reverse string",
            "string::similarity::fuzzy" to "Fuzzy similarity",
            "string::similarity::jaro" to "Jaro similarity",
            "string::similarity::smithwaterman" to "Smith-Waterman similarity",
            "string::slice" to "Get string slice",
            "string::slug" to "Convert to slug",
            "string::split" to "Split string",
            "string::starts_with" to "Check if starts with",
            "string::trim" to "Trim whitespace",
            "string::trim_end" to "Trim end whitespace",
            "string::trim_start" to "Trim start whitespace",
            "string::uppercase" to "Convert to uppercase",
            "string::words" to "Split into words",

            // Time functions
            "time::ceil" to "Ceiling datetime",
            "time::day" to "Get day",
            "time::floor" to "Floor datetime",
            "time::format" to "Format datetime",
            "time::group" to "Group datetime",
            "time::hour" to "Get hour",
            "time::max" to "Maximum datetime",
            "time::micros" to "Get microseconds",
            "time::millis" to "Get milliseconds",
            "time::min" to "Minimum datetime",
            "time::minute" to "Get minute",
            "time::month" to "Get month",
            "time::nano" to "Get nanoseconds",
            "time::now" to "Current datetime",
            "time::round" to "Round datetime",
            "time::second" to "Get second",
            "time::timezone" to "Get timezone",
            "time::unix" to "Get Unix timestamp",
            "time::wday" to "Get weekday",
            "time::week" to "Get week number",
            "time::yday" to "Get day of year",
            "time::year" to "Get year",
            "time::from::micros" to "Create from microseconds",
            "time::from::millis" to "Create from milliseconds",
            "time::from::nanos" to "Create from nanoseconds",
            "time::from::secs" to "Create from seconds",
            "time::from::unix" to "Create from Unix timestamp",

            // Type functions
            "type::bool" to "Convert to boolean",
            "type::bytes" to "Convert to bytes",
            "type::datetime" to "Convert to datetime",
            "type::decimal" to "Convert to decimal",
            "type::duration" to "Convert to duration",
            "type::field" to "Get field path",
            "type::fields" to "Get field paths",
            "type::float" to "Convert to float",
            "type::int" to "Convert to integer",
            "type::is::array" to "Check if array",
            "type::is::bool" to "Check if boolean",
            "type::is::bytes" to "Check if bytes",
            "type::is::collection" to "Check if collection",
            "type::is::datetime" to "Check if datetime",
            "type::is::decimal" to "Check if decimal",
            "type::is::duration" to "Check if duration",
            "type::is::float" to "Check if float",
            "type::is::geometry" to "Check if geometry",
            "type::is::int" to "Check if integer",
            "type::is::line" to "Check if line",
            "type::is::none" to "Check if none",
            "type::is::null" to "Check if null",
            "type::is::multiline" to "Check if multiline",
            "type::is::multipoint" to "Check if multipoint",
            "type::is::multipolygon" to "Check if multipolygon",
            "type::is::number" to "Check if number",
            "type::is::object" to "Check if object",
            "type::is::point" to "Check if point",
            "type::is::polygon" to "Check if polygon",
            "type::is::record" to "Check if record",
            "type::is::string" to "Check if string",
            "type::is::uuid" to "Check if UUID",
            "type::number" to "Convert to number",
            "type::point" to "Convert to point",
            "type::range" to "Create range",
            "type::record" to "Convert to record",
            "type::string" to "Convert to string",
            "type::table" to "Convert to table",
            "type::thing" to "Convert to thing",

            // Vector functions
            "vector::add" to "Add vectors",
            "vector::angle" to "Angle between vectors",
            "vector::cross" to "Cross product",
            "vector::divide" to "Divide vectors",
            "vector::dot" to "Dot product",
            "vector::magnitude" to "Vector magnitude",
            "vector::multiply" to "Multiply vectors",
            "vector::normalize" to "Normalize vector",
            "vector::project" to "Project vector",
            "vector::subtract" to "Subtract vectors",
            "vector::distance::chebyshev" to "Chebyshev distance",
            "vector::distance::euclidean" to "Euclidean distance",
            "vector::distance::hamming" to "Hamming distance",
            "vector::distance::jaccard" to "Jaccard distance",
            "vector::distance::manhattan" to "Manhattan distance",
            "vector::distance::minkowski" to "Minkowski distance",
            "vector::similarity::cosine" to "Cosine similarity",
            "vector::similarity::jaccard" to "Jaccard similarity",
            "vector::similarity::pearson" to "Pearson correlation"
        )

        // Type names
        private val TYPES = listOf(
            "any",
            "array",
            "bool",
            "bytes",
            "datetime",
            "decimal",
            "duration",
            "float",
            "geometry",
            "int",
            "null",
            "number",
            "object",
            "option",
            "point",
            "polygon",
            "line",
            "multipoint",
            "multiline", 
            "multipolygon",
            "collection",
            "record",
            "set",
            "string",
            "uuid",
            "range",
            "literal",
            "either",
            "future",
            "refs"
        )

        // Common code snippets (trigger, snippet with | for cursor, description)
        private val SNIPPETS = listOf(
            Triple("sel", "SELECT * FROM |", "Select all from table"),
            Triple("selw", "SELECT * FROM | WHERE ", "Select with WHERE"),
            Triple("cre", "CREATE | CONTENT {\n\t\n}", "Create record"),
            Triple("upd", "UPDATE | SET ", "Update record"),
            Triple("updm", "UPDATE | MERGE {\n\t\n}", "Update with MERGE"),
            Triple("del", "DELETE |", "Delete record"),
            Triple("ins", "INSERT INTO | [\n\t\n]", "Insert records"),
            Triple("rel", "RELATE |->->", "Create relation"),
            Triple("deftable", "DEFINE TABLE | SCHEMAFULL", "Define table"),
            Triple("deffield", "DEFINE FIELD | ON TABLE  TYPE ", "Define field"),
            Triple("defindex", "DEFINE INDEX | ON TABLE  FIELDS ", "Define index"),
            Triple("deffn", "DEFINE FUNCTION fn::|() {\n\t\n}", "Define function"),
            Triple("defev", "DEFINE EVENT | ON TABLE  WHEN  THEN (\n\t\n)", "Define event"),
            Triple("defacc", "DEFINE ACCESS | ON DATABASE TYPE RECORD\n\tSIGNUP ( )\n\tSIGNIN ( )", "Define access"),
            Triple("txn", "BEGIN TRANSACTION;\n|\nCOMMIT TRANSACTION;", "Transaction block"),
            Triple("ifelse", "IF | THEN\n\t\nELSE\n\t\nEND", "If-else block"),
            Triple("forin", "FOR | IN  {\n\t\n}", "For loop"),
            Triple("letp", "LET $| = ", "Define parameter"),
            Triple("retsel", "RETURN SELECT * FROM |", "Return select"),
            Triple("info", "INFO FOR |", "Info statement"),
            Triple("live", "LIVE SELECT * FROM |", "Live query")
        )
    }
}
