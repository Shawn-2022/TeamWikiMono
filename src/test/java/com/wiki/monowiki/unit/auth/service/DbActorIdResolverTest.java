package com.wiki.monowiki.unit.auth.service;

import com.wiki.monowiki.auth.model.Role;
import com.wiki.monowiki.auth.model.Users;
import com.wiki.monowiki.auth.repo.UserRepository;
import com.wiki.monowiki.auth.service.DbActorIdResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbActorIdResolverTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private DbActorIdResolver resolver;

    @Test
    void resolve_returns_user_id_when_found() {
	Users u = new Users();
	u.setId(7L);
	u.setUsername("editor1");
	u.setRole(Role.EDITOR);

	when(users.findByUsername("editor1")).thenReturn(Optional.of(u));

	assertThat(resolver.resolve("editor1")).isEqualTo(7L);
    }

    @Test
    void resolve_returns_null_for_blank_or_system() {
	assertThat(resolver.resolve(null)).isNull();
	assertThat(resolver.resolve(" ")).isNull();
	assertThat(resolver.resolve("system")).isNull();
	assertThat(resolver.resolve("SYSTEM")).isNull();
	verifyNoInteractions(users);
    }

    @Test
    void resolve_returns_null_when_user_not_found() {
	when(users.findByUsername("missing")).thenReturn(Optional.empty());
	assertThat(resolver.resolve("missing")).isNull();
    }
}
