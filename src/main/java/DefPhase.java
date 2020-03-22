import com.sourcetrail.DefinitionKind;
import com.sourcetrail.SymbolKind;
import com.sourcetrail.sourcetraildb;
import name.NameElement;
import name.NameHierarchy;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.Map;

public class DefPhase extends TLBaseListener {
    GlobalScope globals;
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    Scope currentScope; // define symbols in this scope
    Map<Symbol, Integer> sourcetrailNames;

    String file;
    int fileId;
    NameHierarchy hierarchy = new NameHierarchy(".");

    public DefPhase(GlobalScope globals, Map<Symbol, Integer> sourcetrailNames, String file) {
        this.globals = globals;
        this.sourcetrailNames = sourcetrailNames;
        this.file = file;
        pushScope(globals);

        // sourcetrail record file
        int fileId = sourcetraildb.recordFile(file);
        sourcetraildb.recordFileLanguage(fileId, "");
        if (sourcetraildb.getLastError().length() > 0) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }
        this.fileId = fileId;
    }

    @Override
    public void enterFunctionDecl(TLParser.FunctionDeclContext ctx) {
        String name = ctx.Identifier().getText();

        // push new scope by making new one that points to enclosing scope
        FunctionSymbol function = new FunctionSymbol(name);
        currentScope.define(function); // Define function in current scope
        saveScope(ctx, function);      // Push: set function's parent to current
        pushScope(function);       // Current scope is now function scope

        // sourcetrail create method index.
        NameElement childElement = new NameElement();
        childElement.name = name;
        hierarchy.push(childElement);
        String s = hierarchy.serialize();
        //System.out.println(s);
        int symbolId = sourcetraildb.recordSymbol(s);
        sourcetrailNames.put(function, symbolId);
        sourcetraildb.recordSymbolDefinitionKind(symbolId, DefinitionKind.DEFINITION_EXPLICIT);
        sourcetraildb.recordSymbolKind(symbolId, SymbolKind.SYMBOL_METHOD);
        Token token = ctx.Identifier().getSymbol();
        sourcetraildb.recordSymbolLocation(symbolId, fileId,
                token.getLine(),
                token.getCharPositionInLine() + 1,
                token.getLine(),
                token.getCharPositionInLine() + token.getText().length());
        sourcetraildb.recordSymbolScopeLocation(symbolId, fileId,
                ctx.getStart().getLine(), 1,
                ctx.getStop().getLine(), 1);

        if (sourcetraildb.getLastError().length() > 0) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }

    }

    @Override
    public void exitFunctionDecl(TLParser.FunctionDeclContext ctx) {
        popScope();
        hierarchy.pop();
    }

    void saveScope(ParserRuleContext ctx, Scope s) {
        scopes.put(ctx, s);
    }

    @Override
    public void enterVarDecl(TLParser.VarDeclContext ctx) {
        Token nameToken = ctx.Identifier().getSymbol();
        VariableSymbol var = new VariableSymbol(nameToken.getText());
        if (currentScope.getSymbol(var.getName()) == null) { // first use means decl in tiny lang.
            currentScope.define(var); // Define symbol in current scope

            // sourcetrail create field index.
            NameElement childElement = new NameElement();
            childElement.name = var.getName();
            hierarchy.push(childElement);
            String s = hierarchy.serialize();
            //System.out.println(s);
            hierarchy.pop();
            int symbolId = sourcetraildb.recordSymbol(s);
            sourcetrailNames.put(var, symbolId);
            sourcetraildb.recordSymbolDefinitionKind(symbolId, DefinitionKind.DEFINITION_EXPLICIT);
            sourcetraildb.recordSymbolKind(symbolId, SymbolKind.SYMBOL_FIELD);
            sourcetraildb.recordSymbolLocation(symbolId, fileId,
                    nameToken.getLine(),
                    nameToken.getCharPositionInLine() + 1,
                    nameToken.getLine(),
                    nameToken.getCharPositionInLine() + var.getName().length()
            );

        }
    }

    private void pushScope(Scope s) {
        currentScope = s;
        //System.out.println("entering: " + currentScope.getName() + ":" + s);
    }

    private void popScope() {
        //System.out.println("leaving: " + currentScope.getName() + ":" + currentScope);
        currentScope = currentScope.getEnclosingScope();
    }

}
