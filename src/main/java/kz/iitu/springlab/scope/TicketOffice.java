package kz.iitu.springlab.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TicketOffice {

    private final Ticket ticket;

    private final ObjectProvider<Ticket> provider;

    public TicketOffice(
            Ticket ticket,
            ObjectProvider<Ticket> provider) {
        this.ticket = ticket;
        this.provider = provider;
    }

    public String injectedDirectly() {
        return ticket.id();
    }

    public String viaProvider() {
        return provider.getObject().id();
    }
}