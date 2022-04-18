package org.prototype.services.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthenticationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationRepository.class);
    static PublicKey publicKey;
    private final Map<String, String> apiKeys;
    private final ApplicationKeyPair applicationKeyPair;


    public AuthenticationRepository() {
        apiKeys = new HashMap<>();
        this.applicationKeyPair = new ApplicationKeyPair();
        setPublicKey(applicationKeyPair.getPublicKey());
        initAuthRepo();
    }

    private static void setPublicKey(PublicKey input) {
        AuthenticationRepository.publicKey = input;
    }

    public void initAuthRepo(){
        var privateKey = applicationKeyPair.getPrivateKey();
        Objects.requireNonNull(privateKey, "error during AuthenticationRepository initialization privateKey " +
                "is null");
        var allowedUsers = Arrays.asList(
                ConfigProvider.getConfig().getValue("app.security.allowed.users", String[].class)
        );
        allowedUsers.forEach(entry -> apiKeys.put(entry, RSA.encryptData(privateKey, entry)));

        apiKeys.put("privateKey",applicationKeyPair.getStringPrivateKey());
        apiKeys.put("publicKey",applicationKeyPair.getStringPublicKey());

        String credentials = null;
        try {
            credentials = new ObjectMapper().writeValueAsString(apiKeys);
        } catch (JsonProcessingException e) {
            LOG.info("failed to convert write apiKeys to json {}",e.getMessage());
        }
        LOG.info("\n");
        LOG.info("credentials are as follows {}",credentials);
        LOG.info("\n");
    }

    public Map<String, String> getApiKeys() {
        return apiKeys;
    }
}
