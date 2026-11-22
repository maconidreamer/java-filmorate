package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Map;

public interface FilmStorage {
    List<Film> getAll();

    Film getById(Integer id);

    Film create(Film film);

    Film update(Film film);

    void delete(int id);

    void addLike(int id, int userId);

    void removeLike(int id, int userId);

    Map<Film, Integer> getLikeCounts();
}