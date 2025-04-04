package com.example.Modules.Test;

import java.util.ArrayList;
import java.util.List;

public class ModuleTemplate {
    // Module metadata
    boolean Working = true;
    String Description = "Test module that processes both usernames and emails";
    String Alternative = "No alternative needed";
    
    // Variables to store the collected data
    private final List<String> usernames = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();

    // Method to declare required target data (rename to match Scan() expectations)
    public String[] getRequiredTargetInfo() {
        return new String[]{"usernames", "emails"}; 
    }

    // Method to process incoming data
    public void processData(String key, String value) {
        switch (key) {
            case "usernames":
                usernames.add(value);
                processUsername(value);
                break;
            case "emails":
                emails.add(value);
                processEmail(value);
                break;
            default:
                System.out.println("Unknown key received: " + key);
        }
    }

    // Username processing logic
    private void processUsername(String username) {
        System.out.println("[USERNAME] Processing: " + username);
        // Add your username-specific logic here
    }

    // Email processing logic
    private void processEmail(String email) {
        System.out.println("[EMAIL] Processing: " + email);
        // Add your email-specific logic here
    }

    // Optional: Display collected data (useful for debugging)
    public void showAllData() {
        System.out.println("\n=== Collected Data ===");
        System.out.println("Usernames: " + String.join(", ", usernames));
        System.out.println("Emails: " + String.join(", ", emails));
    }
}