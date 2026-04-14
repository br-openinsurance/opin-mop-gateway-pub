package br.com.opin.mopclient.gateway.infrastructure.filter;

import br.com.opin.mopclient.shared.util.MopReportidContext;
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

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.MOP_REPORTID;

/**
 * HTTP filter that captures mop-reportid from request headers and sets it in the MDC
 * for automatic inclusion in all log statements throughout the request processing.
 * <p>
 * This filter:
 * <ul>
 *   <li>Extracts mop-reportid from the {@code X-Mop-Reportid} HTTP header</li>
 *   <li>Sets it in the MDC for automatic log inclusion</li>
 *   <li>Generates a new traceable mop-reportid if not provided</li>
 *   <li>Clears the MDC at the end of request processing</li>
 *   <li>Adds the mop-reportid to the response header for client traceability</li>
 * </ul>
 * <p>
 * The filter runs early in the filter chain (Order 1) to ensure mop-reportid
 * is available for all subsequent processing and logging.
 *
 * @author MOP Team
 * @since 1.0
 */
@Component
@Order(1)
public class MopReportidFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MopReportidFilter.class);
    private static final String RESPONSE_HEADER_MOP_REPORTID = "X-Mop-Reportid";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract mop-reportid from request header
            String mopReportid = request.getHeader(MOP_REPORTID);
            
            // Set in MDC (will generate if not provided)
            MopReportidContext.setMopReportid(mopReportid);
            
            // Get the actual mop-reportid (may have been generated)
            String actualMopReportid = MopReportidContext.getMopReportid();
            
            // Add to response header for client traceability
            response.setHeader(RESPONSE_HEADER_MOP_REPORTID, actualMopReportid);
            
            LOGGER.debug("MOP Report ID set for request: {}", actualMopReportid);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear MDC at end of request to prevent mop-reportid leakage
            MopReportidContext.clear();
        }
    }
}
