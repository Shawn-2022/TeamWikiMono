package com.wiki.monowiki.unit.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventLog;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.repo.AuditEventLogRepository;
import com.wiki.monowiki.audit.service.ActorIdResolver;
import com.wiki.monowiki.audit.service.AuditEventListener;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditEventLogRepository repo;

    @Mock
    private ActorIdResolver actorIdResolver;

    private AuditEventListener listener;

    @BeforeEach
    void setUp() {
	listener = new AuditEventListener(repo, new ObjectMapper(), actorIdResolver);
    }

    @Test
    void onEvent_persists_audit_log_and_resolves_actor_id() {
	when(actorIdResolver.resolve("editor1")).thenReturn(42L);
	when(repo.save(any(AuditEventLog.class))).thenAnswer(inv -> inv.getArgument(0));

	WikiAuditEvent e = new WikiAuditEvent(
		AuditEventType.ARTICLE_CREATED,
		AuditEntityType.ARTICLE,
		10L,
		"ENG",
		10L,
		"editor1",
		"Created article",
		false,
		Map.of("k", "v")
	);

	listener.onEvent(e);

	ArgumentCaptor<AuditEventLog> captor = ArgumentCaptor.forClass(AuditEventLog.class);
	verify(repo).save(captor.capture());

	AuditEventLog saved = captor.getValue();
	assertThat(saved.getEventType()).isEqualTo(AuditEventType.ARTICLE_CREATED);
	assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.ARTICLE);
	assertThat(saved.getEntityId()).isEqualTo(10L);
	assertThat(saved.getSpaceKey()).isEqualTo("ENG");
	assertThat(saved.getArticleId()).isEqualTo(10L);
	assertThat(saved.getActor()).isEqualTo("editor1");
	assertThat(saved.getActorId()).isEqualTo(42L);
	assertThat(saved.isPublicEvent()).isFalse();
	assertThat(saved.getMetaJson()).contains("\"k\"");
    }

    @Test
    void onEvent_when_meta_not_serializable_does_not_throw() {
	when(actorIdResolver.resolve(any())).thenReturn(null);
	when(repo.save(any(AuditEventLog.class))).thenAnswer(inv -> inv.getArgument(0));

	Object self = new Object();

	WikiAuditEvent e = new WikiAuditEvent(
		AuditEventType.SPACE_CREATED,
		AuditEntityType.SPACE,
		1L,
		"ENG",
		null,
		"admin1",
		"Created space",
		true,
		Map.of("bad", self)
	);

	assertThatCode(() -> listener.onEvent(e)).doesNotThrowAnyException();
	verify(repo).save(any(AuditEventLog.class));
    }
}
