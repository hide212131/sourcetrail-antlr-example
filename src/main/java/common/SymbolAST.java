package common;

import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymbolAST extends BaseTree {
    @NotNull SymbolAST parent;
    @NotNull public Symbol symbol;
    @NotNull public ParserRuleContext ctx;
    @NotNull SymbolASTTable table;

    SymbolAST(Symbol symbol, ParserRuleContext ctx) {
        this.symbol = symbol;
        this.ctx = ctx;
    }

    @NotNull
    public FileAST getFile() {
        SymbolAST ast = this;
        while (! (ast instanceof FileAST)) {
           ast = (SymbolAST) getParent();
           if (ast == null) {
               throw new IllegalStateException("FileSymbol not found.");
           }
        }
        return (FileAST)ast;
    }

    @Override
    public void addChild(Tree t) {
        super.addChild(t);
        table.add((SymbolAST) t);
    }

    @Override
    public Tree getParent() {
        return parent;
    }

    @Override
    public void setParent(Tree t) {
        this.parent = (SymbolAST) t;
    }

    @Override
    public String toString() {
        if ( isNil() ) {
            return "nil";
        }
        List<String> l = new ArrayList<>();
        SymbolAST ast = this;
        while(ast != null && !(ast instanceof FileAST)) {
            l.add(ast.symbol.getName());
            ast = (SymbolAST) ast.getParent();
        }
        Collections.reverse(l);
        return (ast != null ? ast.symbol : "") + "#" + String.join(".", l);
    }

    @Override
    public boolean isNil() {
        return symbol == null;
    }

    @Override
    public int getTokenStartIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTokenStartIndex(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTokenStopIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTokenStopIndex(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tree dupNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getText() {
        throw new UnsupportedOperationException();
    }

}




