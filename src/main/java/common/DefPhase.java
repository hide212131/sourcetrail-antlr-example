package common;

import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

public abstract class DefPhase {
    DefPhaseAdapter adapter;
    SymbolTable symtab;
    Class<? extends Lexer> lexerClass;
    Class<? extends Parser> parserClass;

    public DefPhase(SymbolTable symtab, Class<? extends Lexer> lexerClass, Class<? extends Parser> parserClass) {
        this.symtab = symtab;
        this.lexerClass = lexerClass;
        this.parserClass = parserClass;
    }

    public void setAdapter(DefPhaseAdapter adapter) {
        this.adapter = adapter;
    }

    public void parse(String file, Function<Parser, ParseTree> start) throws IOException {
        try {
            // create parse tree
            CharStream cs = CharStreams.fromPath(Paths.get(file));
            Lexer lexer = lexerClass.getDeclaredConstructor(CharStream.class).newInstance(cs);
            TokenStream tokens = new CommonTokenStream(lexer);
            Parser parser = parserClass.getDeclaredConstructor(TokenStream.class).newInstance(tokens);
            parser.setBuildParseTree(true);
            ParseTree tree = start.apply(parser);

            SymbolAST fileAST = new SymbolAST();
            fileAST.symbol = new FileSymbol(file);
            symtab.fileAST.put(file, fileAST);

            // Def Phase (create symbols)
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new Listener(fileAST), tree);

        } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IOException(e);
        }
    }

    private class Listener implements ParseTreeListener {

        Scope currentScope;
        private SymbolAST currentSymbolAST;

        public Listener(SymbolAST fileAST) {
            this.currentSymbolAST = fileAST;
            this.currentScope = symtab.GLOBALS;
            adapter.defined(fileAST);
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            getMethod(ctx).ifPresent(method -> {
                Symbol symbol = call(method, ctx);

                // define symbol
                SymbolAST symbolAST = new SymbolAST();
                symbolAST.symbol = symbol;
                symbolAST.ctx = ctx;
                currentSymbolAST.addChild(symbolAST);
                // define scope
                currentScope.define(symbol);

                // change current state (When using pop(), it operates in reverse order.)
                pushSymbol(symbolAST);
                if (currentSymbolAST.symbol instanceof Scope) {
                    pushScope((Scope) currentSymbolAST.symbol);
                }

                // call adapter
                adapter.defined(symbolAST);
            });
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            getMethod(ctx).ifPresent(method -> {
                // change current state
                if (currentSymbolAST.symbol instanceof Scope) {
                    popScope();
                }
                popSymbol();
            });
        }


        private void pushSymbol(SymbolAST s) {
            currentSymbolAST = s;
            assert currentSymbolAST != null;
            //System.out.println("entering: " + currentScope.getName() + ":" + s);
        }

        private void popSymbol() {
            //System.out.println("leaving: " + currentScope.getName() + ":" + currentScope);
            currentSymbolAST = (SymbolAST) currentSymbolAST.getParent();
            assert currentSymbolAST != null;
        }

        private void pushScope(Scope s) {
            currentScope = s;
            //System.out.println("entering: " + currentScope.getName() + ":" + s);
        }

        private void popScope() {
            //System.out.println("leaving: " + currentScope.getName() + ":" + currentScope);
            currentScope = currentScope.getEnclosingScope();
        }

        @SuppressWarnings("rawtypes")
        private Optional<Method> getMethod(ParserRuleContext ctx) {
            Class clz = DefPhase.this.getClass();
            for (Method method : clz.getDeclaredMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(ctx.getClass())) {
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        }

        private Symbol call(Method method, ParserRuleContext ctx) {
            try {
                return (Symbol) method.invoke(DefPhase.this, ctx);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void visitTerminal(TerminalNode node) {
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
        }

    }
}
