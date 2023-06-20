package rs.teslaris.core.service.impl;

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.service.CRUDService;

public abstract class CRUDServiceImpl<T extends BaseEntity> implements CRUDService<T> {

    protected abstract JpaRepository<T, Long> getEntityRepository();


    @PersistenceContext
    private EntityManager entityManager;

//    @Override
//    @Transactional(readOnly = true)
//    public List<T> findAll() {
//        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
//        CriteriaQuery<T> query = builder.createQuery(BaseEntity.class);
//        Root<BaseEntity> root = query.from(BaseEntity.class);
//
//
//        query.where(builder.isFalse(root.get("deleted")));
//
//        return entityManager.createQuery(query).getResultList();
//
//    }

    @Override
    public T findOne(Long id) {
        return null;
    }

    @Override
    public T save(T entity) {
        return null;
    }

    @Override
    public T update(T entity) {
        return null;
    }

    @Override
    public List<T> saveAll(Collection<T> entities) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }
}
