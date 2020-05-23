package common;

import org.antlr.symtab.BaseSymbol;

public class FileSymbol extends BaseSymbol {

    public FileSymbol(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
