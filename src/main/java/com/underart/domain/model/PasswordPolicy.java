package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;
    public static final String DESCRIPTION =
            "must contain 8 to 100 characters, including a letter and a number";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        return password.chars().anyMatch(Character::isLetter) && password.chars().anyMatch(Character::isDigit);
    }

    public static void require(String password) {
        if (!isValid(password)) {
            throw new BusinessRuleException("Password " + DESCRIPTION);
        }
    }
}
