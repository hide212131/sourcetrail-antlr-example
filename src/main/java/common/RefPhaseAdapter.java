package common;

import org.antlr.v4.runtime.ParserRuleContext;

public interface RefPhaseAdapter {
    void refVisited(ParserRuleContext prc, SymbolAST fromAst, SymbolAST toAst);
}
