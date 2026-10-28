package com.workmates.posting.repository;

import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    /** One page of an employer's postings in one status. The order comes from {@code pageable}. */
    Page<Posting> findByEmployerIdAndStatus(Long employerId, PostingStatus status, Pageable pageable);

    /** {@code SELECT ... FOR UPDATE}: holds the posting row until the transaction ends (architecture section 8.3). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Posting p where p.id = :id")
    Optional<Posting> findByIdForUpdate(@Param("id") Long id);
}
