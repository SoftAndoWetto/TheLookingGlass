package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TheLookingGlass
{
  // For storing the path to the current target's JSON file
  public static String currentTargetPath;

  // Public static field for the targets info to be called
  public static JsonObject targetInfo;

  // Creates the list that will store the active modules selected by the user
  private static final List<String> selectedModules = new ArrayList<> ();

  // Dictionary storing API keys, session tokens, and cookies loaded from
  // Keys.json
  private static Map<String, String> keys;

  // Clear console function
  public final static void
  clearConsole ()
  {
    System.out.print ("\033[H\033[2J");
    System.out.flush ();
  }

  // Function to check existing JSON files and ask the user if they want to use
  // an existing one or create a new one
  public static JsonObject
  checkAndGatherTargetInformation (Scanner scanner)
  {
    clearConsole ();
    File targetsFolder = new File ("Targets");

    if (!targetsFolder.exists ())
      {
        targetsFolder.mkdir ();
      }

    List<File> validFiles = new ArrayList<> ();
    if (targetsFolder.exists () && targetsFolder.isDirectory ())
      {
        File[] subFolders = targetsFolder.listFiles (File::isDirectory);
        if (subFolders != null)
          {
            for (File folder : subFolders)
              {
                File targetFile
                    = new File (folder, folder.getName () + ".json");
                if (targetFile.exists ())
                  {
                    validFiles.add (targetFile);
                  }
              }
          }
      }

    if (!validFiles.isEmpty ())
      {
        System.out.println ("Existing target files found:");
        for (int i = 0; i < validFiles.size (); i++)
          {
            System.out.println (
                (i + 1) + ") "
                + validFiles.get (i).getParentFile ().getName ());
          }
        System.out.println ("0) Create a new target");
        System.out.print ("Select an option: \n> ");

        try
          {
            int choice = scanner.nextInt ();
            scanner.nextLine ();

            if (choice == 0)
              {
                return gatherTargetInformation (scanner);
              }
            else if (choice > 0 && choice <= validFiles.size ())
              {
                File selectedFile = validFiles.get (choice - 1);
                currentTargetPath
                    = selectedFile.getPath (); // Store selected path
                System.out.println ("Loading: "
                                    + selectedFile.getParentFile ().getName ());
                return loadExistingData (selectedFile);
              }
          }
        catch (Exception e)
          {
            System.out.println ("Invalid input. Creating new target.");
            scanner.nextLine ();
          }
      }

    return gatherTargetInformation (scanner);
  }

  // Function to load the existing JSON data
  public static JsonObject
  loadExistingData (File file)
  {
    clearConsole ();
    try
      {
        JsonObject json
            = new Gson ().fromJson (new FileReader (file), JsonObject.class);
        currentTargetPath = file.getPath (); // Path is stored when loading
        return json.has ("start") ? json.getAsJsonObject ("start")
                                  : new JsonObject ();
      }
    catch (IOException e)
      {
        System.out.println ("Error loading the file.");
        return new JsonObject ();
      }
  }

  public static JsonObject
  gatherTargetInformation (Scanner scanner)
  {
    clearConsole ();
    String firstName = "";
    String surname = "";
    List<String> emails = new ArrayList<> ();
    List<String> usernames = new ArrayList<> ();
    String country = "";
    String city = "";
    String internationalCode = "";
    String phoneNumber = "";

    // Loop until at least one required field is provided
    while (true)
      {
        System.out.print ("Enter First Name (leave blank if unknown): ");
        firstName = scanner.nextLine ().trim ();

        System.out.print ("Enter Surname (leave blank if unknown): ");
        surname = scanner.nextLine ().trim ();

        // Email input
        System.out.println ("Enter possible emails (comma-separated, leave "
                            + "blank if unknown): ");
        String emailInput = scanner.nextLine ().trim ();
        emails.clear ();
        if (!emailInput.isEmpty ())
          {
            for (String email : emailInput.split (","))
              {
                String trimmedEmail = email.trim ();
                if (!trimmedEmail.isEmpty ())
                  {
                    emails.add (trimmedEmail);
                  }
              }
          }

        // Username input
        System.out.println ("Enter possible usernames (comma-separated, leave "
                            + "blank if unknown): ");
        String usernameInput = scanner.nextLine ().trim ();
        usernames.clear ();
        if (!usernameInput.isEmpty ())
          {
            for (String username : usernameInput.split (","))
              {
                String trimmedUsername = username.trim ();
                if (!trimmedUsername.isEmpty ())
                  {
                    usernames.add (trimmedUsername);
                  }
              }
          }

        System.out.print ("Enter Country (leave blank if unknown): ");
        country = scanner.nextLine ().trim ();

        System.out.print ("Enter City (leave blank if unknown): ");
        city = scanner.nextLine ().trim ();

        System.out.print ("Enter International Area Code (e.g., +1, +44, leave "
                          + "blank if unknown): ");
        internationalCode = scanner.nextLine ().trim ();

        System.out.print (
            "Enter Phone Number (without spaces, leave blank if unknown): ");
        phoneNumber = scanner.nextLine ().trim ().replaceAll (" ", "");

        // Validate required fields
        if ((!firstName.isEmpty () && !surname.isEmpty ()) || !emails.isEmpty ()
            || !usernames.isEmpty ())
          {
            break;
          }
        else
          {
            System.out.println ("\nInvalid input. You must enter:");
            System.out.println ("- Both First Name and Surname, OR");
            System.out.println ("- At least one Email, OR");
            System.out.println ("- At least one Username.");
            System.out.println ("Please try again.\n");
          }
      }

    // Create JSON structure
    JsonObject targetInfo = new JsonObject ();
    JsonObject startInfo = new JsonObject ();

    // Add basic info
    if (!firstName.isEmpty ())
      startInfo.addProperty ("first_name", firstName);
    if (!surname.isEmpty ())
      startInfo.addProperty ("surname", surname);

    // Add emails
    if (!emails.isEmpty ())
      {
        JsonArray emailArray = new JsonArray ();
        for (String email : emails)
          {
            emailArray.add (email);
          }
        startInfo.add ("emails", emailArray);
      }

    // Add usernames
    if (!usernames.isEmpty ())
      {
        JsonArray usernameArray = new JsonArray ();
        for (String username : usernames)
          {
            usernameArray.add (username);
          }
        startInfo.add ("usernames", usernameArray);
      }

    // Add location info
    if (!country.isEmpty () || !city.isEmpty ())
      {
        JsonObject location = new JsonObject ();
        if (!country.isEmpty ())
          location.addProperty ("country", country);
        if (!city.isEmpty ())
          location.addProperty ("city", city);
        startInfo.add ("location", location);
      }

    // Add phone info
    if (!internationalCode.isEmpty ())
      startInfo.addProperty ("international_code", internationalCode);
    if (!phoneNumber.isEmpty ())
      startInfo.addProperty ("phone_number", phoneNumber);

    targetInfo.add ("start", startInfo);

    // Generate folder name
    String folderName;
    if (!firstName.isEmpty () && !surname.isEmpty ())
      {
        folderName = sanitizeFileName (firstName + "_" + surname);
      }
    else if (!emails.isEmpty ())
      {
        folderName = sanitizeFileName (emails.get (0).split ("@")[0]);
      }
    else if (!usernames.isEmpty ())
      {
        folderName = sanitizeFileName (usernames.get (0));
      }
    else
      {
        folderName = "target_" + System.currentTimeMillis ();
      }

    // Create target folder structure
    File targetsDir = new File ("Targets");
    if (!targetsDir.exists ())
      {
        targetsDir.mkdir ();
      }

    File targetFolder = new File (targetsDir, folderName);
    if (!targetFolder.exists ())
      {
        targetFolder.mkdir ();
      }

    // Save to JSON file and store path
    File targetFile = new File (targetFolder, folderName + ".json");
    currentTargetPath = targetFile.getPath (); // Store the path persistently

    try (FileWriter writer = new FileWriter (targetFile))
      {
        new Gson ().toJson (targetInfo, writer);
        System.out.println ("\nTarget information saved to: "
                            + currentTargetPath);
      }
    catch (IOException e)
      {
        System.err.println ("Error saving the file: " + e.getMessage ());
      }

    return targetInfo;
  }

  // Helper method to sanitize folder/file names
  private static String
  sanitizeFileName (String name)
  {
    return name.replaceAll ("[^a-zA-Z0-9._-]", "_");
  }

  // Function that explains what my application does, and usage
  public static void
  AboutMe (Scanner scanner)
  {
    clearConsole ();

    // Description text and usecases for the program
        System.out.println("""
            About The Looking Glass
            
            The Looking Glass is a modular OSINT (Open-Source Intelligence) application written in Java.
            It automates the collection of publicly available information from various online platforms.
            The tool uses a dynamic execution system, allowing it to load and run individual reconnaissance
            modules at runtime. It manages target data (e.g., usernames, emails, phone numbers) and API keys
            using dictionaries, ensuring efficient and organized data handling.
            
            Use Cases:
            - Gathering profile information from social media platforms.
            - Verifying and validating email addresses.
            - Automating repetitive OSINT tasks during penetration tests.
            - Collecting intelligence for investigations or research purposes.
            """);

        // So the user can leave when theyre ready
        System.out.println("Press Enter to continue...");

        // Wait for user input to continue
        scanner.nextLine();  // Wait for user to press Enter
  }

  // This function lists all avaliable modules currently installed
  public static void
  ListModules (String path, Scanner scanner) throws IOException
  {
    while (true)
      {
        clearConsole ();
        System.out.println ("Currently Installed Modules:\n");

        Map<String, List<Map<String, String> > > result
            = new LinkedHashMap<> ();

        // Walk through the files and organize them by module (The parent
        // folder)
        Files.walk (Paths.get (path))
            .filter (p -> p.toString ().endsWith (".java"))
            .forEach (p -> {
              String module = p.getParent ().getFileName ().toString ();
              String fileNameWithoutExtension
                  = p.getFileName ().toString ().replace (".java", "");
              Map<String, String> fileData = new HashMap<> ();
              fileData.put ("name", fileNameWithoutExtension);
              fileData.put ("path", p.toString ());

              result.putIfAbsent (module, new ArrayList<> ());
              result.get (module).add (fileData);
            });

        List<String> moduleNames = new ArrayList<> (result.keySet ());
        int fileCounter = 1;

        Map<Integer, Map<String, String> > moduleMap = new LinkedHashMap<> ();

        // Display modules with titles and separators
        for (String module : moduleNames)
          {
            List<Map<String, String> > files = result.get (module);

            // Print module title and separator
            System.out.println (module);
            System.out.println ("=======");

            for (Map<String, String> file : files)
              {
                moduleMap.put (fileCounter, file);
                boolean isSelected
                    = selectedModules.contains (file.get ("path"));
                System.out.printf ("[%s] %d. %s\n", isSelected ? "*" : " ",
                                   fileCounter, file.get ("name"));
                fileCounter++;
              }
            System.out.println (); // New line after each module
          }

        // User command prompt
        System.out.print ("Enter command (use [Number], desc [Number] or press "
                          + "Enter to exit): ");
        String input = scanner.nextLine ().trim ();

        // Exit on empty input
        if (input.isEmpty ())
          {
            System.out.println ("Exiting...");
            break;
          }

        String[] parts = input.split (" ");

        if (parts.length != 2)
          {
            System.out.println ("Invalid command format. Use 'use [Number]' or "
                                + "'desc [Number]'.");
            continue;
          }

        String command = parts[0];
        int fileNumber;

        try
          {
            fileNumber = Integer.parseInt (parts[1]);
            if (!moduleMap.containsKey (fileNumber))
              {
                System.out.println ("Invalid file number.");
                continue;
              }
          }
        catch (NumberFormatException e)
          {
            System.out.println ("Invalid number format.");
            continue;
          }

        Map<String, String> selectedFile = moduleMap.get (fileNumber);
        String filePath = selectedFile.get ("path");

        if ("use".equalsIgnoreCase (command))
          {
            // Toggle selection
            if (selectedModules.contains (filePath))
              {
                selectedModules.remove (filePath);
              }
            else
              {
                selectedModules.add (filePath);
              }
          }
        else if ("desc".equalsIgnoreCase (command))
          {
            // Display the module description
            clearConsole ();
            String description = getDescriptionFromFile (filePath);
            System.out.println ("\nDescription for " + selectedFile.get ("name")
                                + ":");
            System.out.println (description);
            System.out.println ("\nPress Enter to continue...");
            scanner.nextLine ();
          }
        else
          {
            System.out.println ("Unknown command.");
          }
      }
  }

  // Get the description from the file
  private static String
  getDescriptionFromFile (String filePath) throws IOException
  {
    List<String> lines = Files.readAllLines (Paths.get (filePath));
    StringBuilder description = new StringBuilder ();
    boolean inDescription = false;

    for (String line : lines)
      {
        if (line.contains ("Description"))
          {
            inDescription = true;
            continue;
          }
        if (line.contains ("\"\"\"") && inDescription)
          {
            break;
          }
        if (inDescription)
          {
            description.append (line.trim ()).append (" ");
          }
      }

    return description.toString ().trim ();
  }

  // Function to load the JSON and store it as a Map
  public static void
  loadKeys ()
  {
    Gson gson = new

        Gson ();
    try (FileReader reader
         = new FileReader ("src/main/java/com/example/Keys.json"))
      {
        Type type = new TypeToken<Map<String, String> > () {}.getType ();
        keys = gson.fromJson (reader, type);
      }
    catch (IOException e)
      {
        e.printStackTrace ();
      }
  }

  public static void
  Scan ()
  {
    clearConsole ();
    System.out.println ("[+] Starting scan with " + selectedModules.size ()
                        + " modules...\n");

    for (String modulePath : selectedModules)
      {
        String moduleName
            = new File (modulePath).getName ().replace (".java", "");
        System.out.println ("=== Processing Module: " + moduleName + " ===");

        try
          {
            // Convert path to class name
            String className = modulePath.replace ("src\\main\\java\\", "")
                                   .replace ("src/main/java/", "")
                                   .replace (".java", "")
                                   .replace ("\\", ".")
                                   .replace ("/", ".");

            // Load the class
            Class<?> moduleClass;
            try
              {
                moduleClass = Class.forName (className);
              }
            catch (ClassNotFoundException e)
              {
                moduleClass = Thread.currentThread ()
                                  .getContextClassLoader ()
                                  .loadClass (className);
              }

            Object moduleInstance
                = moduleClass.getDeclaredConstructor ().newInstance ();

            // Check if module is disabled
            try
              {
                Field workingField = moduleClass.getDeclaredField ("Working");
                workingField.setAccessible (true);
                if (!workingField.getBoolean (moduleInstance))
                  {
                    Field altField
                        = moduleClass.getDeclaredField ("Alternative");
                    System.out.println ("[!] Module disabled - "
                                        + altField.get (moduleInstance));
                    continue;
                  }
              }
            catch (NoSuchFieldException e)
              {
              }

            // Process API keys
            boolean keysInjected = false;
            try
              {
                Method setKeysMethod
                    = moduleClass.getMethod ("setKeys", Map.class);
                setKeysMethod.invoke (moduleInstance, keys);
                keysInjected = true;
              }
            catch (NoSuchMethodException e)
              {
              }

            Method processDataMethod = moduleClass.getMethod (
                "processData", String.class, String.class);

            // Handle target info requirements
            List<String> missingTargetInfo = new ArrayList<> ();
            try
              {
                Method getTargetInfoMethod
                    = moduleClass.getMethod ("getRequiredTargetInfo");
                String[] requiredTargetInfo
                    = (String[])getTargetInfoMethod.invoke (moduleInstance);

                if (requiredTargetInfo != null && requiredTargetInfo.length > 0)
                  {
                    for (String key : requiredTargetInfo)
                      {
                        JsonElement value = getNestedJsonValue (
                            targetInfo, key.split ("\\."));
                        if (value != null && !value.isJsonNull ())
                          {
                            if (value.isJsonPrimitive ())
                              {
                                processDataMethod.invoke (moduleInstance, key,
                                                          value.getAsString ());
                              }
                            else if (value.isJsonArray ())
                              {
                                for (JsonElement item : value.getAsJsonArray ())
                                  {
                                    processDataMethod.invoke (
                                        moduleInstance, key,
                                        item.getAsString ());
                                  }
                              }
                          }
                        else
                          {
                            missingTargetInfo.add (key);
                          }
                      }
                  }
              }
            catch (NoSuchMethodException e)
              {
              }

            // Handle legacy API key requirements
            List<String> missingKeys = new ArrayList<> ();
            try
              {
                Method getKeysMethod
                    = moduleClass.getMethod ("getRequiredKeys");
                String[] requiredKeys
                    = (String[])getKeysMethod.invoke (moduleInstance);

                if (requiredKeys != null)
                  {
                    for (String key : requiredKeys)
                      {
                        if (keys.containsKey (key))
                          {
                            processDataMethod.invoke (moduleInstance, key,
                                                      keys.get (key));
                          }
                        else
                          {
                            missingKeys.add (key);
                          }
                      }
                  }
              }
            catch (NoSuchMethodException e)
              {
              }

            // Print summary for this module
            if (!missingTargetInfo.isEmpty ())
              {
                System.out.println ("[!] Missing target info: "
                                    + String.join (", ", missingTargetInfo));
              }
            if (!missingKeys.isEmpty ())
              {
                System.out.println ("[!] Missing API keys: "
                                    + String.join (", ", missingKeys));
              }
            if (missingTargetInfo.isEmpty () && missingKeys.isEmpty ())
              {
                System.out.println ("[✓] Executed successfully"
                                    + (keysInjected ? " (with API keys)" : ""));
              }
          }
        catch (Exception e)
          {
            System.err.println ("[X] Execution failed: " + e.toString ());
          }
        System.out.println (); // Blank line between modules
      }

    System.out.println ("[✓] Scan completed. Press Enter to continue...");
    try
      {
        System.in.read ();
      }
    catch (IOException e)
      {
      }
  }

  private static JsonElement
  getNestedJsonValue (JsonElement root, String[] path)
  {
    JsonElement current = root;
    for (String part : path)
      {
        if (current == null || !current.isJsonObject ())
          return null;
        current = current.getAsJsonObject ().get (part);
      }
    return current;
  }

  // Main function to drive the program
  public static void
  main (String[] args)
  {
    loadKeys ();
    Scanner scanner = new Scanner (System.in);

    // Gather the target information before presenting other options
    targetInfo = checkAndGatherTargetInformation (scanner);

    // Main menu loop after target is gathered
    while (true)
      {
        clearConsole ();
            System.out.println("""
                Please select an option:
                1) Social Media Search
                2) About Me
                3) Configure Modules
                0) Exit
                """);
            System.out.print("Enter your choice: \n> ");

            int choice = -1;
            if (scanner.hasNextInt ())
              {
                choice = scanner.nextInt ();
                scanner.nextLine (); // Consume the newline character after the
                                     // number

                switch (choice)
                  {
                    case 1 -> Scan();
                    case 2 -> AboutMe(scanner);
                    case 3 -> {
                        try {
                            ListModules("src/main/java/com/example/Modules", scanner);
                        } catch (IOException e) {
                            System.err.println("Failed to list modules: " + e.getMessage());
                        }
                    }
                    case 0 -> {
                        System.out.println("Exiting program.");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please enter a number between 0 and 3.");
                }
            }
        }
    }
}