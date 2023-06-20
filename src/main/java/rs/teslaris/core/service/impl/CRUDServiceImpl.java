package rs.teslaris.core.service.impl;

import java.util.Collection;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.repository.CRUDRepository;
import rs.teslaris.core.service.CRUDService;

public abstract class CRUDServiceImpl<T extends BaseEntity> implements CRUDService<T> {

    protected abstract CRUDRepository<T> getEntityRepository();

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return getEntityRepository().findAllByDeletedIsFalse();
    }

    @Override
    public T findOne(Integer id) {
        return getEntityRepository().findByIdAndDeletedIsFalse(id)
            .orElseThrow(() -> new NotFoundException("Cannot find entity with id: " + id));
    }

    @Override
    public T save(T entity) {
        return getEntityRepository().save(entity);
    }

    @Override
    public T update(T entity) {
        return this.save(entity);
    }

    @Override
    public List<T> saveAll(Collection<T> entities) {
        return getEntityRepository().saveAll(entities);
    }

    @Override
    public void delete(Integer id) {
        getEntityRepository().delete(this.findOne(id));
    }
}
