package com.kaiburr.task1.controller;

import com.kaiburr.task1.model.Task;
import com.kaiburr.task1.model.TaskExecution;
import com.kaiburr.task1.repository.TaskRepository;
import com.kaiburr.task1.service.TaskExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    // ✅ Get all tasks or one by ID
    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(required = false) String id) {
        if (id != null) {
            Optional<Task> task = taskRepository.findById(id);
            return task.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found"));
        }
        return ResponseEntity.ok(taskRepository.findAll());
    }

    // ✅ Create or update a task
    @PutMapping
    public ResponseEntity<?> createOrUpdateTask(@RequestBody Task task) {
        if (task.getCommand() == null || task.getCommand().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Command cannot be empty");
        }
        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

    // ✅ Delete a task by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return ResponseEntity.ok("Deleted task with id: " + id);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }
    }

    // ✅ Search tasks by name
    @GetMapping("/search")
    public ResponseEntity<?> searchByName(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No matching tasks found");
        }
        return ResponseEntity.ok(tasks);
    }

    // ✅ Execute a command INSIDE a Kubernetes pod
    @PutMapping("/{id}/executions")
    public ResponseEntity<?> executeTaskInK8s(@PathVariable String id) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }

        Task task = optionalTask.get();

        try {
            // Use the service to run the command inside a Kubernetes pod
            TaskExecution execution = taskExecutionService.runCommandInK8sPod(task);
            return ResponseEntity.ok(execution);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing task in Kubernetes: " + e.getMessage());
        }
    }
}
