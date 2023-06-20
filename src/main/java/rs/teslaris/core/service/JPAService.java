package rs.teslaris.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.model.commontypes.BaseEntity;

public interface JPAService<T extends BaseEntity> extends CRUDService<T>{
    Page<T> findAll(Pageable page);
}
