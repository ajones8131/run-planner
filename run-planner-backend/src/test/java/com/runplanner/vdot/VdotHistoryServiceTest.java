package com.runplanner.vdot;

import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VdotHistoryServiceTest {

    @Mock
    private VdotHistoryRepository repository;

    @InjectMocks
    private VdotHistoryService service;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").build();
    }

    // --- recordCalculation ---

    @Test
    void recordCalculation_bothTriggerIdsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.recordCalculation(user(), 50.0, 52.0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of triggeringWorkoutId or triggeringSnapshotId must be provided");
    }

    @Test
    void recordCalculation_bothTriggerIdsProvided_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.recordCalculation(
                        user(), 50.0, 52.0, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of triggeringWorkoutId or triggeringSnapshotId must be provided");
    }

    @Test
    void recordCalculation_normalDelta_createsUnflaggedAcceptedEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();
        UUID workoutId = UUID.randomUUID();

        VdotHistory result = service.recordCalculation(user, 50.0, 52.0, workoutId, null);

        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getPreviousVdot()).isEqualTo(50.0);
        assertThat(result.getNewVdot()).isEqualTo(52.0);
        assertThat(result.getTriggeringWorkoutId()).isEqualTo(workoutId);
        assertThat(result.getTriggeringSnapshotId()).isNull();
        verify(repository).save(any());
    }

    @Test
    void recordCalculation_largeDelta_createsFlaggedUnacceptedEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 40.0, 50.0, UUID.randomUUID(), null);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void recordCalculation_exactlyFivePointDelta_notFlagged() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 50.0, 55.0, UUID.randomUUID(), null);

        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void recordCalculation_initialVdotWithZeroPrevious_createsEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 0.0, 50.0, null, UUID.randomUUID());

        assertThat(result.getPreviousVdot()).isEqualTo(0.0);
        assertThat(result.getTriggeringSnapshotId()).isNotNull();
        assertThat(result.getTriggeringWorkoutId()).isNull();
        // Initial calculation with 0 previous: delta is 50, which is > 5,
        // but 0.0 previousVdot signals first entry — not flagged
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
    }

    // --- getEffectiveVdot ---

    @Test
    void getEffectiveVdot_withAcceptedEntry_returnsNewVdot() {
        User user = user();
        VdotHistory entry = VdotHistory.builder()
                .user(user).newVdot(52.0).accepted(true).build();
        when(repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user))
                .thenReturn(Optional.of(entry));

        Optional<Double> result = service.getEffectiveVdot(user);

        assertThat(result).contains(52.0);
    }

    @Test
    void getEffectiveVdot_noHistory_returnsEmpty() {
        User user = user();
        when(repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user))
                .thenReturn(Optional.empty());

        Optional<Double> result = service.getEffectiveVdot(user);

        assertThat(result).isEmpty();
    }

    // --- acceptFlagged ---

    @Test
    void acceptFlagged_flaggedEntry_setsAcceptedTrue() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acceptFlagged(user, historyId);

        ArgumentCaptor<VdotHistory> captor = ArgumentCaptor.forClass(VdotHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isAccepted()).isTrue();
    }

    @Test
    void acceptFlagged_notFlagged_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(false).accepted(true).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptFlagged_differentUser_throwsException() {
        User user = user();
        User otherUser = User.builder().id(UUID.randomUUID()).email("other@test.com").build();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(otherUser).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptFlagged_notFound_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        when(repository.findById(historyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- dismissFlagged ---

    @Test
    void dismissFlagged_flaggedEntry_keepsAcceptedFalse() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        service.dismissFlagged(user, historyId);

        // Entry is kept but not accepted — verify no save with accepted=true
        verify(repository, never()).save(any());
    }

    @Test
    void dismissFlagged_notFlagged_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(false).accepted(true).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void dismissFlagged_differentUser_throwsException() {
        User user = user();
        User otherUser = User.builder().id(UUID.randomUUID()).email("other@test.com").build();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(otherUser).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void dismissFlagged_notFound_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        when(repository.findById(historyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }
}
