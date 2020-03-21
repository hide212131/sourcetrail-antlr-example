import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class DefPhase extends TLBaseListener {
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    Scope currentScope; // define symbols in this scope

    @Override
    public void enterParse(TLParser.ParseContext ctx) {
        GlobalScope globals = new GlobalScope(null);
        saveScope(ctx, globals);
        pushScope(globals);
    }

    @Override
    public void exitParse(TLParser.ParseContext ctx) {
        popScope();
    }

    @Override
    public void enterFunctionDecl(TLParser.FunctionDeclContext ctx) {
        String name = ctx.Identifier().getText();

        // push new scope by making new one that points to enclosing scope
        FunctionSymbol function = new FunctionSymbol(name);
        currentScope.define(function); // Define function in current scope
        saveScope(ctx, function);      // Push: set function's parent to current
        pushScope(function);       // Current scope is now function scope
    }

    @Override
    public void exitFunctionDecl(TLParser.FunctionDeclContext ctx) {
        popScope();
    }

    void saveScope(ParserRuleContext ctx, Scope s) {
        scopes.put(ctx, s);
    }

    @Override
    public void enterBlock(TLParser.BlockContext ctx) {
        LocalScope l = new LocalScope(currentScope);
        saveScope(ctx, l);
        pushScope(l);
    }

    @Override
    public void exitBlock(TLParser.BlockContext ctx) {
        popScope();
    }

    @Override
    public void enterVarDecl(TLParser.VarDeclContext ctx) {
        defineVar(ctx.Identifier().getSymbol());
    }

    void defineVar(Token nameToken) {
        VariableSymbol var = new VariableSymbol(nameToken.getText());
        if (currentScope.getSymbol(var.getName()) == null) { // first use means decl.
            currentScope.define(var); // Define symbol in current scope
        }
    }

    private void pushScope(Scope s) {
        currentScope = s;
        System.out.println("entering: " + currentScope.getName() + ":" + s);
    }

    private void popScope() {
        System.out.println("leaving: " + currentScope.getName() + ":" + currentScope);
        currentScope = currentScope.getEnclosingScope();
    }

}
