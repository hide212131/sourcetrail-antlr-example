package common;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public interface SymbolSolver {
    Lexer lexerClass(CharStream cs);
    Parser parserClass(TokenStream tokens);
    ParseTree parse(Parser parser);
}
