package com.ates.tasks;

import com.google.gson.Gson;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.UUID;

@SpringBootApplication
public class TasksApplication {

	Gson gson = new Gson();
	@Autowired
	ProfileRepository profileRepository;

	public static void main(String[] args) {
		SpringApplication.run(TasksApplication.class, args);
	}

	@Bean
	public KeycloakConfigResolver keycloakConfigResolver() {
		return new KeycloakSpringBootConfigResolver();
	}

	@KafkaListener(topics = "profile-stream", groupId = "foo") //id = "myId",
	public void listen(@Payload String data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
		System.out.println("############## Received message key= " + key + " , value= " + data);
		Profile profile = gson.fromJson(data, Profile.class);
		UUID profileId = UUID.fromString(key);
		profile.setId(profileId);

		profileRepository.save(profile);
	}
}
