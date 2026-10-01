package kz.iitu.springlab.web;

import kz.iitu.springlab.aspect.CallCounterAspect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CallCounterController {
    private final CallCounterAspect counter;

    public CallCounterController(CallCounterAspect counter) {
        this.counter = counter;
    }

    @GetMapping("/api/lab4/statistics")
    public Map<String, Long> statistics() {
        return counter.snapshot();
    }
}
