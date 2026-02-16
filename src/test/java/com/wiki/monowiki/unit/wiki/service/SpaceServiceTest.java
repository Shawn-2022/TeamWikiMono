package com.wiki.monowiki.unit.wiki.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.unit.util.TestAuth;
import com.wiki.monowiki.wiki.dto.SpaceDtos.CreateSpaceRequest;
import com.wiki.monowiki.wiki.model.Space;
import com.wiki.monowiki.wiki.repository.SpaceRepository;
import com.wiki.monowiki.wiki.service.SpaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceRepository repo;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SpaceService service;

    @AfterEach
    void tearDown() {
	TestAuth.clear();
    }

    @Test
    void create_saves_space_and_emits_audit_event() {
	TestAuth.setAuth("admin1", "ADMIN");

	when(repo.existsBySpaceKey("ENG")).thenReturn(false);
	when(repo.save(any(Space.class))).thenAnswer(inv -> {
	    Space s = inv.getArgument(0);
	    s.setId(101L);
	    return s;
	});

	var res = service.create(new CreateSpaceRequest("ENG", " Engineering "));

	assertThat(res.id()).isEqualTo(101L);
	assertThat(res.spaceKey()).isEqualTo("ENG");
	assertThat(res.name()).isEqualTo("Engineering");

	ArgumentCaptor<WikiAuditEvent> captor = ArgumentCaptor.forClass(WikiAuditEvent.class);
	verify(publisher).publishEvent(captor.capture());

	WikiAuditEvent e = captor.getValue();
	assertThat(e.eventType()).isEqualTo(AuditEventType.SPACE_CREATED);
	assertThat(e.entityType()).isEqualTo(AuditEntityType.SPACE);
	assertThat(e.entityId()).isEqualTo(101L);
	assertThat(e.spaceKey()).isEqualTo("ENG");
	assertThat(e.articleId()).isNull();
	assertThat(e.actor()).isEqualTo("admin1");
	assertThat(e.publicEvent()).isTrue();
	assertThat(e.meta()).containsEntry("spaceKey", "ENG");
	assertThat(e.meta()).containsEntry("name", "Engineering");
    }

    @Test
    void create_when_duplicate_spaceKey_throws() {
	when(repo.existsBySpaceKey("ENG")).thenReturn(true);

	CreateSpaceRequest request =
		new CreateSpaceRequest("ENG", "Engineering");

	assertThatThrownBy(() -> service.create(request))
		.isInstanceOf(IllegalArgumentException.class)
		.hasMessageContaining("Space key already exists");
    }
}
