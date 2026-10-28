package com.workmates.application.repository;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.entity.Application;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

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
