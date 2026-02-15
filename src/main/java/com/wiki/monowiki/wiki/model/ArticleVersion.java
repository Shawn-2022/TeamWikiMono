package com.wiki.monowiki.wiki.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "article_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_article_versionno", columnNames = {"article_id", "version_no"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
