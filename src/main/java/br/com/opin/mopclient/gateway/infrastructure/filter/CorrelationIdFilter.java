package br.com.opin.mopclient.gateway.infrastructure.filter;

import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;

/**
 * HTTP filter that captures correlation ID from request headers and sets it in the MDC
 * for automatic inclusion in all log statements throughout the request processing.
 * <p>
 * This filter:
 * <ul>
 *   <li>Extracts correlation ID from the {@code correlationID} HTTP header</li>
 *   <li>Sets it in the MDC for automatic log inclusion</li>
 *   <li>Generates a new traceable correlation ID if not provided</li>
 *   <li>Clears the MDC at the end of request processing</li>
 *   <li>Adds the correlation ID to the response header for client traceability</li>
 * </ul>
 * <p>
 * The filter runs early in the filter chain (Order 1) to ensure correlation ID
 * is available for all subsequent processing and logging.
 *
 * @author MOP Team
 * @since 1.0
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String RESPONSE_HEADER_CORRELATION_ID = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract correlation ID from request header
            String correlationId = request.getHeader(CORRELATIONID);
            
            // Set in MDC (will generate if not provided)
            CorrelationIdContext.setCorrelationId(correlationId);
            
            // Get the actual correlation ID (may have been generated)
            String actualCorrelationId = CorrelationIdContext.getCorrelationId();
            
            // Add to response header for client traceability
            response.setHeader(RESPONSE_HEADER_CORRELATION_ID, actualCorrelationId);
            
            LOGGER.debug("Correlation ID set for request: {}", actualCorrelationId);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear MDC at end of request to prevent correlation ID leakage
            CorrelationIdContext.clear();
        }
    }
}

