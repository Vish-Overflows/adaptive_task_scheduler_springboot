package com.vishnusinha.orchestrator.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "debug=false",
        "orchestrator.worker.registration-enabled=false"
})
class WorkerServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
