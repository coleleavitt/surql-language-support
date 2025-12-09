package org.surrealdb.surql.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.surrealdb.surql.lexer.SurqlTokenTypes

// ============================================================================
// Clause Implementations
// ============================================================================

/**
 * Base class for all clause implementations.
 */
abstract class SurqlClauseImpl(node: ASTNode) : SurqlPsiElementImpl(node)

/**
 * WHERE clause implementation.
 * WHERE @condition
 */
class SurqlWhereClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val condition: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * ORDER BY clause implementation.
 * ORDER [BY] @field [ASC|DESC] [, @field [ASC|DESC]]*
 */
class SurqlOrderClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    data class OrderItem(val field: PsiElement, val ascending: Boolean)
    
    val orderItems: List<OrderItem>
        get() {
            val items = mutableListOf<OrderItem>()
            var currentField: PsiElement? = null
            var ascending = true
            
            var child = firstChild
            while (child != null) {
                when (child.node.elementType) {
                    SurqlTokenTypes.IDENTIFIER, SurqlTokenTypes.PARAMETER -> {
                        if (currentField != null) {
                            items.add(OrderItem(currentField, ascending))
                        }
                        currentField = child
                        ascending = true // Reset for next field
                    }
                    SurqlTokenTypes.ASC -> ascending = true
                    SurqlTokenTypes.DESC -> ascending = false
                    SurqlTokenTypes.COMMA -> {
                        if (currentField != null) {
                            items.add(OrderItem(currentField, ascending))
                            currentField = null
                            ascending = true
                        }
                    }
                }
                child = child.nextSibling
            }
            
            if (currentField != null) {
                items.add(OrderItem(currentField, ascending))
            }
            
            return items
        }
}

/**
 * LIMIT clause implementation.
 * LIMIT @number
 */
class SurqlLimitClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val limitValue: Int?
        get() {
            val numberNode = node.findChildByType(SurqlTokenTypes.NUMBER)
            return numberNode?.text?.toIntOrNull()
        }
}

/**
 * START clause implementation.
 * START @number
 */
class SurqlStartClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val startValue: Int?
        get() {
            val numberNode = node.findChildByType(SurqlTokenTypes.NUMBER)
            return numberNode?.text?.toIntOrNull()
        }
}

/**
 * GROUP BY clause implementation.
 * GROUP [BY] @field [, @field]*
 */
class SurqlGroupClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val groupFields: List<PsiElement>
        get() {
            val fields = mutableListOf<PsiElement>()
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.IDENTIFIER ||
                    child.node.elementType == SurqlTokenTypes.PARAMETER) {
                    fields.add(child)
                }
                child = child.nextSibling
            }
            return fields
        }
}

/**
 * SPLIT clause implementation.
 * SPLIT [ON] @field
 */
class SurqlSplitClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * FETCH clause implementation.
 * FETCH @field [, @field]*
 */
class SurqlFetchClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val fetchFields: List<PsiElement>
        get() {
            val fields = mutableListOf<PsiElement>()
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.IDENTIFIER) {
                    fields.add(child)
                }
                child = child.nextSibling
            }
            return fields
        }
}

/**
 * TIMEOUT clause implementation.
 * TIMEOUT @duration
 */
class SurqlTimeoutClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * SET clause implementation.
 * SET @field = @value [, @field = @value]*
 */
class SurqlSetClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val assignments: List<SurqlAssignmentImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
}

/**
 * CONTENT clause implementation.
 * CONTENT @object
 */
class SurqlContentClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val contentObject: SurqlObjectLiteralImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * MERGE clause implementation.
 * MERGE @object
 */
class SurqlMergeClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val mergeObject: SurqlObjectLiteralImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * PATCH clause implementation.
 * PATCH @array
 */
class SurqlPatchClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * UNSET clause implementation.
 * UNSET @field [, @field]*
 */
class SurqlUnsetClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * PERMISSIONS clause implementation.
 * PERMISSIONS [NONE | FULL | FOR select, create, update, delete WHERE @condition]
 */
class SurqlPermissionsClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val isNone: Boolean
        get() = node.findChildByType(SurqlTokenTypes.NONE) != null
    
    val isFull: Boolean
        get() = node.findChildByType(SurqlTokenTypes.FULL) != null
}

/**
 * TYPE clause implementation.
 * TYPE @type_expression
 */
class SurqlTypeClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val typeExpression: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * DEFAULT clause implementation.
 * DEFAULT @expression
 */
class SurqlDefaultClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val defaultValue: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * ASSERT clause implementation.
 * ASSERT @condition
 */
class SurqlAssertClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val condition: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * ON TABLE clause implementation (for DEFINE FIELD/INDEX/EVENT).
 * ON [TABLE] @table_name
 */
class SurqlOnTableClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val tableName: String?
        get() {
            var afterOn = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.ON) {
                    afterOn = true
                } else if (afterOn) {
                    if (child.elementType == SurqlTokenTypes.TABLE) {
                        // Skip TABLE keyword
                    } else if (child.elementType == SurqlTokenTypes.IDENTIFIER) {
                        return child.text
                    }
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * FIELDS clause implementation (for DEFINE INDEX).
 * FIELDS @field [, @field]*
 */
class SurqlFieldsClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * COLUMNS clause implementation (alias for FIELDS).
 */
class SurqlColumnsClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * WHEN clause implementation (for DEFINE EVENT).
 * WHEN @condition
 */
class SurqlWhenClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * THEN clause implementation (for DEFINE EVENT and IF).
 * THEN @expression
 */
class SurqlThenClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * COMMENT clause implementation.
 * COMMENT @string
 */
class SurqlCommentClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val commentText: String?
        get() = SurqlPsiImplUtil.getChildText(node, SurqlTokenTypes.STRING)
}

/**
 * WITH clause implementation (for SELECT with index hints).
 * WITH [NOINDEX | INDEX @index_name]
 */
class SurqlWithClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * EXPLAIN clause implementation.
 * EXPLAIN [FULL]
 */
class SurqlExplainClauseImpl(node: ASTNode) : SurqlClauseImpl(node) {
    
    val isFull: Boolean
        get() = node.findChildByType(SurqlTokenTypes.FULL) != null
}

/**
 * PARALLEL clause implementation.
 */
class SurqlParallelClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * VERSION clause implementation.
 * VERSION @datetime
 */
class SurqlVersionClauseImpl(node: ASTNode) : SurqlClauseImpl(node)

/**
 * OMIT clause implementation.
 * OMIT @field [, @field]*
 */
class SurqlOmitClauseImpl(node: ASTNode) : SurqlClauseImpl(node)
