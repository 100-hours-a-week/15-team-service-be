package com.sipomeokjo.commitme.batch.outbox;

import com.sipomeokjo.commitme.config.OutboxCleanupProperties;
import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEvent;
import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEventStatus;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OutboxCleanupJobConfig {

    private static final String JOB_NAME = "outboxCleanupJob";
    private static final String STEP_NAME = "outboxCleanupStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxCleanupProperties props;

    public OutboxCleanupJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EntityManagerFactory entityManagerFactory,
            OutboxEventRepository outboxEventRepository,
            OutboxCleanupProperties props) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.props = props;
    }

    @Bean
    public Job outboxCleanupJob() {
        return new JobBuilder(JOB_NAME, jobRepository).start(outboxCleanupStep()).build();
    }

    @Bean
    public Step outboxCleanupStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<OutboxEvent, OutboxEvent>chunk(props.chunkSize(), transactionManager)
                .reader(outboxCleanupReader(null))
                .writer(outboxCleanupWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<OutboxEvent> outboxCleanupReader(
            @Value("#{jobParameters['cutoffInstant']}") String cutoffInstantStr) {
        Instant cutoff = cutoffInstantStr != null ? Instant.parse(cutoffInstantStr) : Instant.now();
        List<OutboxEventStatus> terminalStatuses =
                List.of(OutboxEventStatus.PUBLISHED, OutboxEventStatus.FAILED);

        return new JpaPagingItemReaderBuilder<OutboxEvent>()
                .name("outboxCleanupReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(props.chunkSize())
                .queryString(
                        "SELECT o FROM OutboxEvent o "
                                + "WHERE o.status IN :statuses "
                                + "AND o.createdAt < :cutoff "
                                + "ORDER BY o.id ASC")
                .parameterValues(Map.of("statuses", terminalStatuses, "cutoff", cutoff))
                .saveState(false)
                .build();
    }

    @Bean
    public ItemWriter<OutboxEvent> outboxCleanupWriter() {
        return chunk ->
                outboxEventRepository.deleteAllInBatch(new java.util.ArrayList<>(chunk.getItems()));
    }
}
