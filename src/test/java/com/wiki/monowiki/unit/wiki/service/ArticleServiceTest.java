package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.ArticleDtos.CreateArticleRequest;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import com.wiki.monowiki.wiki.model.Space;
import com.wiki.monowiki.wiki.repo.*;
import com.wiki.monowiki.wiki.service.ArticleService;
import com.wiki.monowiki.wiki.service.SlugUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock private ArticleRepository articles;
    @Mock private SpaceRepository spaces;
    @Mock private ArticleVersionRepository versions;
    @Mock private ArticleTagRepository articleTags;
    @Mock private VersionCommentRepository comments;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private ArticleService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void create_creates_article_v1_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	when(spaces.findBySpaceKey("ENG")).thenReturn(Optional.of(space));
	when(articles.existsBySpaceAndSlug(eq(space), anyString())).thenReturn(false);
	when(articles.save(any(Article.class))).thenAnswer(inv -> {
	    Article a = inv.getArgument(0);
	    a.setId(10L);
	    return a;
	});
	when(versions.save(any(ArticleVersion.class))).thenAnswer(inv -> {
	    ArticleVersion v = inv.getArgument(0);
	    v.setId(100L);
	    return v;
	});
	when(articleTags.findByArticle(any())).thenReturn(List.of());
	when(comments.countByArticleAndVersionNo(any(), anyInt())).thenReturn(0L);

	var req = new CreateArticleRequest("Onboarding Guide", "v1 content");
	var res = service.create("ENG", req);

	assertThat(res.id()).isEqualTo(10L);
	assertThat(res.spaceKey()).isEqualTo("ENG");
	assertThat(res.title()).isEqualTo("Onboarding Guide");
	assertThat(res.status()).isEqualTo(ArticleStatus.DRAFT);
	assertThat(res.currentVersionNo()).isEqualTo(1);
	assertThat(res.latestContent()).isEqualTo("v1 content");
	assertThat(res.slug()).isEqualTo(SlugUtil.slugify("Onboarding Guide"));
	assertThat(res.tags()).isEmpty();
	assertThat(res.currentVersionCommentCount()).isZero();

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.ARTICLE_CREATED);
	assertThat(captor.getValue().publicEvent()).isFalse();
    }

    @Test
    void getBySlug_viewer_cannot_access_draft_article() {
	TestAuth.setAuth("viewer1", "VIEWER");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	when(spaces.findBySpaceKey("ENG")).thenReturn(Optional.of(space));

	Article draft = Article.builder()
		.id(10L)
		.space(space)
		.title("Draft")
		.slug("draft")
		.status(ArticleStatus.DRAFT)
		.currentVersionNo(1)
		.build();
	when(articles.findBySpaceAndSlug(space, "draft")).thenReturn(Optional.of(draft));

	assertThatThrownBy(() -> service.getBySlug("ENG", "draft"))
		.isInstanceOf(ArticleService.NotFoundException.class);
    }

    @Test
    void list_for_viewer_only_queries_published() {
	TestAuth.setAuth("viewer1", "VIEWER");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	when(spaces.findBySpaceKey("ENG")).thenReturn(Optional.of(space));

	Article published = Article.builder()
		.id(10L)
		.space(space)
		.title("Published")
		.slug("published")
		.status(ArticleStatus.PUBLISHED)
		.currentVersionNo(1)
		.build();

	when(articles.findBySpaceAndStatus(eq(space), eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
		.thenReturn(new PageImpl<>(List.of(published)));

	when(versions.findByArticleAndVersionNo(published, 1))
		.thenReturn(Optional.of(ArticleVersion.builder().id(100L).article(published).versionNo(1).content("latest").build()));
	when(articleTags.findByArticle(published)).thenReturn(List.of());
	when(comments.countByArticleAndVersionNo(published, 1)).thenReturn(0L);

	Page<?> page = service.list("ENG", false,PageRequest.of(0, 10));
	assertThat(page.getTotalElements()).isEqualTo(1);

	verify(articles).findBySpaceAndStatus(eq(space), eq(ArticleStatus.PUBLISHED), any(Pageable.class));
	verify(articles, never()).findBySpace(eq(space), any(Pageable.class));
    }
}
