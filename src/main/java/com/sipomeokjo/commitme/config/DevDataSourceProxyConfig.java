package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.logging.DevQueryLogEntryCreator;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
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
                    SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
                    listener.setLogLevel(SLF4JLogLevel.DEBUG);
                    listener.setQueryLogEntryCreator(new DevQueryLogEntryCreator(true));

                    return ProxyDataSourceBuilder.create((DataSource) bean)
                            .name("CommitmeDS")
                            .countQuery()
                            .listener(listener)
                            .logSlowQueryBySlf4j(1, TimeUnit.SECONDS)
                            .build();
                }
                return bean;
            }
        };
    }
}
