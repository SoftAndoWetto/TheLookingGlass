package com.example.Modules.Instagram;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.*;

public class PhoneLookup
{
  // Module metadata
  boolean Working = true;
  String Description = "Check if a phone number is registered with Instagram";
  String Alternative = "Use Instagram's password reset page to check manually";

  // Constants
  private static final String USERS_LOOKUP_URL
      = "https://i.instagram.com/api/v1/users/lookup/";
  private static final String SIG_KEY_VERSION = "4";
  private static final String IG_SIG_KEY
      = "e6358aeede676184b9fe702b30f4fd35e71744605e39d2181a34cede076b3c33";

  // Variables to store the collected data
  private final List<String> phone_number = new ArrayList<> ();
  private final List<String> international_code = new ArrayList<> ();

  public String[]
  getRequiredTargetInfo ()
  {
    return new String[] { "phone_number", "international_code" };
  }

  public void
  processData (String key, String value)
  {
    try
      {
        if (key == null || value == null)
          {
            throw new IllegalArgumentException ("Key or value cannot be null");
          }

        switch (key)
          {
          case "phone_number":
            phone_number.add (value);
            break;
          case "international_code":
            international_code.add (value);
            break;
          default:
            throw new IllegalArgumentException ("Invalid key: " + key);
          }

        if (phone_number.size () == international_code.size ())
          {
            processPhoneNumber (
                phone_number.get (phone_number.size () - 1),
                international_code.get (international_code.size () - 1));
          }
      }
    catch (Exception e)
      {
        System.err.println ("[ERROR] Failed to process data: "
                            + e.getMessage ());
        if (e.getCause () != null)
          {
            System.err.println ("Caused by: " + e.getCause ().getMessage ());
          }
      }
  }

  private void
  processPhoneNumber (String phoneNumber, String internationalCode)
  {
    try
      {
        OkHttpClient client = new OkHttpClient.Builder ().build ();

        String fullPhoneNumber = internationalCode + phoneNumber;
        String jsonData = generateData (fullPhoneNumber);
        String signedBody = generateSignature (jsonData);

        Request request
            = new Request.Builder ()
                  .url (USERS_LOOKUP_URL)
                  .post (RequestBody.create (
                      signedBody,
                      MediaType.parse (
                          "application/x-www-form-urlencoded; charset=UTF-8")))
                  .addHeader ("User-Agent", "Instagram 101.0.0.15.120")
                  .addHeader (
                      "Content-Type",
                      "application/x-www-form-urlencoded; charset=UTF-8")
                  .build ();

        try (Response response = client.newCall (request).execute ())
          {
            if (!response.isSuccessful ())
              {
                throw new IOException ("Unexpected response code: "
                                       + response.code ());
              }

            String responseBody = response.body ().string ();
            JsonObject jsonResponse
                = new Gson ().fromJson (responseBody, JsonObject.class);

            if (jsonResponse.has ("message")
                && "No users found".equals (
                    jsonResponse.get ("message").getAsString ()))
              {
                System.out.println ("[INSTAGRAM] Phone +" + internationalCode
                                    + " " + phoneNumber
                                    + " is NOT registered");
              }
            else
              {
                System.out.println ("[INSTAGRAM] Phone +" + internationalCode
                                    + " " + phoneNumber + " IS registered");
              }
          }
      }
    catch (Exception e)
      {
        throw new RuntimeException ("Failed to check phone number", e);
      }
  }

  private String
  generateData (String phoneNumberRaw)
  {
    JsonObject data = new JsonObject ();
    data.addProperty ("login_attempt_count", "0");
    data.addProperty ("directly_sign_in", "true");
    data.addProperty ("source", "default");
    data.addProperty ("q", phoneNumberRaw);
    data.addProperty ("ig_sig_key_version", SIG_KEY_VERSION);
    return data.toString ();
  }

  private String
  generateSignature (String data)
      throws NoSuchAlgorithmException, InvalidKeyException
  {
    Mac sha256_HMAC = Mac.getInstance ("HmacSHA256");
    SecretKeySpec secret_key = new SecretKeySpec (
        IG_SIG_KEY.getBytes (StandardCharsets.UTF_8), "HmacSHA256");
    sha256_HMAC.init (secret_key);

    String signature = bytesToHex (
        sha256_HMAC.doFinal (data.getBytes (StandardCharsets.UTF_8)));
    String encodedData = URLEncoder.encode (data, StandardCharsets.UTF_8);

    return "ig_sig_key_version=" + SIG_KEY_VERSION + "&signed_body=" + signature
        + "." + encodedData;
  }

  private static String
  bytesToHex (byte[] bytes)
  {
    StringBuilder result = new StringBuilder ();
    for (byte b : bytes)
      {
        result.append (String.format ("%02x", b));
      }
    return result.toString ();
  }
}