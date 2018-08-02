package cclerc.services;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;

public class Security {

    private static Security securityInstance = new Security();

    private final String ALGORITHM = "RSA";
    private final String ALIAS = "cat";
    private final String FILE_FORMAT = "PKCS12";


    private KeyStore keystore;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // SINGLETON

    /**
     * Returns the singleton
     * @return Security singleton instance
     */
    public static Security getInstance() {
        return securityInstance;
    }

    // CONSTRUCTOR

    private Security(){

        try {

            final String P = "cat4all91;";

            keystore = KeyStore.getInstance(FILE_FORMAT);
            InputStream lKeystoreStream = getClass().getResourceAsStream("/resources/security/cat_pkcs12.ks");
            keystore.load(lKeystoreStream, P.toCharArray());
            lKeystoreStream.close();

            privateKey = (PrivateKey) keystore.getKey(ALIAS, P.toCharArray());
            Certificate lCertificate = keystore.getCertificate(ALIAS);
            publicKey = lCertificate.getPublicKey();

        } catch(Exception e) {
            Display.logUnexpectedError(e);
        }

    }

    // PRIVATE

    private static String byteArrayToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    private static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    // PUBLIC

    public String encrypt(String aInText) {

        byte[] lCipheredText = null;

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            lCipheredText = cipher.doFinal(aInText.getBytes());
        } catch (Exception e) {
            Display.logUnexpectedError(e);
        }

        return byteArrayToHexString(lCipheredText);

    }

    public String decrypt(String aInCipheredText) {

        byte[] lCipheredText = null;

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            lCipheredText = cipher.doFinal(hexStringToByteArray(aInCipheredText));
        } catch (Exception e) {
            return "";
        }

        return new String(lCipheredText);

    }

}
