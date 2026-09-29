package com.gis.logistics.infra.seed;

import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevSeedPasswordBootstrapTest {

    private UserRepository repository() {
        return mock(UserRepository.class);
    }

    private User seedUser() {
        User u = new User();
        u.setId(1L);
        u.setName("演示用户");
        return u;
    }

    @Test
    void backfillsHashWhenDevProfileAndAccountMissingPassword() {
        UserRepository repo = repository();
        User u = seedUser();
        when(repo.findByPhoneHash(anyString())).thenReturn(Optional.of(u));

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        DevSeedPasswordBootstrap bootstrap = new DevSeedPasswordBootstrap(repo, env);
        bootstrap.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).startsWith("$2a$10$");
    }

    @Test
    void skipsAllWritesWhenProdProfile() {
        UserRepository repo = repository();
        when(repo.findByPhoneHash(anyString())).thenReturn(Optional.empty());

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        DevSeedPasswordBootstrap bootstrap = new DevSeedPasswordBootstrap(repo, env);
        bootstrap.run(null);

        verify(repo, never()).save((User) any());
    }

    @Test
    void noOpWhenHashAlreadyPresent() {
        UserRepository repo = repository();
        User u = seedUser();
        u.setPasswordHash("$2a$10$existing-existing-existing-existing-existing");
        List<String> found = List.of();
        when(repo.findByPhoneHash(anyString())).thenReturn(Optional.of(u));

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        DevSeedPasswordBootstrap bootstrap = new DevSeedPasswordBootstrap(repo, env);
        bootstrap.run(null);

        verify(repo, never()).save((User) any());
    }
}
