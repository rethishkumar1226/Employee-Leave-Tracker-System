package com.leavemanager.repository;

import com.leavemanager.entity.Role;
import com.leavemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByManagerId(Long managerId);
    List<User> findByDepartmentId(Long departmentId);
    List<User> findByRole(Role role);
}
