package com.example.demo_pam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
// Habilita las tareas @Scheduled (evaluación periódica de stock bajo)
@EnableScheduling
@RestController
public class DemoPamApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoPamApplication.class, args);
	}

}
