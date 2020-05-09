package common;

import java.util.IdentityHashMap;
import java.util.Map;

public class SymbolTable extends org.antlr.symtab.SymbolTable {
    Map<String, SymbolAST> fileAST = new IdentityHashMap<>();
}
