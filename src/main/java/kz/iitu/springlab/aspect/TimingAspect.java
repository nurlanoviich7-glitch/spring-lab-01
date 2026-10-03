package kz.iitu.springlab.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(3)
public class TimingAspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Around(value = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()", argNames = "joinPoint")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long milliseconds = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            String method = joinPoint.getSignature().toShortString();
            if (milliseconds > 200) {
                log.warn("[TIME] SLOW: {} - {} ms", method, milliseconds);
            } else {
                log.info("[TIME] {} - {} ms", method, milliseconds);
            }
        }
    }
}
