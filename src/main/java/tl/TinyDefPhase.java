package tl;

import common.DefPhase;
import common.SymbolTable;
import org.antlr.symtab.FunctionSymbol;
import tl.generated.TLLexer;
import tl.generated.TLParser;

import java.io.IOException;

public class TinyDefPhase extends DefPhase {

    public TinyDefPhase(SymbolTable symtab) {
        super(symtab, TLLexer.class, TLParser.class);
    }

    public FunctionSymbol functionDef(TLParser.FunctionDeclContext ctx) {
        return new FunctionSymbol(ctx.Identifier().getText());
    }

    public void parse(String file) throws IOException {
        parse(file, parser -> ((TLParser) parser).parse());
    }
}



