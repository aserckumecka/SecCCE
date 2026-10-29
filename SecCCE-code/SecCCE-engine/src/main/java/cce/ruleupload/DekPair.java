package cce.ruleupload;

import javax.crypto.SecretKey;

/**
 * POJO simples para armazenar o par de chaves DEK.
 */
public class DekPair {
    private final SecretKey plaintextKey;
    private final String ciphertextBlob; // Já codificado em Base64

    public DekPair(SecretKey plaintextKey, String ciphertextBlob) {
        this.plaintextKey = plaintextKey;
        this.ciphertextBlob = ciphertextBlob;
    }

    public SecretKey getPlaintextKey() {
        return plaintextKey;
    }

    public String getCiphertextBlob() {
        return ciphertextBlob;
    }
}