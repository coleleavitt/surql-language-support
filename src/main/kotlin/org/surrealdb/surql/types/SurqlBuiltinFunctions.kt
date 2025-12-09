package org.surrealdb.surql.types

/**
 * Comprehensive registry of all SurrealDB built-in functions with their type signatures.
 * 
 * This is used for:
 * - Type inference of function call expressions
 * - Argument count and type validation
 * - Documentation and completion
 */
object SurqlBuiltinFunctions {
    
    /**
     * Represents a function signature with parameter and return types.
     */
    data class FunctionSignature(
        val name: String,
        val parameters: List<FunctionParameter>,
        val returnType: SurqlType,
        val isVariadic: Boolean = false,
        val description: String = ""
    ) {
        val minArgs: Int = parameters.count { !it.isOptional }
        val maxArgs: Int = if (isVariadic) Int.MAX_VALUE else parameters.size
    }
    
    data class FunctionParameter(
        val name: String,
        val type: SurqlType,
        val isOptional: Boolean = false
    )
    
    // Helper functions for creating signatures
    private fun sig(
        name: String,
        returnType: SurqlType,
        vararg params: Pair<String, SurqlType>,
        variadic: Boolean = false,
        description: String = ""
    ): FunctionSignature {
        return FunctionSignature(
            name = name,
            parameters = params.map { FunctionParameter(it.first, it.second) },
            returnType = returnType,
            isVariadic = variadic,
            description = description
        )
    }
    
    private fun sigOpt(
        name: String,
        returnType: SurqlType,
        required: List<Pair<String, SurqlType>>,
        optional: List<Pair<String, SurqlType>>,
        description: String = ""
    ): FunctionSignature {
        val params = required.map { FunctionParameter(it.first, it.second, false) } +
                     optional.map { FunctionParameter(it.first, it.second, true) }
        return FunctionSignature(
            name = name,
            parameters = params,
            returnType = returnType,
            description = description
        )
    }
    
    // Type aliases for readability
    private val T_ANY = SurqlType.Any
    private val T_BOOL = SurqlType.Bool
    private val T_INT = SurqlType.Int
    private val T_FLOAT = SurqlType.Float
    private val T_NUM = SurqlType.Number
    private val T_DEC = SurqlType.Decimal
    private val T_STR = SurqlType.Str
    private val T_DATETIME = SurqlType.Datetime
    private val T_DURATION = SurqlType.Duration
    private val T_BYTES = SurqlType.Bytes
    private val T_UUID = SurqlType.Uuid
    private val T_NULL = SurqlType.Null
    private val T_NONE = SurqlType.None
    private val T_OBJECT = SurqlType.Object()
    private val T_RECORD = SurqlType.Record()
    private val T_GEO = SurqlType.Geometry()
    private fun T_ARRAY(elem: SurqlType = T_ANY) = SurqlType.Array(elem)
    private fun T_SET(elem: SurqlType = T_ANY) = SurqlType.Set(elem)
    private fun T_OPT(inner: SurqlType) = SurqlType.Option(inner)
    
