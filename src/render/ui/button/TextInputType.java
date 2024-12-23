package render.ui.button;

import java.util.function.Predicate;

public enum TextInputType {
    DIGITS(Character::isDigit),
    IP_ADDRESS(c -> Character.isDigit(c) || Character.isLetter(c) || c == '.' || c == ':'),
    TEXT(c -> Character.isLetter(c) || c == ' '),
    ALPHANUMERIC(c -> Character.isDigit(c) || Character.isLetter(c) || c == ' ');

    private final Predicate<Character> isValid;

    TextInputType(Predicate<Character> isValid) {
        this.isValid = isValid;
    }

    public boolean isValid(char c) {
        return isValid.test(c);
    }
}
