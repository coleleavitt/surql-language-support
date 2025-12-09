package org.surrealdb.surql.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.surrealdb.surql.lexer.SurqlTokenTypes

/**
 * Reference from a table name usage to its definition.
 * 
 * This allows features like:
 * - Go to definition (Ctrl+Click on table name)
 * - Find usages
 * - Rename refactoring
 */
class SurqlTableReference(
    element: PsiElement,
    private val tableName: String
) : PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength)) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile ?: return null
        
        // Search for DEFINE TABLE statements with matching name
        return findTableDefinition(file, tableName)
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile ?: return emptyArray()
        
        // Collect all defined table names for completion
        return collectTableNames(file).toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        // For now, just return the element - proper rename requires factory
        return element
    }

    private fun findTableDefinition(file: PsiFile, name: String): PsiElement? {
        // Walk through all elements looking for DEFINE TABLE <name>
        var result: PsiElement? = null
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inDefineTable = false
            private var foundTable = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.DEFINE -> inDefineTable = true
                    SurqlTokenTypes.TABLE -> if (inDefineTable) foundTable = true
                    SurqlTokenTypes.IDENTIFIER -> {
                        if (inDefineTable && foundTable && element.text.equals(name, ignoreCase = true)) {
                            result = element
                            stopWalking()
                            return
                        }
                        // Reset after finding identifier in other contexts
                        if (foundTable) {
                            inDefineTable = false
                            foundTable = false
                        }
                    }
                    SurqlTokenTypes.SEMICOLON -> {
                        inDefineTable = false
                        foundTable = false
                    }
                }
                super.visitElement(element)
            }
        })
        
        return result
    }

    private fun collectTableNames(file: PsiFile): List<String> {
        val tables = mutableListOf<String>()
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inDefineTable = false
            private var foundTable = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.DEFINE -> inDefineTable = true
                    SurqlTokenTypes.TABLE -> if (inDefineTable) foundTable = true
                    SurqlTokenTypes.IDENTIFIER -> {
                        if (inDefineTable && foundTable) {
                            tables.add(element.text)
                            inDefineTable = false
                            foundTable = false
                        }
                    }
                    SurqlTokenTypes.SEMICOLON -> {
                        inDefineTable = false
                        foundTable = false
                    }
                }
                super.visitElement(element)
            }
        })
        
        return tables
    }
}

/**
 * Reference from a parameter usage to its definition.
 */
class SurqlParameterReference(
    element: PsiElement,
    private val paramName: String
) : PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength)) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile ?: return null
        
        // Search for LET $paramName or DEFINE PARAM $paramName
        return findParameterDefinition(file, paramName)
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile ?: return emptyArray()
        
        // Collect all defined parameter names
        return collectParameterNames(file).toTypedArray()
    }

    private fun findParameterDefinition(file: PsiFile, name: String): PsiElement? {
        var result: PsiElement? = null
        val searchName = if (name.startsWith("$")) name else "$$name"
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inLet = false
            private var inDefineParam = false
            private var foundParam = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.LET -> inLet = true
                    SurqlTokenTypes.DEFINE -> inDefineParam = true
                    SurqlTokenTypes.PARAM -> if (inDefineParam) foundParam = true
                    SurqlTokenTypes.PARAMETER -> {
                        if ((inLet || (inDefineParam && foundParam)) && 
                            element.text.equals(searchName, ignoreCase = true)) {
                            result = element
                            stopWalking()
                            return
                        }
                    }
                    SurqlTokenTypes.EQ, SurqlTokenTypes.SEMICOLON -> {
                        inLet = false
                        inDefineParam = false
                        foundParam = false
                    }
                }
                super.visitElement(element)
            }
        })
        
        return result
    }

    private fun collectParameterNames(file: PsiFile): List<String> {
        val params = mutableListOf<String>()
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inLet = false
            private var inDefineParam = false
            private var foundParam = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.LET -> inLet = true
                    SurqlTokenTypes.DEFINE -> inDefineParam = true
                    SurqlTokenTypes.PARAM -> if (inDefineParam) foundParam = true
                    SurqlTokenTypes.PARAMETER -> {
                        if (inLet || (inDefineParam && foundParam)) {
                            params.add(element.text)
                        }
                        inLet = false
                        if (foundParam) {
                            inDefineParam = false
                            foundParam = false
                        }
                    }
                    SurqlTokenTypes.SEMICOLON -> {
                        inLet = false
                        inDefineParam = false
                        foundParam = false
                    }
                }
                super.visitElement(element)
            }
        })
        
        return params
    }
}

