package com.example.Modules.Instagram;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class EmailLookup
{
  // Module metadata
  boolean Working = true;
    String Description = """
            This module will check if an email address is in use for an Instagram account.
            
            Note: If you use this too frequently in the day, you will be rate-limited.
            """;
    String Alternative = """
            To find out if an email is in use for an account, go to this page:
            https://www.instagram.com/accounts/password/reset/
            and put the email in. If it says that an email has been sent, then it is in use.
            
            Please note, your IP Address and Location may be shared using this method.
            """;

    // Variables to store the collected data
    private final List<String> emails = new ArrayList<>();

    // Method to declare required target data
    public String[]
    getRequiredTargetInfo ()
    {
      return new String[] { "emails" };
    }

    // Method to process incoming data
    public void
    processData (String key, String value)
    {
      if (key == null || value == null)
        {
          System.out.println ("Error: Key or value is null. Exiting module.");
          return; // Exit the method, which effectively exits the module since
                  // it's called by main
        }

      if (!"emails".equals (key))
        {
          System.out.println (
              "Error: Invalid key received. Expected 'emails', got: " + key);
          return; // Exit the method
        }

      emails.add (value);
      try
        {
          processEmail (value);
        }
      catch (IOException e)
        {
          System.out.println ("Error processing email " + value + ": "
                              + e.getMessage ());
          return; // Exit on IO error
        }
    }

    // Email processing logic
    private void
    processEmail (String email) throws IOException
    {
      OkHttpClient client = new OkHttpClient ();

      // Fetch CSRF token
      Request request
          = new Request.Builder ()
                .url ("https://www.instagram.com/accounts/emailsignup/")
                .build ();

      Response response = client.newCall (request).execute ();

      // Extract CSRF token from the cookies in the response headers
      String csrfToken = null;
      for (Cookie cookie :
           Cookie.parseAll (response.request ().url (), response.headers ()))
        {
          if ("csrftoken".equals (cookie.name ()))
            {
              csrfToken = cookie.value ();
              break;
            }
        }

      if (csrfToken == null)
        {
          System.out.println ("Failed to retrieve CSRF token for email: "
                              + email);
          throw new IOException ("CSRF token not found");
        }

      // Set headers and send POST request
      RequestBody formBody
          = new FormBody.Builder ().add ("email", email).build ();

      Request postRequest
          = new Request.Builder ()
                .url ("https://www.instagram.com/api/v1/web/accounts/"
                      + "web_create_ajax/attempt/")
                .post (formBody)
                .addHeader ("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; "
                            + "rv:134.0) Gecko/20100101 Firefox/134.0")
                .addHeader ("X-CSRFToken", csrfToken)
                .addHeader ("Referer",
                            "https://www.instagram.com/accounts/emailsignup/")
                .addHeader ("Content-Type", "application/x-www-form-urlencoded")
                .addHeader ("X-Requested-With", "XMLHttpRequest")
                .build ();

      Response postResponse = client.newCall (postRequest).execute ();
      String responseBody = postResponse.body ().string ();

      // Parse JSON response
      Gson gson = new Gson ();
      JsonElement jsonElement = gson.fromJson (responseBody, JsonElement.class);

      // Check if the email is in use
      if (jsonElement.isJsonObject ())
        {
          JsonObject jsonResponse = jsonElement.getAsJsonObject ();
          JsonObject errors = jsonResponse.has ("errors")
                                  ? jsonResponse.getAsJsonObject ("errors")
                                  : null;

          if (errors != null && errors.has ("email"))
            {
              for (JsonElement errorElement : errors.getAsJsonArray ("email"))
                {
                  JsonObject errorObj = errorElement.getAsJsonObject ();
                  if (errorObj.has ("code")
                      && "email_is_taken".equals (
                          errorObj.get ("code").getAsString ()))
                    {
                      System.out.println ("[INSTAGRAM] " + email
                                          + " is in use.");
                      return;
                    }
                }
            }
        }

      // If email is not in use
      System.out.println ("[INSTAGRAM] " + email + " is not in use.");
    }
}