package id.ac.ui.cs.advprog.bidmartcore.infrastructure.adapter.input.rest;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void skipsLoggingForHealthPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsWrappingForEventStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bidding/auctions/1/stream");
        request.addHeader("Accept", "text/event-stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // SSE requests must pass the raw response through, not a wrapper
        verify(chain).doFilter(request, response);
    }

    @Test
    void logsAndProceedsForNormalRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/bidding/bids");
        request.setQueryString("foo=bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Normal requests are wrapped, so the chain receives a wrapper (not the raw response)
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void copiesBodyBackForErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/listings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(500);
            res.getWriter().write("error-body");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("error-body");
    }
}
