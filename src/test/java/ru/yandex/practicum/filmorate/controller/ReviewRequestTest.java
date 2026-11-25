package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.exception.ApiExceptionHandler;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewRequestTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(
                new InMemoryFilmStorage(),
                userStorage
        );

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FilmController(filmService),
                        new UserController(new UserService(userStorage))
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.org\","
                                + "\"login\":\"user\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Film\","
                                + "\"releaseDate\":\"2000-01-01\","
                                + "\"duration\":100}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1"})
    void shouldRejectNonPositivePopularCount(int count) throws Exception {
        mockMvc.perform(get("/films/popular")
                        .param("count", String.valueOf(count)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "/users/1/friends/1, PUT",
            "/users/1/friends/1, DELETE",
            "/users/1/friends/common/1, GET"
    })
    void shouldRejectFriendshipOperationsWithSelf(
            String path, String method
    ) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "/users/999, GET",
            "/films/999, GET",
            "/users/999/friends, GET",
            "/users/1/friends/999, PUT",
            "/users/999/friends/1, PUT",
            "/users/1/friends/999, DELETE",
            "/users/999/friends/1, DELETE",
            "/users/1/friends/common/999, GET",
            "/users/999/friends/common/1, GET",
            "/films/999/like/1, PUT",
            "/films/1/like/999, PUT",
            "/films/999/like/1, DELETE",
            "/films/1/like/999, DELETE"
    })
    void shouldReturnNotFoundForMissingEntities(
            String path, String method
    ) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "/films, null",
            "/films, 999",
            "/users, null",
            "/users, 999"
    })
    void shouldRejectUpdateWithoutExistingEntity(
            String path, String id
    ) throws Exception {
        String body;

        if (path.equals("/films")) {
            body = "{\"id\":" + id + ","
                    + "\"name\":\"Updated\","
                    + "\"releaseDate\":\"2000-01-01\","
                    + "\"duration\":100}";
        } else {
            body = "{\"id\":" + id + ","
                    + "\"email\":\"updated@example.org\","
                    + "\"login\":\"updated\"}";
        }

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}