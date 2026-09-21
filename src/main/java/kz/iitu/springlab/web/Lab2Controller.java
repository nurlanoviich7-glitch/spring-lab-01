package kz.iitu.springlab.web;

import kz.iitu.springlab.lifecycle.LifecycleDemo;
import kz.iitu.springlab.notify.Notifier;
import kz.iitu.springlab.notify.NotificationService;
import kz.iitu.springlab.scope.Ticket;
import kz.iitu.springlab.scope.TicketOffice;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab2")
public class Lab2Controller {

    private final NotificationService notifications;
    private final LifecycleDemo lifecycle;
    private final Ticket ticket;
    private final TicketOffice office;
    private final Notifier masking;

    public Lab2Controller(
            NotificationService notifications,
            LifecycleDemo lifecycle,
            Ticket ticket,
            TicketOffice office,
            @Qualifier("masking") Notifier masking) {

        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.ticket = ticket;
        this.office = office;
        this.masking = masking;
    }

    @GetMapping("/notify")
    public Map<String, Object> notify(
            @RequestParam(defaultValue = "Hello") String text) {

        return Map.of(
                "primary", notifications.viaPrimary(text),
                "console", notifications.viaConsole(text),
                "all", notifications.viaAll(text),
                "beanNames", notifications.names()
        );
    }

    @GetMapping("/lifecycle")
    public List<String> lifecycle() {
        return lifecycle.events();
    }

    @GetMapping("/scopes")
    public Map<String, String> scopes() {
        return Map.of(
                "injectedDirectly", ticket.id(),
                "viaProvider", office.viaProvider(),
                "office", office.injectedDirectly()
        );
    }

    @GetMapping("/custom")
    public String custom(
            @RequestParam(defaultValue = "Hello 123") String text) {
        return masking.send(text);
    }
}