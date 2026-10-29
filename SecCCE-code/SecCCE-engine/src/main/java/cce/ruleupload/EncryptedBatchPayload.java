package cce.ruleupload;

import org.json.JSONObject;

//POJO para representar o novo payload de entrada
public class EncryptedBatchPayload {
    private final String encryptedDek;   // O CiphertextBlob da DEK
    private final String encryptedBlock; // JSON contendo {iv, ciphertext}

    public EncryptedBatchPayload(String encryptedDek, String encryptedBlock) {
        this.encryptedDek = encryptedDek;
        this.encryptedBlock = encryptedBlock;
    }

    /**
     * Converte o payload para a string JSON que a Lambda espera.
     */
    public String toJsonString() {
        JSONObject json = new JSONObject();
        json.put("encryptedDek", this.encryptedDek);
        json.put("encryptedBlock", this.encryptedBlock);
        return json.toString();
    }
}