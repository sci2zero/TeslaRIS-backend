package rs.teslaris.core.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import rs.teslaris.core.model.commontypes.BaseEntity;

@NoRepositoryBean
public interface JPASoftDeleteRepository<T extends BaseEntity> extends JpaRepository<T, Integer> {
    List<T> findAllByDeletedIsFalse();

    List<T> findAllByDeletedIsFalse(Sort sort);

    Page<T> findAllByDeletedIsFalse(Pageable pageable);

    List<T> findAllByDeletedIsTrue();

    Optional<T> findByIdAndDeletedIsFalse(Integer integer);


}
