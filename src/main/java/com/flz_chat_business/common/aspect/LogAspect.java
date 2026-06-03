package com.flz_chat_business.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.flz_chat_business.common.annotation.Log;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@Aspect
public class LogAspect {

    private static final Pattern SENSITIVE_FIELDS = Pattern.compile("(password|token|phone|authorization)", Pattern.CASE_INSENSITIVE);
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Cost Time");

    @Before(value = "@annotation(controllerLog)")
    public void boBefore(JoinPoint joinPoint, Log controllerLog) {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult, true);
    }

    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null, false);
    }

    private void handleLog(JoinPoint joinPoint, Log controllerLog, Exception e, Object jsonResult, boolean success) {
        Method method = getMethod(joinPoint);
        String[] parameterNames = new DefaultParameterNameDiscoverer().getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        Map<String, Object> argsMap = new HashMap<>();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                String name = parameterNames[i];
                Object value = args[i];
                argsMap.put(name, SENSITIVE_FIELDS.matcher(name).find() ? "******" : value);
            }
        }

        long cost = System.currentTimeMillis() - TIME_THREADLOCAL.get();
        if (success) {
            log.info("biz-log op={}, method={}, costMs={}, args={}, result={}",
                    controllerLog.value(),
                    joinPoint.getSignature().toShortString(),
                    cost,
                    JSON.toJSONString(argsMap),
                    truncate(JSON.toJSONString(jsonResult)));
        } else {
            log.error("biz-log op={}, method={}, costMs={}, args={}, error={}",
                    controllerLog.value(),
                    joinPoint.getSignature().toShortString(),
                    cost,
                    JSON.toJSONString(argsMap),
                    e == null ? "unknown" : e.getMessage());
        }
    }

    private Method getMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes();
        try {
            return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Cannot resolve join point method", ex);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}