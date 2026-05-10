package com.vishnusinha.orchestrator.scheduler.policy.api;

import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;

import java.util.List;

public record PolicyResponse(
        SchedulingPolicyType activePolicy,
        List<SchedulingPolicyType> supportedPolicies
) {
}
