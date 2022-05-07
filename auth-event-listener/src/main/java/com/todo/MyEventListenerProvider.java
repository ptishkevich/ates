package com.todo;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

public class MyEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(MyEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public MyEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        if(EventType.LOGIN.equals(event.getType()) || EventType.CLIENT_LOGIN.equals(event.getType())) {
            log.infof("## NEW %s EVENT", event.getType());
            log.infof("realm: %s, userId: %s, clientId: %s",  event.getRealmId(), event.getUserId(), event.getClientId());
        }

        if (EventType.REGISTER.equals(event.getType())) {
            log.infof("## NEW %s EVENT", event.getType());
            log.info("-----------------------------------------------------------");

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel u = this.session.users().getUserById(event.getUserId(), realm);

            log.infof("User: %s, %s, %s, %s", u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
            log.info("-----------------------------------------------------------");
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {

    }

    @Override
    public void close() {

    }
}
