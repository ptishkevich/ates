package com.ates.billing.account;

import javax.persistence.*;
import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "account_id")
    private int accountId;
    private int profileId;
    @OneToMany(orphanRemoval=true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="account_id")
    private List<AccountLogRecord> auditLog;

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public List<AccountLogRecord> getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(List<AccountLogRecord> auditLog) {
        this.auditLog = auditLog;
    }

    public void addAuditLogRecord(AccountLogRecord record) {
        this.auditLog.add(record);
    }
}
