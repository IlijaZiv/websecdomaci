package com.example.websecurity.service;

import com.example.websecurity.exception.WebSecForbiddenException;
import com.example.websecurity.exception.WebSecMissingDataException;
import com.example.websecurity.persistence.Movie;
import com.example.websecurity.persistence.MovieRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PACKAGE;

@Service
@AllArgsConstructor(access = PACKAGE)
@Slf4j
public class MovieService {
    private final MovieRepository movieRepository;

    /**
     * IDOR fix: fetch the movie only when it belongs to requestingUserId.
     * Returns 404 if the movie doesn't exist, 403 if it exists but belongs
     * to a different user.
     */
    public Movie getMovieById(Long id, Long requestingUserId) {
        log.info("Movie Service: Getting movie from database by id: {} for user: {}", id, requestingUserId);

        // First check whether the movie exists at all
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new WebSecMissingDataException("Movie with id " + id + " not found"));

        // Now enforce ownership - deny access to other users' movies
        if (movie.getUser() != null && !movie.getUser().getId().equals(requestingUserId)) {
            log.warn("Movie Service: User {} attempted to access movie {} owned by user {}",
                    requestingUserId, id, movie.getUser().getId());
            throw new WebSecForbiddenException("Access denied: you do not own this resource");
        }

        log.info("Movie Service: Found movie with title: {}", movie.getTitle());
        return movie;
    }
}
