package cclerc.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class Security {

    private static Security securityInstance = new Security();

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


    private Security(){

        try {

            final String P = "cat4all91;";
            final String ALIAS = "cat";

            keystore = KeyStore.getInstance("PKCS12");
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

}
