import org.antlr.symtab.FunctionSymbol;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class RefPhase extends TLBaseListener {
    ParseTreeProperty<Scope> scopes;
    Scope currentScope; // resolve symbols starting in this scope

    public RefPhase(ParseTreeProperty<Scope> scopes) {
        this.scopes = scopes;
    }

    @Override
    public void enterParse(TLParser.ParseContext ctx) {
        currentScope = scopes.get(ctx);
    }

    @Override
    public void exitParse(TLParser.ParseContext ctx) {
        currentScope = currentScope.getEnclosingScope();
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
    public void enterBlock(TLParser.BlockContext ctx) {
        currentScope = scopes.get(ctx);
    }

    @Override
    public void exitBlock(TLParser.BlockContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterVarExpression(TLParser.VarExpressionContext ctx) {
        String name = ctx.Identifier().getSymbol().getText();
        Symbol var = currentScope.resolve(name);
        if (var == null) {
            CheckSymbols.error(ctx.Identifier().getSymbol(), "no such variable: " + name);
        }
        if (var instanceof FunctionSymbol) {
            CheckSymbols.error(ctx.Identifier().getSymbol(), name + " is not a variable");
        }
    }

    @Override
    public void enterIdentifierFunctionCall(TLParser.IdentifierFunctionCallContext ctx) {
        // can only handle f(...) not expr(...)
        String funcName = ctx.Identifier().getText();
        Symbol meth = currentScope.resolve(funcName);
        if (meth == null) {
            CheckSymbols.error(ctx.Identifier().getSymbol(), "no such function: " + funcName);
        }
        if (meth instanceof VariableSymbol) {
            CheckSymbols.error(ctx.Identifier().getSymbol(), funcName + " is not a function");
        }
    }
}