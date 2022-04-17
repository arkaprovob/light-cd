package org.prototype.services.security;


import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;

public class RSA {

    private RSA(){}

    private static final Logger LOG = LoggerFactory.getLogger(RSA.class);


    public static String getRequesterNameFromApiKey(String base64encryptedData){

        return getRequesterNameFromApiKey(AuthenticationRepository.publicKey,base64encryptedData);

    }

    static String getRequesterNameFromApiKey(PublicKey publicKey,String base64encryptedData){
        String deEncryptedData="John Doe";
        try{
            deEncryptedData = decryptAPIKey(publicKey,base64encryptedData);
        }catch(NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                BadPaddingException  e){
            LOG.error("Exception during access de-encryption {}",e.getMessage());
        }

        return deEncryptedData;
    }


    String getRequesterNameFromApiKey(String encodedPublicKey,String base64encryptedData){
        String deEncryptedData="John Doe";
        try{
            var publicKey = publicKeyFromString(encodedPublicKey);
            deEncryptedData = decryptAPIKey(publicKey,base64encryptedData);
        }catch(NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                BadPaddingException | InvalidKeySpecException e){
            LOG.error("Exception during access de-encryption {}",e.getMessage());
        }

        return deEncryptedData;
    }

    @SneakyThrows
    public static String encryptData(PrivateKey pvt, String data)  {
        var cipher  = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pvt);
        byte[] encryptedData =
                cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }




    private static PublicKey publicKeyFromString(String encodedPublicKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pks = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey));
        return kf.generatePublic(pks);
    }


    private static String decryptAPIKey(PublicKey pub, String base64encryptedData) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            Cipher decryptionCipher = Cipher.getInstance("RSA");
            var encryptedDataBytes =  Base64.getDecoder().decode(base64encryptedData);
            decryptionCipher.init(Cipher.DECRYPT_MODE, pub);
            byte[] decryptedMessage =
                    decryptionCipher.doFinal(encryptedDataBytes);
            return  new String(decryptedMessage);
    }



}