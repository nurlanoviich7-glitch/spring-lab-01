package kz.iitu.springlab.aspect;

import kz.iitu.springlab.audit.Audited;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Aspect
@Component
@Order(1)
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Around(value = "kz.iitu.springlab.aspect.Pointcuts.auditedOperation(audited)",
            argNames = "joinPoint,audited")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String arguments = audited.logArguments() ? " args=" + Arrays.toString(joinPoint.getArgs()) : "";
        log.info("[AUDIT] start {} timestamp={}{}", audited.action(), Instant.now(), arguments);
        try {
            Object result = joinPoint.proceed();
            log.info("[AUDIT] {} success timestamp={}", audited.action(), Instant.now());
            return result;
        } catch (Throwable exception) {
            log.error("[AUDIT] {} failure timestamp={} error={}: {}", audited.action(), Instant.now(),
                    exception.getClass().getSimpleName(), exception.getMessage());
            throw exception;
        }
    }
}
