package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> friends = new HashMap<>();
    private int nextId = 1;

    @Override
    public synchronized List<User> getAll() {
        return List.copyOf(users.values());
    }

    @Override
    public synchronized User getById(Integer id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    @Override
    public synchronized User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friends.put(user.getId(), new LinkedHashSet<>());
        return user;
    }

    @Override
    public synchronized User update(User user) {
        getById(user.getId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public synchronized void delete(int id) {
        getById(id);
        users.remove(id);
        friends.remove(id);
        friends.values().forEach(friendIds -> friendIds.remove(id));
    }

    @Override
    public synchronized void addFriendship(int id, int friendId) {
        getById(id);
        getById(friendId);
        friends.get(id).add(friendId);
        friends.get(friendId).add(id);
    }

    @Override
    public synchronized void removeFriendship(int id, int friendId) {
        getById(id);
        getById(friendId);
        friends.get(id).remove(friendId);
        friends.get(friendId).remove(id);
    }

    @Override
    public synchronized List<User> getFriends(int id) {
        getById(id);
        return friends.get(id).stream().map(users::get).toList();
    }
}