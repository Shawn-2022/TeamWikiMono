package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.ReviewDtos.RejectRequest;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repository.ArticleRepository;
import com.wiki.monowiki.wiki.repository.ReviewRequestRepository;
import com.wiki.monowiki.wiki.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ArticleRepository articles;
    @Mock private ReviewRequestRepository reviews;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private ReviewService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void submit_moves_article_to_in_review_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).slug("onboarding").status(ArticleStatus.DRAFT).build();

	when(articles.findById(10L)).thenReturn(Optional.of(a));
	when(reviews.existsByArticleIdAndStatus(10L, ReviewStatus.PENDING)).thenReturn(false);
	when(reviews.save(any(ReviewRequest.class))).thenAnswer(inv -> {
	    ReviewRequest rr = inv.getArgument(0);
	    rr.setId(100L);
	    return rr;
	});

	var res = service.submit(10L);

	assertThat(a.getStatus()).isEqualTo(ArticleStatus.IN_REVIEW);
	assertThat(res.id()).isEqualTo(100L);
	assertThat(res.articleId()).isEqualTo(10L);
	assertThat(res.status()).isEqualTo(ReviewStatus.PENDING);

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.REVIEW_SUBMITTED);
	assertThat(captor.getValue().publicEvent()).isFalse();
    }

    @Test
    void approve_publishes_article_and_emits_public_audit_event() {
	TestAuth.setAuth("admin1", "ADMIN");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).slug("onboarding").status(ArticleStatus.IN_REVIEW).build();

	ReviewRequest rr = ReviewRequest.builder()
		.id(200L)
		.article(a)
		.status(ReviewStatus.PENDING)
		.requestedBy("editor1")
		.build();

	when(reviews.findById(200L)).thenReturn(Optional.of(rr));

	var res = service.approve(200L);

	assertThat(rr.getStatus()).isEqualTo(ReviewStatus.APPROVED);
	assertThat(a.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
	assertThat(res.status()).isEqualTo(ReviewStatus.APPROVED);

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.REVIEW_APPROVED);
	assertThat(captor.getValue().publicEvent()).isTrue();
    }

    @Test
    void reject_moves_article_back_to_draft_and_sets_reason() {
	TestAuth.setAuth("admin1", "ADMIN");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).slug("onboarding").status(ArticleStatus.IN_REVIEW).build();

	ReviewRequest rr = ReviewRequest.builder()
		.id(300L)
		.article(a)
		.status(ReviewStatus.PENDING)
		.requestedBy("editor1")
		.build();

	when(reviews.findById(300L)).thenReturn(Optional.of(rr));

	var res = service.reject(300L, new RejectRequest(" Needs more details "));

	assertThat(rr.getStatus()).isEqualTo(ReviewStatus.REJECTED);
	assertThat(rr.getReason()).isEqualTo("Needs more details");
	assertThat(a.getStatus()).isEqualTo(ArticleStatus.DRAFT);
	assertThat(res.status()).isEqualTo(ReviewStatus.REJECTED);

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.REVIEW_REJECTED);
	assertThat(captor.getValue().publicEvent()).isFalse();
    }
}
