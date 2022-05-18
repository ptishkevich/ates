package com.ates.billing.account;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private long beginningTimestamp;
    private long endTimestamp;
    private Status status;

    BillingCycle() {}

    public BillingCycle(long beginningTimestamp, long endTimestamp, Status status) {
        this.beginningTimestamp = beginningTimestamp;
        this.endTimestamp = endTimestamp;
        this.status = status;
    }

    enum Status { OPEN, CLOSED}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBeginningTimestamp() {
        return beginningTimestamp;
    }

    public void setBeginningTimestamp(long beginningTimestamp) {
        this.beginningTimestamp = beginningTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
