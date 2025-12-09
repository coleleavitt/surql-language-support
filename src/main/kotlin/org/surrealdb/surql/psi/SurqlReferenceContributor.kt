package org.surrealdb.surql.psi

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.surrealdb.surql.lang.SurqlLanguage
import org.surrealdb.surql.lexer.SurqlTokenTypes

/**
 * Contributes PsiReferences for SurrealQL elements.
 * 
 * This enables:
 * - Go to Definition (Ctrl+Click)
 * - Find Usages
 * - Rename Refactoring
 */
class SurqlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for identifiers
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(SurqlTokenTypes.IDENTIFIER)
                .withLanguage(SurqlLanguage),
            SurqlIdentifierReferenceProvider()
        )

        // Register reference provider for parameters
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(SurqlTokenTypes.PARAMETER)
                .withLanguage(SurqlLanguage),
            SurqlParameterReferenceProvider()
        )

        // Register reference provider for record IDs
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(SurqlTokenTypes.RECORD_ID)
                .withLanguage(SurqlLanguage),
            SurqlRecordIdReferenceProvider()
        )
    }
}

/**
 * Provides references for identifier elements.
 * 
 * Determines the semantic context of the identifier and creates
 * the appropriate reference type.
 */
private class SurqlIdentifierReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = element.text
        
        // Determine context by looking at preceding tokens
        val referenceContext = determineContext(element)
        
        return when (referenceContext) {
            ReferenceContext.TABLE_REFERENCE -> arrayOf(SurqlTableReference(element, text))
            ReferenceContext.FIELD_REFERENCE -> {
                val tableName = findAssociatedTable(element)
                arrayOf(SurqlFieldReference(element, text, tableName))
            }
            ReferenceContext.FUNCTION_NAMESPACE -> emptyArray() // Built-in, no reference needed
            ReferenceContext.DEFINITION -> emptyArray() // This is the definition, not a reference
            ReferenceContext.UNKNOWN -> emptyArray()
        }
    }

    private fun determineContext(element: PsiElement): ReferenceContext {
        var current = skipWhitespacePrev(element)
        
        // Track if we're in a definition context
        var sawDefine = false
        var sawTable = false
        var sawField = false
        var sawFrom = false
        var sawCreate = false
        var sawUpdate = false
        var sawDelete = false
        var sawInsert = false
        var sawInto = false
        var sawOn = false
        
        // Look back through siblings to determine context
        while (current != null) {
            when (current.elementType) {
                SurqlTokenTypes.DEFINE -> sawDefine = true
                SurqlTokenTypes.TABLE -> sawTable = true
                SurqlTokenTypes.FIELD -> sawField = true
                SurqlTokenTypes.FROM -> sawFrom = true
                SurqlTokenTypes.CREATE -> sawCreate = true
                SurqlTokenTypes.UPDATE -> sawUpdate = true
                SurqlTokenTypes.DELETE -> sawDelete = true
                SurqlTokenTypes.INSERT -> sawInsert = true
                SurqlTokenTypes.INTO -> sawInto = true
                SurqlTokenTypes.ON -> sawOn = true
                SurqlTokenTypes.COLONCOLON -> return ReferenceContext.FUNCTION_NAMESPACE
                SurqlTokenTypes.SEMICOLON -> break // Start of different statement
            }
            current = skipWhitespacePrev(current)
        }
        
        // Determine context based on what we saw
        return when {
            sawDefine && sawTable -> ReferenceContext.DEFINITION
            sawDefine && sawField && sawOn -> ReferenceContext.TABLE_REFERENCE
            sawDefine && sawField -> ReferenceContext.DEFINITION
            sawFrom -> ReferenceContext.TABLE_REFERENCE
            sawCreate || sawUpdate || sawDelete -> ReferenceContext.TABLE_REFERENCE
            sawInsert && sawInto -> ReferenceContext.TABLE_REFERENCE
            sawField -> ReferenceContext.FIELD_REFERENCE
            else -> ReferenceContext.UNKNOWN
        }
    }

    private fun findAssociatedTable(element: PsiElement): String? {
        // Try to find FROM clause to determine which table this field belongs to
        var current = element.prevSibling
        var sawFrom = false
        
        while (current != null) {
            when (current.elementType) {
                SurqlTokenTypes.FROM -> sawFrom = true
                SurqlTokenTypes.IDENTIFIER -> {
                    if (sawFrom) return current.text
                }
                SurqlTokenTypes.SEMICOLON -> break
            }
            current = current.prevSibling
        }
        
        return null
    }

    private fun skipWhitespacePrev(element: PsiElement): PsiElement? {
        var prev = element.prevSibling
        while (prev != null && prev.elementType == SurqlTokenTypes.WHITE_SPACE) {
            prev = prev.prevSibling
        }
        return prev
    }

    private enum class ReferenceContext {
        TABLE_REFERENCE,
        FIELD_REFERENCE,
        FUNCTION_NAMESPACE,
        DEFINITION,
        UNKNOWN
    }
}

/**
 * Provides references for parameter elements ($param).
 */
private class SurqlParameterReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = element.text
        
        // Check if this is a definition (LET $x = ...) or a reference
        if (isParameterDefinition(element)) {
            return emptyArray()
        }
        
        return arrayOf(SurqlParameterReference(element, text))
    }

    private fun isParameterDefinition(element: PsiElement): Boolean {
        var prev = element.prevSibling
        while (prev != null) {
            when (prev.elementType) {
                SurqlTokenTypes.LET -> return true
                SurqlTokenTypes.PARAM -> return true // DEFINE PARAM
                SurqlTokenTypes.WHITE_SPACE -> { /* continue */ }
                else -> return false
            }
            prev = prev.prevSibling
        }
        return false
    }
}

/**
 * Provides references for record ID elements (table:id).
 */
private class SurqlRecordIdReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = element.text
        val colonIndex = text.indexOf(':')
        
        if (colonIndex > 0) {
            val tableName = text.substring(0, colonIndex)
            // Create a reference for the table part of the record ID
            return arrayOf(SurqlTableReference(element, tableName))
        }
        
        return emptyArray()
    }
}
