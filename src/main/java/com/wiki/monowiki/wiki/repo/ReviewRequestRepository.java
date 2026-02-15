package com.wiki.monowiki.wiki.repo;

import com.wiki.monowiki.wiki.model.ReviewRequest;
import com.wiki.monowiki.wiki.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

    Page<ReviewRequest> findByStatus(ReviewStatus status, Pageable pageable);

    boolean existsByArticleIdAndStatus(Long articleId, ReviewStatus status);
}