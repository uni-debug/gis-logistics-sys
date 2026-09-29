package com.gis.logistics.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void userHasCorrectDefaults() {
        User user = new User();
        assertEquals(User.Role.USER, user.getRole());
        assertEquals(1, user.getStatus());
        assertNull(user.getId());
    }

    @Test
    void prePersistFillsTimestamps() {
        User user = new User();
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    void preUpdateRefreshesUpdatedAtToNow() {
        User user = new User();
        user.onCreate();
        user.setUpdatedAt(user.getCreatedAt().minusDays(1));
        user.onUpdate();
        assertFalse(user.getUpdatedAt().isBefore(user.getCreatedAt()));
    }
}


