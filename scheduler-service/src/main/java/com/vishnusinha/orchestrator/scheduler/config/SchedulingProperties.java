package com.vishnusinha.orchestrator.scheduler.config;

import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator.scheduling")
public record SchedulingProperties(
        SchedulingPolicyType policy
) {
    public SchedulingProperties {
        if (policy == null) {
            policy = SchedulingPolicyType.LEAST_LOADED;
        }
    }
}
