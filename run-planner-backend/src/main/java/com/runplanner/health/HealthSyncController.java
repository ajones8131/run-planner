package com.runplanner.health;

import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthSyncController {

    private final HealthSyncService healthSyncService;

    @PostMapping("/sync")
    public HealthSyncResponse sync(@AuthenticationPrincipal User user,
                                    @Valid @RequestBody HealthSyncRequest request) {
        return healthSyncService.sync(user, request);
    }
}
