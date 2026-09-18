package kz.iitu.springlab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(
            @RequestParam(defaultValue = "world") String name) {

        return new Greeting(
                "Hello, " + name + "!",
                owner,
                LocalDateTime.now()
        );
    }

    @GetMapping("/info")
    public Info info() {

        return new Info(
                owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors()
        );
    }

    @GetMapping("/stats")
    public Stats stats(@RequestParam String numbers) {

        String[] values = numbers.split(",");

        double min = Double.parseDouble(values[0].trim());
        double max = Double.parseDouble(values[0].trim());
        double sum = 0;

        for (String value : values) {
            double number = Double.parseDouble(value.trim());

            if (number < min) {
                min = number;
            }

            if (number > max) {
                max = number;
            }

            sum += number;
        }

        double average = sum / values.length;

        return new Stats(min, max, average);
    }

    public record Greeting(
            String message,
            String owner,
            LocalDateTime timestamp) {
    }

    public record Info(
            String owner,
            String javaVersion,
            int cpuCores) {
    }

    public record Stats(
            double min,
            double max,
            double average) {
    }
}