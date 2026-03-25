package com.runplanner.vdot;

import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.runplanner.vdot.VdotConstants.FLAGGING_THRESHOLD;

@Service
@RequiredArgsConstructor
public class VdotHistoryService {

    private final VdotHistoryRepository repository;

    @Transactional
    public VdotHistory recordCalculation(User user, double previousVdot, double newVdot,
                                         UUID triggeringWorkoutId, UUID triggeringSnapshotId) {
        if ((triggeringWorkoutId == null) == (triggeringSnapshotId == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of triggeringWorkoutId or triggeringSnapshotId must be provided");
        }
        boolean isInitial = previousVdot == 0.0;
        boolean shouldFlag = !isInitial && Math.abs(newVdot - previousVdot) > FLAGGING_THRESHOLD;

        var entry = VdotHistory.builder()
                .user(user)
                .triggeringWorkoutId(triggeringWorkoutId)
                .triggeringSnapshotId(triggeringSnapshotId)
                .previousVdot(previousVdot)
                .newVdot(newVdot)
                .flagged(shouldFlag)
                .accepted(!shouldFlag)
                .build();

        return repository.save(entry);
    }

    @Transactional(readOnly = true)
    public Optional<Double> getEffectiveVdot(User user) {
        return repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user)
                .map(VdotHistory::getNewVdot);
    }

    @Transactional(readOnly = true)
    public List<VdotHistory> getHistory(User user) {
        return repository.findAllByUserOrderByCalculatedAtDesc(user);
    }

    @Transactional
    public void acceptFlagged(User user, UUID historyId) {
        VdotHistory entry = findOwnedEntry(user, historyId);
        if (!entry.isFlagged()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry is not flagged");
        }
        entry.setAccepted(true);
        repository.save(entry);
    }

    @Transactional
    public void dismissFlagged(User user, UUID historyId) {
        VdotHistory entry = findOwnedEntry(user, historyId);
        if (!entry.isFlagged()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry is not flagged");
        }
        // No mutation — entry stays flagged and unaccepted
    }

    private VdotHistory findOwnedEntry(User user, UUID historyId) {
        VdotHistory entry = repository.findById(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "VDOT history entry not found"));
        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "VDOT history entry not found");
        }
        return entry;
    }
}
