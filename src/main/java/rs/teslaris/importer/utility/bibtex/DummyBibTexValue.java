package rs.teslaris.importer.utility.bibtex;

import org.jbibtex.StringValue;

public class DummyBibTexValue extends StringValue {

    private String value = "";

    public DummyBibTexValue() {
        super("", Style.BRACED);
    }

    public DummyBibTexValue(String value) {
        super("", Style.BRACED);
        this.value = value;
    }

    @Override
    protected String format() {
        return "";
    }

    @Override
    public String toUserString() {
        return value;
    }
}
