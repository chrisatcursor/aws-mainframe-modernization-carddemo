package com.carddemo.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("jms")
@ImportAutoConfiguration({ArtemisAutoConfiguration.class, JmsAutoConfiguration.class})
public class JmsInfrastructureImport {
}
