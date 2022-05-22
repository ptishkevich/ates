package com.ates.analytics;

import com.ates.analytics.task.Task;
import com.ates.analytics.task.TaskBillingOperation;
import com.ates.analytics.task.TaskBillingOperationRepository;
import com.ates.analytics.task.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class AnalyticsLogic {
    @Autowired
    TaskBillingOperationRepository taskBillingOperationRepository;
    @Autowired
    TaskRepository taskRepository;

    public int calculateRevenueForToday() {
        LocalDate today = LocalDate.now();
        long todayBegin = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long todayEnd = today.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        return StreamSupport
                .stream(taskBillingOperationRepository.findAll().spliterator(), false)
                .filter(operation -> operation.getPerformedAt() > todayBegin && operation.getPerformedAt() < todayEnd)
                .map(this::getSignedAmount)
                .reduce(0, Integer::sum);
    }

    public Optional<Task> getTopPricedTaskForToday() {
        LocalDate today = LocalDate.now();
        long todayBegin = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long todayEnd = today.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        return StreamSupport
                .stream(taskRepository.findAll().spliterator(), false)
                .filter(task -> task.getCompletedAt() > todayBegin && task.getCompletedAt() < todayEnd)
                .max(Comparator.comparingInt(Task::getPrice));
    }

    public long numberOfEmployeesWithNegativeBalanceForToday() {
        LocalDate today = LocalDate.now();
        long todayBegin = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long todayEnd = today.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        Collection<List<TaskBillingOperation>> transactionsPerAccount = StreamSupport
                .stream(taskBillingOperationRepository.findAll().spliterator(), false)
                .filter(operation -> operation.getPerformedAt() > todayBegin && operation.getPerformedAt() < todayEnd)
                .collect(Collectors.groupingBy(TaskBillingOperation::getAccountOwnerPublicId))
                .values();

        return transactionsPerAccount
                .stream()
                .map(taskBillingOperations -> taskBillingOperations
                        .stream()
                        .map(this::getSignedAmount)
                        .reduce(0, Integer::sum)
                )
                .filter(balance -> balance < 0)
                .count();
    }

    private int getSignedAmount(TaskBillingOperation taskBillingOperation) {
        if (TaskBillingOperation.OperationType.PAYMENT == taskBillingOperation.getOperationType()) {
            return taskBillingOperation.getAmount();
        } else if (TaskBillingOperation.OperationType.FEE == taskBillingOperation.getOperationType()) {
            return -taskBillingOperation.getAmount();
        }
        return 0;
    }
}