package tl.sourcetrail;

import com.sourcetrail.DefinitionKind;
import com.sourcetrail.ReferenceKind;
import com.sourcetrail.SymbolKind;
import com.sourcetrail.sourcetraildb;
import common.*;
import org.antlr.symtab.FunctionSymbol;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import sourcetrail.NameElement;
import sourcetrail.NameHierarchy;
import tl.TinySymbolSolver;
import tl.generated.TLParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.IdentityHashMap;
import java.util.Map;

public class TinyIndexer {
    private int dBVersion;
    private String dBFilePath;
    private String sourceDirPath;

    public static void main(String[] args) throws Exception {
        TinyIndexer indexer = new TinyIndexer();
        indexer.configure(args);
        indexer.process();
    }

    public void configure(String[] args) {
        for (String arg : args) {
            String name = "";
            String value = "";
            try {
                name = arg.split("=")[0];
                value = arg.split("=")[1];
            } catch (Exception e) {
                System.out.println("Invalid parameter:" + arg);
                System.exit(1);
            }
            switch (name) {
                case "--database-file-path":
                    this.dBFilePath = value;
                    break;

                case "--source-dir-path":
                    this.sourceDirPath = value;
                    break;

                case "--database-version":
                    try {
                        this.dBVersion = Integer.parseInt(value);
                    } catch (Exception e) {
                        System.out.println("Invalid DB version:" + value);
                    }
                    break;

                default:
                    System.out.println("Invalid parameter:" + name);
                    System.exit(1);
            }
        }
    }

    public void process() throws IOException {
        beginTransaction();

        SymbolASTTable symtab = new SymbolASTTable();
        Map<SymbolAST, Integer> sourcetrailNames = new IdentityHashMap<>();

        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:**/*.tl");
        Path startDir = Paths.get(this.sourceDirPath);
        Files.walk(startDir).filter(matcher::matches).forEach(file -> {
            System.out.println("Indexing " + file);
            try {
                processFile(file.toString(), symtab, sourcetrailNames);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        endTransaction();
    }

    public void beginTransaction() {

        System.out.println("SourcetrailDB Tiny Language Example");
        System.out.println("Supported database version: " + sourcetraildb.getSupportedDatabaseVersion());

        if (this.dBVersion > 0 && this.dBVersion != sourcetraildb.getSupportedDatabaseVersion()) {
            throw new RuntimeException("ERROR: Only supports database version: " + sourcetraildb.getSupportedDatabaseVersion() +
                    ". Requested version: " + this.dBVersion);
        }

        try {
            sourcetraildb.open(this.dBFilePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Clearing loaded database now...");
        sourcetraildb.clear();

        System.out.println("start indexing");
        sourcetraildb.beginTransaction();

        if (sourcetraildb.getLastError().length() > 0) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }
    }

    public void processFile(String file, SymbolASTTable symtab, Map<SymbolAST, Integer> sourcetrailNames) throws IOException {

        TinySymbolSolver symbolSolver = new TinySymbolSolver();
        Register r = new Register(sourcetrailNames);

        DefPhase defPhase = new DefPhase(symbolSolver);
        FileAST fileAST = defPhase.parse(file, symtab, r);

        RefPhase refPhase = new RefPhase(symbolSolver);
        refPhase.walk(fileAST, symtab, r);
    }


    private void endTransaction() {
        sourcetraildb.commitTransaction();

        if (sourcetraildb.getLastError().length() > 0) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }

        if (!sourcetraildb.close()) {
            throw new RuntimeException(sourcetraildb.getLastError());
        }

        System.out.println("done");
    }

    public static class Register implements DefPhaseAdapter, RefPhaseAdapter {

        Map<SymbolAST, Integer> sourcetrailNames;
        public Register(Map<SymbolAST, Integer> sourcetrailNames) {
            this.sourcetrailNames = sourcetrailNames;
        }

        @Override
        public void defined(SymbolAST ast) {
            int id;
            if (ast instanceof FileAST) {
                id = defFile((FileSymbol)ast.symbol, ast);
            }
            else if (ast.symbol instanceof FunctionSymbol) {
                id = defFunction(ast);
            } else {
                throw new IllegalStateException();
            }
            sourcetrailNames.put(ast, id);
        }

        public int defFile(@NotNull FileSymbol f, SymbolAST ast){
            // sourcetrail record file
            int fileId = sourcetraildb.recordFile(f.getName());
            sourcetraildb.recordFileLanguage(fileId, "");
            if (sourcetraildb.getLastError().length() > 0) {
                throw new RuntimeException(sourcetraildb.getLastError());
            }
            return fileId;
        }

        public int defFunction(@NotNull SymbolAST ast){
            int fileId = sourcetrailNames.get(ast.getFile());
            TLParser.FunctionDeclContext ctx = (TLParser.FunctionDeclContext) ast.ctx;
            String s = getSignature(ast);
            int symbolId = sourcetraildb.recordSymbol(s);
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
            return symbolId;
        }

        private static String getSignature(SymbolAST symbolAST) {
            NameHierarchy nameHierarchy = new NameHierarchy(".");
            createNameHierarchy(symbolAST, nameHierarchy);
            return nameHierarchy.serialize();
        }

        private static void createNameHierarchy(SymbolAST symbolAST, NameHierarchy nameHierarchy) {
             if (symbolAST == null) {
                 throw new IllegalStateException("FileSymbol not found.");
             } else if (! (symbolAST instanceof FileAST)) {
                 SymbolAST parent = (SymbolAST) symbolAST.getParent();
                 createNameHierarchy(parent, nameHierarchy);
                 NameElement nameElement = new NameElement();
                 nameElement.name = symbolAST.symbol.getName();
                 nameHierarchy.push(nameElement);
             }
        }

        @Override
        public void refVisited(ParserRuleContext prc, SymbolAST fromAst, SymbolAST toAst) {
            if (toAst.symbol instanceof FunctionSymbol) {
                functionRef(prc, fromAst, toAst);
            }
        }

        public void functionRef(ParserRuleContext prc, SymbolAST fromAst, SymbolAST toAst) {
            TLParser.IdentifierFunctionCallContext ctx = (TLParser.IdentifierFunctionCallContext) prc;
            int contextSymbolId = sourcetrailNames.get(fromAst);
            int toId = sourcetrailNames.get(toAst);

            int referenceId = sourcetraildb.recordReference(
                    contextSymbolId,
                    toId,
                    ReferenceKind.REFERENCE_CALL
            );

            int fileId = sourcetrailNames.get(fromAst.getFile());
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
}
