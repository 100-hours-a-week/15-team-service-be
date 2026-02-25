package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.metrics.JdbcQueryMetricsListener;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProdDataSourceProxyConfig {

    @Bean
    public BeanPostProcessor dataSourceProxyPostProcessor(
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            JdbcMetricsProperties jdbcMetricsProperties) {
        return new BeanPostProcessor() {
            private JdbcQueryMetricsListener jdbcQueryMetricsListener;

            private JdbcQueryMetricsListener jdbcQueryMetricsListener() {
                if (jdbcQueryMetricsListener == null) {
                    jdbcQueryMetricsListener =
                            new JdbcQueryMetricsListener(
                                    meterRegistryProvider.getObject(),
                                    jdbcMetricsProperties.slowQueryThresholdMs());
                }
                return jdbcQueryMetricsListener;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create((DataSource) bean)
                            .name("CommitmeDS")
                            .listener(jdbcQueryMetricsListener())
                            .logSlowQueryBySlf4j(
                                    jdbcMetricsProperties.slowQueryThresholdMs(),
                                    TimeUnit.MILLISECONDS)
                            .build();
                }
                return bean;
            }
        };
    }
}
