package br.com.opin.mopclient.gateway.infrastructure.filter;

import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @DisplayName("Deve capturar correlationId do header e definir no MDC")
    void shouldCaptureCorrelationIdFromHeaderAndSetInMDC() throws ServletException, IOException {
        // Arrange
        String correlationId = "test-correlation-id-123";
        when(request.getHeader(CORRELATIONID)).thenReturn(correlationId);
        
        // Use Answer to verify correlationId while filter is still executing (before finally clears it)
        doAnswer(invocation -> {
            assertEquals(correlationId, CorrelationIdContext.getCorrelationId());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Correlation-ID", correlationId);
        verify(filterChain).doFilter(request, response);
        // MDC is cleared in finally, so correlationId should be null after filter execution
        assertNull(CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve gerar correlationId quando header não está presente")
    void shouldGenerateCorrelationIdWhenHeaderNotPresent() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(CORRELATIONID)).thenReturn(null);
        
        // Use Answer to capture and verify correlationId while filter is still executing
        final String[] capturedCorrelationId = new String[1];
        doAnswer(invocation -> {
            capturedCorrelationId[0] = CorrelationIdContext.getCorrelationId();
            assertNotNull(capturedCorrelationId[0]);
            assertTrue(capturedCorrelationId[0].startsWith("mop-gateway-"));
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(capturedCorrelationId[0]);
        assertTrue(capturedCorrelationId[0].startsWith("mop-gateway-"));
        verify(response).setHeader("X-Correlation-ID", capturedCorrelationId[0]);
        verify(filterChain).doFilter(request, response);
        // MDC is cleared in finally, so correlationId should be null after filter execution
        assertNull(CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve gerar correlationId quando header está vazio")
    void shouldGenerateCorrelationIdWhenHeaderIsEmpty() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(CORRELATIONID)).thenReturn("");
        
        // Use Answer to capture and verify correlationId while filter is still executing
        final String[] capturedCorrelationId = new String[1];
        doAnswer(invocation -> {
            capturedCorrelationId[0] = CorrelationIdContext.getCorrelationId();
            assertNotNull(capturedCorrelationId[0]);
            assertTrue(capturedCorrelationId[0].startsWith("mop-gateway-"));
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(capturedCorrelationId[0]);
        assertTrue(capturedCorrelationId[0].startsWith("mop-gateway-"));
        verify(response).setHeader("X-Correlation-ID", capturedCorrelationId[0]);
        verify(filterChain).doFilter(request, response);
        // MDC is cleared in finally, so correlationId should be null after filter execution
        assertNull(CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve limpar MDC após processamento mesmo em caso de exceção")
    void shouldClearMDCAfterProcessingEvenOnException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(CORRELATIONID)).thenReturn("test-id");
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, filterChain));
        
        // MDC deve estar limpo após o finally
        assertNull(CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve adicionar correlationId no header de resposta")
    void shouldAddCorrelationIdToResponseHeader() throws ServletException, IOException {
        // Arrange
        String correlationId = "response-correlation-id";
        when(request.getHeader(CORRELATIONID)).thenReturn(correlationId);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Correlation-ID", correlationId);
    }
}

