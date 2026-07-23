/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.reasoning.scripting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * ContextCacheFingerprintService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ContextCacheFingerprintService {
    /**
     * Builds the configured target fingerprint.
     *
     * @param sourceHash the source hash
     * @param configFingerprint the config fingerprint
     * @param policyFingerprint the policy fingerprint
     * @param messageTypePlan the message type plan
     * @return the string result
     */
    public String buildFingerprint(String sourceHash,
                                   String configFingerprint,
                                   String policyFingerprint,
                                   List<MessageTypePlan> messageTypePlan) {
        return buildFingerprint(sourceHash, configFingerprint, policyFingerprint, messageTypePlan, Map.of());
        }

        /**
         * Builds the configured target fingerprint.
         *
         * @param sourceHash the source hash
         * @param configFingerprint the config fingerprint
         * @param policyFingerprint the policy fingerprint
         * @param messageTypePlan the message type plan
         * @param stepScriptFingerprints the step script fingerprints
         * @return the string result
         */
        public String buildFingerprint(String sourceHash,
                       String configFingerprint,
                       String policyFingerprint,
                       List<MessageTypePlan> messageTypePlan,
                       Map<String, String> stepScriptFingerprints) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(nullToEmpty(sourceHash)).append('|')
                .append(nullToEmpty(configFingerprint)).append('|')
                .append(nullToEmpty(policyFingerprint));
        if (messageTypePlan != null) {
            for (MessageTypePlan plan : messageTypePlan) {
                buffer.append('|').append(plan.getOrdinal()).append(':').append(plan.getStepName());
            }
        }
        if (stepScriptFingerprints != null && !stepScriptFingerprints.isEmpty()) {
            stepScriptFingerprints.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> buffer.append("|fp:")
                            .append(entry.getKey())
                            .append('=')
                            .append(nullToEmpty(entry.getValue())));
        }
        return sha256(buffer.toString());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
