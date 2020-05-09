package common;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.ParserRuleContext;

public class SymbolAST extends BaseTree {
    public SymbolAST parent;
    public Symbol symbol;
    //public Scope scope;
    public ParserRuleContext ctx;

    public SymbolAST getFileSymbolAST() {
        SymbolAST ast = this;
        while (! (ast.symbol instanceof FileSymbol)) {
           ast = (SymbolAST) getParent();
           if (ast == null) {
               throw new IllegalStateException("FileSymbol not found.");
           }
        }
        return ast;
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
        return symbol.getName();
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




