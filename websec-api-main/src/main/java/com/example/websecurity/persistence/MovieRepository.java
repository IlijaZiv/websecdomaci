package com.example.websecurity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * IDOR fix: find a movie only if it belongs to the requesting user.
     */
    Optional<Movie> findByIdAndUserId(Long id, Long userId);
}
