package org.prototype.services.security;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;

public class AuthenticationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationRepository.class);

    private final Map<String,String> apiKeys;
    private final ApplicationKeyPair applicationKeyPair;
    static PublicKey publicKey;


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
        Objects.requireNonNull(privateKey,"error during AuthenticationRepository initialization privateKey " +
                "is null");
        var allowedUsers = Arrays.asList(
                ConfigProvider.getConfig().getValue("app.security.allowed.users", String[].class)
        );
        allowedUsers.forEach(entry-> apiKeys.put(entry,RSA.encryptData(privateKey,entry)));
        apiKeys.forEach((k,v)-> LOG.info("{\"name\":\"{}\", \"apiKey\":\"{}\"}",k,v));
    }

    public Map<String,String> getApiKeys() {
        return apiKeys;
    }
}
