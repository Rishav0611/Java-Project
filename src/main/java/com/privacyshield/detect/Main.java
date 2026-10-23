package com.privacyshield.detect;

import com.privacyshield.mask.MaskResult;
import com.privacyshield.mask.Masker;
import com.privacyshield.mask.Unmasker;
import com.privacyshield.vault.CryptoService;
import com.privacyshield.vault.RetentionPolicy;
import com.privacyshield.vault.Vault;

import java.time.Duration;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String original = "Mera naam form me bharo. Phone 9876543210, email rahul@gmail.com, "
                + "PAN ABCPD1234E, Aadhaar 2345 6789 0124.";

        System.out.println("1. Original:");
        System.out.println("   " + original);

        // Detect + mask
        DetectionEngine engine = DetectionEngine.withDefaults();
        Masker masker = new Masker();
        MaskResult result = masker.mask(original, engine.detect(original));

        System.out.println("\n2. AI ko ye jayega (masked):");
        System.out.println("   " + result.maskedText());

        // Vault me encrypted save
        Vault vault = new Vault(new CryptoService("demo-password".toCharArray(), CryptoService.newSalt()));
        vault.put("session-1", result.tokenToValue(), RetentionPolicy.ttl(Duration.ofHours(24)));
        System.out.println("\n3. Vault me save hua (encrypted, 24 ghante TTL). Entries: " + vault.size());

        // AI ka reply (simulate: tokens thode badle hue)
        String aiReply = "Form ready: phone PHONE_1, email [email 1], PAN [PAN_1], Aadhaar [AADHAAR_1].";
        System.out.println("\n4. AI ka reply:");
        System.out.println("   " + aiReply);

        // Vault se mapping nikalo aur unmask karo
        Map<String, String> mapping = vault.get("session-1").orElseThrow();
        String restored = new Unmasker().unmask(aiReply, mapping);

        System.out.println("\n5. Unmask ke baad user ko final output:");
        System.out.println("   " + restored);
    }
}