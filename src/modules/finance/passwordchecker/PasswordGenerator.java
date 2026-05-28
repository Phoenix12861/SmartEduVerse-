package modules.finance.passwordchecker;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    public static String generate(int length, boolean useUpper, boolean useDigits, boolean useSymbols) {
        StringBuilder chars = new StringBuilder(LOWER);
        if (useUpper) chars.append(UPPER);
        if (useDigits) chars.append(DIGITS);
        if (useSymbols) chars.append(SYMBOLS);

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
