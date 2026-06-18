package rs.teslaris.revisioner.hydrator;

public interface RevisionHydrator<T> {

    String entityType();

    void hydrate(T dto);
}
