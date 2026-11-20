package rs.teslaris.exporter.util.push;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class NTriplesSerializer {

    public static List<String> toLines(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getSubject().isAnon() || statement.getObject().isAnon()) {
                throw new IllegalStateException("Blank nodes are not allowed in pushed RDF.");
            }
        });

        var out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, Lang.NTRIPLES);

        return Arrays.stream(out.toString(StandardCharsets.UTF_8).split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .distinct()
            .sorted()
            .toList();
    }
}
