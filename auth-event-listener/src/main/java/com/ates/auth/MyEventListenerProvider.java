package com.ates.auth;

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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.util.Properties;

public class MyEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(MyEventListenerProvider.class);
    private static final String PROFILE_TOPIC_NAME = "profile";
    private final KeycloakSession session;
    private final RealmProvider model;
    private final Producer<String, String> producer;

    public MyEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
        Thread.currentThread().setContextClassLoader(null);
        this.producer = new KafkaProducer<>(getProducerProps());
    }

    private Properties getProducerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
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
            producer.send(new ProducerRecord<>(PROFILE_TOPIC_NAME, userId, event.getRepresentation()));
        }
    }

    private String getUserIdFromResourcePath(String resourcePath) {
         return resourcePath.split("/")[1];
    }

    @Override
    public void close() {
        producer.close();
    }
}
