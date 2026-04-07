package com.solera.interview.service;

import com.solera.interview.dto.user.UserPageResponseDto;
import com.solera.interview.dto.user.UserRequestDto;
import com.solera.interview.dto.user.UserResponseDto;
import com.solera.interview.exception.UserNotFoundException;
import com.solera.interview.model.user.UserEntity;
import com.solera.interview.repository.UserRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "firstName", "lastName", "email", "createdAt", "updatedAt");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserPageResponseDto listUsers(int page, int size, String sortBy, String sortDirection) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));
        Page<UserEntity> userPage = userRepository.findAll(pageable);

        return new UserPageResponseDto(
                userPage.getContent().stream().map(this::toResponse).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                safeSortBy,
                direction.name().toLowerCase()
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        UserEntity user = getUserEntity(id);
        return toResponse(user);
    }

    @Transactional
    public UserResponseDto createUser(UserRequestDto requestDto) {
        UserEntity user = new UserEntity();
        applyRequest(user, requestDto);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        UserEntity existingUser = getUserEntity(id);
        applyRequest(existingUser, requestDto);
        return toResponse(userRepository.save(existingUser));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private UserEntity getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void applyRequest(UserEntity user, UserRequestDto requestDto) {
        user.setFirstName(requestDto.firstName().trim());
        user.setLastName(requestDto.lastName().trim());
        user.setEmail(requestDto.email().trim().toLowerCase());
    }

    private UserResponseDto toResponse(UserEntity userEntity) {
        return new UserResponseDto(
                userEntity.getId(),
                userEntity.getFirstName(),
                userEntity.getLastName(),
                userEntity.getEmail(),
                userEntity.getCreatedAt(),
                userEntity.getUpdatedAt()
        );
    }
}
