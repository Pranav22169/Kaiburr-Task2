package com.kaiburr.task1.service;

import com.kaiburr.task1.model.Task;
import com.kaiburr.task1.model.TaskExecution;
import com.kaiburr.task1.repository.TaskRepository;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TaskExecutionService {

    @Autowired
    private KubernetesClient k8s;

    @Autowired
    private TaskRepository taskRepository;

    // This namespace must match what we'll use in the Kubernetes manifests
    private final String namespace = "kaiburr";

    public TaskExecution runCommandInK8sPod(Task task) {
        String command = task.getCommand();
        String podName = "task-runner-" + UUID.randomUUID().toString().substring(0, 8);

        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(podName)
                    .addToLabels("job", "task-runner")
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("runner")
                        .withImage("busybox:1.36")
                        .withCommand("sh", "-c", command)
                        .withTty(false)
                    .endContainer()
                    .withRestartPolicy("Never")
                .endSpec()
                .build();

        Instant start = Instant.now();
        k8s.pods().inNamespace(namespace).create(pod);

        // Wait until pod finishes
        String phase = "";
        for (int i = 0; i < 120; i++) { // timeout after 2 mins
            Pod p = k8s.pods().inNamespace(namespace).withName(podName).get();
            if (p != null && p.getStatus() != null && p.getStatus().getPhase() != null) {
                phase = p.getStatus().getPhase();
                if ("Succeeded".equals(phase) || "Failed".equals(phase)) {
                    break;
                }
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Read pod logs
        String logs = "";
        try (LogWatch lw = k8s.pods().inNamespace(namespace).withName(podName).watchLog()) {
            InputStream is = lw.getOutput();
            if (is != null) {
                byte[] all = is.readAllBytes();
                logs = new String(all, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            logs = "Error fetching logs: " + ex.getMessage();
        }

        Instant end = Instant.now();

        // Create and save a TaskExecution entry
        TaskExecution exec = new TaskExecution();
        exec.setStartTime(start);
        exec.setEndTime(end);

        exec.setOutput(logs);

        task.getTaskExecutions().add(exec);
        taskRepository.save(task);

        // Cleanup pod after completion
        try {
            k8s.pods().inNamespace(namespace).withName(podName).delete();
        } catch (Exception ignored) { }

        return exec;
    }
}
