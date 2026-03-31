package com.carddemo.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
@ConditionalOnProperty(name = "carddemo.mq.enabled", havingValue = "true")
public class JmsConfig {

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate t = new JmsTemplate(connectionFactory);
        t.setSessionTransacted(true);
        return t;
    }
}