    /**
     * All built-in function signatures, keyed by full function name (lowercase).
     */
    val signatures: Map<String, FunctionSignature> by lazy {
        buildMap {
            // ==================== Array Functions ====================
            put("array::add", sig("array::add", T_ARRAY(), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::all", sig("array::all", T_BOOL, "array" to T_ARRAY()))
            put("array::any", sig("array::any", T_BOOL, "array" to T_ARRAY()))
            put("array::at", sig("array::at", T_OPT(T_ANY), "array" to T_ARRAY(), "index" to T_INT))
            put("array::append", sig("array::append", T_ARRAY(), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::boolean_and", sig("array::boolean_and", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::boolean_not", sig("array::boolean_not", T_ARRAY(T_BOOL), "array" to T_ARRAY(T_BOOL)))
            put("array::boolean_or", sig("array::boolean_or", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::boolean_xor", sig("array::boolean_xor", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::clump", sig("array::clump", T_ARRAY(T_ARRAY()), "array" to T_ARRAY(), "size" to T_INT))
            put("array::combine", sig("array::combine", T_ARRAY(), "a" to T_ARRAY(), "b" to T_ARRAY()))
            put("array::complement", sig("array::complement", T_ARRAY(), "a" to T_ARRAY(), "b" to T_ARRAY()))
            put("array::concat", sig("array::concat", T_ARRAY(), "arrays" to T_ARRAY(), variadic = true))
            put("array::difference", sig("array::difference", T_ARRAY(), "a" to T_ARRAY(), "b" to T_ARRAY()))
            put("array::distinct", sig("array::distinct", T_ARRAY(), "array" to T_ARRAY()))
            put("array::filter", sig("array::filter", T_ARRAY(), "array" to T_ARRAY(), "closure" to T_ANY))
            put("array::filter_index", sig("array::filter_index", T_ARRAY(T_INT), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::find", sig("array::find", T_OPT(T_ANY), "array" to T_ARRAY(), "closure" to T_ANY))
            put("array::find_index", sig("array::find_index", T_OPT(T_INT), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::first", sig("array::first", T_OPT(T_ANY), "array" to T_ARRAY()))
            put("array::flatten", sig("array::flatten", T_ARRAY(), "array" to T_ARRAY()))
            put("array::fold", sig("array::fold", T_ANY, "array" to T_ARRAY(), "init" to T_ANY, "closure" to T_ANY))
            put("array::group", sig("array::group", T_ARRAY(), "array" to T_ARRAY()))
            put("array::includes", sig("array::includes", T_BOOL, "array" to T_ARRAY(), "value" to T_ANY))
            put("array::index_of", sig("array::index_of", T_OPT(T_INT), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::insert", sig("array::insert", T_ARRAY(), "array" to T_ARRAY(), "value" to T_ANY, "index" to T_INT))
            put("array::intersect", sig("array::intersect", T_ARRAY(), "a" to T_ARRAY(), "b" to T_ARRAY()))
            put("array::is_empty", sig("array::is_empty", T_BOOL, "array" to T_ARRAY()))
            put("array::join", sig("array::join", T_STR, "array" to T_ARRAY(), "separator" to T_STR))
            put("array::knn", sig("array::knn", T_ARRAY(), "array" to T_ARRAY(), "point" to T_ARRAY(), "k" to T_INT))
            put("array::last", sig("array::last", T_OPT(T_ANY), "array" to T_ARRAY()))
            put("array::len", sig("array::len", T_INT, "array" to T_ARRAY()))
            put("array::logical_and", sig("array::logical_and", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::logical_or", sig("array::logical_or", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::logical_xor", sig("array::logical_xor", T_ARRAY(T_BOOL), "a" to T_ARRAY(T_BOOL), "b" to T_ARRAY(T_BOOL)))
            put("array::map", sig("array::map", T_ARRAY(), "array" to T_ARRAY(), "closure" to T_ANY))
            put("array::matches", sig("array::matches", T_ARRAY(T_BOOL), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::max", sig("array::max", T_OPT(T_NUM), "array" to T_ARRAY(T_NUM)))
            put("array::min", sig("array::min", T_OPT(T_NUM), "array" to T_ARRAY(T_NUM)))
            put("array::pop", sig("array::pop", T_ARRAY(), "array" to T_ARRAY()))
            put("array::prepend", sig("array::prepend", T_ARRAY(), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::push", sig("array::push", T_ARRAY(), "array" to T_ARRAY(), "value" to T_ANY))
            put("array::range", sig("array::range", T_ARRAY(T_INT), "start" to T_INT, "end" to T_INT))
            put("array::reduce", sig("array::reduce", T_ANY, "array" to T_ARRAY(), "closure" to T_ANY))
            put("array::remove", sig("array::remove", T_ARRAY(), "array" to T_ARRAY(), "index" to T_INT))
            put("array::repeat", sig("array::repeat", T_ARRAY(), "value" to T_ANY, "count" to T_INT))
            put("array::reverse", sig("array::reverse", T_ARRAY(), "array" to T_ARRAY()))
            put("array::shuffle", sig("array::shuffle", T_ARRAY(), "array" to T_ARRAY()))
            put("array::slice", sigOpt("array::slice", T_ARRAY(), 
                listOf("array" to T_ARRAY(), "start" to T_INT), 
                listOf("length" to T_INT)))
            put("array::sort", sigOpt("array::sort", T_ARRAY(), 
                listOf("array" to T_ARRAY()), 
                listOf("order" to T_STR)))
            put("array::swap", sig("array::swap", T_ARRAY(), "array" to T_ARRAY(), "i" to T_INT, "j" to T_INT))
            put("array::transpose", sig("array::transpose", T_ARRAY(), "array" to T_ARRAY()))
            put("array::union", sig("array::union", T_ARRAY(), "a" to T_ARRAY(), "b" to T_ARRAY()))
            put("array::windows", sig("array::windows", T_ARRAY(T_ARRAY()), "array" to T_ARRAY(), "size" to T_INT))
            
            // ==================== Bytes Functions ====================
            put("bytes::len", sig("bytes::len", T_INT, "bytes" to T_BYTES))
            
            // ==================== Count Function ====================
            put("count", sigOpt("count", T_INT, listOf(), listOf("value" to T_ANY)))
            
            // ==================== Crypto Functions ====================
            put("crypto::argon2::compare", sig("crypto::argon2::compare", T_BOOL, "hash" to T_STR, "password" to T_STR))
            put("crypto::argon2::generate", sig("crypto::argon2::generate", T_STR, "password" to T_STR))
            put("crypto::bcrypt::compare", sig("crypto::bcrypt::compare", T_BOOL, "hash" to T_STR, "password" to T_STR))
            put("crypto::bcrypt::generate", sig("crypto::bcrypt::generate", T_STR, "password" to T_STR))
            put("crypto::md5", sig("crypto::md5", T_STR, "value" to T_ANY))
            put("crypto::pbkdf2::compare", sig("crypto::pbkdf2::compare", T_BOOL, "hash" to T_STR, "password" to T_STR))
            put("crypto::pbkdf2::generate", sig("crypto::pbkdf2::generate", T_STR, "password" to T_STR))
            put("crypto::scrypt::compare", sig("crypto::scrypt::compare", T_BOOL, "hash" to T_STR, "password" to T_STR))
            put("crypto::scrypt::generate", sig("crypto::scrypt::generate", T_STR, "password" to T_STR))
            put("crypto::sha1", sig("crypto::sha1", T_STR, "value" to T_ANY))
            put("crypto::sha256", sig("crypto::sha256", T_STR, "value" to T_ANY))
            put("crypto::sha512", sig("crypto::sha512", T_STR, "value" to T_ANY))
            
            // ==================== Duration Functions ====================
            put("duration::days", sig("duration::days", T_INT, "duration" to T_DURATION))
            put("duration::hours", sig("duration::hours", T_INT, "duration" to T_DURATION))
            put("duration::micros", sig("duration::micros", T_INT, "duration" to T_DURATION))
            put("duration::millis", sig("duration::millis", T_INT, "duration" to T_DURATION))
            put("duration::mins", sig("duration::mins", T_INT, "duration" to T_DURATION))
            put("duration::nanos", sig("duration::nanos", T_INT, "duration" to T_DURATION))
            put("duration::secs", sig("duration::secs", T_INT, "duration" to T_DURATION))
            put("duration::weeks", sig("duration::weeks", T_INT, "duration" to T_DURATION))
            put("duration::years", sig("duration::years", T_INT, "duration" to T_DURATION))
            put("duration::from::days", sig("duration::from::days", T_DURATION, "days" to T_INT))
            put("duration::from::hours", sig("duration::from::hours", T_DURATION, "hours" to T_INT))
            put("duration::from::micros", sig("duration::from::micros", T_DURATION, "micros" to T_INT))
            put("duration::from::millis", sig("duration::from::millis", T_DURATION, "millis" to T_INT))
            put("duration::from::mins", sig("duration::from::mins", T_DURATION, "mins" to T_INT))
            put("duration::from::nanos", sig("duration::from::nanos", T_DURATION, "nanos" to T_INT))
            put("duration::from::secs", sig("duration::from::secs", T_DURATION, "secs" to T_INT))
            put("duration::from::weeks", sig("duration::from::weeks", T_DURATION, "weeks" to T_INT))
            
            // ==================== Encoding Functions ====================
            put("encoding::base64::decode", sig("encoding::base64::decode", T_BYTES, "value" to T_STR))
            put("encoding::base64::encode", sig("encoding::base64::encode", T_STR, "value" to T_BYTES))
            
            // ==================== Geo Functions ====================
            put("geo::area", sig("geo::area", T_FLOAT, "geometry" to T_GEO))
            put("geo::bearing", sig("geo::bearing", T_FLOAT, "point1" to T_GEO, "point2" to T_GEO))
            put("geo::centroid", sig("geo::centroid", SurqlType.Geometry(GeometryKind.Point), "geometry" to T_GEO))
            put("geo::distance", sig("geo::distance", T_FLOAT, "point1" to T_GEO, "point2" to T_GEO))
            put("geo::hash::decode", sig("geo::hash::decode", SurqlType.Geometry(GeometryKind.Point), "hash" to T_STR))
            put("geo::hash::encode", sigOpt("geo::hash::encode", T_STR, 
                listOf("point" to T_GEO), 
                listOf("accuracy" to T_INT)))
            
            // ==================== HTTP Functions ====================
            put("http::delete", sigOpt("http::delete", T_ANY, 
                listOf("url" to T_STR), 
                listOf("headers" to T_OBJECT)))
            put("http::get", sigOpt("http::get", T_ANY, 
                listOf("url" to T_STR), 
                listOf("headers" to T_OBJECT)))
            put("http::head", sigOpt("http::head", T_ANY, 
                listOf("url" to T_STR), 
                listOf("headers" to T_OBJECT)))
            put("http::patch", sigOpt("http::patch", T_ANY, 
                listOf("url" to T_STR, "body" to T_ANY), 
                listOf("headers" to T_OBJECT)))
            put("http::post", sigOpt("http::post", T_ANY, 
                listOf("url" to T_STR, "body" to T_ANY), 
                listOf("headers" to T_OBJECT)))
            put("http::put", sigOpt("http::put", T_ANY, 
                listOf("url" to T_STR, "body" to T_ANY), 
                listOf("headers" to T_OBJECT)))
            
            // ==================== Math Functions ====================
            put("math::abs", sig("math::abs", T_NUM, "number" to T_NUM))
            put("math::acos", sig("math::acos", T_FLOAT, "number" to T_NUM))
            put("math::acot", sig("math::acot", T_FLOAT, "number" to T_NUM))
            put("math::asin", sig("math::asin", T_FLOAT, "number" to T_NUM))
            put("math::atan", sig("math::atan", T_FLOAT, "number" to T_NUM))
            put("math::bottom", sig("math::bottom", T_ARRAY(T_NUM), "array" to T_ARRAY(T_NUM), "count" to T_INT))
            put("math::ceil", sig("math::ceil", T_INT, "number" to T_NUM))
            put("math::clamp", sig("math::clamp", T_NUM, "number" to T_NUM, "min" to T_NUM, "max" to T_NUM))
            put("math::cos", sig("math::cos", T_FLOAT, "number" to T_NUM))
            put("math::cot", sig("math::cot", T_FLOAT, "number" to T_NUM))
            put("math::deg2rad", sig("math::deg2rad", T_FLOAT, "degrees" to T_NUM))
            put("math::e", sig("math::e", T_FLOAT))
            put("math::fixed", sig("math::fixed", T_NUM, "number" to T_NUM, "precision" to T_INT))
            put("math::floor", sig("math::floor", T_INT, "number" to T_NUM))
            put("math::inf", sig("math::inf", T_FLOAT))
            put("math::interquartile", sig("math::interquartile", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::lerp", sig("math::lerp", T_FLOAT, "a" to T_NUM, "b" to T_NUM, "t" to T_NUM))
            put("math::lerpangle", sig("math::lerpangle", T_FLOAT, "a" to T_NUM, "b" to T_NUM, "t" to T_NUM))
            put("math::ln", sig("math::ln", T_FLOAT, "number" to T_NUM))
            put("math::log", sig("math::log", T_FLOAT, "number" to T_NUM, "base" to T_NUM))
            put("math::log10", sig("math::log10", T_FLOAT, "number" to T_NUM))
            put("math::log2", sig("math::log2", T_FLOAT, "number" to T_NUM))
            put("math::max", sig("math::max", T_NUM, "array" to T_ARRAY(T_NUM)))
            put("math::mean", sig("math::mean", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::median", sig("math::median", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::midhinge", sig("math::midhinge", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::min", sig("math::min", T_NUM, "array" to T_ARRAY(T_NUM)))
            put("math::mode", sig("math::mode", T_NUM, "array" to T_ARRAY(T_NUM)))
            put("math::nearestrank", sig("math::nearestrank", T_NUM, "array" to T_ARRAY(T_NUM), "percentile" to T_NUM))
            put("math::neg_inf", sig("math::neg_inf", T_FLOAT))
            put("math::percentile", sig("math::percentile", T_FLOAT, "array" to T_ARRAY(T_NUM), "percentile" to T_NUM))
            put("math::pi", sig("math::pi", T_FLOAT))
            put("math::pow", sig("math::pow", T_NUM, "base" to T_NUM, "exponent" to T_NUM))
            put("math::product", sig("math::product", T_NUM, "array" to T_ARRAY(T_NUM)))
            put("math::rad2deg", sig("math::rad2deg", T_FLOAT, "radians" to T_NUM))
            put("math::round", sig("math::round", T_INT, "number" to T_NUM))
            put("math::sign", sig("math::sign", T_INT, "number" to T_NUM))
            put("math::sin", sig("math::sin", T_FLOAT, "number" to T_NUM))
            put("math::spread", sig("math::spread", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::sqrt", sig("math::sqrt", T_FLOAT, "number" to T_NUM))
            put("math::stddev", sig("math::stddev", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::sum", sig("math::sum", T_NUM, "array" to T_ARRAY(T_NUM)))
            put("math::tan", sig("math::tan", T_FLOAT, "number" to T_NUM))
            put("math::tau", sig("math::tau", T_FLOAT))
            put("math::top", sig("math::top", T_ARRAY(T_NUM), "array" to T_ARRAY(T_NUM), "count" to T_INT))
            put("math::trimean", sig("math::trimean", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            put("math::variance", sig("math::variance", T_FLOAT, "array" to T_ARRAY(T_NUM)))
            
            // ==================== Meta Functions ====================
            put("meta::id", sig("meta::id", T_ANY, "record" to T_RECORD))
            put("meta::tb", sig("meta::tb", T_STR, "record" to T_RECORD))
            put("meta::type", sig("meta::type", T_STR, "value" to T_ANY))
            
            // ==================== Object Functions ====================
            put("object::entries", sig("object::entries", T_ARRAY(), "object" to T_OBJECT))
            put("object::from_entries", sig("object::from_entries", T_OBJECT, "entries" to T_ARRAY()))
            put("object::keys", sig("object::keys", T_ARRAY(T_STR), "object" to T_OBJECT))
            put("object::len", sig("object::len", T_INT, "object" to T_OBJECT))
            put("object::values", sig("object::values", T_ARRAY(), "object" to T_OBJECT))
            
            // ==================== Parse Functions ====================
            put("parse::email::host", sig("parse::email::host", T_OPT(T_STR), "email" to T_STR))
            put("parse::email::user", sig("parse::email::user", T_OPT(T_STR), "email" to T_STR))
            put("parse::url::domain", sig("parse::url::domain", T_OPT(T_STR), "url" to T_STR))
            put("parse::url::fragment", sig("parse::url::fragment", T_OPT(T_STR), "url" to T_STR))
            put("parse::url::host", sig("parse::url::host", T_OPT(T_STR), "url" to T_STR))
            put("parse::url::path", sig("parse::url::path", T_OPT(T_STR), "url" to T_STR))
            put("parse::url::port", sig("parse::url::port", T_OPT(T_INT), "url" to T_STR))
            put("parse::url::query", sig("parse::url::query", T_OPT(T_STR), "url" to T_STR))
            put("parse::url::scheme", sig("parse::url::scheme", T_OPT(T_STR), "url" to T_STR))
            
            // ==================== Rand Functions ====================
            put("rand", sig("rand", T_FLOAT))
            put("rand::bool", sig("rand::bool", T_BOOL))
            put("rand::enum", sig("rand::enum", T_ANY, "values" to T_ARRAY(), variadic = true))
            put("rand::float", sigOpt("rand::float", T_FLOAT, listOf(), listOf("min" to T_FLOAT, "max" to T_FLOAT)))
            put("rand::guid", sigOpt("rand::guid", T_STR, listOf(), listOf("length" to T_INT)))
            put("rand::int", sigOpt("rand::int", T_INT, listOf(), listOf("min" to T_INT, "max" to T_INT)))
            put("rand::string", sigOpt("rand::string", T_STR, listOf(), listOf("length" to T_INT, "min" to T_INT, "max" to T_INT)))
            put("rand::time", sigOpt("rand::time", T_DATETIME, listOf(), listOf("min" to T_DATETIME, "max" to T_DATETIME)))
            put("rand::ulid", sig("rand::ulid", T_STR))
            put("rand::uuid", sig("rand::uuid", T_UUID))
            put("rand::uuid::v4", sig("rand::uuid::v4", T_UUID))
            put("rand::uuid::v7", sig("rand::uuid::v7", T_UUID))
            
            // ==================== Record Functions ====================
            put("record::exists", sig("record::exists", T_BOOL, "record" to T_RECORD))
            put("record::id", sig("record::id", T_ANY, "record" to T_RECORD))
            put("record::tb", sig("record::tb", T_STR, "record" to T_RECORD))
            
            // ==================== Search Functions ====================
            put("search::analyze", sig("search::analyze", T_ARRAY(T_STR), "analyzer" to T_STR, "text" to T_STR))
            put("search::highlight", sig("search::highlight", T_STR, "prefix" to T_STR, "suffix" to T_STR, "field" to T_STR))
            put("search::offsets", sig("search::offsets", T_OBJECT, "field" to T_STR))
            put("search::score", sig("search::score", T_FLOAT, "index" to T_INT))
            
            // ==================== Session Functions ====================
            put("session::ac", sig("session::ac", T_OPT(T_STR)))
            put("session::db", sig("session::db", T_OPT(T_STR)))
            put("session::id", sig("session::id", T_OPT(T_STR)))
            put("session::ip", sig("session::ip", T_OPT(T_STR)))
            put("session::ns", sig("session::ns", T_OPT(T_STR)))
            put("session::origin", sig("session::origin", T_OPT(T_STR)))
            put("session::rd", sig("session::rd", T_OPT(T_RECORD)))
            put("session::token", sig("session::token", T_OPT(T_OBJECT)))
            
            // ==================== Sleep Function ====================
            put("sleep", sig("sleep", T_NULL, "duration" to T_DURATION))
            
            // ==================== String Functions ====================
            put("string::concat", sig("string::concat", T_STR, "strings" to T_STR, variadic = true))
            put("string::contains", sig("string::contains", T_BOOL, "string" to T_STR, "search" to T_STR))
            put("string::ends_with", sig("string::ends_with", T_BOOL, "string" to T_STR, "suffix" to T_STR))
            put("string::html::encode", sig("string::html::encode", T_STR, "string" to T_STR))
            put("string::html::sanitize", sig("string::html::sanitize", T_STR, "string" to T_STR))
            put("string::is::alphanum", sig("string::is::alphanum", T_BOOL, "string" to T_STR))
            put("string::is::alpha", sig("string::is::alpha", T_BOOL, "string" to T_STR))
            put("string::is::ascii", sig("string::is::ascii", T_BOOL, "string" to T_STR))
            put("string::is::datetime", sig("string::is::datetime", T_BOOL, "string" to T_STR, "format" to T_STR))
            put("string::is::domain", sig("string::is::domain", T_BOOL, "string" to T_STR))
            put("string::is::email", sig("string::is::email", T_BOOL, "string" to T_STR))
            put("string::is::hexadecimal", sig("string::is::hexadecimal", T_BOOL, "string" to T_STR))
            put("string::is::ip", sig("string::is::ip", T_BOOL, "string" to T_STR))
            put("string::is::ipv4", sig("string::is::ipv4", T_BOOL, "string" to T_STR))
            put("string::is::ipv6", sig("string::is::ipv6", T_BOOL, "string" to T_STR))
            put("string::is::latitude", sig("string::is::latitude", T_BOOL, "string" to T_STR))
            put("string::is::longitude", sig("string::is::longitude", T_BOOL, "string" to T_STR))
            put("string::is::numeric", sig("string::is::numeric", T_BOOL, "string" to T_STR))
            put("string::is::record", sigOpt("string::is::record", T_BOOL, listOf("string" to T_STR), listOf("table" to T_STR)))
            put("string::is::semver", sig("string::is::semver", T_BOOL, "string" to T_STR))
            put("string::is::url", sig("string::is::url", T_BOOL, "string" to T_STR))
            put("string::is::uuid", sig("string::is::uuid", T_BOOL, "string" to T_STR))
            put("string::join", sig("string::join", T_STR, "separator" to T_STR, "strings" to T_ARRAY(T_STR)))
            put("string::len", sig("string::len", T_INT, "string" to T_STR))
            put("string::lowercase", sig("string::lowercase", T_STR, "string" to T_STR))
            put("string::matches", sig("string::matches", T_BOOL, "string" to T_STR, "pattern" to T_STR))
            put("string::repeat", sig("string::repeat", T_STR, "string" to T_STR, "count" to T_INT))
            put("string::replace", sig("string::replace", T_STR, "string" to T_STR, "search" to T_STR, "replace" to T_STR))
            put("string::reverse", sig("string::reverse", T_STR, "string" to T_STR))
            put("string::semver::compare", sig("string::semver::compare", T_INT, "a" to T_STR, "b" to T_STR))
            put("string::semver::major", sig("string::semver::major", T_INT, "version" to T_STR))
            put("string::semver::minor", sig("string::semver::minor", T_INT, "version" to T_STR))
            put("string::semver::patch", sig("string::semver::patch", T_INT, "version" to T_STR))
            put("string::semver::inc::major", sig("string::semver::inc::major", T_STR, "version" to T_STR))
            put("string::semver::inc::minor", sig("string::semver::inc::minor", T_STR, "version" to T_STR))
            put("string::semver::inc::patch", sig("string::semver::inc::patch", T_STR, "version" to T_STR))
            put("string::semver::set::major", sig("string::semver::set::major", T_STR, "version" to T_STR, "major" to T_INT))
            put("string::semver::set::minor", sig("string::semver::set::minor", T_STR, "version" to T_STR, "minor" to T_INT))
            put("string::semver::set::patch", sig("string::semver::set::patch", T_STR, "version" to T_STR, "patch" to T_INT))
            put("string::similarity::fuzzy", sig("string::similarity::fuzzy", T_INT, "a" to T_STR, "b" to T_STR))
            put("string::similarity::jaro", sig("string::similarity::jaro", T_FLOAT, "a" to T_STR, "b" to T_STR))
            put("string::similarity::smithwaterman", sig("string::similarity::smithwaterman", T_FLOAT, "a" to T_STR, "b" to T_STR))
            put("string::slice", sigOpt("string::slice", T_STR, 
                listOf("string" to T_STR, "start" to T_INT), 
                listOf("length" to T_INT)))
            put("string::slug", sigOpt("string::slug", T_STR, 
                listOf("string" to T_STR), 
                listOf("separator" to T_STR)))
            put("string::split", sig("string::split", T_ARRAY(T_STR), "string" to T_STR, "separator" to T_STR))
            put("string::starts_with", sig("string::starts_with", T_BOOL, "string" to T_STR, "prefix" to T_STR))
            put("string::trim", sig("string::trim", T_STR, "string" to T_STR))
            put("string::uppercase", sig("string::uppercase", T_STR, "string" to T_STR))
            put("string::words", sig("string::words", T_ARRAY(T_STR), "string" to T_STR))
            
            // ==================== Time Functions ====================
            put("time::ceil", sig("time::ceil", T_DATETIME, "datetime" to T_DATETIME, "duration" to T_DURATION))
            put("time::day", sig("time::day", T_INT, "datetime" to T_DATETIME))
            put("time::floor", sig("time::floor", T_DATETIME, "datetime" to T_DATETIME, "duration" to T_DURATION))
            put("time::format", sig("time::format", T_STR, "datetime" to T_DATETIME, "format" to T_STR))
            put("time::group", sig("time::group", T_DATETIME, "datetime" to T_DATETIME, "group" to T_STR))
            put("time::hour", sig("time::hour", T_INT, "datetime" to T_DATETIME))
            put("time::max", sig("time::max", T_DATETIME, "datetimes" to T_ARRAY(T_DATETIME)))
            put("time::micros", sig("time::micros", T_INT, "datetime" to T_DATETIME))
            put("time::millis", sig("time::millis", T_INT, "datetime" to T_DATETIME))
            put("time::min", sig("time::min", T_DATETIME, "datetimes" to T_ARRAY(T_DATETIME)))
            put("time::minute", sig("time::minute", T_INT, "datetime" to T_DATETIME))
            put("time::month", sig("time::month", T_INT, "datetime" to T_DATETIME))
            put("time::nano", sig("time::nano", T_INT, "datetime" to T_DATETIME))
            put("time::now", sig("time::now", T_DATETIME))
            put("time::round", sig("time::round", T_DATETIME, "datetime" to T_DATETIME, "duration" to T_DURATION))
            put("time::second", sig("time::second", T_INT, "datetime" to T_DATETIME))
            put("time::timezone", sig("time::timezone", T_STR))
            put("time::unix", sig("time::unix", T_INT, "datetime" to T_DATETIME))
            put("time::wday", sig("time::wday", T_INT, "datetime" to T_DATETIME))
            put("time::week", sig("time::week", T_INT, "datetime" to T_DATETIME))
            put("time::yday", sig("time::yday", T_INT, "datetime" to T_DATETIME))
            put("time::year", sig("time::year", T_INT, "datetime" to T_DATETIME))
            put("time::from::micros", sig("time::from::micros", T_DATETIME, "micros" to T_INT))
            put("time::from::millis", sig("time::from::millis", T_DATETIME, "millis" to T_INT))
            put("time::from::nanos", sig("time::from::nanos", T_DATETIME, "nanos" to T_INT))
            put("time::from::secs", sig("time::from::secs", T_DATETIME, "secs" to T_INT))
            put("time::from::unix", sig("time::from::unix", T_DATETIME, "unix" to T_INT))
            
            // ==================== Type Functions ====================
            put("type::bool", sig("type::bool", T_BOOL, "value" to T_ANY))
            put("type::datetime", sig("type::datetime", T_DATETIME, "value" to T_ANY))
            put("type::decimal", sig("type::decimal", T_DEC, "value" to T_ANY))
            put("type::duration", sig("type::duration", T_DURATION, "value" to T_ANY))
            put("type::field", sig("type::field", T_ANY, "field" to T_STR))
            put("type::fields", sig("type::fields", T_ARRAY(), "fields" to T_ARRAY(T_STR)))
            put("type::float", sig("type::float", T_FLOAT, "value" to T_ANY))
            put("type::int", sig("type::int", T_INT, "value" to T_ANY))
            put("type::is::array", sig("type::is::array", T_BOOL, "value" to T_ANY))
            put("type::is::bool", sig("type::is::bool", T_BOOL, "value" to T_ANY))
            put("type::is::bytes", sig("type::is::bytes", T_BOOL, "value" to T_ANY))
            put("type::is::collection", sig("type::is::collection", T_BOOL, "value" to T_ANY))
            put("type::is::datetime", sig("type::is::datetime", T_BOOL, "value" to T_ANY))
            put("type::is::decimal", sig("type::is::decimal", T_BOOL, "value" to T_ANY))
            put("type::is::duration", sig("type::is::duration", T_BOOL, "value" to T_ANY))
            put("type::is::float", sig("type::is::float", T_BOOL, "value" to T_ANY))
            put("type::is::geometry", sig("type::is::geometry", T_BOOL, "value" to T_ANY))
            put("type::is::int", sig("type::is::int", T_BOOL, "value" to T_ANY))
            put("type::is::line", sig("type::is::line", T_BOOL, "value" to T_ANY))
            put("type::is::none", sig("type::is::none", T_BOOL, "value" to T_ANY))
            put("type::is::null", sig("type::is::null", T_BOOL, "value" to T_ANY))
            put("type::is::multiline", sig("type::is::multiline", T_BOOL, "value" to T_ANY))
            put("type::is::multipoint", sig("type::is::multipoint", T_BOOL, "value" to T_ANY))
            put("type::is::multipolygon", sig("type::is::multipolygon", T_BOOL, "value" to T_ANY))
            put("type::is::number", sig("type::is::number", T_BOOL, "value" to T_ANY))
            put("type::is::object", sig("type::is::object", T_BOOL, "value" to T_ANY))
            put("type::is::point", sig("type::is::point", T_BOOL, "value" to T_ANY))
            put("type::is::polygon", sig("type::is::polygon", T_BOOL, "value" to T_ANY))
            put("type::is::record", sigOpt("type::is::record", T_BOOL, 
                listOf("value" to T_ANY), 
                listOf("table" to T_STR)))
            put("type::is::string", sig("type::is::string", T_BOOL, "value" to T_ANY))
            put("type::is::uuid", sig("type::is::uuid", T_BOOL, "value" to T_ANY))
            put("type::number", sig("type::number", T_NUM, "value" to T_ANY))
            put("type::point", sigOpt("type::point", SurqlType.Geometry(GeometryKind.Point), 
                listOf(), 
                listOf("x" to T_NUM, "y" to T_NUM)))
            put("type::range", sig("type::range", T_STR, "value" to T_ANY))
            put("type::record", sigOpt("type::record", T_RECORD, 
                listOf(), 
                listOf("table" to T_STR, "id" to T_ANY)))
            put("type::string", sig("type::string", T_STR, "value" to T_ANY))
            put("type::table", sig("type::table", T_STR, "table" to T_STR))
            put("type::thing", sig("type::thing", T_RECORD, "table" to T_STR, "id" to T_ANY))
            put("type::uuid", sig("type::uuid", T_UUID, "value" to T_ANY))
            
            // ==================== Value Functions ====================
            put("value::diff", sig("value::diff", T_ARRAY(), "a" to T_ANY, "b" to T_ANY))
            put("value::patch", sig("value::patch", T_ANY, "value" to T_ANY, "patch" to T_ARRAY()))
            
            // ==================== Vector Functions ====================
            put("vector::add", sig("vector::add", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::angle", sig("vector::angle", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::cross", sig("vector::cross", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::divide", sig("vector::divide", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::dot", sig("vector::dot", T_NUM, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::magnitude", sig("vector::magnitude", T_FLOAT, "vector" to T_ARRAY(T_NUM)))
            put("vector::multiply", sig("vector::multiply", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::normalize", sig("vector::normalize", T_ARRAY(T_FLOAT), "vector" to T_ARRAY(T_NUM)))
            put("vector::project", sig("vector::project", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::scale", sig("vector::scale", T_ARRAY(T_NUM), "vector" to T_ARRAY(T_NUM), "scalar" to T_NUM))
            put("vector::subtract", sig("vector::subtract", T_ARRAY(T_NUM), "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::chebyshev", sig("vector::distance::chebyshev", T_NUM, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::euclidean", sig("vector::distance::euclidean", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::hamming", sig("vector::distance::hamming", T_INT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::knn", sig("vector::distance::knn", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::manhattan", sig("vector::distance::manhattan", T_NUM, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::distance::minkowski", sig("vector::distance::minkowski", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM), "p" to T_NUM))
            put("vector::similarity::cosine", sig("vector::similarity::cosine", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::similarity::jaccard", sig("vector::similarity::jaccard", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::similarity::pearson", sig("vector::similarity::pearson", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
            put("vector::similarity::spearman", sig("vector::similarity::spearman", T_FLOAT, "a" to T_ARRAY(T_NUM), "b" to T_ARRAY(T_NUM)))
        }
    }
    
    /**
     * Gets the signature for a built-in function by name.
     * The name is case-insensitive.
     */
    fun getSignature(name: String): FunctionSignature? {
        return signatures[name.lowercase()]
    }
    
    /**
     * Returns true if a function with the given name exists.
     */
    fun exists(name: String): Boolean {
        return signatures.containsKey(name.lowercase())
    }
    
    /**
     * Gets all function names that start with the given prefix.
     * Useful for code completion.
     */
    fun getFunctionsWithPrefix(prefix: String): List<FunctionSignature> {
        val lowerPrefix = prefix.lowercase()
        return signatures.entries
            .filter { it.key.startsWith(lowerPrefix) }
            .map { it.value }
    }
    
    /**
     * Gets all functions in a specific namespace (e.g., "array", "string").
     */
    fun getFunctionsInNamespace(namespace: String): List<FunctionSignature> {
        val prefix = "${namespace.lowercase()}::"
        return signatures.entries
            .filter { it.key.startsWith(prefix) }
            .map { it.value }
    }
    
    /**
     * Gets all available namespaces.
     */
    fun getNamespaces(): Set<String> {
        return signatures.keys
            .mapNotNull { name -> name.substringBefore("::").takeIf { "::" in name } }
            .toSet()
    }
}
