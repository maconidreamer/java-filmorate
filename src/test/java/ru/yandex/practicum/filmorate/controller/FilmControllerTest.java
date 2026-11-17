package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilmControllerTest {
    private FilmController controller;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
    }

    @Test
    void shouldCreateValidFilm() {
        Film film = validFilm();

        Film created = controller.create(film);

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Film");
        assertThat(created.getDescription()).isEqualTo("Description");
        assertThat(created.getReleaseDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(created.getDuration()).isEqualTo(100);
        assertThat(controller.getFilms()).containsExactly(created);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFilms")
    void shouldRejectInvalidFilmOnCreate(String reason, Consumer<Film> makeInvalid) {
        Film film = validFilm();
        makeInvalid.accept(film);

        assertThatThrownBy(() -> controller.create(film))
                .isInstanceOf(ValidationException.class);
        assertThat(controller.getFilms()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFilms")
    void shouldRejectInvalidFilmOnUpdateAndKeepOriginal(String reason, Consumer<Film> makeInvalid) {
        Film original = controller.create(validFilm());
        Film update = validFilm();
        update.setId(original.getId());
        update.setName("Updated film");
        makeInvalid.accept(update);

        assertThatThrownBy(() -> controller.update(update))
                .isInstanceOf(ValidationException.class);
        assertThat(controller.getFilms()).containsExactly(original);
        assertThat(original.getName()).isEqualTo("Film");
    }

    @ParameterizedTest
    @ValueSource(ints = {199, 200})
    void shouldAcceptDescriptionWithinLimit(int length) {
        Film film = validFilm();
        film.setDescription("a".repeat(length));

        Film created = controller.create(film);

        assertThat(created.getDescription()).hasSize(length);
        assertThat(controller.getFilms()).containsExactly(created);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1895-12-28", "1895-12-29"})
    void shouldAcceptReleaseDateAtOrAfterMinimum(String date) {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.parse(date));

        Film created = controller.create(film);

        assertThat(created.getReleaseDate()).isEqualTo(LocalDate.parse(date));
        assertThat(controller.getFilms()).containsExactly(created);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void shouldAcceptPositiveDuration(int duration) {
        Film film = validFilm();
        film.setDuration(duration);

        Film created = controller.create(film);

        assertThat(created.getDuration()).isEqualTo(duration);
        assertThat(controller.getFilms()).containsExactly(created);
    }

    @Test
    void shouldUpdateFilmWithValidBoundaryValues() {
        Film original = controller.create(validFilm());
        Film update = validFilm();
        update.setId(original.getId());
        update.setName("Updated film");
        update.setDescription("a".repeat(200));
        update.setReleaseDate(LocalDate.of(1895, 12, 28));
        update.setDuration(1);

        Film updated = controller.update(update);

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getName()).isEqualTo("Updated film");
        assertThat(updated.getDescription()).hasSize(200);
        assertThat(updated.getReleaseDate()).isEqualTo(LocalDate.of(1895, 12, 28));
        assertThat(updated.getDuration()).isEqualTo(1);
        assertThat(controller.getFilms()).containsExactly(updated);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {999})
    void shouldRejectUpdateWithoutExistingFilm(Integer id) {
        Film film = validFilm();
        film.setId(id);

        assertThatThrownBy(() -> controller.update(film))
                .isInstanceOf(NotFoundException.class);
        assertThat(controller.getFilms()).isEmpty();
    }

    private static Stream<Arguments> invalidFilms() {
        return Stream.of(
                invalidFilm("null name", film -> film.setName(null)),
                invalidFilm("empty name", film -> film.setName("")),
                invalidFilm("blank name", film -> film.setName("   ")),
                invalidFilm("description of 201 characters", film -> film.setDescription("a".repeat(201))),
                invalidFilm("null release date", film -> film.setReleaseDate(null)),
                invalidFilm("day before minimum release date",
                        film -> film.setReleaseDate(LocalDate.of(1895, 12, 27))),
                invalidFilm("zero duration", film -> film.setDuration(0)),
                invalidFilm("negative duration", film -> film.setDuration(-1))
        );
    }

    private static Arguments invalidFilm(String reason, Consumer<Film> makeInvalid) {
        return Arguments.of(reason, makeInvalid);
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        return film;
    }
}
