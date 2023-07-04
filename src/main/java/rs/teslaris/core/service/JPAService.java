package rs.teslaris.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import rs.teslaris.core.model.commontypes.BaseEntity;

public interface JPAService<T extends BaseEntity> extends CRUDService<T> {
    Iterable<T> findAll(Sort sorter);

    Page<T> findAll(Pageable page);

}
