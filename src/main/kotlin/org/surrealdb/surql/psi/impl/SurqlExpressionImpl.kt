package org.surrealdb.surql.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.types.SurqlType
import org.surrealdb.surql.types.SurqlTypeInference

// ============================================================================
// Expression Implementations
// ============================================================================

/**
 * Base class for all expression implementations.
 * Provides type inference support via the [inferredType] property.
 */
abstract class SurqlExpressionImpl(node: ASTNode) : SurqlPsiElementImpl(node) {
    
    /**
     * Returns the inferred type of this expression.
     * The result is cached per PSI modification stamp.
     */
    val inferredType: SurqlType
        get() = SurqlTypeInference.inferType(this)
    
    /**
     * Returns a human-readable string representation of the inferred type.
     */
    val typeDisplayName: kotlin.String
        get() = inferredType.displayName()
}

/**
 * Binary expression implementation.
 * @left @operator @right
 */
class SurqlBinaryExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val leftOperand: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val rightOperand: SurqlExpressionImpl?
        get() {
            var foundFirst = false
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) {
                    if (foundFirst) return child
                    foundFirst = true
                }
                child = child.nextSibling
            }
            return null
        }
    
    val operator: PsiElement?
        get() {
            var child = firstChild
            while (child != null) {
                val type = child.node.elementType
                if (type in SurqlTokenTypes.OPERATORS ||
                    type == SurqlTokenTypes.AND_KW ||
                    type == SurqlTokenTypes.OR_KW ||
                    type == SurqlTokenTypes.IS ||
                    type == SurqlTokenTypes.IN ||
                    type == SurqlTokenTypes.CONTAINS ||
                    type == SurqlTokenTypes.CONTAINSALL ||
                    type == SurqlTokenTypes.CONTAINSANY ||
                    type == SurqlTokenTypes.CONTAINSNONE ||
                    type == SurqlTokenTypes.CONTAINSNOT ||
                    type == SurqlTokenTypes.INSIDE ||
                    type == SurqlTokenTypes.NOTINSIDE ||
                    type == SurqlTokenTypes.ALLINSIDE ||
                    type == SurqlTokenTypes.ANYINSIDE ||
                    type == SurqlTokenTypes.NONEINSIDE ||
                    type == SurqlTokenTypes.OUTSIDE ||
                    type == SurqlTokenTypes.INTERSECTS ||
                    type == SurqlTokenTypes.MATCHES ||
                    type == SurqlTokenTypes.LIKE) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val operatorText: String?
        get() = operator?.text
}

/**
 * Unary expression implementation.
 * @operator @operand (prefix) or @operand @operator (postfix)
 */
class SurqlUnaryExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val operand: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val operator: PsiElement?
        get() {
            var child = firstChild
            while (child != null) {
                val type = child.node.elementType
                if (type == SurqlTokenTypes.NOT ||
                    type == SurqlTokenTypes.MINUS ||
                    type == SurqlTokenTypes.PLUS ||
                    type == SurqlTokenTypes.TILDE) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val isPrefix: Boolean
        get() {
            val op = operator ?: return false
            val expr = operand ?: return false
            return op.textOffset < expr.textOffset
        }
}

/**
 * Parenthesized expression implementation.
 * ( @expression )
 */
class SurqlParenExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val innerExpression: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Function call expression implementation.
 * @namespace::@name(@args) or @name(@args)
 */
class SurqlFunctionCallImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val functionName: String?
        get() {
            val parts = mutableListOf<String>()
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    parts.add(child.text)
                } else if (child.elementType == SurqlTokenTypes.LPAREN) {
                    break
                }
                child = child.treeNext
            }
            return if (parts.isNotEmpty()) parts.joinToString("::") else null
        }
    
    val namespace: String?
        get() {
            val parts = mutableListOf<String>()
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    parts.add(child.text)
                } else if (child.elementType == SurqlTokenTypes.LPAREN) {
                    break
                }
                child = child.treeNext
            }
            return if (parts.size > 1) parts.dropLast(1).joinToString("::") else null
        }
    
    val simpleName: String?
        get() {
            var lastIdentifier: String? = null
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    lastIdentifier = child.text
                } else if (child.elementType == SurqlTokenTypes.LPAREN) {
                    break
                }
                child = child.treeNext
            }
            return lastIdentifier
        }
    
    val arguments: SurqlArgumentListImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Argument list implementation.
 * (@arg1, @arg2, ...)
 */
