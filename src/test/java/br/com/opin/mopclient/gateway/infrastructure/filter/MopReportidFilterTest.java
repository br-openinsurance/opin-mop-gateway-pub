package br.com.opin.mopclient.gateway.infrastructure.filter;

import br.com.opin.mopclient.shared.util.MopReportidContext;
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

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.MOP_REPORTID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MopReportidFilter Tests")
class MopReportidFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private MopReportidFilter filter;

    @BeforeEach
    void setUp() {
        MopReportidContext.clear();
    }

    @AfterEach
    void tearDown() {
        MopReportidContext.clear();
    }

    @Test
    @DisplayName("Captures mop-reportid from header and sets MDC")
    void shouldCaptureMopReportidFromHeaderAndSetInMDC() throws ServletException, IOException {
        // Arrange
        String mopReportid = "test-mop-report-id-123";
        when(request.getHeader(MOP_REPORTID)).thenReturn(mopReportid);
        
        // Use Answer to verify mop-reportid while filter is still executing (before finally clears it)
        doAnswer(invocation -> {
            assertEquals(mopReportid, MopReportidContext.getMopReportid());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Mop-Reportid", mopReportid);
        verify(filterChain).doFilter(request, response);
        // MDC is cleared in finally, so mop-reportid should be null after filter execution
        assertNull(MopReportidContext.getMopReportid());
    }

    @Test
    @DisplayName("Generates mop-reportid when header is absent")
    void shouldGenerateMopReportidWhenHeaderNotPresent() throws ServletException, IOException {
        when(request.getHeader(MOP_REPORTID)).thenReturn(null);
        final String[] capturedMopReportid = new String[1];
        doAnswer(invocation -> {
            capturedMopReportid[0] = MopReportidContext.getMopReportid();
            assertNotNull(capturedMopReportid[0]);
            assertTrue(capturedMopReportid[0].startsWith("mop-gateway-"));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(capturedMopReportid[0]);
        assertTrue(capturedMopReportid[0].startsWith("mop-gateway-"));
        verify(response).setHeader("X-Mop-Reportid", capturedMopReportid[0]);
        assertNull(MopReportidContext.getMopReportid());
    }

    @Test
    @DisplayName("Clears MDC after processing even when an exception occurs")
    void shouldClearMDCAfterProcessingEvenOnException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(MOP_REPORTID)).thenReturn("test-id");
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, filterChain));
        
        // MDC deve estar limpo após o finally
        assertNull(MopReportidContext.getMopReportid());
    }

}
