package com.y9pro.v5;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Pure-JCA crypto helpers (no Android classes, so they can be unit-tested on a normal JVM). */
final class Crypto {
    private Crypto() {}

    static byte[] b64(String s) {
        return Base64.getDecoder().decode(s.trim());
    }

    /** ECDSA P-256 / SHA-256 signature check. Key = X.509 SubjectPublicKeyInfo DER, signature = DER. */
    static boolean verify(byte[] spki, byte[] data, byte[] derSignature) throws GeneralSecurityException {
        PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(spki));
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initVerify(key);
        s.update(data);
        return s.verify(derSignature);
    }

    /** AES-256-GCM. Input is ciphertext||tag(16 bytes). Throws if the tag does not match. */
    static byte[] decrypt(byte[] key, byte[] iv, byte[] ctAndTag) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return c.doFinal(ctAndTag);
    }

    /** PBKDF2-HMAC-SHA256, 32-byte output, password encoded as UTF-8 (same as Python hashlib.pbkdf2_hmac). */
    static byte[] pbkdf2(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] pw = new byte[bb.remaining()];
        bb.get(pw);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pw, "HmacSHA256"));
            mac.update(salt);
            mac.update(new byte[]{0, 0, 0, 1});
            byte[] u = mac.doFinal();
            byte[] t = u.clone();
            for (int i = 1; i < iterations; i++) {
                u = mac.doFinal(u);
                for (int j = 0; j < t.length; j++) t[j] ^= u[j];
            }
            return t;
        } finally {
            Arrays.fill(pw, (byte) 0);
        }
    }
}
