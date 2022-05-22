package com.ates.billing.task;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private UUID publicId;
    private int debitAmount;
    private int creditAmount;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public int getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(int debitAmount) {
        this.debitAmount = debitAmount;
    }

    public int getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(int creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
