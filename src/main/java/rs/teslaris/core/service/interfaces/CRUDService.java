package rs.teslaris.core.service.interfaces;

import java.util.Collection;
import java.util.List;

public interface CRUDService<T> extends ExistenceCheckable {
    List<T> findAll();

    T findOne(Integer id);

    T save(T entity);

    T update(T entity);

    List<T> saveAll(Collection<T> entities);

    void delete(Integer id);
}
