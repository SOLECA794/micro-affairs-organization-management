package com.assoc.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 拦截器内直接输出统一 JSON 响应（401/403 场景）。
 */
public final class ResponseWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResponseWriter() {
    }

    public static void write(HttpServletResponse response, int httpStatus, ApiResponse<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }
}