class SurqlArgumentListImpl(node: ASTNode) : SurqlPsiElementImpl(node) {
    
    val arguments: List<SurqlExpressionImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
    
    val argumentCount: Int
        get() = arguments.size
}

/**
 * Field access expression implementation.
 * @object.@field or @object.@field.@subfield
 */
class SurqlFieldAccessImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val target: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val fieldName: String?
        get() {
            var foundDot = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.DOT) {
                    foundDot = true
                } else if (foundDot && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * Index access expression implementation.
 * @array[@index] or @object[@key]
 */
class SurqlIndexAccessImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val target: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val index: SurqlExpressionImpl?
        get() {
            var inBrackets = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.LBRACKET) {
                    inBrackets = true
                } else if (inBrackets && child is SurqlExpressionImpl) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
}

/**
 * Object literal expression implementation.
 * { @key: @value, ... }
 */
class SurqlObjectLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val entries: List<SurqlObjectEntryImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
    
    val isEmpty: Boolean
        get() = entries.isEmpty()
}

/**
 * Object entry implementation.
 * @key: @value
 */
class SurqlObjectEntryImpl(node: ASTNode) : SurqlPsiElementImpl(node) {
    
    val key: PsiElement?
        get() {
            var child = firstChild
            while (child != null) {
                val type = child.node.elementType
                if (type == SurqlTokenTypes.IDENTIFIER || type == SurqlTokenTypes.STRING) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val keyText: String?
        get() {
            val k = key ?: return null
            val text = k.text
            // Remove quotes from string keys
            return if (text.startsWith("\"") || text.startsWith("'")) {
                text.substring(1, text.length - 1)
            } else {
                text
            }
        }
    
    val value: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Array literal expression implementation.
 * [ @element, ... ]
 */
class SurqlArrayLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val elements: List<SurqlExpressionImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
    
    val isEmpty: Boolean
        get() = elements.isEmpty()
    
    val size: Int
        get() = elements.size
}

/**
 * Subquery expression implementation.
 * ( SELECT ... )
 */
class SurqlSubqueryImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val innerStatement: SurqlStatementImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Ternary/conditional expression implementation.
 * @condition ? @then : @else
 */
class SurqlTernaryExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val condition: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val thenExpression: SurqlExpressionImpl?
        get() {
            var foundQuestion = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.QUESTION) {
                    foundQuestion = true
                } else if (foundQuestion && child is SurqlExpressionImpl) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val elseExpression: SurqlExpressionImpl?
        get() {
            var foundColon = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.COLON) {
                    foundColon = true
                } else if (foundColon && child is SurqlExpressionImpl) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
}

/**
 * Range expression implementation.
 * @start..@end or @start..=@end
 */
class SurqlRangeExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val start: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val end: SurqlExpressionImpl?
        get() {
            var foundRange = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.DOTDOT ||
                    child.node.elementType == SurqlTokenTypes.DOTDOTDOT) {
                    foundRange = true
                } else if (foundRange && child is SurqlExpressionImpl) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val isInclusive: Boolean
        get() = node.findChildByType(SurqlTokenTypes.DOTDOTDOT) == null
}

/**
 * Graph traversal expression implementation.
 * @record->@edge->@target or @record<-@edge<-@source
 */
class SurqlGraphTraversalImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val source: SurqlExpressionImpl?
        get() {
            var child = firstChild
            while (child != null) {
                if (child is SurqlExpressionImpl) return child
                child = child.nextSibling
            }
            return null
        }
    
    val isOutgoing: Boolean
        get() = node.findChildByType(SurqlTokenTypes.ARROW) != null
    
    val isIncoming: Boolean
        get() = node.findChildByType(SurqlTokenTypes.LARROW) != null
    
    val isBidirectional: Boolean
        get() = node.findChildByType(SurqlTokenTypes.BIARROW) != null
}

/**
 * Cast expression implementation.
 * <@type> @expression
 */
class SurqlCastExpressionImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val targetType: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val expression: SurqlExpressionImpl?
        get() {
            val children = SurqlPsiImplUtil.findChildrenOfType<SurqlExpressionImpl>(this)
            return children.lastOrNull()
        }
}

/**
 * Assignment expression implementation.
 * @field = @value or @field += @value, etc.
 */
class SurqlAssignmentImpl(node: ASTNode) : SurqlPsiElementImpl(node) {
    
    val target: PsiElement?
        get() {
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.IDENTIFIER ||
                    child.node.elementType == SurqlTokenTypes.PARAMETER) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val targetName: String?
        get() = target?.text
    
