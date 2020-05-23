package common;

import org.antlr.symtab.Symbol;
import org.antlr.symtab.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;

public class SymbolASTTable extends SymbolTable {
    final Map<String, FileAST> parseFile = new IdentityHashMap<>();
    final Map<ParseTree, SymbolAST> ctxAST = new IdentityHashMap<>();
    final Map<Symbol, SymbolAST> symbolAST = new IdentityHashMap<>();
    //final Map<Scope, SymbolAST> scopeAST = new IdentityHashMap<>();
    void add(@NotNull SymbolAST ast) {
        ast.table = this;
        if (ast instanceof FileAST) {
            parseFile.put(ast.symbol.getName(), (FileAST)ast);
        } else {
            symbolAST.put(ast.symbol, ast);
            ctxAST.put(ast.ctx, ast);
//            if (ast.symbol instanceof Scope) {
//                //scopeAST.put((Scope) ast.symbol, ast);
//            }
        }
    }
}
