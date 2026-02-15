package com.wiki.monowiki.audit.repo;

import com.wiki.monowiki.audit.model.AuditEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventLogRepository extends JpaRepository<AuditEventLog, Long>,
        JpaSpecificationExecutor<AuditEventLog> {
}
