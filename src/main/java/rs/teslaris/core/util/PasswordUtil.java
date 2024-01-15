package rs.teslaris.core.util;

import java.security.SecureRandom;

public class PasswordUtil {

    public static char[] generatePassword(int length) {
        var capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        var lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        var numbers = "1234567890";
        var specialCharacters = "!@#$";
        var combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;
        var random = new SecureRandom();
        char[] password = new char[length];

        password[0] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
        password[1] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
        password[2] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
        password[3] = numbers.charAt(random.nextInt(numbers.length()));

        for (int i = 4; i < length; i++) {
            password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
        }

        fisherYatesShuffle(password, random);
        return password;
    }

    private static void fisherYatesShuffle(char[] array, SecureRandom random) {
        for (int i = array.length - 1; i > 0; i--) {
            var index = random.nextInt(i + 1);
            char a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
    }
}
