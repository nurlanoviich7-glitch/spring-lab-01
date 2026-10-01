package kz.iitu.springlab.aspect;

import kz.iitu.springlab.service.CallCounterTestService;
import kz.iitu.springlab.web.CallCounterController;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CallCounterAspectTest {
    private Fixture fixture() {
        CallCounterAspect counter = new CallCounterAspect();
        AspectJProxyFactory factory = new AspectJProxyFactory(new CallCounterTestService());
        factory.setProxyTargetClass(true);
        factory.addAspect(new Pointcuts());
        factory.addAspect(counter);
        return new Fixture(counter, factory.getProxy());
    }

    @Test
    void directServiceCallsIncreaseTheirOwnMethodCounts() {
        Fixture fixture = fixture();
        fixture.service().findById(1);
        fixture.service().findById(2);
        fixture.service().remove(5);

        assertEquals(Map.of("findById", 2L, "remove", 1L), fixture.counter().snapshot());
    }

    @Test
    void failingCallIsCountedBecauseBeforeAdviceCountsAttempts() {
        Fixture fixture = fixture();

        assertThrows(IllegalArgumentException.class, () -> fixture.service().remove(0));

        assertEquals(Map.of("remove", 1L), fixture.counter().snapshot());
    }

    @Test
    void twoCallsThroughThisBypassTheProxyAndAreNotCounted() {
        Fixture fixture = fixture();
        fixture.service().remove(5);

        assertEquals("Removed item no. 5; Removed item no. 6", fixture.service().removeTwice(5));

        assertEquals(Map.of("remove", 1L, "removeTwice", 1L), fixture.counter().snapshot());
    }

    @Test
    void concurrentCallsDoNotLoseIncrements() throws Exception {
        Fixture fixture = fixture();
        int threads = 8;
        int callsPerThread = 100;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> completions = new ArrayList<>();
        try (var workers = Executors.newFixedThreadPool(threads)) {
            for (int thread = 0; thread < threads; thread++) {
                completions.add(workers.submit(() -> {
                    start.await();
                    for (int call = 0; call < callsPerThread; call++) {
                        fixture.service().findById(call);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> completion : completions) {
                completion.get(10, TimeUnit.SECONDS);
            }
        }

        assertEquals((long) threads * callsPerThread, fixture.counter().snapshot().get("findById").longValue());
    }

    @Test
    void endpointReturnsAnImmutableSortedCopyWithoutChangingCounts() {
        Fixture fixture = fixture();
        fixture.service().remove(5);
        fixture.service().findById(1);
        CallCounterController controller = new CallCounterController(fixture.counter());

        Map<String, Long> snapshot = controller.statistics();
        assertEquals(List.of("findById", "remove"), new ArrayList<>(snapshot.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("remove", 999L));
        fixture.service().findById(2);

        assertEquals(1L, snapshot.get("findById").longValue());
        assertEquals(2L, controller.statistics().get("findById").longValue());
        assertEquals(Map.of("findById", 2L, "remove", 1L), fixture.counter().snapshot());
    }

    private record Fixture(CallCounterAspect counter, CallCounterTestService service) { }
}
