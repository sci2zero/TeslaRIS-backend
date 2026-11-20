package rs.teslaris.exporter.util.push;

import org.apache.jena.rdf.model.Model;

/**
 * A node that more than one entity can link to. The entity's own model contains only the
 * links to the node; the node's triples (type, label) are in this model.
 *
 * @param replaceWhenChanged true for a node made from a TeslaRIS entity (for example a
 *                           publisher), so a changed name is sent. False for a node made from a
 *                           free-text name: there the first entity wins, because names that
 *                           differ only in case or spaces have the same node.
 */
public record SharedNode(String uri, Model model, boolean replaceWhenChanged) {

    public SharedNode(String uri, Model model) {
        this(uri, model, false);
    }
}
