package com.workmates.application.repository;

import com.workmates.application.entity.ApplicationStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    /** The application's history, oldest first. The ID breaks ties between steps saved in the same instant. */
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAscIdAsc(Long applicationId);
}
