package rs.teslaris.exporter.model.skgif;

import java.io.StringReader;
import java.io.StringWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;

@Slf4j
public class JsonLdToTurtleConverter {

    public static String convertJsonLdToTurtle(String jsonLdString) {
        var model = ModelFactory.createDefaultModel();

        try (var reader = new StringReader(jsonLdString)) {
            RDFParser.create()
                .source(reader)
                .lang(Lang.JSONLD)
                .errorHandler(ErrorHandlerFactory.errorHandlerIgnoreWarnings(log))
                .parse(model);

            try (var writer = new StringWriter()) {
                RDFDataMgr.write(writer, model, Lang.TTL);
                return writer.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON-LD to Turtle", e);
        }
    }
}
