package com.ates.auth;

import com.ates.messages.Profile;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MyEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(MyEventListenerProvider.class);
    private static final String PROFILE_TOPIC_NAME = "profile-stream";
    private final KeycloakSession session;
    private final RealmProvider model;
    private final Producer<String, Profile.Created> createdProducer;
    private final Producer<String, Profile.RoleUpdated> roleUpdatedProducer;


    public MyEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
        Thread.currentThread().setContextClassLoader(null);
        Properties producerProps = getProducerProps();
        this.createdProducer = new KafkaProducer<>(producerProps);
        this.roleUpdatedProducer = new KafkaProducer<>(producerProps);
    }

    private Properties getProducerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker:29092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer");
        props.put("schema.registry.url", "http://schema-registry:8081");
        props.put("auto.register.schemas", "true");
//        props.put("auto.create.topics.enable", true);
        return props;
    }

    @Override
    public void onEvent(Event event) {
        log.infof("## NEW %s EVENT", event.getType());
        log.infof("realm: %s, userId: %s, clientId: %s", event.getRealmId(), event.getUserId(), event.getClientId());

        if (EventType.REGISTER.equals(event.getType())) {
            log.info("-----------------------------------------------------------");

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel u = this.session.users().getUserById(event.getUserId(), realm);

            log.infof("User: %s, %s, %s, %s", u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
            log.info("-----------------------------------------------------------");
        }

    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        log.infof("## NEW ADMIN EVENT, %s -> %s, id: %s", event.getOperationType(), event.getResourceType(), event.getId());
        log.infof("representation: %s", event.getRepresentation());

        if (OperationType.CREATE == event.getOperationType() && ResourceType.USER == event.getResourceType()) {
            String userId = getUserIdFromResourcePath(event.getResourcePath());
            UserModel user = getUser(userId, event);
            Profile.Created message = Profile.Created
                    .newBuilder()
                    .setName(user.getUsername())
                    .setEmail(user.getEmail())
                    .setPublicId(userId)
                    .build();
            createdProducer.send(new ProducerRecord<>(PROFILE_TOPIC_NAME, userId, message));
        }

        if (OperationType.CREATE == event.getOperationType() && ResourceType.REALM_ROLE_MAPPING == event.getResourceType()) {
            String userId = getUserIdFromResourcePath(event.getResourcePath());
            UserModel user = getUser(userId, event);
            Profile.Role userRole = getUserRole(user);

            Profile.RoleUpdated message = Profile.RoleUpdated
                    .newBuilder()
                    .setPublicId(userId)
                    .setRole(userRole)
                    .build();

            roleUpdatedProducer.send(new ProducerRecord<>(PROFILE_TOPIC_NAME, userId, message));
        }
    }

    private Profile.Role getUserRole(UserModel user) {
        List<String> userRoleNames = user
                .getRealmRoleMappingsStream()
                .map(RoleModel::getName)
                .collect(Collectors.toList());
        for (Profile.Role value : Profile.Role.values()) {
            if (userRoleNames.contains(value.name())) {
                return value;
            }
        }

        return null;
    }

    private UserModel getUser(String userId, AdminEvent event) {
        RealmModel realm = this.model.getRealm(event.getRealmId());
        return this.session.users().getUserById(userId, realm);
    }
    
    private String getUserIdFromResourcePath(String resourcePath) {
         return resourcePath.split("/")[1];
    }

    @Override
    public void close() {
        createdProducer.close();
        roleUpdatedProducer.close();
    }
}
