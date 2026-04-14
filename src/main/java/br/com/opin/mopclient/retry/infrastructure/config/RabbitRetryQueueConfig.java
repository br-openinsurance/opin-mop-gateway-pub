package br.com.opin.mopclient.retry.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitRetryQueueConfig {

    @Bean
    public Queue mopClientRetryQueue(@Value("${mop.client.retry.queue}") String queueName) {
        return new Queue(queueName, true, false, false);
    }
}
