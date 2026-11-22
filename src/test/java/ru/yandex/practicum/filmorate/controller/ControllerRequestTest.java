package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerRequestTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(
                new InMemoryFilmStorage(),
                userStorage
        );
        FilmController filmController = new FilmController(filmService);
        UserController userController = new UserController(new UserService(userStorage));

        mockMvc = MockMvcBuilders.standaloneSetup(filmController, userController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "/films, POST",
            "/films, PUT",
            "/users, POST",
            "/users, PUT"
    })
    void shouldRejectRequestWithoutBody(String path, String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"/films, POST", "/users, POST"})
    void shouldRejectCreationWithEmptyJson(String path, String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void friendshipIsMutualAndSurvivesProfileUpdate() throws Exception {
        for (int id = 1; id <= 3; id++) {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user" + id
                                    + "@example.org\",\"login\":\"user" + id + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(put("/users/1/friends/3"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/users/2/friends/3"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/users/3/friends/1"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": 3,
                                    "email": "user3@example.org",
                                    "login": "updated"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/1/friends/common/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].login").value("updated"));

        mockMvc.perform(get("/users/3/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/users/3/friends/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void duplicateLikesDoNotAffectRankingAndFilmUpdateKeepsLikes() throws Exception {
        String film = """
                {
                    "name": "Film",
                    "releaseDate": "2000-01-01",
                    "duration": 100
                }
                """;

        for (int id = 1; id <= 2; id++) {
            mockMvc.perform(post("/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(film))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user" + id
                                    + "@example.org\",\"login\":\"user" + id + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(put("/films/2/like/1"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/2/like/1"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/2/like/2"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": 2,
                                    "name": "Updated",
                                    "releaseDate": "2000-01-01",
                                    "duration": 100
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/popular").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Updated"));

        mockMvc.perform(delete("/films/2/like/1"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/films/2/like/2"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}