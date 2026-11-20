package rs.teslaris.exporter.util.push;

import rs.teslaris.exporter.model.push.PushEntityType;

/**
 * An entity that the mapped entity links to, for example the journal of an article. It is used
 * to count links to entities that are not pushed yet.
 */
public record EntityReference(PushEntityType entityType, Integer databaseId) {
}
