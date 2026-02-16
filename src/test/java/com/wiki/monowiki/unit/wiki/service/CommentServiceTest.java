package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.CommentDtos.CreateCommentRequest;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.wiki.repo.VersionCommentRepository;
import com.wiki.monowiki.wiki.service.CommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private ArticleRepository articles;
    @Mock private ArticleVersionRepository versions;
    @Mock private VersionCommentRepository comments;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private CommentService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void create_trims_body_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).status(ArticleStatus.PUBLISHED).build();

	when(articles.findById(10L)).thenReturn(Optional.of(a));
	when(versions.findByArticleAndVersionNo(a, 2))
		.thenReturn(Optional.of(ArticleVersion.builder().id(100L).article(a).versionNo(2).content("x").build()));
	when(comments.save(any(VersionComment.class))).thenAnswer(inv -> {
	    VersionComment c = inv.getArgument(0);
	    c.setId(1000L);
	    return c;
	});

	var res = service.create(10L, 2, new CreateCommentRequest("  LGTM  "));

	assertThat(res.id()).isEqualTo(1000L);
	assertThat(res.articleId()).isEqualTo(10L);
	assertThat(res.versionNo()).isEqualTo(2);
	assertThat(res.body()).isEqualTo("LGTM");

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.COMMENT_ADDED);
	assertThat(captor.getValue().publicEvent()).isTrue();
    }

    @Test
    void viewer_cannot_access_comments_for_non_published_article() {
	TestAuth.setAuth("viewer1", "VIEWER");

	Article a = Article.builder().id(10L).status(ArticleStatus.DRAFT).build();
	when(articles.findById(10L)).thenReturn(Optional.of(a));

	assertThatThrownBy(() -> service.list(10L, 1, PageRequest.of(0, 10)))
		.isInstanceOf(CommentService.NotFoundException.class);
    }
}
