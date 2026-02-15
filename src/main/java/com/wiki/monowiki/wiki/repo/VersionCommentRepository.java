package com.wiki.monowiki.wiki.repo;

import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.VersionComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionCommentRepository extends JpaRepository<VersionComment, Long> {

    Page<VersionComment> findByArticleAndVersionNo(Article article, Integer versionNo, Pageable pageable);

    long countByArticleAndVersionNo(Article article, Integer versionNo);
}
