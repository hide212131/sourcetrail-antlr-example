package common;

import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public class FileAST extends SymbolAST {

    @NotNull final SymbolASTTable symtab;
    @NotNull final ParseTree tree;

    public FileAST(Symbol fileSymbol, ParseTree tree, SymbolASTTable symtab) {
        super(fileSymbol, null);
        this.tree = tree;
        this.symtab = symtab;
        this.symtab.add(this);
        this.symtab.defineGlobalSymbol(fileSymbol);
    }

}
