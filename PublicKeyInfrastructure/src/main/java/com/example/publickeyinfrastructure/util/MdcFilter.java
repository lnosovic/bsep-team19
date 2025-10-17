package com.example.publickeyinfrastructure.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Dodajemo jedinstveni ID za svaki zahtev, korisno za praćenje
            MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));

            // Dodajemo IP adresu
            MDC.put("sourceIp", getClientIp(request));

            // Pokušavamo da dodamo ime ulogovanog korisnika
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                MDC.put("username", authentication.getName());
            } else {
                // Za javne endpoint-e (npr. login), korisnik još uvek nije poznat
                MDC.put("username", "anonymous");
            }

            // Prosleđujemo zahtev dalje niz lanac filtera
            filterChain.doFilter(request, response);

        } finally {
            // KLJUČNO: Očisti MDC na kraju obrade zahteva da ne bi "procureo" u druge threadove
            MDC.clear();
        }
    }

    // Pomoćna metoda za dobavljanje IP adrese, uzimajući u obzir proxy servere
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
}