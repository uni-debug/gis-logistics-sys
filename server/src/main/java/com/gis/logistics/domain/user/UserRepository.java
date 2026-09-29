package com.gis.logistics.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneHash(String phoneHash);

    boolean existsByPhoneHash(String phoneHash);

    Optional<User> findByIdAndRole(Long id, User.Role role);
}
