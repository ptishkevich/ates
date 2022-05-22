package com.ates.analytics.task;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TaskRepository extends CrudRepository<Task, Integer> {
    List<Task> findByPublicTaskId(String publicTaskId);
}
