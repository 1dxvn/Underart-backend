package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"password123", "Arte2026!", "a1234567"})
    void acceptsPasswordsWithLettersAndDigits(String password) {
        assertThat(PasswordPolicy.isValid(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "soloLetras", "12345678", ""})
    void rejectsWeakPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password)).isFalse();
    }

    @Test
    void rejectsNullAndOverlyLongPasswords() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("a1".repeat(51))).isFalse();
    }

    @Test
    void requireExplainsTheRule() {
        assertThatThrownBy(() -> PasswordPolicy.require("abcdefgh"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos una letra y un número");
    }
}