    val value: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val operator: PsiElement?
        get() {
            var child = firstChild
            while (child != null) {
                val type = child.node.elementType
                if (type == SurqlTokenTypes.EQ ||
                    type == SurqlTokenTypes.PLUSEQ ||
                    type == SurqlTokenTypes.MINUSEQ ||
                    type == SurqlTokenTypes.STAREQ ||
                    type == SurqlTokenTypes.SLASHEQ) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }
    
    val isCompoundAssignment: Boolean
        get() {
            val op = operator?.node?.elementType
            return op == SurqlTokenTypes.PLUSEQ ||
                   op == SurqlTokenTypes.MINUSEQ ||
                   op == SurqlTokenTypes.STAREQ ||
                   op == SurqlTokenTypes.SLASHEQ
        }
}

// ============================================================================
// Literal Implementations
// ============================================================================

/**
 * String literal implementation.
 */
class SurqlStringLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val value: String?
        get() {
            val text = node.text
            if (text.length < 2) return null
            // Remove quotes
            return text.substring(1, text.length - 1)
        }
    
    val isDoubleQuoted: Boolean
        get() = node.text.startsWith("\"")
    
    val isSingleQuoted: Boolean
        get() = node.text.startsWith("'")
}

/**
 * Number literal implementation.
 */
class SurqlNumberLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val isInteger: Boolean
        get() = !node.text.contains('.') && !node.text.contains('e', ignoreCase = true)
    
    val isFloat: Boolean
        get() = node.text.contains('.') || node.text.contains('e', ignoreCase = true) ||
                node.text.endsWith('f', ignoreCase = true)
    
    val isDecimal: Boolean
        get() = node.text.endsWith("dec", ignoreCase = true)
    
    val isHex: Boolean
        get() = node.text.startsWith("0x", ignoreCase = true)
    
    val isBinary: Boolean
        get() = node.text.startsWith("0b", ignoreCase = true)
    
    val isOctal: Boolean
        get() = node.text.startsWith("0o", ignoreCase = true)
}

/**
 * Boolean literal implementation.
 */
class SurqlBooleanLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val value: Boolean
        get() = node.text.equals("true", ignoreCase = true)
}

/**
 * Null literal implementation.
 */
class SurqlNullLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node)

/**
 * None literal implementation (SurrealDB-specific).
 */
class SurqlNoneLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node)

/**
 * Datetime literal implementation.
 * d"2024-01-15T10:30:00Z"
 */
class SurqlDatetimeLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val value: String?
        get() {
            val text = node.text
            if (text.length < 3) return null
            // Remove d prefix and quotes
            return text.substring(2, text.length - 1)
        }
}

/**
 * Duration literal implementation.
 * 1h30m, 7d, 100ms
 */
class SurqlDurationLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node)

/**
 * UUID literal implementation.
 * u"550e8400-e29b-41d4-a716-446655440000"
 */
class SurqlUuidLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val value: String?
        get() {
            val text = node.text
            if (text.length < 3) return null
            // Remove u prefix and quotes
            return text.substring(2, text.length - 1)
        }
}

/**
 * Record ID literal implementation.
 * table:id or table:⟨complex-id⟩
 */
class SurqlRecordIdLiteralImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val tableName: String?
        get() {
            val text = node.text
            val colonIndex = text.indexOf(':')
            return if (colonIndex > 0) text.substring(0, colonIndex) else null
        }
    
    val idPart: String?
        get() {
            val text = node.text
            val colonIndex = text.indexOf(':')
            return if (colonIndex >= 0 && colonIndex < text.length - 1) {
                text.substring(colonIndex + 1)
            } else null
        }
}

/**
 * Parameter reference implementation.
 * $param
 */
class SurqlParameterRefImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val parameterName: String
        get() = node.text.removePrefix("$")
    
    val isSystemParameter: Boolean
        get() {
            val name = parameterName.lowercase()
            return name in SYSTEM_PARAMETERS
        }
    
    companion object {
        private val SYSTEM_PARAMETERS = setOf(
            "this", "parent", "value", "input", "before", "after",
            "event", "auth", "session", "scope", "token"
        )
    }
}

/**
 * Identifier reference implementation.
 */
class SurqlIdentifierRefImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val identifierName: String
        get() = node.text
}

/**
 * Future block implementation.
 * <future> { @expression }
 */
class SurqlFutureBlockImpl(node: ASTNode) : SurqlExpressionImpl(node) {
    
    val innerExpression: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}
