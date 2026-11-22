package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(int id) {
        return filmStorage.getById(id);
    }

    public Film create(Film film) {
        validateFilm(film);
        Film created = filmStorage.create(film);
        log.info("Добавлен фильм: id={}, name={}",
                created.getId(), created.getName());
        return created;
    }

    public Film update(Film film) {
        filmStorage.getById(film.getId());
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Обновлён фильм: id={}, name={}",
                updated.getId(), updated.getName());
        return updated;
    }

    public void addLike(int id, int userId) {
        userStorage.getById(userId);
        filmStorage.addLike(id, userId);
        log.debug("Добавлен лайк: filmId={}, userId={}", id, userId);
    }

    public void removeLike(int id, int userId) {
        userStorage.getById(userId);
        filmStorage.removeLike(id, userId);
        log.debug("Удалён лайк: filmId={}, userId={}", id, userId);
    }

    public List<Film> getPopular(int count) {
        if (count < 0) {
            throw new ValidationException("Количество фильмов не может быть отрицательным");
        }

        return filmStorage.getLikeCounts().entrySet().stream()
                .sorted(Map.Entry.<Film, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getId()))
                .limit(count)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации фильма: пустое название");

            throw new ValidationException(
                    "Название фильма не может быть пустым"
            );
        }

        if (film.getDescription() != null
                && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации фильма: описание длиннее 200 символов");

            throw new ValidationException(
                    "Описание фильма не может быть длиннее 200 символов"
            );
        }

        if (film.getReleaseDate() == null
                || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза");

            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }

        if (film.getDuration() <= 0) {
            log.warn("Ошибка валидации фильма: "
                    + "продолжительность должна быть положительной");

            throw new ValidationException(
                    "Продолжительность фильма должна быть положительной"
            );
        }
    }
}