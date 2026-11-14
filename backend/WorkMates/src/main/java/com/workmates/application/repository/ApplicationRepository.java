package com.workmates.application.repository;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** Current application if a student has applied to this posting (US 3.4). */
    Optional<Application> findFirstByPostingIdAndStudentIdOrderByCreatedAtDescIdDesc(
            Long postingId, Long studentId);
  
    boolean existsByPostingIdAndStudentId(Long postingId, Long studentId);

    /** Only the posting ID, so no application entity is loaded (and cached) before the posting is locked. */
    @Query("select a.postingId from Application a where a.id = :id")
    Optional<Long> findPostingIdById(@Param("id") Long id);

    long countByPostingIdAndStatusIn(Long postingId, Collection<ApplicationStatus> statuses);

    List<Application> findByPostingIdAndStatusIn(Long postingId, Collection<ApplicationStatus> statuses);

    /** One row per posting that has applications. Postings without any are not returned. */
    @Query("""
            select new com.workmates.application.dto.PostingCounts(a.postingId,
                sum(case when a.status in (com.workmates.application.entity.ApplicationStatus.ACCEPTED,
                                           com.workmates.application.entity.ApplicationStatus.COMPLETED)
                         then 1L else 0L end),
                sum(case when a.status in (com.workmates.application.entity.ApplicationStatus.APPLIED,
                                           com.workmates.application.entity.ApplicationStatus.REVIEWED,
                                           com.workmates.application.entity.ApplicationStatus.INTERVIEW,
                                           com.workmates.application.entity.ApplicationStatus.OFFER)
                         then 1L else 0L end),
                sum(case when a.status = com.workmates.application.entity.ApplicationStatus.REJECTED
                          and a.statusReason = com.workmates.application.entity.ApplicationStatusReason.POSITION_FILLED
                         then 1L else 0L end))
            from Application a
            where a.postingId in :postingIds
            group by a.postingId
            """)
    List<PostingCounts> countByPostings(@Param("postingIds") Collection<Long> postingIds);
}
