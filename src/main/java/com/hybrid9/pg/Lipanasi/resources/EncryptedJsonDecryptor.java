package com.hybrid9.pg.Lipanasi.resources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptedJsonDecryptor {

    @Value("${decrypt.app.secret-key}")
    private String appSecretKey;

    // Must be exactly 32 characters (256 bits)
    private static final String APP_SECRET_KEY = System.getenv("APP_SECRET_KEY");

    public String decrypt(String base64Data) throws Exception {
        byte[] encryptedWithIv = Base64.getDecoder().decode(base64Data);

        if (APP_SECRET_KEY == null || APP_SECRET_KEY.length() != 32) {
            throw new IllegalArgumentException("APP_SECRET_KEY must be exactly 32 characters");
        }

        byte[] keyBytes = APP_SECRET_KEY.getBytes("UTF-8");

        // Extract IV and ciphertext
        byte[] iv = new byte[16];
        byte[] ciphertext = new byte[encryptedWithIv.length - 16];

        System.arraycopy(encryptedWithIv, 0, iv, 0, 16);
        System.arraycopy(encryptedWithIv, 16, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);

        return new String(decrypted, "UTF-8");
    }

    /*public static void main(String[] args) {
        try {
            // Replace with your actual Base64 string
            String encrypted = "BASE64_STRING_HERE";
            String decryptedJson = decrypt(encrypted);
            System.out.println("Decrypted JSON: " + decryptedJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
