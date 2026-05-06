package br.com.opin.mopclient.retry.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpAuthenticationException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Startup prerequisite: AMQP broker must be reachable. Runs before
 * {@link org.springframework.boot.context.event.ApplicationReadyEvent}; if it fails, the app won't become ready.
 * The retry queue is declared by {@link org.springframework.amqp.rabbit.core.RabbitAdmin} after the connection is up.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnProperty(
        name = "mop.rabbit.require-at-startup",
        havingValue = "true",
        matchIfMissing = true)
public class RabbitBrokerStartupValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RabbitBrokerStartupValidator.class);

    private final RabbitTemplate rabbitTemplate;
    private final ConnectionFactory connectionFactory;

    public RabbitBrokerStartupValidator(RabbitTemplate rabbitTemplate, ConnectionFactory connectionFactory) {
        this.rabbitTemplate = rabbitTemplate;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            rabbitTemplate.execute(channel -> null);
        } catch (Exception e) {
            String target = describeTarget(connectionFactory);
            Throwable root = rootCause(e);

            StringBuilder msg = new StringBuilder()
                    .append("RabbitMQ (startup prerequisite): failed to connect during startup")
                    .append(" | target=").append(target)
                    .append(" | root=").append(root.getClass().getSimpleName())
                    .append(" | message=").append(Optional.ofNullable(root.getMessage()).orElse("<no message>"))
                    .append(". ");

            if (isAuthFailure(e)) {
                msg.append("Likely cause: invalid credentials or insufficient permissions. ")
                        .append("Check spring.rabbitmq.username/password and whether the broker allows the PLAIN mechanism for this user. ");
            } else if (isConnectivityFailure(e)) {
                msg.append("Likely cause: broker is down, wrong host/port, DNS, firewall, or TLS mismatch. ")
                        .append("Check spring.rabbitmq.host/port and connectivity to the AMQP port (5672). ");
            } else {
                msg.append("Check spring.rabbitmq configuration (host/port/username/password) and the broker logfile. ");
            }

            logger.error("RabbitMQ startup check FAILED | target={} | root={} | message={}",
                    target,
                    root.getClass().getSimpleName(),
                    Optional.ofNullable(root.getMessage()).orElse("<no message>"),
                    e);

            throw new IllegalStateException(msg.toString(), e);
        }
        logger.info("RabbitMQ: broker reachable — startup prerequisite satisfied.");
    }

    private static boolean isAuthFailure(Throwable e) {
        return hasCause(e, AmqpAuthenticationException.class)
                || messageContains(e, "ACCESS_REFUSED")
                || messageContains(e, "Login was refused")
                || messageContains(e, "authentication mechanism PLAIN");
    }

    private static boolean isConnectivityFailure(Throwable e) {
        return hasCause(e, AmqpConnectException.class)
                || messageContains(e, "Connection refused")
                || messageContains(e, "Timed out")
                || messageContains(e, "UnknownHostException");
    }

    private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
        Throwable cur = e;
        while (cur != null && cur.getCause() != cur) {
            if (type.isInstance(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean messageContains(Throwable e, String needle) {
        Throwable cur = e;
        while (cur != null && cur.getCause() != cur) {
            String m = cur.getMessage();
            if (m != null && m.contains(needle)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static Throwable rootCause(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String describeTarget(ConnectionFactory cf) {
        if (cf instanceof CachingConnectionFactory ccf) {
            String host = ccf.getHost();
            int port = ccf.getPort();
            String user = ccf.getUsername();
            return String.format("%s:%d user=%s", host, port, user);
        }
        return cf != null ? cf.getClass().getSimpleName() : "<no-connection-factory>";
    }
}
