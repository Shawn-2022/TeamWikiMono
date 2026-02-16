package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.VersionDtos.CreateVersionRequest;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.wiki.service.VersionService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock private ArticleRepository articles;
    @Mock private ArticleVersionRepository versions;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private VersionService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void create_adds_next_version_no_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).status(ArticleStatus.DRAFT).build();

	when(articles.findById(10L)).thenReturn(Optional.of(a));
	when(versions.findTopByArticleOrderByVersionNoDesc(a))
		.thenReturn(Optional.of(ArticleVersion.builder().versionNo(1).build()));
	when(versions.save(any(ArticleVersion.class))).thenAnswer(inv -> {
	    ArticleVersion v = inv.getArgument(0);
	    v.setId(100L);
	    return v;
	});

	var res = service.create(10L, new CreateVersionRequest("v2 content"));

	assertThat(res.articleId()).isEqualTo(10L);
	assertThat(res.versionNo()).isEqualTo(2);
	assertThat(res.content()).isEqualTo("v2 content");
	assertThat(a.getCurrentVersionNo()).isEqualTo(2);

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.VERSION_ADDED);
	assertThat(captor.getValue().publicEvent()).isFalse();
    }

    @Test
    void create_when_article_not_draft_throws() {
	Article a = Article.builder().id(10L).status(ArticleStatus.PUBLISHED).build();
	when(articles.findById(10L)).thenReturn(Optional.of(a));

	CreateVersionRequest request = new CreateVersionRequest("x");

	assertThatThrownBy(() -> service.create(10L, request))
		.isInstanceOf(IllegalArgumentException.class)
		.hasMessageContaining("DRAFT");
    }

    @Test
    void list_for_viewer_on_non_published_article_throws_notFound() {
	TestAuth.setAuth("viewer1", "VIEWER");

	Article a = Article.builder().id(10L).status(ArticleStatus.DRAFT).build();
	when(articles.findById(10L)).thenReturn(Optional.of(a));

	assertThatThrownBy(() -> service.list(10L, PageRequest.of(0, 10)))
		.isInstanceOf(VersionService.NotFoundException.class);
    }
}
