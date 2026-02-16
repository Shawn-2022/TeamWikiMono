package com.wiki.monowiki.wiki.repository;

import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleTag;
import com.wiki.monowiki.wiki.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {

    boolean existsByArticleAndTag(Article article, Tag tag);

    void deleteByArticleAndTag(Article article, Tag tag);

    List<ArticleTag> findByArticle(Article article);
}
