package kz.iitu.springlab.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component("masking")
@Order(3)
public class MaskingNotifier implements Notifier {

    private static final Logger log =
            LoggerFactory.getLogger(MaskingNotifier.class);

    @PostConstruct
    public void init() {
        log.info("MASKING >> initialized");
    }

    @Override
    public String send(String message) {
        String masked = message.replaceAll("\\d", "*");
        log.info("MASKING >> {}", masked);
        return "masking: " + masked;
    }

    @Override
    public String channel() {
        return "masking";
    }
}