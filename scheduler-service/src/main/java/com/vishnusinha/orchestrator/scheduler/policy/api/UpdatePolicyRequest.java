package com.vishnusinha.orchestrator.scheduler.policy.api;

import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;
import jakarta.validation.constraints.NotNull;

public record UpdatePolicyRequest(
        @NotNull
        SchedulingPolicyType policy
) {
}
