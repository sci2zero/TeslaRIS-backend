package rs.teslaris.exporter.util.push;

import java.util.List;
import org.apache.jena.rdf.model.Model;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

/**
 * Use only URI nodes, because DELETE DATA cannot remove blank nodes.
 * <p>
 * Each triple must have one owner. The model can contain a triple about another entity only
 * when the triple also contains a URI of this entity. Put the type and label of a node that
 * many entities link to in a SharedNode.
 */
public interface RdfMapper<T extends BaseExportEntity> {

    String profile();

    Class<T> sourceType();

    Model toRdf(T snapshot, PushTarget target);

    default RdfMapping map(T snapshot, PushTarget target) {
        return new RdfMapping(toRdf(snapshot, target), List.of());
    }
}
