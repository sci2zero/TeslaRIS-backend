package rs.teslaris.importer.utility.bibtex;

import org.jbibtex.StringValue;

public class DummyBibTexValue extends StringValue {


    public DummyBibTexValue() {
        super("", Style.BRACED);
    }

    @Override
    protected String format() {
        return "";
    }

    @Override
    public String toUserString() {
        return "";
    }
}
