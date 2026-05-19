package com.personblog.admin.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.personblog.admin.entity.AdminLog;
import com.personblog.admin.service.IAdminLogService;
import com.personblog.common.utils.IpUtil;
import com.personblog.common.utils.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员操作日志切面
 *
 * @author LSH
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperatorLogAspect {

    private final IAdminLogService adminLogService;

    @Around("@annotation(recordLog)")
    public Object around(ProceedingJoinPoint point, RecordLog recordLog) throws Throwable {
        AdminLog adminLog = new AdminLog();
        adminLog.setActionType(recordLog.Type());
        adminLog.setTargetType(recordLog.businessType());
        adminLog.setDescription(recordLog.description());
        adminLog.setCreatedAt(LocalDateTime.now());

        Long userId = UserContextHolder.getUserId();
        adminLog.setAdminId(userId);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ipAddress = IpUtil.getClientIpAddress(request);
            adminLog.setIpAddress(ipAddress);
        }

        Object[] args = point.getArgs();
        String actionDetail = buildActionDetail(point, args);
        adminLog.setActionDetail(actionDetail);

        Long targetId = extractTargetId(args);
        adminLog.setTargetId(targetId);

        Object result;
        try {
            result = point.proceed();
            return result;
        } finally {
            saveLogAsync(adminLog);
        }
    }

    private String buildActionDetail(ProceedingJoinPoint point, Object[] args) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String methodName = signature.getMethod().getName();
            
            Map<String, Object> detail = new HashMap<>();
            detail.put("method", methodName);
            
            String[] paramNames = signature.getParameterNames();
            if (paramNames != null && args != null) {
                Map<String, Object> params = new HashMap<>();
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof MultipartFile) {
                        params.put(paramNames[i], "[file]");
                    } else if (arg instanceof HttpServletRequest) {
                        params.put(paramNames[i], "[request]");
                    } else if (arg != null) {
                        String jsonStr = JSONUtil.toJsonStr(arg);
                        if (jsonStr.length() > 500) {
                            params.put(paramNames[i], jsonStr.substring(0, 500) + "...");
                        } else {
                            params.put(paramNames[i], arg);
                        }
                    }
                }
                detail.put("params", params);
            }
            
            return JSONUtil.toJsonStr(detail);
        } catch (Exception e) {
            log.warn("构建操作详情失败: {}", e.getMessage());
            return "{}";
        }
    }

    private Long extractTargetId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        try {
            Object firstArg = args[0];
            if (firstArg instanceof Long) {
                return (Long) firstArg;
            }
            if (firstArg instanceof Integer) {
                return ((Integer) firstArg).longValue();
            }
            if (firstArg instanceof String str) {
                if (StrUtil.isNumeric(str)) {
                    return Long.parseLong(str);
                }
            }
            if (firstArg != null && firstArg.getClass().getDeclaredFields().length > 0) {
                try {
                    var idField = firstArg.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object idValue = idField.get(firstArg);
                    if (idValue instanceof Long) {
                        return (Long) idValue;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("提取目标ID失败: {}", e.getMessage());
        }
        
        return null;
    }

    @Async
    public void saveLogAsync(AdminLog adminLog) {
        try {
            adminLogService.save(adminLog);
            log.debug("管理员操作日志保存成功: adminId={}, actionType={}", 
                    adminLog.getAdminId(), adminLog.getActionType());
        } catch (Exception e) {
            log.error("管理员操作日志保存失败: {}", e.getMessage(), e);
        }
    }
}
