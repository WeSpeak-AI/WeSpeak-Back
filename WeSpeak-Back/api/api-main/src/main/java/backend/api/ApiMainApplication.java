package backend.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "backend")
public class ApiMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiMainApplication.class, args);
    }
}
