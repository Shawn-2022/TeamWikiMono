package com.wiki.monowiki.wiki.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_tags",
	uniqueConstraints = @UniqueConstraint(name = "uk_article_tag", columnNames = {"article_id", "tag_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleTag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}