package tl.sourcetrail;

import com.sourcetrail.DefinitionKind;
import com.sourcetrail.SymbolKind;
import com.sourcetrail.sourcetraildb;
import common.DefPhaseAdapter;
import common.FileSymbol;
import common.SymbolAST;
import common.SymbolTable;
import org.antlr.symtab.FunctionSymbol;
import org.antlr.v4.runtime.Token;
import sourcetrail.NameElement;
import sourcetrail.NameHierarchy;
import tl.TinyDefPhase;
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

        SymbolTable symtab = new SymbolTable();

        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:**/*.tl");
        Path startDir = Paths.get(this.sourceDirPath);
        Files.walk(startDir).filter(matcher::matches).forEach(file -> {
            System.out.println("Indexing " + file);
            try {
                processFile(file.toString(), symtab);
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

    public void processFile(String file, SymbolTable symtab) throws IOException {
        // parse
        TinyDefPhase defPhase = new TinyDefPhase(symtab);
        defPhase.setAdapter(new TLDefListener());
        defPhase.parse(file);
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

    public static class TLDefListener implements DefPhaseAdapter {

        Map<SymbolAST, Integer> sourcetrailNames = new IdentityHashMap<>();

        public void defined(SymbolAST ast) {
            if (ast.symbol instanceof FileSymbol) {
                defFile((FileSymbol)ast.symbol, ast);
            }
            else if (ast.symbol instanceof FunctionSymbol) {
                defFunction((FunctionSymbol)ast.symbol, ast);
            }
        }

        public void defFile(FileSymbol f, SymbolAST ast){
            // sourcetrail record file
            int fileId = sourcetraildb.recordFile(f.getName());
            sourcetraildb.recordFileLanguage(fileId, "");
            if (sourcetraildb.getLastError().length() > 0) {
                throw new RuntimeException(sourcetraildb.getLastError());
            }
            sourcetrailNames.put(ast, fileId);
        }

        public void defFunction(FunctionSymbol f, SymbolAST ast){
            int fileId = sourcetrailNames.get(ast.getFileSymbolAST());
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
        }

        private static String getSignature(SymbolAST symbolAST) {
            NameHierarchy nameHierarchy = new NameHierarchy(".");
            createNameHierarchy(symbolAST, nameHierarchy);
            return nameHierarchy.serialize();
        }

        private static void createNameHierarchy(SymbolAST symbolAST, NameHierarchy nameHierarchy) {
             if (symbolAST == null) {
                 throw new IllegalStateException("FileSymbol not found.");
             } else if (symbolAST.symbol instanceof FileSymbol) {
                 // nothing
             } else {
                 SymbolAST parent = (SymbolAST) symbolAST.getParent();
                 createNameHierarchy(parent, nameHierarchy);
                 NameElement nameElement = new NameElement();
                 nameElement.name = symbolAST.symbol.getName();
                 nameHierarchy.push(nameElement);
             }
        }
    }
}
