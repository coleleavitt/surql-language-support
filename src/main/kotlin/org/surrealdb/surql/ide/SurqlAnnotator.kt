package org.surrealdb.surql.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.surrealdb.surql.lexer.SurqlTokenTypes
import java.awt.Font

/**
 * Provides semantic annotations for SurrealQL code.
 * 
 * This annotator provides additional highlighting and validation beyond
 * the basic syntax highlighting:
 * - Function namespace highlighting (e.g., array::, string::, etc.)
 * - Record ID validation
 * - Parameter usage highlighting
 * - Type annotation highlighting
 */
class SurqlAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType

        when (elementType) {
            SurqlTokenTypes.IDENTIFIER -> annotateIdentifier(element, holder)
            SurqlTokenTypes.PARAMETER -> annotateParameter(element, holder)
            SurqlTokenTypes.RECORD_ID -> annotateRecordId(element, holder)
            SurqlTokenTypes.RECORD_STRING -> annotateRecordString(element, holder)
        }
    }

    private fun annotateIdentifier(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text

        // Check if this is a function call (followed by ::)
        val nextSibling = element.nextSibling
        if (nextSibling?.elementType == SurqlTokenTypes.COLONCOLON) {
            // This is a function namespace (e.g., "array" in "array::len")
            if (text.lowercase() in FUNCTION_NAMESPACES) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(SurqlSyntaxHighlighter.FUNCTION_NAMESPACE)
                    .create()
            }
        }

        // Check for "fn" prefix indicating user-defined function
        if (text == "fn" && nextSibling?.elementType == SurqlTokenTypes.COLONCOLON) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(SurqlSyntaxHighlighter.USER_FUNCTION)
                .create()
        }

        // Check for type annotations in common positions
        if (text.lowercase() in TYPE_NAMES) {
            // Check if preceded by TYPE keyword or used in type context
            val prevSibling = skipWhitespace(element, forward = false)
            if (prevSibling?.elementType == SurqlTokenTypes.TYPE ||
                prevSibling?.elementType == SurqlTokenTypes.COLON ||
                prevSibling?.elementType == SurqlTokenTypes.LT ||
                prevSibling?.elementType == SurqlTokenTypes.PIPE) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(SurqlSyntaxHighlighter.TYPE_NAME)
                    .create()
            }
        }
    }

    private fun annotateParameter(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        
        // Check for special system parameters
        if (text in SYSTEM_PARAMETERS) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(SurqlSyntaxHighlighter.SYSTEM_PARAMETER)
                .create()
        }
    }

    private fun annotateRecordId(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        val colonIndex = text.indexOf(':')
        
        if (colonIndex > 0) {
            // Highlight the table part differently from the ID part
            val tableRange = TextRange(element.textRange.startOffset, element.textRange.startOffset + colonIndex)
            val idRange = TextRange(element.textRange.startOffset + colonIndex + 1, element.textRange.endOffset)
            
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(tableRange)
                .textAttributes(SurqlSyntaxHighlighter.RECORD_TABLE)
                .create()
                
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(idRange)
                .textAttributes(SurqlSyntaxHighlighter.RECORD_ID_PART)
                .create()
        }
    }

    private fun annotateRecordString(element: PsiElement, holder: AnnotationHolder) {
        // Record strings like r"table:id" get special highlighting
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(SurqlSyntaxHighlighter.RECORD_STRING)
            .create()
    }

    private fun skipWhitespace(element: PsiElement, forward: Boolean): PsiElement? {
        var current = if (forward) element.nextSibling else element.prevSibling
        while (current != null && current.elementType == SurqlTokenTypes.WHITE_SPACE) {
            current = if (forward) current.nextSibling else current.prevSibling
        }
        return current
    }

    companion object {
        // Function namespaces recognized by SurrealDB
        private val FUNCTION_NAMESPACES = setOf(
            "array", "bytes", "count", "crypto", "duration", "encoding",
            "geo", "http", "math", "meta", "object", "parse", "rand",
            "record", "search", "session", "sleep", "string", "time",
            "type", "vector"
        )

        // System parameters that start with $
        private val SYSTEM_PARAMETERS = setOf(
            "\$this", "\$parent", "\$value", "\$input", "\$before", "\$after",
            "\$event", "\$auth", "\$session", "\$scope", "\$token"
        )

        // Type names for type annotation highlighting
        private val TYPE_NAMES = setOf(
            "any", "array", "bool", "bytes", "datetime", "decimal",
            "duration", "float", "geometry", "int", "null", "number",
            "object", "option", "point", "polygon", "line", "multipoint",
            "multiline", "multipolygon", "collection", "record", "set",
            "string", "uuid", "range", "literal", "either", "future", "refs"
        )
    }
}
