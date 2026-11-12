package com.workmates.application.repository;

import com.workmates.application.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reading the history for the application detail page is added with US 4.2 / 4.6. */
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
}
