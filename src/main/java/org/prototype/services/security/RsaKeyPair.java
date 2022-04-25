package org.prototype.services.security;


import lombok.SneakyThrows;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;


public class RsaKeyPair {


    private static final Logger LOG = LoggerFactory.getLogger(RsaKeyPair.class);

    private PrivateKey privateKey;
    private PublicKey publicKey;


    public RsaKeyPair() {
        onStart();
    }


    @SneakyThrows
    void onStart() {

        Map<String,Key> keyPair = generateKeyPairFromProperties();

        if(!keyPair.isEmpty()){
            LOG.info("key value pair found in configuration!");
            this.privateKey = (PrivateKey) keyPair.get("private");
            this.publicKey = (PublicKey) keyPair.get("public");
            return;
        }

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

    private Map<String,Key> generateKeyPairFromProperties() throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        String encodedPublicKey = ConfigProvider.getConfig()
                .getValue("rsa.public.key", String.class);
        String encodedPrivateKey = ConfigProvider.getConfig()
                .getValue("rsa.private.key", String.class);

        if(Objects.isNull(encodedPrivateKey) || Objects.isNull(encodedPublicKey) ||
                encodedPrivateKey.isEmpty() || encodedPublicKey.isBlank())
            return Map.of();

        return Map.of("public",publicKeyFromString(encodedPublicKey),
                "private",privateKeyFromString(encodedPrivateKey));
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


    private PublicKey publicKeyFromString(String encodedPublicKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pks = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey));
        return kf.generatePublic(pks);
    }

    private PrivateKey privateKeyFromString(String encodedPrivateKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec pks = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedPrivateKey));
        return kf.generatePrivate(pks);
    }



    @Override
    public String toString() {
        return "{"
                + "\"privateKey\":" + Base64.getEncoder().encodeToString(privateKey.getEncoded())
                + ", \"publicKey\":" + Base64.getEncoder().encodeToString(publicKey.getEncoded())
                + "}";
    }
}
