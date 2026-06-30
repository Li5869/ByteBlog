package com.personblog.common.utils;


import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
    };

    /**
     * 获取请求客户端的真实 IP 地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多个 IP 时，取第一个非 unknown 的
                if (header.equals("X-Forwarded-For")) {
                    // X-Forwarded-For 格式: client, proxy1, proxy2
                    int index = ip.indexOf(',');
                    if (index != -1) {
                        ip = ip.substring(0, index);
                    }
                }
                return ip.trim();
            }
        }
        // 如果请求头都没有，回退到 getRemoteAddr
        return request.getRemoteAddr();
    }
}