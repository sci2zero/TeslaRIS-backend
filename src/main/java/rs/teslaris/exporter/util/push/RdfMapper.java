package rs.teslaris.exporter.util.push;

import org.apache.jena.rdf.model.Model;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

/**
 * Use only URI nodes, because DELETE DATA cannot remove blank nodes.
 */
public interface RdfMapper<T extends BaseExportEntity> {

    String profile();

    Class<T> sourceType();

    Model toRdf(T snapshot, PushTarget target);
}
