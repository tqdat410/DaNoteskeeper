package app.notekeeper.external.oauth;

import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    /**
     * Get user info from Google using access token
     */
    public Payload verifyGoogleIdToken(String idTokenString) throws IOException, GeneralSecurityException {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            log.warn("Google ID token is null or empty");
            return null;
        }

        log.info("Verifying Google ID token");

        try {
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jsonFactory)
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                log.info("Google ID token verified successfully");
                return idToken.getPayload();
            }

            log.warn("Invalid Google ID token - verification failed");
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Google ID token format: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage(), e);
            throw e;
        }
    }
}
