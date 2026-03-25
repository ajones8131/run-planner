package com.runplanner.user;

import com.runplanner.user.dto.UpdateProfileRequest;
import com.runplanner.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.name() != null) user.setName(request.name());
        if (request.dateOfBirth() != null) user.setDateOfBirth(request.dateOfBirth());
        if (request.maxHr() != null) user.setMaxHr(request.maxHr());
        if (request.preferredUnits() != null) user.setPreferredUnits(request.preferredUnits());
        return UserResponse.from(userRepository.save(user));
    }
}
