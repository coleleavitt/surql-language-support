package org.surrealdb.surql.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.surrealdb.surql.lexer.SurqlTokenTypes

/**
 * Provides brace matching functionality for SurrealQL.
 * Matches parentheses (), brackets [], and braces {}.
 */
class SurqlBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?
    ): Boolean {
        // Allow paired braces before whitespace, comments, and closing brackets
        return contextType == null ||
                contextType == SurqlTokenTypes.WHITE_SPACE ||
                contextType == SurqlTokenTypes.LINE_COMMENT ||
                contextType == SurqlTokenTypes.BLOCK_COMMENT ||
                contextType == SurqlTokenTypes.RPAREN ||
                contextType == SurqlTokenTypes.RBRACKET ||
                contextType == SurqlTokenTypes.RBRACE ||
                contextType == SurqlTokenTypes.SEMICOLON ||
                contextType == SurqlTokenTypes.COMMA
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }

    companion object {
        private val PAIRS = arrayOf(
            BracePair(SurqlTokenTypes.LPAREN, SurqlTokenTypes.RPAREN, false),
            BracePair(SurqlTokenTypes.LBRACKET, SurqlTokenTypes.RBRACKET, false),
            BracePair(SurqlTokenTypes.LBRACE, SurqlTokenTypes.RBRACE, true)  // structural = true for code blocks
        )
    }
}
