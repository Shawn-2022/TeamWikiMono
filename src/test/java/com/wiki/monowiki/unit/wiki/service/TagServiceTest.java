package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.TagDtos.CreateTagRequest;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleTagRepository;
import com.wiki.monowiki.wiki.repo.TagRepository;
import com.wiki.monowiki.wiki.service.TagService;
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
class TagServiceTest {

    @Mock private TagRepository tags;
    @Mock private ArticleRepository articles;
    @Mock private ArticleTagRepository articleTags;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private TagService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void create_trims_name_and_saves() {
	when(tags.existsByNameIgnoreCase("howto")).thenReturn(false);
	when(tags.save(any(Tag.class))).thenAnswer(inv -> {
	    Tag t = inv.getArgument(0);
	    t.setId(1L);
	    return t;
	});

	var res = service.create(new CreateTagRequest(" howto "));

	assertThat(res.id()).isEqualTo(1L);
	assertThat(res.name()).isEqualTo("howto");
    }

    @Test
    void addTagToArticle_saves_relation_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).status(ArticleStatus.PUBLISHED).build();
	Tag t = Tag.builder().id(2L).name("howto").build();

	when(articles.findById(10L)).thenReturn(Optional.of(a));
	when(tags.findById(2L)).thenReturn(Optional.of(t));
	when(articleTags.existsByArticleAndTag(a, t)).thenReturn(false);

	service.addTagToArticle(10L, 2L);

	verify(articleTags).save(any(ArticleTag.class));

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.TAG_ADDED_TO_ARTICLE);
	assertThat(captor.getValue().publicEvent()).isTrue();
    }

    @Test
    void removeTagFromArticle_deletes_relation_and_emits_audit_event() {
	TestAuth.setAuth("editor1", "EDITOR");

	Space space = Space.builder().id(1L).spaceKey("ENG").name("Engineering").build();
	Article a = Article.builder().id(10L).space(space).status(ArticleStatus.DRAFT).build();
	Tag t = Tag.builder().id(2L).name("howto").build();

	when(articles.findById(10L)).thenReturn(Optional.of(a));
	when(tags.findById(2L)).thenReturn(Optional.of(t));

	service.removeTagFromArticle(10L, 2L);

	verify(articleTags).deleteByArticleAndTag(a, t);

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());
	assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.TAG_REMOVED_FROM_ARTICLE);
	assertThat(captor.getValue().publicEvent()).isFalse();
    }
}
