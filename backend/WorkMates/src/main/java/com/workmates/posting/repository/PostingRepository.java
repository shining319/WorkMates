package com.workmates.posting.repository;

import com.workmates.posting.entity.Posting;
import jakarta.persistence.LockModeType;
import com.workmates.posting.entity.PostingStatus;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, Long>, JpaSpecificationExecutor<Posting> {

    /** Only jobs belonging to the authenticated employer, newest first through the supplied PageRequest. */
    Page<Posting> findByEmployerIdAndStatus(Long employerId, PostingStatus status, Pageable pageable);

    /** Separate fetches avoid the cartesian product of simultaneously fetching two collections. */
    @Query("select distinct p from Posting p left join fetch p.tags where p.id in :ids")
    List<Posting> fetchTagsByIds(@Param("ids") Collection<Long> ids);

    @Query("select distinct p from Posting p left join fetch p.timeSlots where p.id in :ids")
    List<Posting> fetchSlotsByIds(@Param("ids") Collection<Long> ids);


    /** {@code SELECT ... FOR UPDATE}: holds the posting row until the transaction ends (architecture section 8.3). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Posting p where p.id = :id")
    Optional<Posting> findByIdForUpdate(@Param("id") Long id);
}
