package com.underart.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesAnIdAndExposesItDuringTheRequest() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> seen.set(RequestIdFilter.current());

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(seen.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen.get());
        assertThat(RequestIdFilter.current()).isNull();
    }

    @Test
    void reusesAValidIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "frontend-1234abcd");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("frontend-1234abcd");
    }

    @Test
    void replacesAnUnsafeIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "<script>alert(1)</script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).doesNotContain("<").hasSize(36);
    }
}
