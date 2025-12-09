package org.surrealdb.surql.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.surrealdb.surql.lang.SurqlLanguage
import org.surrealdb.surql.lexer.SurqlLexer
import org.surrealdb.surql.lexer.SurqlTokenTypes
import org.surrealdb.surql.psi.SurqlElementTypes
import org.surrealdb.surql.psi.SurqlFile
import org.surrealdb.surql.psi.impl.*

class SurqlParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(SurqlLanguage)
    }

    override fun createLexer(project: Project?): Lexer = SurqlLexer()

    override fun createParser(project: Project?): PsiParser = SurqlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = SurqlTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = SurqlTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            // Statement types
            SurqlElementTypes.SELECT_STATEMENT -> SurqlSelectStatementImpl(node)
            SurqlElementTypes.CREATE_STATEMENT -> SurqlCreateStatementImpl(node)
            SurqlElementTypes.UPDATE_STATEMENT -> SurqlUpdateStatementImpl(node)
            SurqlElementTypes.DELETE_STATEMENT -> SurqlDeleteStatementImpl(node)
            SurqlElementTypes.INSERT_STATEMENT -> SurqlInsertStatementImpl(node)
            SurqlElementTypes.UPSERT_STATEMENT -> SurqlUpsertStatementImpl(node)
            SurqlElementTypes.RELATE_STATEMENT -> SurqlRelateStatementImpl(node)
            SurqlElementTypes.DEFINE_STATEMENT -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.REMOVE_STATEMENT -> SurqlRemoveStatementImpl(node)
            SurqlElementTypes.LET_STATEMENT -> SurqlLetStatementImpl(node)
            SurqlElementTypes.RETURN_STATEMENT -> SurqlReturnStatementImpl(node)
            SurqlElementTypes.IF_STATEMENT -> SurqlIfStatementImpl(node)
            SurqlElementTypes.FOR_STATEMENT -> SurqlForStatementImpl(node)
            SurqlElementTypes.TRANSACTION_STATEMENT -> SurqlTransactionStatementImpl(node)
            SurqlElementTypes.USE_STATEMENT -> SurqlUseStatementImpl(node)
            SurqlElementTypes.INFO_STATEMENT -> SurqlInfoStatementImpl(node)
            SurqlElementTypes.LIVE_STATEMENT -> SurqlLiveStatementImpl(node)
            
            // Expression types
            SurqlElementTypes.EXPRESSION -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.BINARY_EXPRESSION -> SurqlBinaryExpressionImpl(node)
            SurqlElementTypes.UNARY_EXPRESSION -> SurqlUnaryExpressionImpl(node)
            SurqlElementTypes.FUNCTION_CALL -> SurqlFunctionCallImpl(node)
            SurqlElementTypes.SUBQUERY -> SurqlSubqueryImpl(node)
            SurqlElementTypes.OBJECT_LITERAL -> SurqlObjectLiteralImpl(node)
            SurqlElementTypes.ARRAY_LITERAL -> SurqlArrayLiteralImpl(node)
            SurqlElementTypes.FIELD_ACCESS -> SurqlFieldAccessImpl(node)
            SurqlElementTypes.INDEX_ACCESS -> SurqlIndexAccessImpl(node)
            SurqlElementTypes.GRAPH_PATH -> SurqlGraphTraversalImpl(node)
            
            // Literal types
            SurqlElementTypes.STRING_LITERAL -> SurqlStringLiteralImpl(node)
            SurqlElementTypes.NUMBER_LITERAL -> SurqlNumberLiteralImpl(node)
            SurqlElementTypes.BOOLEAN_LITERAL -> SurqlBooleanLiteralImpl(node)
            SurqlElementTypes.NULL_LITERAL -> SurqlNullLiteralImpl(node)
            SurqlElementTypes.DATETIME_LITERAL -> SurqlDatetimeLiteralImpl(node)
            SurqlElementTypes.DURATION_LITERAL -> SurqlDurationLiteralImpl(node)
            SurqlElementTypes.UUID_LITERAL -> SurqlUuidLiteralImpl(node)
            
            // Reference types
            SurqlElementTypes.TABLE_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.FIELD_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.RECORD_ID_EXPR -> SurqlRecordIdLiteralImpl(node)
            SurqlElementTypes.PARAMETER_REF -> SurqlParameterRefImpl(node)
            SurqlElementTypes.FUNCTION_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.NAMESPACE_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.DATABASE_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.INDEX_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.EVENT_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.ANALYZER_NAME -> SurqlIdentifierRefImpl(node)
            SurqlElementTypes.ACCESS_NAME -> SurqlIdentifierRefImpl(node)
            
            // Clause types
            SurqlElementTypes.WHERE_CLAUSE -> SurqlWhereClauseImpl(node)
            SurqlElementTypes.ORDER_CLAUSE -> SurqlOrderClauseImpl(node)
            SurqlElementTypes.LIMIT_CLAUSE -> SurqlLimitClauseImpl(node)
            SurqlElementTypes.GROUP_CLAUSE -> SurqlGroupClauseImpl(node)
            SurqlElementTypes.SPLIT_CLAUSE -> SurqlSplitClauseImpl(node)
            SurqlElementTypes.FETCH_CLAUSE -> SurqlFetchClauseImpl(node)
            SurqlElementTypes.TIMEOUT_CLAUSE -> SurqlTimeoutClauseImpl(node)
            SurqlElementTypes.SET_CLAUSE -> SurqlSetClauseImpl(node)
            SurqlElementTypes.CONTENT_CLAUSE -> SurqlContentClauseImpl(node)
            SurqlElementTypes.MERGE_CLAUSE -> SurqlMergeClauseImpl(node)
            SurqlElementTypes.PERMISSIONS_CLAUSE -> SurqlPermissionsClauseImpl(node)
            
            // Definition types
            SurqlElementTypes.TABLE_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.FIELD_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.INDEX_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.EVENT_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.FUNCTION_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.PARAM_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.ANALYZER_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.ACCESS_DEFINITION -> SurqlDefineStatementImpl(node)
            SurqlElementTypes.USER_DEFINITION -> SurqlDefineStatementImpl(node)
            
            // Type expressions
            SurqlElementTypes.TYPE_EXPRESSION -> SurqlSimpleTypeImpl(node)
            SurqlElementTypes.GENERIC_TYPE -> SurqlGenericTypeImpl(node)
            SurqlElementTypes.UNION_TYPE -> SurqlUnionTypeImpl(node)
            
            // Other
            SurqlElementTypes.ASSIGNMENT -> SurqlAssignmentImpl(node)
            SurqlElementTypes.OBJECT_ENTRY -> SurqlObjectEntryImpl(node)
            SurqlElementTypes.ARGUMENT_LIST -> SurqlArgumentListImpl(node)
            SurqlElementTypes.PARAMETER_LIST -> SurqlPsiElementImpl(node)
            SurqlElementTypes.EDGE_TYPE -> SurqlIdentifierRefImpl(node)
            
            // Default fallback
            else -> SurqlPsiElementImpl(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = SurqlFile(viewProvider)
}
