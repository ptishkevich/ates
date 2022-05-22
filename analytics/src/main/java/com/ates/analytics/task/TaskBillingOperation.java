package com.ates.analytics.task;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TaskBillingOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private int taskId;
    private int amount;
    private OperationType operationType;
    private String accountOwnerPublicId;
    @ColumnDefault(value = "0")
    private long performedAt;

    public enum OperationType {PAYMENT, FEE}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getAccountOwnerPublicId() {
        return accountOwnerPublicId;
    }

    public void setAccountOwnerPublicId(String accountOwnerPublicId) {
        this.accountOwnerPublicId = accountOwnerPublicId;
    }

    public long getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(long performedAt) {
        this.performedAt = performedAt;
    }
}
