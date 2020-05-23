package tl;

import common.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class TinySymbolSolverTest {
    @Test
    void testExample() throws IOException {
        SymbolASTTable symtab = new SymbolASTTable();
        TinySymbolSolver symbolSolver = new TinySymbolSolver();
        DefPhase defPhase = new DefPhase(symbolSolver);
        FileAST fileAST;
        fileAST = defPhase.parse("examples/test.tl", symtab,
                (SymbolAST symbolAST)-> System.out.println("Def.Code: " + symbolAST));
        RefPhase refPhase = new RefPhase(symbolSolver);
        refPhase.walk(fileAST, symtab,
                (ParserRuleContext prc, SymbolAST fromAst, SymbolAST toAst) -> {
                    System.out.println("Ref.Code: " + prc.getText());
                    System.out.println("Ref.From: " + fromAst);
                    System.out.println("Ref.To: " + toAst);
        });
    }
}