package rs.teslaris.exporter.util.push;

import java.util.List;
import org.apache.jena.rdf.model.Model;

/**
 * @param model       triples that only this entity owns.
 * @param sharedNodes nodes that this entity links to, but does not own.
 * @param references  entities with their own push state that this entity links to.
 */
public record RdfMapping(Model model, List<SharedNode> sharedNodes,
                         List<EntityReference> references) {

    public RdfMapping(Model model, List<SharedNode> sharedNodes) {
        this(model, sharedNodes, List.of());
    }
}
