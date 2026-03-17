package com.example.websecurity.facade;


import com.example.websecurity.api.dto.MovieResponse;
import com.example.websecurity.persistence.Movie;
import com.example.websecurity.persistence.User;
import com.example.websecurity.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieFacade {

    private final MovieService movieService;

    /**
     * IDOR fix: the requesting user is passed through so the service layer
     * can verify ownership before returning any data.
     */
    public MovieResponse getMovieById(Long id, User requestingUser) {
        log.info("Movie Facade: Getting movie by id: {} for user: {}", id, requestingUser.getId());
        Movie movie = movieService.getMovieById(id, requestingUser.getId());
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .director(movie.getDirector())
                .year(movie.getYear())
                .runningTime(movie.getRunningTime())
                .description(movie.getDescription())
                .build();
    }
}
