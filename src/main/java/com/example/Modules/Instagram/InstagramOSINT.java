package com.example.Modules.Instagram;

import com.example.TheLookingGlass;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import okhttp3.*;

public class InstagramOSINT
{
  // Preserved metadata
  boolean Working = true;
    String Description = """
            This module uses Instagram's custom user-agent to query the API for all of the data Instagram's front end has on an account.
            It then parses it into a readable format for the user.
            Information includes: Obfuscated email, Obfuscated Phone Number, Full Name, Location.

            Note: if you use this too frequently in the day, you will be rate limited
            """;
    String Alternative = """
            There is currently no alternative for this, other than this method.
            """;

    private static final String BASE_URL = "https://i.instagram.com/api/v1";
    private final OkHttpClient client = new OkHttpClient ();
    private String sessionId;

    // Required methods for module system
    public String[]
    getRequiredTargetInfo ()
    {
      return new String[] { "usernames" };
    }

    public String[]
    getRequiredKeys ()
    {
      return new String[] { "Instagram" };
    }

    public void
    setKeys (Map<String, String> keys)
    {
      this.sessionId = keys.get ("Instagram");
      if (this.sessionId == null || this.sessionId.isEmpty ())
        {
          System.out.println ("[Instagram] Warning: No session ID provided");
        }
    }

    public void
    processData (String key, String value)
    {
      if ("usernames".equals (key))
        {
          if (sessionId == null)
            {
              System.out.println (
                  "[Instagram] Error: Session ID not initialized");
              return;
            }
          JsonObject result = processUsername (value);
          saveResultsToTargetFolder (value, result);
        }
    }

    private JsonObject
    processUsername (String username)
    {
      System.out.println ("\n[Instagram] Scanning username: " + username);

      try
        {
          String info = Lookup (username, sessionId);
          JsonObject jsonOutput
              = JsonParser.parseString (info).getAsJsonObject ();

          // Check for rate limit/spam errors
          if (isRateLimited (jsonOutput))
            {
              System.out.println ("[Instagram] Error: Rate limited - please "
                                  + "wait before trying again");
              JsonObject error = new JsonObject ();
              error.addProperty (
                  "error", "Rate limited - please wait before trying again");
              return error;
            }

          if (isSpamDetected (jsonOutput))
            {
              System.out.println ("[Instagram] Error: Activity flagged as spam "
                                  + "- try again later");
              JsonObject error = new JsonObject ();
              error.addProperty ("error",
                                 "Activity flagged as spam - try again later");
              return error;
            }

          System.out.println ("\nInfo:\n" + jsonOutput);
          return jsonOutput;
        }
      catch (Exception e)
        {
          System.out.println ("[Instagram] Error processing username: "
                              + e.getMessage ());
          JsonObject error = new JsonObject ();
          error.addProperty ("error", e.getMessage ());
          return error;
        }
    }

    private boolean
    isRateLimited (JsonObject response)
    {
      try
        {
          if (response.has ("user"))
            {
              JsonObject user = response.getAsJsonObject ("user");
              return user.has ("status")
                  && "fail".equals (user.get ("status").getAsString ())
                  && user.has ("message")
                  && user.get ("message").getAsString ().contains ("wait");
            }
        }
      catch (Exception e)
        {
          return false;
        }
      return false;
    }

    private boolean
    isSpamDetected (JsonObject response)
    {
      try
        {
          if (response.has ("user"))
            {
              JsonObject user = response.getAsJsonObject ("user");
              return user.has ("status")
                  && "fail".equals (user.get ("status").getAsString ())
                  && user.has ("spam") && user.get ("spam").getAsBoolean ();
            }
        }
      catch (Exception e)
        {
          return false;
        }
      return false;
    }

    private void
    saveResultsToTargetFolder (String username, JsonObject results)
    {
      if (TheLookingGlass.currentTargetPath == null)
        {
          System.out.println ("[Instagram] Warning: No target folder available "
                              + "to save results");
          return;
        }

      File targetFolder
          = new File (TheLookingGlass.currentTargetPath).getParentFile ();
      File instagramFolder = new File (targetFolder, "Instagram_Results");

      if (!instagramFolder.exists ())
        {
          instagramFolder.mkdir ();
        }

      File resultFile
          = new File (instagramFolder, "instagram_" + username + ".json");

      try (FileWriter writer = new FileWriter (resultFile))
        {
          new Gson ().toJson (results, writer);
          System.out.println ("[Instagram] Results saved to: "
                              + resultFile.getAbsolutePath ());
        }
      catch (IOException e)
        {
          System.out.println ("[Instagram] Error saving results: "
                              + e.getMessage ());
        }
    }

    public String
    getInfo (String search, String sessionId, boolean isUsername)
    {
      String userId = isUsername ? search : search;
      Request request = new Request.Builder ()
                            .url (BASE_URL + "/users/" + userId + "/info/")
                            .addHeader ("User-Agent", "Instagram 64.0.0.14.96")
                            .addHeader ("Cookie", "sessionid=" + sessionId)
                            .build ();
      return executeRequest (request);
    }

    public String
    Lookup (String username, String sessionId)
    {
      String data = "signed_body=SIGNATURE.{\"q\":\"" + username
                    + "\",\"skip_recovery\":\"1\"}";
      RequestBody body = RequestBody.create (
          data,
          MediaType.parse ("application/x-www-form-urlencoded; charset=UTF-8"));
      Request request
          = new Request.Builder ()
                .url (BASE_URL + "/users/lookup/")
                .addHeader ("User-Agent", "Instagram 101.0.0.15.120")
                .addHeader ("X-IG-App-ID", "124024574287414")
                .addHeader ("Cookie", "sessionid=" + sessionId)
                .post (body)
                .build ();
      return executeRequest (request);
    }

    private String
    executeRequest (Request request)
    {
      try (Response response = client.newCall (request).execute ())
        {
          if (!response.isSuccessful ())
            {
              return "{\"error\":\"" + response.code () + " - "
                  + response.message () + "\"}";
            }
          if ("gzip".equalsIgnoreCase (response.header ("Content-Encoding")))
            {
              return decompressGzip (response.body ().bytes ());
            }
          return response.body () != null ? response.body ().string ()
                                          : "{\"error\":\"Empty response\"}";
        }
      catch (IOException e)
        {
          return "{\"error\":\"" + e.getMessage () + "\"}";
        }
    }

    private String
    decompressGzip (byte[] compressedBytes) throws IOException
    {
      try (GZIPInputStream gis = new GZIPInputStream (
               new java.io.ByteArrayInputStream (compressedBytes));
           BufferedReader reader
           = new BufferedReader (new InputStreamReader (gis)))
        {
          StringBuilder result = new StringBuilder ();
          String line;
          while ((line = reader.readLine ()) != null)
            {
              result.append (line);
            }
          return result.toString ();
        }
    }
}