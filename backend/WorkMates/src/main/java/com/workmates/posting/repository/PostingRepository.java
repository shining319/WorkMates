package com.workmates.posting.repository;

import com.workmates.posting.entity.Posting;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    /** {@code SELECT ... FOR UPDATE}: holds the posting row until the transaction ends (architecture section 8.3). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Posting p where p.id = :id")
    Optional<Posting> findByIdForUpdate(@Param("id") Long id);
}
