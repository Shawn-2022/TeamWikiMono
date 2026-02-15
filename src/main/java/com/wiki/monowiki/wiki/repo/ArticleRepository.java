package com.wiki.monowiki.wiki.repo;

import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.Space;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findBySpace(Space space, Pageable pageable);

    Page<Article> findBySpaceAndStatus(Space space, ArticleStatus status, Pageable pageable);

    Optional<Article> findBySpaceAndSlug(Space space, String slug);

    boolean existsBySpaceAndSlug(Space space, String slug);


    @Query("""
    select a from Article a
    join ArticleVersion v
      on v.article = a and v.versionNo = a.currentVersionNo
    where a.space.spaceKey = :spaceKey
      and (lower(a.title) like lower(concat('%', :q, '%'))
           or lower(v.content) like lower(concat('%', :q, '%')))
""")
    Page<Article> searchLatestInSpace(@Param("spaceKey") String spaceKey,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
    select a from Article a
    join ArticleVersion v
      on v.article = a and v.versionNo = a.currentVersionNo
    where a.space.spaceKey = :spaceKey
      and a.status = com.wiki.monowiki.wiki.model.ArticleStatus.PUBLISHED
      and (lower(a.title) like lower(concat('%', :q, '%'))
           or lower(v.content) like lower(concat('%', :q, '%')))
""")
    Page<Article> searchLatestPublishedInSpace(@Param("spaceKey") String spaceKey,
            @Param("q") String q,
            Pageable pageable);
}