/**
 * Reference from a field name usage to its definition.
 */
class SurqlFieldReference(
    element: PsiElement,
    private val fieldName: String,
    private val tableName: String?
) : PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength)) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile ?: return null
        
        // Search for DEFINE FIELD <fieldName> ON <tableName>
        return findFieldDefinition(file, fieldName, tableName)
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile ?: return emptyArray()
        
        // Collect all field names (optionally filtered by table)
        return collectFieldNames(file, tableName).toTypedArray()
    }

    private fun findFieldDefinition(file: PsiFile, field: String, table: String?): PsiElement? {
        var result: PsiElement? = null
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inDefineField = false
            private var foundField = false
            private var capturedFieldName: String? = null
            private var foundOn = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.DEFINE -> inDefineField = true
                    SurqlTokenTypes.FIELD -> if (inDefineField) foundField = true
                    SurqlTokenTypes.ON -> if (inDefineField && foundField) foundOn = true
                    SurqlTokenTypes.IDENTIFIER -> {
                        if (inDefineField && foundField && capturedFieldName == null) {
                            capturedFieldName = element.text
                        } else if (inDefineField && foundOn && capturedFieldName != null) {
                            // This is the table name
                            if (capturedFieldName.equals(field, ignoreCase = true) &&
                                (table == null || element.text.equals(table, ignoreCase = true))) {
                                // Find the field name element
                                result = findFieldElement(element.parent, capturedFieldName!!)
                                stopWalking()
                                return
                            }
                            resetState()
                        }
                    }
                    SurqlTokenTypes.SEMICOLON -> resetState()
                }
                super.visitElement(element)
            }
            
            private fun resetState() {
                inDefineField = false
                foundField = false
                capturedFieldName = null
                foundOn = false
            }
            
            private fun findFieldElement(parent: PsiElement?, fieldName: String): PsiElement? {
                if (parent == null) return null
                var found: PsiElement? = null
                parent.accept(object : PsiRecursiveElementWalkingVisitor() {
                    override fun visitElement(element: PsiElement) {
                        if (element.elementType == SurqlTokenTypes.IDENTIFIER && 
                            element.text.equals(fieldName, ignoreCase = true)) {
                            found = element
                            stopWalking()
                            return
                        }
                        super.visitElement(element)
                    }
                })
                return found
            }
        })
        
        return result
    }

    private fun collectFieldNames(file: PsiFile, forTable: String?): List<String> {
        val fields = mutableListOf<String>()
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            private var inDefineField = false
            private var foundField = false
            private var capturedFieldName: String? = null
            private var foundOn = false
            
            override fun visitElement(element: PsiElement) {
                when (element.elementType) {
                    SurqlTokenTypes.DEFINE -> inDefineField = true
                    SurqlTokenTypes.FIELD -> if (inDefineField) foundField = true
                    SurqlTokenTypes.ON -> if (inDefineField && foundField) foundOn = true
                    SurqlTokenTypes.IDENTIFIER -> {
                        if (inDefineField && foundField && capturedFieldName == null) {
                            capturedFieldName = element.text
                        } else if (inDefineField && foundOn && capturedFieldName != null) {
                            // This is the table name
                            if (forTable == null || element.text.equals(forTable, ignoreCase = true)) {
                                fields.add(capturedFieldName!!)
                            }
                            resetState()
                        }
                    }
                    SurqlTokenTypes.SEMICOLON -> resetState()
                }
                super.visitElement(element)
            }
            
            private fun resetState() {
                inDefineField = false
                foundField = false
                capturedFieldName = null
                foundOn = false
            }
        })
        
        return fields
    }
}
