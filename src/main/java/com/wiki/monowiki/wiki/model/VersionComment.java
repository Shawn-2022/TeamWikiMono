package com.wiki.monowiki.wiki.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "version_comments", indexes = {
	@Index(name = "idx_comment_article_version", columnList = "article_id, version_no")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionComment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

