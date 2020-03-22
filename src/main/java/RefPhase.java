import com.sourcetrail.ReferenceKind;
import com.sourcetrail.sourcetraildb;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.Map;

public class RefPhase extends TLBaseListener {
    Map<Symbol, Integer> sourcetrailNames;
    ParseTreeProperty<Scope> scopes;
    Scope currentScope; // resolve symbols starting in this scope
    int fileId;

    public RefPhase(DefPhase def) {
        this.currentScope = def.globals;
        this.scopes = def.scopes;
        this.sourcetrailNames = def.sourcetrailNames;
        this.fileId = def.fileId;
    }

    @Override
    public void enterFunctionDecl(TLParser.FunctionDeclContext ctx) {
        currentScope = scopes.get(ctx);
    }

    @Override
    public void exitFunctionDecl(TLParser.FunctionDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterVarExpression(TLParser.VarExpressionContext ctx) {
        String name = ctx.Identifier().getSymbol().getText();
        Symbol var = currentScope.resolve(name);

        if (var != null) {
            // sourcetrail create var index.
            int contextSymbolId;
            if (currentScope instanceof Symbol) {
                Symbol s = (Symbol) currentScope;
                contextSymbolId = sourcetrailNames.get(s);
            } else {
                contextSymbolId = fileId;
            }

            int referenceId = sourcetraildb.recordReference(
                    contextSymbolId,
                    sourcetrailNames.get(var),
                    ReferenceKind.REFERENCE_CALL
            );
            Token token = ctx.Identifier().getSymbol();
            sourcetraildb.recordReferenceLocation(referenceId, fileId,
                    token.getLine(),
                    token.getCharPositionInLine() + 1,
                    token.getLine(),
                    token.getCharPositionInLine() + token.getText().length()
            );
            if (sourcetraildb.getLastError().length() > 0) {
                throw new RuntimeException(sourcetraildb.getLastError());
            }
        }
    }

    @Override
    public void enterIdentifierFunctionCall(TLParser.IdentifierFunctionCallContext ctx) {
        // can only handle f(...) not expr(...)
        String funcName = ctx.Identifier().getText();
        Symbol func = currentScope.resolve(funcName);

        int contextSymbolId;
        if (currentScope instanceof Symbol) {
            Symbol s = (Symbol) currentScope;
            contextSymbolId = sourcetrailNames.get(s);
        } else {
            contextSymbolId = fileId;
        }

        int referenceId = sourcetraildb.recordReference(
                contextSymbolId,
                sourcetrailNames.get(func),
                ReferenceKind.REFERENCE_CALL
        );
        Token token = ctx.Identifier().getSymbol();
        sourcetraildb.recordReferenceLocation(referenceId, fileId,
                token.getLine(),
                token.getCharPositionInLine() + 1,
                token.getLine(),
                token.getCharPositionInLine() + token.getText().length()
        );
        if (sourcetraildb.getLastError().length() > 0) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }
    }
}