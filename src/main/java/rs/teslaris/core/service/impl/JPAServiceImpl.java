package rs.teslaris.core.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.service.JPAService;

public abstract class JPAServiceImpl<T extends BaseEntity> extends CRUDServiceImpl<T> implements
    JPAService<T> {

    @Override
    @Transactional(readOnly = true)
    public Iterable<T> findAll(Sort sorter) {
        return getEntityRepository().findAllByDeletedIsFalse(sorter);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable page) {
        return getEntityRepository().findAllByDeletedIsFalse(page);
    }
}