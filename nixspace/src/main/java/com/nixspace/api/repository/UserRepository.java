package com.nixspace.api.repository;

import com.nixspace.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByUserId(String userId);

    User findByEmail(String email);

    List<User> findByUserIdInAndDisplayNameContainingIgnoreCase(List<String> userIds, String displayName);
}