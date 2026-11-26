package com.donornexa.user.repository;

import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entities.
 * Provides standard CRUD operations plus domain-specific query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Find a user by their unique email address. */
    Optional<User> findByEmail(String email);

    /** Find a user by their unique email address ignoring case. */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Check whether an account with the given email already exists. */
    boolean existsByEmail(String email);

    /** Check whether an account with the given phone number already exists. */
    boolean existsByPhoneNumber(String phoneNumber);

    /** Check whether an account with the given phone number already exists for another user. */
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    /** Find all users who hold a specific role. */
    List<User> findByRole(Role role);

    /** Find all users with a specific account status. */
    List<User> findByAccountStatus(AccountStatus accountStatus);

    /** Find all users with a specific role, with pagination. */
    Page<User> findByRole(Role role, Pageable pageable);

    /** Find all enabled or disabled users. */
    List<User> findByEnabled(Boolean enabled);

    /** Find top 5 recently registered users. */
    List<User> findTop5ByOrderByCreatedAtDesc();

    /**
     * Search and filter registered users with pagination for administrator user management.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT u FROM User u " +
                    "WHERE (:query IS NULL OR :query = '' " +
                    "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "       OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                    "  AND (:role IS NULL OR u.role = :role) " +
                    "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus) " +
                    "ORDER BY u.createdAt DESC",
            countQuery = "SELECT COUNT(u) FROM User u " +
                         "WHERE (:query IS NULL OR :query = '' " +
                         "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                         "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                         "       OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                         "  AND (:role IS NULL OR u.role = :role) " +
                         "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)")
    Page<User> searchAndFilter(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("role") Role role,
            @org.springframework.data.repository.query.Param("accountStatus") AccountStatus accountStatus,
            Pageable pageable);
}
