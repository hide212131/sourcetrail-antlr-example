package tl;

import common.SymbolSolver;
import common.annotation.Define;
import common.annotation.Reference;
import org.antlr.symtab.FunctionSymbol;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import tl.generated.TLLexer;
import tl.generated.TLParser;

public class TinySymbolSolver implements SymbolSolver {

    @Override
    public Lexer lexerClass(CharStream cs) { return new TLLexer(cs); }

    @Override
    public Parser parserClass(TokenStream tokens) { return new TLParser(tokens); }

    @Override
    public ParseTree parse(Parser parser) { return ((TLParser) parser).parse(); }

    @Define
    public FunctionSymbol functionDef(@NotNull TLParser.FunctionDeclContext ctx) {
        String funcName = ctx.Identifier().getText();
        return new FunctionSymbol(funcName);
    }

    @Reference
    public FunctionSymbol functionRef(@NotNull TLParser.IdentifierFunctionCallContext ctx, @NotNull Scope currentScope) {
        // can only handle f(...) not expr(...)
        String funcName = ctx.Identifier().getText();
        return (FunctionSymbol) currentScope.resolve(funcName);
    }

}
