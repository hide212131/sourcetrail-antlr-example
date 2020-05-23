package common;

import common.annotation.Define;
import common.annotation.Reference;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class RefPhase {

    @NotNull private final SymbolSolver symbolSolver;

    public RefPhase(SymbolSolver symbolSolver) {
        this.symbolSolver = symbolSolver;
    }

    public void walk(@NotNull FileAST fileAST, SymbolASTTable symtab, RefPhaseAdapter adapter) {
        // Def Phase (create symbols)
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new Listener(fileAST, symtab, adapter), fileAST.tree);
    }

    private class Listener extends PhaseListener {

        private final SymbolASTTable symtab;
        private final RefPhaseAdapter adapter;

        public Listener(@NotNull FileAST fileAST, @NotNull SymbolASTTable symtab, RefPhaseAdapter adapter) {
            super(fileAST, fileAST.symbol.getScope());
            this.symtab = symtab;
            this.adapter = adapter;
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            getMethod(symbolSolver, ctx, Define.class).ifPresentOrElse(method -> {
                SymbolAST currentSymbolAST = symtab.ctxAST.get(ctx);
                push(currentSymbolAST);
            }, () -> getMethod(symbolSolver, ctx, Reference.class).ifPresent(method -> {
                Symbol symbol = call(method, ctx);
                SymbolAST toAst = symtab.symbolAST.get(symbol);
                adapter.refVisited(ctx, currentSymbolAST, toAst);
            }));
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            getMethod(symbolSolver, ctx, Define.class).ifPresent(method -> {
                pop();
            });
        }

        private Symbol call(Method method, ParserRuleContext ctx) {
            try {
                return (Symbol) method.invoke(symbolSolver, ctx, currentScope);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }
}
