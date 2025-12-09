package org.surrealdb.surql.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.SurqlNamedElement
import org.surrealdb.surql.psi.SurqlPsiElement

/**
 * Base implementation for all SurrealQL PSI elements.
 */
open class SurqlPsiElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), SurqlPsiElement

/**
 * Base implementation for named SurrealQL elements.
 * Used for tables, fields, functions, parameters, etc.
 */
abstract class SurqlNamedElementImpl(node: ASTNode) : SurqlPsiElementImpl(node), SurqlNamedElement {

    override fun getName(): String? {
        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier
        if (identifier != null) {
            // For now, return self - proper rename requires element factory
            // TODO: Implement SurqlElementFactory for proper rename support
        }
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return node.findChildByType(SurqlTokenTypes.IDENTIFIER)?.psi
    }

    override fun getTextOffset(): Int {
        return nameIdentifier?.textOffset ?: super.getTextOffset()
    }
}

// ============================================================================
// Statement Implementations
// ============================================================================

/**
 * Base class for all statement implementations.
 */
abstract class SurqlStatementImpl(node: ASTNode) : SurqlPsiElementImpl(node)

/**
 * SELECT statement implementation.
 * SELECT [VALUE] @projections FROM @targets [WHERE @condition] [clauses...]
 */
class SurqlSelectStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isValueSelect: Boolean
        get() = node.findChildByType(SurqlTokenTypes.VALUE) != null
    
    val projections: List<PsiElement>
        get() = SurqlPsiImplUtil.findChildrenOfType<SurqlExpressionImpl>(this)
    
    val fromTargets: List<PsiElement>
        get() {
            // Find elements after FROM keyword
            var afterFrom = false
            val targets = mutableListOf<PsiElement>()
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.FROM) {
                    afterFrom = true
                } else if (afterFrom) {
                    if (child.node.elementType == SurqlTokenTypes.WHERE ||
                        child.node.elementType == SurqlTokenTypes.ORDER ||
                        child.node.elementType == SurqlTokenTypes.LIMIT ||
                        child.node.elementType == SurqlTokenTypes.GROUP ||
                        child.node.elementType == SurqlTokenTypes.SPLIT ||
                        child.node.elementType == SurqlTokenTypes.FETCH) {
                        break
                    }
                    if (child is SurqlExpressionImpl || 
                        child.node.elementType == SurqlTokenTypes.IDENTIFIER ||
                        child.node.elementType == SurqlTokenTypes.RECORD_ID) {
                        targets.add(child)
                    }
                }
                child = child.nextSibling
            }
            return targets
        }
    
    val whereClause: SurqlWhereClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val orderClause: SurqlOrderClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val limitClause: SurqlLimitClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val groupClause: SurqlGroupClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * CREATE statement implementation.
 * CREATE [ONLY] @targets [CONTENT @object | SET @field = @value, ...]
 */
class SurqlCreateStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isOnly: Boolean
        get() = node.findChildByType(SurqlTokenTypes.ONLY) != null
    
    val target: PsiElement?
        get() {
            var afterCreate = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.CREATE) {
                    afterCreate = true
                } else if (afterCreate && child.node.elementType != SurqlTokenTypes.WHITE_SPACE) {
                    if (child.node.elementType == SurqlTokenTypes.ONLY) {
                        // Skip ONLY keyword
                    } else {
                        return child
                    }
                }
                child = child.nextSibling
            }
            return null
        }
    
    val contentClause: SurqlContentClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val setClause: SurqlSetClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * UPDATE statement implementation.
 * UPDATE [ONLY] @targets [CONTENT @object | MERGE @object | SET @field = @value, ...] [WHERE @condition]
 */
class SurqlUpdateStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isOnly: Boolean
        get() = node.findChildByType(SurqlTokenTypes.ONLY) != null
    
    val target: PsiElement?
        get() {
            var afterUpdate = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.UPDATE) {
                    afterUpdate = true
                } else if (afterUpdate && child.node.elementType != SurqlTokenTypes.WHITE_SPACE) {
                    if (child.node.elementType == SurqlTokenTypes.ONLY) {
                        // Skip ONLY keyword
                    } else if (child.node.elementType != SurqlTokenTypes.SET &&
                               child.node.elementType != SurqlTokenTypes.CONTENT &&
                               child.node.elementType != SurqlTokenTypes.MERGE &&
                               child.node.elementType != SurqlTokenTypes.WHERE) {
                        return child
                    } else {
                        break
                    }
                }
                child = child.nextSibling
            }
            return null
        }
    
    val contentClause: SurqlContentClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val mergeClause: SurqlMergeClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val setClause: SurqlSetClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
    
    val whereClause: SurqlWhereClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * DELETE statement implementation.
 * DELETE [ONLY] @targets [WHERE @condition]
 */
class SurqlDeleteStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isOnly: Boolean
        get() = node.findChildByType(SurqlTokenTypes.ONLY) != null
    
    val target: PsiElement?
        get() {
            var afterDelete = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.DELETE) {
                    afterDelete = true
                } else if (afterDelete && child.node.elementType != SurqlTokenTypes.WHITE_SPACE) {
                    if (child.node.elementType == SurqlTokenTypes.ONLY) {
                        // Skip ONLY keyword
                    } else if (child.node.elementType != SurqlTokenTypes.WHERE) {
                        return child
                    } else {
                        break
                    }
                }
                child = child.nextSibling
            }
            return null
        }
    
    val whereClause: SurqlWhereClauseImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * INSERT statement implementation.
 * INSERT [IGNORE] INTO @table @values [ON DUPLICATE KEY UPDATE @assignments]
 */
class SurqlInsertStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isIgnore: Boolean
        get() = node.findChildByType(SurqlTokenTypes.IGNORE) != null
    
    val tableName: String?
        get() {
            var afterInto = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.INTO) {
                    afterInto = true
                } else if (afterInto && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * UPSERT statement implementation.
 * UPSERT [ONLY] @targets [CONTENT @object | MERGE @object | SET @field = @value, ...] [WHERE @condition]
 */
class SurqlUpsertStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val isOnly: Boolean
        get() = node.findChildByType(SurqlTokenTypes.ONLY) != null
    
    val target: PsiElement?
        get() {
            var afterUpsert = false
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == SurqlTokenTypes.UPSERT) {
                    afterUpsert = true
                } else if (afterUpsert && child.node.elementType != SurqlTokenTypes.WHITE_SPACE) {
                    if (child.node.elementType == SurqlTokenTypes.ONLY) {
                        // Skip ONLY keyword
                    } else if (child.node.elementType != SurqlTokenTypes.SET &&
                               child.node.elementType != SurqlTokenTypes.CONTENT &&
                               child.node.elementType != SurqlTokenTypes.MERGE &&
                               child.node.elementType != SurqlTokenTypes.WHERE) {
                        return child
                    } else {
                        break
                    }
                }
                child = child.nextSibling
            }
            return null
        }
}

/**
 * RELATE statement implementation.
 * RELATE @from -> @edge -> @to [CONTENT @object | SET @field = @value, ...]
 */
class SurqlRelateStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * DEFINE statement implementation.
 * DEFINE [NAMESPACE|DATABASE|TABLE|FIELD|INDEX|EVENT|FUNCTION|PARAM|ANALYZER|ACCESS|USER] ...
 */
class SurqlDefineStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    enum class DefineType {
        NAMESPACE, DATABASE, TABLE, FIELD, INDEX, EVENT, FUNCTION, PARAM, ANALYZER, ACCESS, USER, CONFIG, API, MODEL, UNKNOWN
    }
    
    val defineType: DefineType
        get() {
            // First check if this node's element type directly indicates the define type
            // (for TABLE_DEFINITION, FIELD_DEFINITION, etc.)
            when (node.elementType) {
                SurqlElementTypes.TABLE_DEFINITION -> return DefineType.TABLE
                SurqlElementTypes.FIELD_DEFINITION -> return DefineType.FIELD
                SurqlElementTypes.INDEX_DEFINITION -> return DefineType.INDEX
                SurqlElementTypes.EVENT_DEFINITION -> return DefineType.EVENT
                SurqlElementTypes.FUNCTION_DEFINITION -> return DefineType.FUNCTION
                SurqlElementTypes.PARAM_DEFINITION -> return DefineType.PARAM
                SurqlElementTypes.ANALYZER_DEFINITION -> return DefineType.ANALYZER
                SurqlElementTypes.ACCESS_DEFINITION -> return DefineType.ACCESS
                SurqlElementTypes.USER_DEFINITION -> return DefineType.USER
            }
            
            // For DEFINE_STATEMENT, look at child tokens and nested definition elements
            var afterDefine = false
            var child = node.firstChildNode
            while (child != null) {
                val elementType = child.elementType
                // Skip whitespace and comments
                if (elementType == SurqlTokenTypes.WHITE_SPACE ||
                    elementType == SurqlTokenTypes.LINE_COMMENT ||
                    elementType == SurqlTokenTypes.BLOCK_COMMENT) {
                    child = child.treeNext
                    continue
                }
                
                if (elementType == SurqlTokenTypes.DEFINE) {
                    afterDefine = true
                } else if (afterDefine) {
                    // Check for direct token types (e.g., DEFINE NAMESPACE without nested element)
                    when (elementType) {
                        SurqlTokenTypes.NAMESPACE, SurqlTokenTypes.NS -> return DefineType.NAMESPACE
                        SurqlTokenTypes.DATABASE, SurqlTokenTypes.DB -> return DefineType.DATABASE
                        SurqlTokenTypes.TABLE -> return DefineType.TABLE
                        SurqlTokenTypes.FIELD -> return DefineType.FIELD
                        SurqlTokenTypes.INDEX -> return DefineType.INDEX
                        SurqlTokenTypes.EVENT -> return DefineType.EVENT
                        SurqlTokenTypes.FUNCTION -> return DefineType.FUNCTION
                        SurqlTokenTypes.PARAM -> return DefineType.PARAM
                        SurqlTokenTypes.ANALYZER -> return DefineType.ANALYZER
                        SurqlTokenTypes.ACCESS -> return DefineType.ACCESS
                        SurqlTokenTypes.USER -> return DefineType.USER
                        SurqlTokenTypes.CONFIG -> return DefineType.CONFIG
                        SurqlTokenTypes.API -> return DefineType.API
                        SurqlTokenTypes.MODEL -> return DefineType.MODEL
                    }
                    // Check for nested definition element types
                    when (elementType) {
                        SurqlElementTypes.TABLE_DEFINITION -> return DefineType.TABLE
                        SurqlElementTypes.FIELD_DEFINITION -> return DefineType.FIELD
                        SurqlElementTypes.INDEX_DEFINITION -> return DefineType.INDEX
                        SurqlElementTypes.EVENT_DEFINITION -> return DefineType.EVENT
                        SurqlElementTypes.FUNCTION_DEFINITION -> return DefineType.FUNCTION
                        SurqlElementTypes.PARAM_DEFINITION -> return DefineType.PARAM
                        SurqlElementTypes.ANALYZER_DEFINITION -> return DefineType.ANALYZER
                        SurqlElementTypes.ACCESS_DEFINITION -> return DefineType.ACCESS
                        SurqlElementTypes.USER_DEFINITION -> return DefineType.USER
                    }
                    // If we reach here with an unknown element type after DEFINE, 
                    // it might be an identifier or something else - continue looking
                    // only return UNKNOWN if we've exhausted all children
                }
                child = child.treeNext
            }
            return DefineType.UNKNOWN
        }
    
    val definedName: String?
        get() {
            // Helper function to extract text from a name wrapper element (TABLE_NAME, FIELD_NAME, etc.)
            fun extractNameFromWrapper(wrapperNode: ASTNode): String? {
                var wrapperChild = wrapperNode.firstChildNode
                while (wrapperChild != null) {
                    val wrapperChildType = wrapperChild.elementType
                    if (wrapperChildType == SurqlTokenTypes.IDENTIFIER ||
                        wrapperChildType == SurqlTokenTypes.PARAMETER) {
                        return wrapperChild.text
                    }
                    wrapperChild = wrapperChild.treeNext
                }
                // If no identifier found inside, use the wrapper's text directly
                return wrapperNode.text.takeIf { it.isNotBlank() }
            }
            
            // Helper function to find name in a node tree
            fun findNameInNode(searchNode: ASTNode): String? {
                var foundType = false
                var child = searchNode.firstChildNode
                while (child != null) {
                    val type = child.elementType
                    // Skip whitespace and comments
                    if (type == SurqlTokenTypes.WHITE_SPACE ||
                        type == SurqlTokenTypes.LINE_COMMENT ||
                        type == SurqlTokenTypes.BLOCK_COMMENT) {
                        child = child.treeNext
                        continue
                    }
                    
                    if (type == SurqlTokenTypes.TABLE || type == SurqlTokenTypes.FIELD ||
                        type == SurqlTokenTypes.INDEX || type == SurqlTokenTypes.EVENT ||
                        type == SurqlTokenTypes.FUNCTION || type == SurqlTokenTypes.PARAM ||
                        type == SurqlTokenTypes.ANALYZER || type == SurqlTokenTypes.ACCESS ||
                        type == SurqlTokenTypes.USER || type == SurqlTokenTypes.NAMESPACE ||
                        type == SurqlTokenTypes.NS || type == SurqlTokenTypes.DATABASE ||
                        type == SurqlTokenTypes.DB || type == SurqlTokenTypes.MODEL ||
                        type == SurqlTokenTypes.CONFIG || type == SurqlTokenTypes.API) {
                        foundType = true
                    } else if (foundType) {
                        // Skip OVERWRITE and IF NOT EXISTS tokens
                        if (type == SurqlTokenTypes.OVERWRITE ||
                            type == SurqlTokenTypes.IF_KW || type == SurqlTokenTypes.IF ||
                            type == SurqlTokenTypes.NOT || type == SurqlTokenTypes.EXISTS) {
                            child = child.treeNext
                            continue
                        }
                        
                        // Direct identifier or parameter
                        if (type == SurqlTokenTypes.IDENTIFIER) {
                            return child.text
                        } else if (type == SurqlTokenTypes.PARAMETER) {
                            return child.text
                        }
                        
                        // Check for name wrapper elements (TABLE_NAME, FIELD_NAME, etc.)
                        if (type == SurqlElementTypes.TABLE_NAME ||
                            type == SurqlElementTypes.FIELD_NAME ||
                            type == SurqlElementTypes.INDEX_NAME ||
                            type == SurqlElementTypes.EVENT_NAME ||
                            type == SurqlElementTypes.FUNCTION_NAME ||
                            type == SurqlElementTypes.NAMESPACE_NAME ||
                            type == SurqlElementTypes.DATABASE_NAME ||
                            type == SurqlElementTypes.ANALYZER_NAME ||
                            type == SurqlElementTypes.ACCESS_NAME ||
                            type == SurqlElementTypes.PARAMETER_REF) {
                            return extractNameFromWrapper(child)
                        }
                    }
                    child = child.treeNext
                }
                return null
            }
            
            // First try to find the name in this node directly
            val directName = findNameInNode(node)
            if (directName != null) return directName
            
            // If not found, look inside nested definition elements
            var child = node.firstChildNode
            while (child != null) {
                val type = child.elementType
                if (type == SurqlElementTypes.TABLE_DEFINITION ||
                    type == SurqlElementTypes.FIELD_DEFINITION ||
                    type == SurqlElementTypes.INDEX_DEFINITION ||
                    type == SurqlElementTypes.EVENT_DEFINITION ||
                    type == SurqlElementTypes.FUNCTION_DEFINITION ||
                    type == SurqlElementTypes.PARAM_DEFINITION ||
                    type == SurqlElementTypes.ANALYZER_DEFINITION ||
                    type == SurqlElementTypes.ACCESS_DEFINITION ||
                    type == SurqlElementTypes.USER_DEFINITION) {
                    val nestedName = findNameInNode(child)
                    if (nestedName != null) return nestedName
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * REMOVE statement implementation.
 * REMOVE [NAMESPACE|DATABASE|TABLE|FIELD|INDEX|EVENT|FUNCTION|PARAM|ANALYZER|ACCESS|USER] @name
 */
class SurqlRemoveStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * LET statement implementation.
 * LET $param = @value
 */
class SurqlLetStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val parameterName: String?
        get() = SurqlPsiImplUtil.getChildText(node, SurqlTokenTypes.PARAMETER)
    
    val value: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * IF statement implementation.
 * IF @condition THEN @result [ELSE IF @condition THEN @result]* [ELSE @result] END
 */
class SurqlIfStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * FOR statement implementation.
 * FOR $item IN @collection { @statements }
 */
class SurqlForStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val iteratorName: String?
        get() = SurqlPsiImplUtil.getChildText(node, SurqlTokenTypes.PARAMETER)
}

/**
 * RETURN statement implementation.
 * RETURN @value
 */
class SurqlReturnStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val value: SurqlExpressionImpl?
        get() = SurqlPsiImplUtil.findChildOfType(this)
}

/**
 * THROW statement implementation.
 * THROW @value
 */
class SurqlThrowStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * BEGIN/COMMIT/CANCEL transaction statements.
 */
class SurqlTransactionStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * USE statement implementation.
 * USE [NS @namespace] [DB @database]
 */
class SurqlUseStatementImpl(node: ASTNode) : SurqlStatementImpl(node) {
    
    val namespaceName: String?
        get() {
            var afterNs = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.NS || child.elementType == SurqlTokenTypes.NAMESPACE) {
                    afterNs = true
                } else if (afterNs && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                }
                child = child.treeNext
            }
            return null
        }
    
    val databaseName: String?
        get() {
            var afterDb = false
            var child = node.firstChildNode
            while (child != null) {
                if (child.elementType == SurqlTokenTypes.DB || child.elementType == SurqlTokenTypes.DATABASE) {
                    afterDb = true
                } else if (afterDb && child.elementType == SurqlTokenTypes.IDENTIFIER) {
                    return child.text
                }
                child = child.treeNext
            }
            return null
        }
}

/**
 * INFO statement implementation.
 * INFO FOR [ROOT|NAMESPACE|DATABASE|TABLE @name]
 */
class SurqlInfoStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * LIVE SELECT statement implementation.
 * LIVE SELECT @fields FROM @targets [WHERE @condition]
 */
class SurqlLiveStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * KILL statement implementation.
 * KILL @uuid
 */
class SurqlKillStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * BREAK statement implementation.
 */
class SurqlBreakStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * CONTINUE statement implementation.
 */
class SurqlContinueStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * SLEEP statement implementation.
 * SLEEP @duration
 */
class SurqlSleepStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * SHOW statement implementation.
 * SHOW CHANGES FOR TABLE @name [SINCE @version] [LIMIT @limit]
 */
class SurqlShowStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * REBUILD statement implementation.
 * REBUILD INDEX [IF EXISTS] @name ON @table
 */
class SurqlRebuildStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * ALTER statement implementation.
 * ALTER TABLE @name [actions]
 */
class SurqlAlterStatementImpl(node: ASTNode) : SurqlStatementImpl(node)

/**
 * OPTION statement implementation.
 * OPTION @name = @value
 */
class SurqlOptionStatementImpl(node: ASTNode) : SurqlStatementImpl(node)
