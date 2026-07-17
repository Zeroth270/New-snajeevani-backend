package com.anvesha.core.repository;

import com.anvesha.core.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByRecipientUserId(Long userId, Pageable pageable);

    Page<Alert> findByRecipientUserIdAndIsRead(Long userId, boolean isRead, Pageable pageable);

    long countByRecipientUserIdAndIsRead(Long userId, boolean isRead);
}
