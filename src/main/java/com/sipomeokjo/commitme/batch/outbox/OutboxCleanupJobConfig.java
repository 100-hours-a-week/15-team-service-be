package com.sipomeokjo.commitme.batch.outbox;

import com.sipomeokjo.commitme.config.OutboxCleanupProperties;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
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
    private final DataSource dataSource;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxCleanupProperties props;

    public OutboxCleanupJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            OutboxEventRepository outboxEventRepository,
            OutboxCleanupProperties props) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
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
                .<Long, Long>chunk(props.chunkSize(), transactionManager)
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
    public JdbcPagingItemReader<Long> outboxCleanupReader(
            @Value("#{jobParameters['cutoffInstant']}") String cutoffInstantStr) {
        Instant cutoff = cutoffInstantStr != null ? Instant.parse(cutoffInstantStr) : Instant.now();

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id");
        queryProvider.setFromClause("FROM outbox_event");
        queryProvider.setWhereClause(
                "WHERE status IN ('PUBLISHED', 'FAILED') AND created_at < :cutoff");
        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<Long>()
                .name("outboxCleanupReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .parameterValues(Map.of("cutoff", Timestamp.from(cutoff)))
                .pageSize(props.chunkSize())
                .rowMapper((rs, rowNum) -> rs.getLong("id"))
                .saveState(false)
                .build();
    }

    @Bean
    public ItemWriter<Long> outboxCleanupWriter() {
        return chunk ->
                outboxEventRepository.deleteAllByIdInBatch(
                        new java.util.ArrayList<>(chunk.getItems()));
    }
}
