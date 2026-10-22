package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserControllerTest {
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @Test
    void shouldCreateValidUser() {
        User created = controller.create(validUser());

        assertThat(created.getId()).isPositive();
        assertThat(created.getEmail()).isEqualTo("user@example.org");
        assertThat(created.getLogin()).isEqualTo("user");
        assertThat(created.getName()).isEqualTo("User name");
        assertThat(created.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(controller.getUsers()).containsExactly(created);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUsers")
    void shouldRejectInvalidUserOnCreate(String reason, Consumer<User> makeInvalid) {
        User user = validUser();
        makeInvalid.accept(user);

        assertThatThrownBy(() -> controller.create(user))
                .isInstanceOf(ValidationException.class);
        assertThat(controller.getUsers()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUsers")
    void shouldRejectInvalidUserOnUpdateAndKeepOriginal(String reason, Consumer<User> makeInvalid) {
        User original = controller.create(validUser());
        User update = validUser();
        update.setId(original.getId());
        update.setName("Updated name");
        makeInvalid.accept(update);

        assertThatThrownBy(() -> controller.update(update))
                .isInstanceOf(ValidationException.class);
        assertThat(controller.getUsers()).containsExactly(original);
        assertThat(original.getName()).isEqualTo("User name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldUseLoginWhenNameIsMissingOnCreate(String name) {
        User user = validUser();
        user.setName(name);

        User created = controller.create(user);

        assertThat(created.getName()).isEqualTo("user");
        assertThat(controller.getUsers()).containsExactly(created);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldUseUpdatedLoginWhenNameIsMissingOnUpdate(String name) {
        User original = controller.create(validUser());
        User update = validUser();
        update.setId(original.getId());
        update.setLogin("updatedLogin");
        update.setName(name);

        User updated = controller.update(update);

        assertThat(updated.getName()).isEqualTo("updatedLogin");
        assertThat(controller.getUsers()).containsExactly(updated);
    }

    @Test
    void shouldAcceptBirthdayToday() {
        User user = validUser();
        LocalDate today = LocalDate.now();
        user.setBirthday(today);

        User created = controller.create(user);

        assertThat(created.getBirthday()).isEqualTo(today);
        assertThat(controller.getUsers()).containsExactly(created);
    }

    @Test
    void shouldUpdateUserWithValidData() {
        User original = controller.create(validUser());
        User update = validUser();
        update.setId(original.getId());
        update.setEmail("updated@example.org");
        update.setLogin("updatedLogin");
        update.setName("Updated name");
        LocalDate today = LocalDate.now();
        update.setBirthday(today);

        User updated = controller.update(update);

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getEmail()).isEqualTo("updated@example.org");
        assertThat(updated.getLogin()).isEqualTo("updatedLogin");
        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getBirthday()).isEqualTo(today);
        assertThat(controller.getUsers()).containsExactly(updated);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {999})
    void shouldRejectUpdateWithoutExistingUser(Integer id) {
        User user = validUser();
        user.setId(id);

        assertThatThrownBy(() -> controller.update(user))
                .isInstanceOf(NotFoundException.class);
        assertThat(controller.getUsers()).isEmpty();
    }

    private static Stream<Arguments> invalidUsers() {
        return Stream.of(
                invalidUser("null email", user -> user.setEmail(null)),
                invalidUser("empty email", user -> user.setEmail("")),
                invalidUser("blank email", user -> user.setEmail("   ")),
                invalidUser("email without @", user -> user.setEmail("mail.example.org")),
                invalidUser("null login", user -> user.setLogin(null)),
                invalidUser("empty login", user -> user.setLogin("")),
                invalidUser("blank login", user -> user.setLogin("   ")),
                invalidUser("login containing a space", user -> user.setLogin("user name")),
                invalidUser("birthday tomorrow", user -> user.setBirthday(LocalDate.now().plusDays(1)))
        );
    }

    private static Arguments invalidUser(String reason, Consumer<User> makeInvalid) {
        return Arguments.of(reason, makeInvalid);
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@example.org");
        user.setLogin("user");
        user.setName("User name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
