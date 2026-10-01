package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Individual variant 1: count calls reaching the service proxy. */
@Aspect
@Component
@Order(4)
public class CallCounterAspect {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    @Before("kz.iitu.springlab.aspect.Pointcuts.serviceOperation()")
    public void countCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        counts.computeIfAbsent(methodName, ignored -> new LongAdder()).increment();
    }

    /**
     * Returns a sorted, immutable copy. During concurrent updates, the values are
     * weakly consistent; after calls finish, every completed increment is visible.
     */
    public Map<String, Long> snapshot() {
        Map<String, Long> snapshot = new TreeMap<>();
        counts.forEach((method, count) -> snapshot.put(method, count.sum()));
        return Collections.unmodifiableMap(snapshot);
    }
}
