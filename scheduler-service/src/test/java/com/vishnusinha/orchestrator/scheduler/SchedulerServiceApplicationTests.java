package com.vishnusinha.orchestrator.scheduler;

import com.vishnusinha.orchestrator.scheduler.job.JobQueue;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "debug=false",
        "orchestrator.dispatch.enabled=false",
        "orchestrator.fault-tolerance.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class SchedulerServiceApplicationTests {

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private JobQueue jobQueue;

    @MockBean
    private WorkerRegistry workerRegistry;

    @Test
    void contextLoads() {
    }
}
