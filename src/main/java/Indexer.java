import com.sourcetrail.sourcetraildb;
import org.antlr.symtab.GlobalScope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.*;
import java.util.IdentityHashMap;
import java.util.Map;

public class Indexer {
    private int dBVersion;
    private String dBFilePath;
    private String sourceDirPath;

    public static void main(String[] args) throws Exception {
        Indexer indexer = new Indexer();
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

        Map<Symbol, Integer> sourcetrailNames = new IdentityHashMap<>();
        GlobalScope globals = new GlobalScope(null);

        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:**/*.tl");
        Path startDir = Paths.get(this.sourceDirPath);
        Files.walk(startDir).filter(matcher::matches).forEach(file -> {
            System.out.println("Indexing " + file);
            try {
                processFile(globals, sourcetrailNames, file.toString());
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

    public void processFile(GlobalScope globals, Map<Symbol, Integer> sourcetrailNames, String file) throws IOException {

        // parse
        TLLexer lexer = new TLLexer(CharStreams.fromPath(Paths.get(file)));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TLParser parser = new TLParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.parse();
        // show tree in text formÒ
        // System.out.println(tree.toStringTree(parser));

        // create symbols
        ParseTreeWalker walker = new ParseTreeWalker();
        DefPhase def = new DefPhase(globals, sourcetrailNames, file);
        walker.walk(def, tree);

        // create references
        RefPhase ref = new RefPhase(def);
        walker.walk(ref, tree);

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
}
