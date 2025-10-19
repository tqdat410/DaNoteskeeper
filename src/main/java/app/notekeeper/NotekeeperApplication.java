package app.notekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NotekeeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotekeeperApplication.class, args);
	}

}
