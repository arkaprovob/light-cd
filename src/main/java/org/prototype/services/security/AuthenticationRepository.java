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
    private PublicKey publicKey;
    private final Map<String, String> apiKeys;
    private final RsaKeyPair rsaKeyPair;
    private static AuthenticationRepository authRepoInstance;





    private AuthenticationRepository() {
        apiKeys = new HashMap<>();
        this.rsaKeyPair = new RsaKeyPair();
        setPublicKey(rsaKeyPair.getPublicKey());
        initAuthRepo();
    }

    private void setPublicKey(PublicKey input) {
        this.publicKey = input;
    }


    public static AuthenticationRepository authenticationRepoInstance(){
        LOG.info("auth instance requested");
        if(Objects.isNull(authRepoInstance)){
            LOG.info("AuthenticationRepository.authRepoInstance is null hence creating and setting a new object");
            AuthenticationRepository.authRepoInstance = new AuthenticationRepository();
        }

        return AuthenticationRepository.authRepoInstance;
    }

    static PublicKey getPublicKey(){
        return authenticationRepoInstance().publicKey;
    }

    public void initAuthRepo(){
        var privateKey = rsaKeyPair.getPrivateKey();
        Objects.requireNonNull(privateKey, "error during AuthenticationRepository initialization privateKey " +
                "is null");
        var allowedUsers = Arrays.asList(
                ConfigProvider.getConfig().getValue("app.security.allowed.users", String[].class)
        );
        allowedUsers.forEach(entry -> apiKeys.put(entry, RSAUtil.encryptData(privateKey, entry)));

        apiKeys.put("privateKey", rsaKeyPair.getStringPrivateKey());
        apiKeys.put("publicKey", rsaKeyPair.getStringPublicKey());

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
