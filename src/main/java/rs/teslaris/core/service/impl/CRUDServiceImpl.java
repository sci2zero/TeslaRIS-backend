package rs.teslaris.core.service.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.service.CRUDService;

public abstract class CRUDServiceImpl<T extends BaseEntity> implements CRUDService<T> {

    protected abstract JPASoftDeleteRepository<T> getEntityRepository();

    private String printGenericTypeName() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    Class<?> typeClass = (Class<?>) typeArgument;
                    String typeName = typeClass.getSimpleName();
                    return typeName;
                }
            }
        }
        return getClass().getName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return getEntityRepository().findAllByDeletedIsFalse();
    }

    @Override
    public T findOne(Integer id) {
        return getEntityRepository().findByIdAndDeletedIsFalse(id)
            .orElseThrow(() -> {
                printGenericTypeName();
                return new NotFoundException("Cannot find entity " + printGenericTypeName() + " with id: " + id);
            });
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
        T entity = findOne(id);
        entity.setDeleted(true);
        this.save(entity);
    }
}
