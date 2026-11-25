package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    List<User> getAll();

    Optional<User> getById(Integer id);

    User create(User user);

    User update(User user);

    void delete(int id);

    void addFriendship(int id, int friendId);

    void removeFriendship(int id, int friendId);

    List<User> getFriends(int id);
}