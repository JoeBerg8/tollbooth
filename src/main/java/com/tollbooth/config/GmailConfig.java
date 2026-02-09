package com.tollbooth.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailConfig {

  private static final Logger logger = LogManager.getLogger(GmailConfig.class);
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  @Value("${gmail.credentials-json}")
  private String credentialsJson;

  @Value("${gmail.email}")
  private String gmailEmail;

  private NetHttpTransport httpTransport;
  private GoogleClientSecrets clientSecrets;

  @PostConstruct
  public void init() throws GeneralSecurityException, IOException {
    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    InputStream credentialsStream =
        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
    clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(credentialsStream));
  }

  public Gmail getGmailClient() throws IOException {
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();

    // Try to load existing credentials
    Credential credential = flow.loadCredential(gmailEmail);

    // If no existing credentials, trigger OAuth flow
    if (credential == null || credential.getRefreshToken() == null) {
      logger.warn(
          "No existing credentials found. OAuth flow will be triggered. "
              + "Make sure port 8888 is accessible for the callback.");
      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).setCallbackPath("/").build();
      String redirectUri = receiver.getRedirectUri();
      logger.info("OAuth redirect URI: " + redirectUri + " - Make sure this exact URI is registered in Google Cloud Console");
      credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(gmailEmail);
    }

    return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName("Inbox Toll")
        .build();
  }

  public String getGmailEmail() {
    return gmailEmail;
  }
}
