package org.surrealdb.surql.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.surrealdb.surql.lexer.SurqlTokenTypes

// ============================================================================
// Type Expression Implementations
// ============================================================================

/**
 * Base class for type expression implementations.
 */
abstract class SurqlTypeExpressionImpl(node: ASTNode) : SurqlPsiElementImpl(node)

/**
 * Simple type implementation.
 * string, int, bool, etc.
 */
class SurqlSimpleTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val typeName: String?
        get() = SurqlPsiImplUtil.getIdentifierText(node) ?: node.text
}

/**
 * Generic type implementation.
 * array<string>, option<int>, record<user>, etc.
 */
class SurqlGenericTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val baseType: String?
        get() {
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                } else if (child.elementType == SurqlTokenTypes.LT) {
                    break
                }
                child = child.treeNext
            }
            return null
        }
    
    val typeArguments: List<SurqlTypeExpressionImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
}

/**
 * Union type implementation.
 * string | int | null
 */
class SurqlUnionTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val typeOptions: List<SurqlTypeExpressionImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
}

/**
 * Record type implementation.
 * record<user> or record<user | post>
 */
class SurqlRecordTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val tableNames: List<String>
        get() {
            val names = mutableListOf<String>()
            var inAngleBrackets = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.LT) {
                    inAngleBrackets = true
                } else if (child.elementType == SurqlTokenTypes.GT) {
                    break
                } else if (inAngleBrackets && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    names.add(child.text)
                }
                child = child.treeNext
            }
            return names
        }
}

/**
 * Geometry type implementation.
 * geometry<point>, geometry<polygon>, etc.
 */
class SurqlGeometryTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val geometryKind: String?
        get() {
            var inAngleBrackets = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.LT) {
                    inAngleBrackets = true
                } else if (inAngleBrackets && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * Literal type implementation.
 * literal<"active" | "inactive">
 */
class SurqlLiteralTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val literalValues: List<String>
        get() {
            val values = mutableListOf<String>()
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.STRING) {
                    val text = child.text
                    if (text.length >= 2) {
                        values.add(text.substring(1, text.length - 1))
                    }
                }
                child = child.treeNext
            }
            return values
        }
}

/**
 * Either type implementation.
 * either<T, U>
 */
class SurqlEitherTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val typeOptions: List<SurqlTypeExpressionImpl>
        get() = SurqlPsiImplUtil.findChildrenOfType(this)
}

/**
 * Set type implementation.
 * set<string>
 */
class SurqlSetTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val elementType: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Option type implementation.
 * option<string>
 */
class SurqlOptionTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val innerType: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * Array type implementation.
 * array<string> or array<int, 10>
 */
class SurqlArrayTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val elementType: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val maxLength: Int?
        get() {
            var foundComma = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.COMMA) {
                    foundComma = true
                } else if (foundComma && child.elementType == SurqlTokenTypes.NUMBER) {
                    return child.text.toIntOrNull()
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * Range type implementation.
 * range<int, int>
 */
class SurqlRangeTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node)

/**
 * Future type implementation.
 * future<T>
 */
class SurqlFutureTypeImpl(node: ASTNode) : SurqlTypeExpressionImpl(node) {
    
    val innerType: SurqlTypeExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}
