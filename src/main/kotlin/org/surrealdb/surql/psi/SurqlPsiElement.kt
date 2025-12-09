package org.surrealdb.surql.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Base interface for all SurrealQL PSI elements.
 */
interface SurqlPsiElement : PsiElement

/**
 * Interface for SurrealQL elements that have a name and can be referenced.
 * 
 * This includes:
 * - Table names
 * - Field names  
 * - Parameter names
 * - Function names
 * - Index names
 * - Event names
 * - etc.
 */
interface SurqlNamedElement : SurqlPsiElement, PsiNameIdentifierOwner {
    /**
     * Returns the name of this element.
     */
    override fun getName(): String?
    
    /**
     * Sets a new name for this element.
     * Used during refactoring (rename).
     */
    override fun setName(name: String): PsiElement
}

/**
 * Interface for elements that can be targets of references.
 * For example, table definitions can be referenced by SELECT statements.
 */
interface SurqlDefinitionElement : SurqlNamedElement {
    /**
     * Returns the type of definition (e.g., "table", "field", "function").
     */
    fun getDefinitionType(): String
}

/**
 * Interface for elements that reference other named elements.
 * For example, a table name in a SELECT statement references a table definition.
 */
interface SurqlReferenceElement : SurqlPsiElement {
    /**
     * Returns the name being referenced.
     */
    fun getReferenceName(): String?
}
