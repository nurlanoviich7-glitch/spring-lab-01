package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Order(2)
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Before(value = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()", argNames = "joinPoint")
    public void before(JoinPoint joinPoint) {
        log.info("[LOG] -> {} args={}", joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(pointcut = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()",
            returning = "result", argNames = "joinPoint,result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("[LOG] <- {} returned {}", joinPoint.getSignature().toShortString(), result);
    }

    @AfterThrowing(pointcut = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()",
            throwing = "exception", argNames = "joinPoint,exception")
    public void afterThrowing(JoinPoint joinPoint, Throwable exception) {
        log.error("[LOG] !! {} threw {}: {}", joinPoint.getSignature().toShortString(),
                exception.getClass().getSimpleName(), exception.getMessage());
    }
}
