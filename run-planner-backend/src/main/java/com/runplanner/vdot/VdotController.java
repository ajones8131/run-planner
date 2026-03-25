package com.runplanner.vdot;

import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vdot")
@RequiredArgsConstructor
public class VdotController {

    private final VdotHistoryService vdotHistoryService;

    @GetMapping("/history")
    public List<VdotHistoryResponse> getHistory(@AuthenticationPrincipal User user) {
        return vdotHistoryService.getHistory(user).stream()
                .map(VdotHistoryResponse::from)
                .toList();
    }

    @PostMapping("/history/{id}/accept")
    public void acceptFlagged(@AuthenticationPrincipal User user,
                              @PathVariable UUID id) {
        vdotHistoryService.acceptFlagged(user, id);
    }

    @PostMapping("/history/{id}/dismiss")
    public void dismissFlagged(@AuthenticationPrincipal User user,
                               @PathVariable UUID id) {
        vdotHistoryService.dismissFlagged(user, id);
    }
}
