package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(int id) {
        return userStorage.getById(id);
    }

    public User create(User user) {
        validateUser(user);
        User created = userStorage.create(user);
        log.info("Добавлен пользователь: id={}, login={}",
                created.getId(), created.getLogin());
        return created;
    }

    public User update(User user) {
        userStorage.getById(user.getId());
        validateUser(user);
        User updated = userStorage.update(user);
        log.info("Обновлён пользователь: id={}, login={}",
                updated.getId(), updated.getLogin());
        return updated;
    }

    public void addFriend(int id, int friendId) {
        if (id == friendId) {
            userStorage.getById(id);
            throw new ValidationException("Пользователь не может добавить себя в друзья");
        }
        userStorage.addFriendship(id, friendId);
        log.debug("Добавлена дружба: userId={}, friendId={}", id, friendId);
    }

    public void removeFriend(int id, int friendId) {
        userStorage.removeFriendship(id, friendId);
        log.debug("Удалена дружба: userId={}, friendId={}", id, friendId);
    }

    public List<User> getFriends(int id) {
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(int id, int otherId) {
        List<User> friends = userStorage.getFriends(id);
        Set<Integer> otherFriendIds = userStorage.getFriends(otherId).stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        return friends.stream()
                .filter(friend -> otherFriendIds.contains(friend.getId()))
                .toList();
    }

    private void validateUser(User user) {
        if (user.getEmail() == null
                || user.getEmail().isBlank()
                || !user.getEmail().contains("@")) {

            log.warn("Ошибка валидации пользователя: некорректный email");

            throw new ValidationException(
                    "Электронная почта не может быть пустой и должна содержать символ @"
            );
        }

        if (user.getLogin() == null
                || user.getLogin().isBlank()
                || user.getLogin().contains(" ")) {

            log.warn("Ошибка валидации пользователя: некорректный login");

            throw new ValidationException(
                    "Логин не может быть пустым и содержать пробелы"
            );
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        if (user.getBirthday() != null
                && user.getBirthday().isAfter(LocalDate.now())) {

            log.warn("Ошибка валидации пользователя: дата рождения в будущем");

            throw new ValidationException(
                    "Дата рождения не может быть в будущем"
            );
        }
    }
}