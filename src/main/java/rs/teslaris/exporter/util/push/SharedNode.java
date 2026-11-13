package rs.teslaris.exporter.util.push;

import org.apache.jena.rdf.model.Model;

/**
 * A node that more than one entity can link to. The entity's own model contains only the
 * links to the node; the node's triples (type, label) are in this model.
 */
public record SharedNode(String uri, Model model) {
}
