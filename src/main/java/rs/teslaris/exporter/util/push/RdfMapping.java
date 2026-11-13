package rs.teslaris.exporter.util.push;

import java.util.List;
import org.apache.jena.rdf.model.Model;

/**
 * @param model       triples that only this entity owns.
 * @param sharedNodes nodes that this entity links to, but does not own.
 */
public record RdfMapping(Model model, List<SharedNode> sharedNodes) {
}
