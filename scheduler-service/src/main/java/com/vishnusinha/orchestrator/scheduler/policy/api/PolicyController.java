package com.vishnusinha.orchestrator.scheduler.policy.api;

import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final SchedulingPolicyResolver schedulingPolicyResolver;

    public PolicyController(SchedulingPolicyResolver schedulingPolicyResolver) {
        this.schedulingPolicyResolver = schedulingPolicyResolver;
    }

    @GetMapping
    public PolicyResponse getPolicies() {
        return response();
    }

    @PutMapping("/active")
    public PolicyResponse updateActivePolicy(@Valid @RequestBody UpdatePolicyRequest request) {
        schedulingPolicyResolver.setActivePolicy(request.policy());
        return response();
    }

    private PolicyResponse response() {
        return new PolicyResponse(
                schedulingPolicyResolver.activePolicyType(),
                schedulingPolicyResolver.supportedPolicies()
        );
    }
}
