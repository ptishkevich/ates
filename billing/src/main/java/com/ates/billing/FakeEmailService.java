package com.ates.billing;

import com.ates.billing.profile.Profile;
import org.springframework.stereotype.Component;

@Component
public class FakeEmailService {

    public void sendPayoutEmail(Profile profile, int amount) {
        String email = profile.getEmail();
        if(email != null) {
            System.out.println("###################################");
            System.out.println("####### Sending email to " + email);
            System.out.println("Good day, " + profile.getName());
            System.out.println("You've been payed " + amount + " SEEDs today!");
            System.out.println("###################################");
        }
    }
}
