package org.prototype.services.security;


import lombok.SneakyThrows;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.util.Base64;


public class RsaKeyPair {


    private static final Logger LOG = LoggerFactory.getLogger(RsaKeyPair.class);

    private PrivateKey privateKey;
    private PublicKey publicKey;


    public RsaKeyPair() {
        onStart();
    }


    @SneakyThrows
    void onStart() {

        KeyPairGenerator keyPairGenerator =
                java.security.KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom();
        keyPairGenerator.initialize(ConfigProvider.getConfig()
                .getValue("app.security.keysize", Integer.class), secureRandom);

        KeyPair pair = keyPairGenerator.generateKeyPair();

        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
        LOG.info("RsaKeyPair generated");
    }


    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }


    public String getStringPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    public String getStringPrivateKey() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }




    @Override
    public String toString() {
        return "{"
                + "\"privateKey\":" + Base64.getEncoder().encodeToString(privateKey.getEncoded())
                + ", \"publicKey\":" + Base64.getEncoder().encodeToString(publicKey.getEncoded())
                + "}";
    }
}
