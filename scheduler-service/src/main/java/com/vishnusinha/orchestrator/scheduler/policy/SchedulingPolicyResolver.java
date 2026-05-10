package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.config.SchedulingProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SchedulingPolicyResolver {

    private final SchedulingProperties properties;
    private final Map<SchedulingPolicyType, SchedulingPolicy> policies;
    private final AtomicReference<SchedulingPolicyType> activePolicyType;

    public SchedulingPolicyResolver(SchedulingProperties properties, List<SchedulingPolicy> policies) {
        this.properties = properties;
        this.activePolicyType = new AtomicReference<>(properties.policy());
        this.policies = new EnumMap<>(SchedulingPolicyType.class);
        for (SchedulingPolicy policy : policies) {
            this.policies.put(policy.type(), policy);
        }
    }

    public SchedulingPolicy activePolicy() {
        SchedulingPolicy policy = policies.get(activePolicyType.get());
        if (policy == null) {
            throw new IllegalStateException("No scheduling policy implementation found for " + activePolicyType.get());
        }
        return policy;
    }

    public SchedulingPolicyType activePolicyType() {
        return activePolicyType.get();
    }

    public List<SchedulingPolicyType> supportedPolicies() {
        return policies.keySet().stream().sorted().toList();
    }

    public SchedulingPolicyType setActivePolicy(SchedulingPolicyType policyType) {
        if (!policies.containsKey(policyType)) {
            throw new IllegalArgumentException("Unsupported scheduling policy: " + policyType);
        }
        activePolicyType.set(policyType);
        return policyType;
    }
}
