package com.dmdr.personal.portal.users.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.users.model.UserSettings;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.repository.UserSettingsRepository;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceImplTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsServiceImpl userSettingsService;

    @Test
    void shouldReturnDefaultZoneWhenSettingsAreMissing() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ZoneId zoneId = userSettingsService.getUserZoneIdOrDefault(userId);

        assertEquals(ZoneId.of("Europe/Moscow"), zoneId);
    }

    @Test
    void shouldReturnZoneFromTimezoneIdWhenSettingsExist() {
        UUID userId = UUID.randomUUID();
        UserSettings settings = new UserSettings();
        settings.setTimezoneId(16);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        ZoneId zoneId = userSettingsService.getUserZoneIdOrDefault(userId);

        assertEquals(ZoneId.of("+03:00"), zoneId);
    }
}
