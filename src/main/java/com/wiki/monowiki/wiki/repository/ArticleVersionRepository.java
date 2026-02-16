package com.wiki.monowiki.wiki.repository;

import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Long> {

    Page<ArticleVersion> findByArticle(Article article, Pageable pageable);

    Optional<ArticleVersion> findByArticleAndVersionNo(Article article, Integer versionNo);

    Optional<ArticleVersion> findTopByArticleOrderByVersionNoDesc(Article article);
}
