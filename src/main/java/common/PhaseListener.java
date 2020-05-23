package common;

import org.antlr.symtab.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public abstract class PhaseListener implements ParseTreeListener {

    @NotNull protected SymbolAST currentSymbolAST;
    @NotNull protected Scope currentScope;

    public PhaseListener(SymbolAST currentSymbolAST, Scope currentScope) {
        this.currentSymbolAST = currentSymbolAST;
        this.currentScope = currentScope;
    }

    protected void push(SymbolAST symbolAST) {
        // change current state (When using pop(), it operates in reverse order.)
        pushSymbol(symbolAST);
        if (currentSymbolAST.symbol instanceof Scope) {
            pushScope((Scope) currentSymbolAST.symbol);
        }
    }

    protected void pop() {
        // change current state
        if (currentSymbolAST.symbol instanceof Scope) {
            popScope();
        }
        popSymbol();
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
    protected Optional<Method> getMethod(@NotNull SymbolSolver symbolSolver, ParserRuleContext ctx, Class<? extends Annotation> annotation) {
        Class clz = symbolSolver.getClass();
        //  MethodAnnotation)
        for (Method method : clz.getDeclaredMethods()) {
            if (Arrays.stream(method.getAnnotations()).anyMatch(p -> p.annotationType().equals(annotation)) &&
                    method.getParameterTypes()[0].equals(ctx.getClass())) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    @Override
    public void visitTerminal(TerminalNode node) {
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
    }

}
