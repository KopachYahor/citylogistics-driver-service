package com.kopach.logistics.driver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Пускает к REST API (/api/**) только того, кто знает общий ключ — то есть core.
 *
 * core отправляет ключ в заголовке X-Internal-Api-Key. Без заголовка или с неверным
 * ключом — 401. Ключ задаётся переменной окружения INTERNAL_API_KEY, одинаковой
 * у обоих сервисов. Это простая защита сервис-сервис для учебного проекта:
 * у ключа нет срока жизни и ролей, его нельзя выдать отдельному клиенту.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private final byte[] expectedKey;

    public InternalApiKeyFilter(@Value("${internal.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            // пустой ключ совпал бы с пустым заголовком, то есть защиты бы не было
            throw new IllegalStateException("internal.api-key (INTERNAL_API_KEY) must not be empty");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // защищаем только API; служебные адреса вроде /error не трогаем
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);

        // MessageDigest.isEqual сравнивает за одинаковое время, сколько бы символов ни совпало:
        // так ключ нельзя подобрать по времени ответа
        if (providedKey == null
                || !MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
