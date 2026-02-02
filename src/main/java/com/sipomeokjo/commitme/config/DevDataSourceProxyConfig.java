package com.sipomeokjo.commitme.config;

import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevDataSourceProxyConfig {

    @Bean
    public BeanPostProcessor dataSourceProxyPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create((DataSource) bean)
                        .name("CommitmeDS")
                        .countQuery()
                        .logQueryBySlf4j()
                        .multiline()
                        .formatQuery(query -> FormatStyle.BASIC.getFormatter().format(query))
                        .logSlowQueryBySlf4j(1, TimeUnit.SECONDS)
                        .build();
                }
                return bean;
            }
        };
    }
}
