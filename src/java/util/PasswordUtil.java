package util;

public class PasswordUtil {
    // No hashing - store passwords as plain text (for development only)
    public static String hashPassword(String password) {
        return password; // Return plain text
    }
    
    public static boolean verifyPassword(String password, String storedPassword) {
        return password.equals(storedPassword); // Compare plain text
    }
    
    public static String generateRandomPassword() {
        // Keep the same random password generator
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}