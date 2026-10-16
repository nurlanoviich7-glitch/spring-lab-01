package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Locale;

@ControllerAdvice(assignableTypes = BookPageController.class)
public class PageExceptionHandler {
    private final MessageSource messages;
    private final String owner;

    public PageExceptionHandler(MessageSource messages, @Value("${app.owner:}") String owner) {
        this.messages = messages;
        this.owner = owner;
    }

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(BookNotFoundException exception, Model model, Locale locale) {
        model.addAttribute("bookId", exception.getBookId());
        model.addAttribute("title", messages.getMessage("book.notFound.title", null, locale));
        model.addAttribute("message", messages.getMessage("book.notFound",
                new Object[]{exception.getBookId()}, locale));
        model.addAttribute("owner", owner);
        return "error/not-found";
    }
}
