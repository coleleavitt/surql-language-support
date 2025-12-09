package org.surrealdb.surql.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.surrealdb.surql.lexer.SurqlTokenTypes

/**
 * Utility functions for PSI element implementations.
 */
object SurqlPsiImplUtil {

    /**
     * Finds the first child node of a specific type.
     */
    fun findChildByType(node: ASTNode, type: IElementType): ASTNode? {
        return node.findChildByType(type)
    }

    /**
     * Finds all child nodes of a specific type.
     */
    fun findChildrenByType(node: ASTNode, type: IElementType): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == type) {
                result.add(child)
            }
            child = child.treeNext
        }
        return result
    }

    /**
     * Gets the text of a child node with the specified type.
     */
    fun getChildText(node: ASTNode, type: IElementType): String? {
        return findChildByType(node, type)?.text
    }

    /**
     * Finds the identifier text within a node.
     */
    fun getIdentifierText(node: ASTNode): String? {
        return getChildText(node, SurqlTokenTypes.IDENTIFIER)
    }

    /**
     * Checks if a node has a child of the specified type.
     */
    fun hasChild(node: ASTNode, type: IElementType): Boolean {
        return findChildByType(node, type) != null
    }

    /**
     * Gets all children as PSI elements.
     */
    fun getChildrenAsPsi(element: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var child = element.firstChild
        while (child != null) {
            result.add(child)
            child = child.nextSibling
        }
        return result
    }

    /**
     * Finds the first child PSI element of a specific type.
     */
    inline fun <reified T : PsiElement> findChildOfType(element: PsiElement): T? {
        var child = element.firstChild
        while (child != null) {
            if (child is T) return child
            child = child.nextSibling
        }
        return null
    }

    /**
     * Finds all children PSI elements of a specific type.
     */
    inline fun <reified T : PsiElement> findChildrenOfType(element: PsiElement): List<T> {
        val result = mutableListOf<T>()
        var child = element.firstChild
        while (child != null) {
            if (child is T) result.add(child)
            child = child.nextSibling
        }
        return result
    }
}
