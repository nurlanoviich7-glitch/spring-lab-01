package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Demonstrates the fifth advice type without changing the findAll sequence. */
@Aspect
@Component
@Order(4)
public class SelfInvocationTraceAspect {
    private static final Logger log = LoggerFactory.getLogger(SelfInvocationTraceAspect.class);

    @After(value = "kz.iitu.springlab.aspect.Pointcuts.selfInvocationDemo()", argNames = "joinPoint")
    public void after(JoinPoint joinPoint) {
        log.info("[TRACE] @After finally {}", joinPoint.getSignature().toShortString());
    }
}
