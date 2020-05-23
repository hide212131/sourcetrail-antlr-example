package common;

import common.annotation.Define;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class DefPhase {

    @NotNull private final SymbolSolver symbolSolver;

    public DefPhase(@NotNull SymbolSolver symbolSolver) {
        this.symbolSolver = symbolSolver;
    }

    public FileAST parse(String file, SymbolASTTable symtab, DefPhaseAdapter adapter) throws IOException {
        // create parse tree
        CharStream cs = CharStreams.fromPath(Paths.get(file));
        Lexer lexer = symbolSolver.lexerClass(cs);
        TokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = symbolSolver.parserClass(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = symbolSolver.parse(parser);

        FileSymbol fileSymbol = new FileSymbol(file); // TODO: Case of FileSymbolWithScope language
        FileAST fileAST = new FileAST(fileSymbol, tree, symtab);

        // Def Phase (create symbols)
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new Listener(fileAST, adapter), tree);

        return fileAST;
    }

    private class Listener extends PhaseListener {

        DefPhaseAdapter adapter;

        public Listener(@NotNull FileAST fileAST, DefPhaseAdapter adapter) {
            super(fileAST, fileAST.symbol.getScope());
            this.adapter = adapter;
            if (adapter != null) adapter.defined(fileAST);
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            getMethod(symbolSolver, ctx, Define.class).ifPresent(method -> {
                Symbol symbol = call(method, ctx);

                // define symbol
                SymbolAST symbolAST = new SymbolAST(symbol, ctx);
                currentSymbolAST.addChild(symbolAST);
                // define scope
                currentScope.define(symbol);

                push(symbolAST);

                // call adapter
                if (adapter != null) adapter.defined(symbolAST);
            });
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            getMethod(symbolSolver, ctx, Define.class).ifPresent(method -> {
                pop();
            });
        }

        private Symbol call(Method method, ParserRuleContext ctx) {
            try {
                return (Symbol) method.invoke(symbolSolver, ctx);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }
}
