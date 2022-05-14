package com.ates.tasks;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
public class TasksApplication {

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
	public void listen(@Payload ConsumerRecord<String, DynamicMessage> data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) throws InvalidProtocolBufferException {
		System.out.println("############## Received message key= " + key + " , value= " + data);
		DynamicMessage message = data.value();
		String messageType = message.getDescriptorForType().getFullName();

		if ("profile.Created".equals(messageType)) {
			handleProfileCreated(message);
		}
		else if ("profile.RoleUpdated".equals(messageType)) {
			handleProfileRoleUpdated(message);
		}
	}

	private void handleProfileCreated(DynamicMessage message) throws InvalidProtocolBufferException {
		com.ates.messages.Profile.Created profileCreatedMsg = com.ates.messages.Profile.Created
				.newBuilder()
				.build()
				.getParserForType()
				.parseFrom(message.toByteArray());
		UUID profileId = UUID.fromString(profileCreatedMsg.getPublicId());
		Profile profile = new Profile();
		profile.setId(profileId);
		profile.setName(profileCreatedMsg.getName());
		profile.setEmail(profileCreatedMsg.getEmail());

		profileRepository.save(profile);
	}

	private void handleProfileRoleUpdated(DynamicMessage message) throws InvalidProtocolBufferException {
		com.ates.messages.Profile.RoleUpdated roleUpdatedMsg = com.ates.messages.Profile.RoleUpdated
				.newBuilder()
				.build()
				.getParserForType()
				.parseFrom(message.toByteArray());
		UUID profileId = UUID.fromString(roleUpdatedMsg.getPublicId());
		Optional<Profile> profileForUpdate = profileRepository.findById(profileId);
		if (profileForUpdate.isPresent()) {
			Profile profile = profileForUpdate.get();
			profile.setRole(roleUpdatedMsg.getRole().name());

			profileRepository.save(profile);
		}
	}
}
