package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashComputeWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "HASH_COMPUTE";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int iterations = WorkloadPayloads.intValue(payload, "iterations", 100_000, 1_000, 500_000);
        byte[] state = "orchestrator-workload".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = sha256();
        for (int index = 0; index < iterations; index++) {
            digest.update(state);
            digest.update((byte) index);
            state = digest.digest();
        }
        return new WorkloadResult("sha256 digest " + HexFormat.of().formatHex(state, 0, 8), iterations);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new WorkloadExecutionException("SHA-256 is unavailable");
        }
    }
}
