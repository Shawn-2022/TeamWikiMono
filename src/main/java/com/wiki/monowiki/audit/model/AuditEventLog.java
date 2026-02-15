package com.wiki.monowiki.audit.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "audit_event_log",
        indexes = {
                @Index(name = "idx_audit_created_at", columnList = "createdAt"),
                @Index(name = "idx_audit_space_created", columnList = "spaceKey, createdAt"),
                @Index(name = "idx_audit_article_created", columnList = "articleId, createdAt"),
                @Index(name = "idx_audit_actor_created", columnList = "actor, createdAt"),
                @Index(name = "idx_audit_actor_id_created", columnList = "actor_id, createdAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * PostgreSQL ENUM: audit_event_type
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_event_type")
    private AuditEventType eventType;

    /**
     * PostgreSQL ENUM: audit_entity_type
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_entity_type")
    private AuditEntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(length = 64)
    private String spaceKey;

    private Long articleId;

    /**
     * Optional user id resolved from auth/users table (in monolith).
     * Column added via migration: "7 - audit add actor id"
     */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_public", nullable = false)
    private boolean publicEvent;

    @Column(columnDefinition = "text")
    private String metaJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
