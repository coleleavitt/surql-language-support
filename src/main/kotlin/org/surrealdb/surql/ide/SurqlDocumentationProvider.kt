package org.surrealdb.surql.ide

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.surrealdb.surql.lexer.SurqlTokenTypes

/**
 * Provides documentation for SurrealQL elements.
 * 
 * Shows documentation for:
 * - Keywords (SELECT, FROM, WHERE, etc.)
 * - Built-in functions
 * - Types
 * - Operators
 */
class SurqlDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        
        val text = element.text.uppercase()
        val elementType = element.elementType

        // Check for keywords
        if (elementType in SurqlTokenTypes.KEYWORDS) {
            return generateKeywordDoc(text)
        }

        // Check for identifiers that might be functions
        if (elementType == SurqlTokenTypes.IDENTIFIER) {
            val lowerText = element.text.lowercase()
            
            // Check if it's a function namespace
            FUNCTION_DOCS[lowerText]?.let { return it }
            
            // Check for full function names with namespace
            val fullFunctionName = buildFullFunctionName(element)
            FUNCTION_DOCS[fullFunctionName]?.let { return it }
        }

        // Check for types
        if (elementType == SurqlTokenTypes.IDENTIFIER) {
            TYPE_DOCS[element.text.lowercase()]?.let { return it }
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        
        val text = element.text.uppercase()
        val elementType = element.elementType

        if (elementType in SurqlTokenTypes.KEYWORDS) {
            return KEYWORD_BRIEF[text]
        }

        return null
    }

    private fun buildFullFunctionName(element: PsiElement): String {
        val parts = mutableListOf<String>()
        var current: PsiElement? = element
        
        // Walk backwards to find namespace parts
        while (current != null) {
            if (current.elementType == SurqlTokenTypes.IDENTIFIER) {
                parts.add(0, current.text.lowercase())
            }
            val prev = current.prevSibling
            if (prev?.elementType == SurqlTokenTypes.COLONCOLON) {
                current = prev.prevSibling
            } else {
                break
            }
        }
        
        // Walk forwards to find more parts
        current = element.nextSibling
        while (current != null) {
            if (current.elementType == SurqlTokenTypes.COLONCOLON) {
                current = current.nextSibling
                if (current?.elementType == SurqlTokenTypes.IDENTIFIER) {
                    parts.add(current.text.lowercase())
                    current = current.nextSibling
                } else {
                    break
                }
            } else {
                break
            }
        }
        
        return parts.joinToString("::")
    }

    private fun generateKeywordDoc(keyword: String): String? {
        val doc = KEYWORD_DOCS[keyword] ?: return null
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append("<b>$keyword</b>")
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(doc)
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    companion object {
        // Brief keyword descriptions for quick info
        private val KEYWORD_BRIEF = mapOf(
            "SELECT" to "Query data from tables",
            "FROM" to "Specify data source",
            "WHERE" to "Filter results",
            "CREATE" to "Create a new record",
            "UPDATE" to "Update existing records",
            "DELETE" to "Delete records",
            "INSERT" to "Insert new records",
            "UPSERT" to "Insert or update records",
            "RELATE" to "Create graph relationships",
            "DEFINE" to "Define schema elements",
            "REMOVE" to "Remove schema definitions"
        )

        // Full keyword documentation
        private val KEYWORD_DOCS = mapOf(
            "SELECT" to """
                <p>The SELECT statement retrieves data from the database.</p>
                <h4>Syntax</h4>
                <pre>SELECT [VALUE] @fields FROM @targets [WHERE @condition] [SPLIT @field] [GROUP [BY] @field] [ORDER [BY] @field [ASC|DESC]] [LIMIT @limit] [START @start] [FETCH @field] [TIMEOUT @duration] [PARALLEL]</pre>
                <h4>Examples</h4>
                <pre>SELECT * FROM person;
SELECT name, age FROM person WHERE age > 18;
SELECT VALUE name FROM person;
SELECT * FROM person:john;</pre>
            """.trimIndent(),

            "FROM" to """
                <p>Specifies the data source for a query.</p>
                <h4>Usage</h4>
                <pre>SELECT * FROM table_name;
SELECT * FROM record_id;
SELECT * FROM table1, table2;</pre>
            """.trimIndent(),

            "WHERE" to """
                <p>Filters results based on conditions.</p>
                <h4>Operators</h4>
                <ul>
                    <li><code>=</code>, <code>==</code> - Equal</li>
                    <li><code>!=</code> - Not equal</li>
                    <li><code>&lt;</code>, <code>&gt;</code>, <code>&lt;=</code>, <code>&gt;=</code> - Comparison</li>
                    <li><code>AND</code>, <code>OR</code>, <code>NOT</code> - Logical</li>
                    <li><code>CONTAINS</code>, <code>IN</code> - Containment</li>
                </ul>
                <h4>Examples</h4>
                <pre>WHERE age > 18
WHERE name = 'John' AND active = true
WHERE tags CONTAINS 'admin'</pre>
            """.trimIndent(),

            "CREATE" to """
                <p>Creates a new record in the database.</p>
                <h4>Syntax</h4>
                <pre>CREATE @targets [CONTENT @object | SET @field = @value, ...]</pre>
                <h4>Examples</h4>
                <pre>CREATE person CONTENT { name: 'John', age: 30 };
CREATE person:john SET name = 'John', age = 30;
CREATE person SET name = 'Jane';</pre>
            """.trimIndent(),

            "UPDATE" to """
                <p>Updates existing records in the database.</p>
                <h4>Syntax</h4>
                <pre>UPDATE @targets [CONTENT @object | MERGE @object | SET @field = @value, ...] [WHERE @condition]</pre>
                <h4>Examples</h4>
                <pre>UPDATE person SET age = 31 WHERE name = 'John';
UPDATE person:john MERGE { email: 'john@example.com' };
UPDATE person CONTENT { status: 'active' } WHERE verified = true;</pre>
            """.trimIndent(),

            "DELETE" to """
                <p>Deletes records from the database.</p>
                <h4>Syntax</h4>
                <pre>DELETE @targets [WHERE @condition]</pre>
                <h4>Examples</h4>
                <pre>DELETE person:john;
DELETE person WHERE active = false;
DELETE person;</pre>
            """.trimIndent(),

            "INSERT" to """
                <p>Inserts one or more records into a table.</p>
                <h4>Syntax</h4>
                <pre>INSERT [IGNORE] INTO @table @values [ON DUPLICATE KEY UPDATE @assignments]</pre>
                <h4>Examples</h4>
                <pre>INSERT INTO person { name: 'John', age: 30 };
INSERT INTO person [
    { name: 'John', age: 30 },
    { name: 'Jane', age: 25 }
];</pre>
            """.trimIndent(),

            "UPSERT" to """
                <p>Inserts a new record or updates an existing one.</p>
                <h4>Syntax</h4>
                <pre>UPSERT @targets [CONTENT @object | MERGE @object | SET @field = @value, ...]</pre>
                <h4>Examples</h4>
                <pre>UPSERT person:john CONTENT { name: 'John', age: 31 };
UPSERT person:john SET age = 31;</pre>
            """.trimIndent(),

            "RELATE" to """
                <p>Creates graph relationships between records.</p>
                <h4>Syntax</h4>
                <pre>RELATE @from -> @edge -> @to [CONTENT @object | SET @field = @value, ...]</pre>
                <h4>Examples</h4>
                <pre>RELATE person:john -> knows -> person:jane;
RELATE person:john -> likes -> post:123 SET when = time::now();
RELATE user:1 -> purchased -> product:abc CONTENT { quantity: 2 };</pre>
            """.trimIndent(),

            "DEFINE" to """
                <p>Defines schema elements in the database.</p>
                <h4>Available Definitions</h4>
                <ul>
                    <li><code>DEFINE NAMESPACE</code> - Define a namespace</li>
                    <li><code>DEFINE DATABASE</code> - Define a database</li>
                    <li><code>DEFINE TABLE</code> - Define a table</li>
                    <li><code>DEFINE FIELD</code> - Define a field</li>
                    <li><code>DEFINE INDEX</code> - Define an index</li>
                    <li><code>DEFINE EVENT</code> - Define an event trigger</li>
                    <li><code>DEFINE FUNCTION</code> - Define a custom function</li>
                    <li><code>DEFINE ANALYZER</code> - Define a search analyzer</li>
                    <li><code>DEFINE ACCESS</code> - Define access control</li>
                    <li><code>DEFINE USER</code> - Define a user</li>
                </ul>
            """.trimIndent(),

            "REMOVE" to """
                <p>Removes schema definitions from the database.</p>
                <h4>Examples</h4>
                <pre>REMOVE TABLE person;
REMOVE FIELD email ON person;
REMOVE INDEX idx_name ON person;
REMOVE FUNCTION fn::custom;</pre>
            """.trimIndent(),

            "LET" to """
                <p>Defines a parameter for use in subsequent queries.</p>
                <h4>Syntax</h4>
                <pre>LET ${'$'}param = @value;</pre>
                <h4>Examples</h4>
                <pre>LET ${'$'}name = 'John';
LET ${'$'}users = SELECT * FROM user;
LET ${'$'}now = time::now();</pre>
            """.trimIndent(),

            "IF" to """
                <p>Conditional expression.</p>
                <h4>Syntax</h4>
                <pre>IF @condition THEN @result [ELSE IF @condition THEN @result]* [ELSE @result] END</pre>
                <h4>Examples</h4>
                <pre>IF ${'$'}age >= 18 THEN 'adult' ELSE 'minor' END;
IF active THEN status ELSE 'inactive' END;</pre>
            """.trimIndent(),

            "FOR" to """
                <p>Iterates over a collection.</p>
                <h4>Syntax</h4>
                <pre>FOR ${'$'}item IN @collection { @statements }</pre>
                <h4>Examples</h4>
                <pre>FOR ${'$'}user IN (SELECT * FROM user) {
    UPDATE ${'$'}user SET checked = true;
};</pre>
            """.trimIndent(),

            "BEGIN" to """
                <p>Starts a transaction block.</p>
                <h4>Syntax</h4>
                <pre>BEGIN [TRANSACTION];
-- statements
COMMIT [TRANSACTION];</pre>
                <h4>Example</h4>
                <pre>BEGIN TRANSACTION;
CREATE account:from SET balance -= 100;
CREATE account:to SET balance += 100;
COMMIT TRANSACTION;</pre>
            """.trimIndent(),

            "RETURN" to """
                <p>Returns a value from a function or query block.</p>
                <h4>Examples</h4>
                <pre>RETURN ${'$'}result;
RETURN SELECT * FROM person WHERE id = ${'$'}id;</pre>
            """.trimIndent(),

            "LIVE" to """
                <p>Creates a live query that receives real-time updates.</p>
                <h4>Syntax</h4>
                <pre>LIVE SELECT @fields FROM @targets [WHERE @condition]</pre>
                <h4>Example</h4>
                <pre>LIVE SELECT * FROM person WHERE active = true;</pre>
            """.trimIndent(),

            "INFO" to """
                <p>Returns information about database structure.</p>
                <h4>Syntax</h4>
                <pre>INFO FOR [ROOT | NAMESPACE | DATABASE | TABLE @name]</pre>
                <h4>Examples</h4>
                <pre>INFO FOR DB;
INFO FOR TABLE person;
INFO FOR NS;</pre>
            """.trimIndent(),

            "USE" to """
                <p>Switches the current namespace or database.</p>
                <h4>Syntax</h4>
                <pre>USE [NS @namespace] [DB @database]</pre>
                <h4>Examples</h4>
                <pre>USE NS production DB main;
USE DB testing;</pre>
            """.trimIndent(),

            "SCHEMAFULL" to """
                <p>Table option that enforces strict schema validation.</p>
                <p>Only defined fields are allowed on records.</p>
                <h4>Example</h4>
                <pre>DEFINE TABLE person SCHEMAFULL;</pre>
            """.trimIndent(),

            "SCHEMALESS" to """
                <p>Table option that allows any fields on records (default).</p>
                <h4>Example</h4>
                <pre>DEFINE TABLE person SCHEMALESS;</pre>
            """.trimIndent(),

            "PERMISSIONS" to """
                <p>Defines access permissions for tables and fields.</p>
                <h4>Syntax</h4>
                <pre>PERMISSIONS [NONE | FULL | FOR select, create, update, delete WHERE @condition]</pre>
                <h4>Example</h4>
                <pre>DEFINE TABLE post PERMISSIONS
    FOR select FULL
    FOR create, update WHERE ${'$'}auth.id = author
    FOR delete WHERE ${'$'}auth.role = 'admin';</pre>
            """.trimIndent()
        )

        // Function documentation
        private val FUNCTION_DOCS = mapOf(
            "array" to """
                ${DocumentationMarkup.DEFINITION_START}<b>array</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Functions for working with arrays.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>array::len()</code> - Get array length</li>
                    <li><code>array::push()</code> - Add item to end</li>
                    <li><code>array::pop()</code> - Remove last item</li>
                    <li><code>array::first()</code> - Get first item</li>
                    <li><code>array::last()</code> - Get last item</li>
                    <li><code>array::distinct()</code> - Get unique items</li>
                    <li><code>array::sort()</code> - Sort array</li>
                    <li><code>array::reverse()</code> - Reverse array</li>
                    <li><code>array::flatten()</code> - Flatten nested arrays</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "string" to """
                ${DocumentationMarkup.DEFINITION_START}<b>string</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Functions for working with strings.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>string::len()</code> - Get string length</li>
                    <li><code>string::lowercase()</code> - Convert to lowercase</li>
                    <li><code>string::uppercase()</code> - Convert to uppercase</li>
                    <li><code>string::trim()</code> - Remove whitespace</li>
                    <li><code>string::split()</code> - Split into array</li>
                    <li><code>string::join()</code> - Join array to string</li>
                    <li><code>string::replace()</code> - Replace substring</li>
                    <li><code>string::contains()</code> - Check substring</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "time" to """
                ${DocumentationMarkup.DEFINITION_START}<b>time</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Functions for working with dates and times.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>time::now()</code> - Current datetime</li>
                    <li><code>time::year()</code> - Get year</li>
                    <li><code>time::month()</code> - Get month</li>
                    <li><code>time::day()</code> - Get day</li>
                    <li><code>time::hour()</code> - Get hour</li>
                    <li><code>time::minute()</code> - Get minute</li>
                    <li><code>time::format()</code> - Format datetime</li>
                    <li><code>time::unix()</code> - Get Unix timestamp</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "math" to """
                ${DocumentationMarkup.DEFINITION_START}<b>math</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Mathematical functions.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>math::abs()</code> - Absolute value</li>
                    <li><code>math::ceil()</code> - Ceiling</li>
                    <li><code>math::floor()</code> - Floor</li>
                    <li><code>math::round()</code> - Round</li>
                    <li><code>math::sqrt()</code> - Square root</li>
                    <li><code>math::pow()</code> - Power</li>
                    <li><code>math::min()</code> - Minimum</li>
                    <li><code>math::max()</code> - Maximum</li>
                    <li><code>math::sum()</code> - Sum</li>
                    <li><code>math::mean()</code> - Average</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "crypto" to """
                ${DocumentationMarkup.DEFINITION_START}<b>crypto</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Cryptographic functions.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>crypto::argon2::generate()</code> - Hash with Argon2</li>
                    <li><code>crypto::argon2::compare()</code> - Verify Argon2 hash</li>
                    <li><code>crypto::bcrypt::generate()</code> - Hash with bcrypt</li>
                    <li><code>crypto::bcrypt::compare()</code> - Verify bcrypt hash</li>
                    <li><code>crypto::md5()</code> - MD5 hash</li>
                    <li><code>crypto::sha256()</code> - SHA-256 hash</li>
                    <li><code>crypto::sha512()</code> - SHA-512 hash</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "rand" to """
                ${DocumentationMarkup.DEFINITION_START}<b>rand</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Random value generation functions.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>rand()</code> - Random float 0-1</li>
                    <li><code>rand::bool()</code> - Random boolean</li>
                    <li><code>rand::int()</code> - Random integer</li>
                    <li><code>rand::float()</code> - Random float</li>
                    <li><code>rand::string()</code> - Random string</li>
                    <li><code>rand::uuid()</code> - Random UUID</li>
                    <li><code>rand::ulid()</code> - Random ULID</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "type" to """
                ${DocumentationMarkup.DEFINITION_START}<b>type</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Type conversion and checking functions.</p>
                <h4>Conversion Functions</h4>
                <ul>
                    <li><code>type::bool()</code> - Convert to boolean</li>
                    <li><code>type::int()</code> - Convert to integer</li>
                    <li><code>type::float()</code> - Convert to float</li>
                    <li><code>type::string()</code> - Convert to string</li>
                    <li><code>type::datetime()</code> - Convert to datetime</li>
                    <li><code>type::number()</code> - Convert to number</li>
                </ul>
                <h4>Check Functions</h4>
                <ul>
                    <li><code>type::is::bool()</code> - Check if boolean</li>
                    <li><code>type::is::int()</code> - Check if integer</li>
                    <li><code>type::is::string()</code> - Check if string</li>
                    <li><code>type::is::array()</code> - Check if array</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "geo" to """
                ${DocumentationMarkup.DEFINITION_START}<b>geo</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Geospatial functions.</p>
                <h4>Common Functions</h4>
                <ul>
                    <li><code>geo::distance()</code> - Calculate distance</li>
                    <li><code>geo::area()</code> - Calculate area</li>
                    <li><code>geo::bearing()</code> - Calculate bearing</li>
                    <li><code>geo::centroid()</code> - Calculate centroid</li>
                    <li><code>geo::hash::encode()</code> - Encode geohash</li>
                    <li><code>geo::hash::decode()</code> - Decode geohash</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "http" to """
                ${DocumentationMarkup.DEFINITION_START}<b>http</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>HTTP request functions.</p>
                <h4>Functions</h4>
                <ul>
                    <li><code>http::get()</code> - HTTP GET request</li>
                    <li><code>http::post()</code> - HTTP POST request</li>
                    <li><code>http::put()</code> - HTTP PUT request</li>
                    <li><code>http::patch()</code> - HTTP PATCH request</li>
                    <li><code>http::delete()</code> - HTTP DELETE request</li>
                    <li><code>http::head()</code> - HTTP HEAD request</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "meta" to """
                ${DocumentationMarkup.DEFINITION_START}<b>meta</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Record metadata functions.</p>
                <h4>Functions</h4>
                <ul>
                    <li><code>meta::id()</code> - Get record ID part</li>
                    <li><code>meta::table()</code> / <code>meta::tb()</code> - Get table name</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "record" to """
                ${DocumentationMarkup.DEFINITION_START}<b>record</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Record utility functions.</p>
                <h4>Functions</h4>
                <ul>
                    <li><code>record::exists()</code> - Check if record exists</li>
                    <li><code>record::id()</code> - Get record ID part</li>
                    <li><code>record::table()</code> / <code>record::tb()</code> - Get table name</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "session" to """
                ${DocumentationMarkup.DEFINITION_START}<b>session</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Session information functions.</p>
                <h4>Functions</h4>
                <ul>
                    <li><code>session::db()</code> - Current database</li>
                    <li><code>session::ns()</code> - Current namespace</li>
                    <li><code>session::id()</code> - Session ID</li>
                    <li><code>session::ip()</code> - Client IP address</li>
                    <li><code>session::origin()</code> - Request origin</li>
                    <li><code>session::token()</code> - Session token</li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "vector" to """
                ${DocumentationMarkup.DEFINITION_START}<b>vector</b> namespace${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Vector math functions for machine learning and similarity search.</p>
                <h4>Operations</h4>
                <ul>
                    <li><code>vector::add()</code> - Add vectors</li>
                    <li><code>vector::subtract()</code> - Subtract vectors</li>
                    <li><code>vector::multiply()</code> - Multiply vectors</li>
                    <li><code>vector::dot()</code> - Dot product</li>
                    <li><code>vector::magnitude()</code> - Vector magnitude</li>
                    <li><code>vector::normalize()</code> - Normalize vector</li>
                </ul>
                <h4>Distance Functions</h4>
                <ul>
                    <li><code>vector::distance::euclidean()</code></li>
                    <li><code>vector::distance::cosine()</code></li>
                    <li><code>vector::distance::manhattan()</code></li>
                </ul>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "count" to """
                ${DocumentationMarkup.DEFINITION_START}<b>count</b>()${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Counts items in an array or results.</p>
                <h4>Syntax</h4>
                <pre>count(@value)</pre>
                <h4>Example</h4>
                <pre>SELECT count() FROM person GROUP ALL;
SELECT count(tags) FROM post;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent()
        )

        // Type documentation
        private val TYPE_DOCS = mapOf(
            "any" to """
                ${DocumentationMarkup.DEFINITION_START}<b>any</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Accepts any value type. Use when the field can contain any data.</p>
                <h4>Example</h4>
                <pre>DEFINE FIELD data ON table TYPE any;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "array" to """
                ${DocumentationMarkup.DEFINITION_START}<b>array</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>An ordered collection of values.</p>
                <h4>Variants</h4>
                <ul>
                    <li><code>array</code> - Array of any type</li>
                    <li><code>array&lt;string&gt;</code> - Array of strings</li>
                    <li><code>array&lt;int&gt;</code> - Array of integers</li>
                    <li><code>array&lt;record&lt;user&gt;&gt;</code> - Array of user records</li>
                </ul>
                <h4>Example</h4>
                <pre>DEFINE FIELD tags ON post TYPE array&lt;string&gt;;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "bool" to """
                ${DocumentationMarkup.DEFINITION_START}<b>bool</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Boolean value: <code>true</code> or <code>false</code>.</p>
                <h4>Example</h4>
                <pre>DEFINE FIELD active ON user TYPE bool DEFAULT true;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "datetime" to """
                ${DocumentationMarkup.DEFINITION_START}<b>datetime</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>ISO 8601 datetime value.</p>
                <h4>Literals</h4>
                <pre>d"2024-01-15T10:30:00Z"
d"2024-01-15"</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD created ON post TYPE datetime DEFAULT time::now();</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "decimal" to """
                ${DocumentationMarkup.DEFINITION_START}<b>decimal</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Arbitrary precision decimal number. Ideal for financial calculations.</p>
                <h4>Literals</h4>
                <pre>123.45dec
99.99dec</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD price ON product TYPE decimal;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "duration" to """
                ${DocumentationMarkup.DEFINITION_START}<b>duration</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Time duration value.</p>
                <h4>Units</h4>
                <ul>
                    <li><code>ns</code> - nanoseconds</li>
                    <li><code>us</code> / <code>µs</code> - microseconds</li>
                    <li><code>ms</code> - milliseconds</li>
                    <li><code>s</code> - seconds</li>
                    <li><code>m</code> - minutes</li>
                    <li><code>h</code> - hours</li>
                    <li><code>d</code> - days</li>
                    <li><code>w</code> - weeks</li>
                    <li><code>y</code> - years</li>
                </ul>
                <h4>Examples</h4>
                <pre>1h30m
7d
100ms</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "float" to """
                ${DocumentationMarkup.DEFINITION_START}<b>float</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>64-bit floating point number.</p>
                <h4>Literals</h4>
                <pre>3.14
1.5e10
-0.001f</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD latitude ON location TYPE float;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "int" to """
                ${DocumentationMarkup.DEFINITION_START}<b>int</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>64-bit signed integer.</p>
                <h4>Example</h4>
                <pre>DEFINE FIELD age ON person TYPE int;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "object" to """
                ${DocumentationMarkup.DEFINITION_START}<b>object</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>A JSON-like object with key-value pairs.</p>
                <h4>Example</h4>
                <pre>DEFINE FIELD metadata ON record TYPE object;

{ name: 'John', age: 30 }</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "record" to """
                ${DocumentationMarkup.DEFINITION_START}<b>record</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>A reference to a record in a table.</p>
                <h4>Variants</h4>
                <ul>
                    <li><code>record</code> - Any record</li>
                    <li><code>record&lt;user&gt;</code> - Record from user table</li>
                    <li><code>record&lt;user | post&gt;</code> - Record from user or post table</li>
                </ul>
                <h4>Example</h4>
                <pre>DEFINE FIELD author ON post TYPE record&lt;user&gt;;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "string" to """
                ${DocumentationMarkup.DEFINITION_START}<b>string</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>UTF-8 text string.</p>
                <h4>Literals</h4>
                <pre>"double quoted"
'single quoted'</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD name ON person TYPE string;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "uuid" to """
                ${DocumentationMarkup.DEFINITION_START}<b>uuid</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Universally Unique Identifier (UUID).</p>
                <h4>Literals</h4>
                <pre>u"550e8400-e29b-41d4-a716-446655440000"</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD external_id ON record TYPE uuid DEFAULT rand::uuid();</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "geometry" to """
                ${DocumentationMarkup.DEFINITION_START}<b>geometry</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>GeoJSON geometry type for spatial data.</p>
                <h4>Subtypes</h4>
                <ul>
                    <li><code>geometry&lt;point&gt;</code></li>
                    <li><code>geometry&lt;line&gt;</code></li>
                    <li><code>geometry&lt;polygon&gt;</code></li>
                    <li><code>geometry&lt;multipoint&gt;</code></li>
                    <li><code>geometry&lt;multiline&gt;</code></li>
                    <li><code>geometry&lt;multipolygon&gt;</code></li>
                    <li><code>geometry&lt;collection&gt;</code></li>
                </ul>
                <h4>Example</h4>
                <pre>DEFINE FIELD location ON store TYPE geometry&lt;point&gt;;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "option" to """
                ${DocumentationMarkup.DEFINITION_START}<b>option</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>Optional type that may or may not have a value.</p>
                <h4>Usage</h4>
                <pre>option&lt;string&gt;  -- string or NONE
option&lt;int&gt;     -- int or NONE</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD nickname ON person TYPE option&lt;string&gt;;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent(),

            "set" to """
                ${DocumentationMarkup.DEFINITION_START}<b>set</b> type${DocumentationMarkup.DEFINITION_END}
                ${DocumentationMarkup.CONTENT_START}
                <p>A collection of unique values (no duplicates).</p>
                <h4>Usage</h4>
                <pre>set&lt;string&gt;  -- set of unique strings
set&lt;int&gt;     -- set of unique integers</pre>
                <h4>Example</h4>
                <pre>DEFINE FIELD tags ON post TYPE set&lt;string&gt;;</pre>
                ${DocumentationMarkup.CONTENT_END}
            """.trimIndent()
        )
    }
}
