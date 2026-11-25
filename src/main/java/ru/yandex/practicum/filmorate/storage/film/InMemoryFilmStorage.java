package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> likes = new HashMap<>();
    private int nextId = 1;

    @Override
    public synchronized List<Film> getAll() {
        return List.copyOf(films.values());
    }

    @Override
    public synchronized Optional<Film> getById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public synchronized Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new LinkedHashSet<>());
        return film;
    }

    @Override
    public synchronized Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public synchronized void delete(int id) {
        films.remove(id);
        likes.remove(id);
    }

    @Override
    public synchronized void addLike(int id, int userId) {
        likes.get(id).add(userId);
    }

    @Override
    public synchronized void removeLike(int id, int userId) {
        likes.get(id).remove(userId);
    }

    @Override
    public synchronized Map<Film, Integer> getLikeCounts() {
        Map<Film, Integer> counts = new LinkedHashMap<>();
        films.forEach((id, film) -> counts.put(film, likes.get(id).size()));
        return counts;
    }
}