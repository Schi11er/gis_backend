package com.gisbackend.buildingstreamer.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    // CORS-Einstellungen direkt im Code definiert
    private static final String ALLOWED_ORIGINS = "*"; // In Production hier spezifische Domains eintragen
    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization, X-Requested-With, Accept, Origin";
    private static final String MAX_AGE = "3600";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // CORS-Header setzen
        response.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGINS);
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        response.setHeader("Access-Control-Max-Age", MAX_AGE);

        // Preflight-Requests direkt beantworten
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Initialisierung falls nötig
    }

    @Override
    public void destroy() {
        // Cleanup falls nötig
    }
}