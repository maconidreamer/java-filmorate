package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @GetMapping
    public Collection<User> getUsers() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validateUser(user);

        user.setId(nextId++);
        users.put(user.getId(), user);

        log.info("Добавлен пользователь: id={}, login={}",
                user.getId(), user.getLogin());

        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Не удалось обновить пользователя: id={} не найден",
                    user.getId());

            throw new NotFoundException(
                    "Пользователь с id=" + user.getId() + " не найден"
            );
        }

        validateUser(user);

        users.put(user.getId(), user);

        log.info("Обновлён пользователь: id={}, login={}",
                user.getId(), user.getLogin());

        return user;
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