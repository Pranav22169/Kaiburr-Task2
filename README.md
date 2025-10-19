
---

# Kaiburr Task 2 – Kubernetes Deployment and Pod Execution

This task was completed as a continuation of **Task 1**.
The application from Task 1 — a Java Spring Boot REST API that manages *Task* objects and executions — was extended to run inside Docker containers and then deployed to a Kubernetes cluster along with a MongoDB database.

---

##  Overview

The goal of this task was to:

1. Containerize the Java Spring Boot application using Docker.
2. Deploy the application and MongoDB to a Kubernetes cluster.
3. Enable the application to execute shell commands inside short-lived Kubernetes pods using the BusyBox image.
4. Implement persistence for MongoDB using a StatefulSet and PVC.

---

##  Project Directory

All files for this task were placed in the following path:

```
C:\Users\prabi\OneDrive\Desktop\PranavBijuNair\Kaiburr\Task2\task2
```

All source code from Task 1 was copied here to ensure the original project remained unaffected.

---

##  Dockerization

A **Dockerfile** was created in the project root using a **multi-stage build** approach:

1. **Build Stage:**
   Uses a Maven container to build and package the Spring Boot JAR.

2. **Runtime Stage:**
   Copies the packaged JAR into a lightweight Eclipse Temurin 17 image and exposes port 8080.

**Build Command:**

```bash
docker build -t pranav/task2:latest .
```

The image built successfully, producing:

```
pranav/task2:latest
```

This verified that the Java application can compile, package, and run inside a container.

---

##  Kubernetes Configuration

All Kubernetes manifests are located inside the `k8s` folder.
These YAML files define the deployment of the application and MongoDB, as well as RBAC permissions.

| File                       | Description                                                                                                                                                                                                                                                                        |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **app-deployment.yaml**    | Defines the Deployment and NodePort Service for the Spring Boot application. Uses one replica and exposes port 8080 via NodePort 30080. The environment variable `SPRING_DATA_MONGODB_URI` points to the internal MongoDB service. Uses a custom service account `task-runner-sa`. |
| **mongo-statefulset.yaml** | Deploys MongoDB as a StatefulSet with one replica. Includes a PersistentVolumeClaim for data persistence.                                                                                                                                                                          |
| **rbac.yaml**              | Creates a ServiceAccount (`task-runner-sa`) and assigns a Role/RoleBinding allowing pod creation and log retrieval — required for TaskExecution.                                                                                                                                   |

**Apply Manifests:**

```bash
kubectl apply -f k8s/
```

This created:

* Namespace `kaiburr`
* Service account and RBAC resources
* Application Deployment and Service
* MongoDB StatefulSet and PVC

---

##  Cluster Setup

A local **Minikube** cluster was used with Docker as the driver.

**Start Minikube:**

```bash
minikube start --driver=docker
```

**Verify Cluster Resources:**

```bash
kubectl get nodes
kubectl get pods -n kaiburr
```

Both the application pod and MongoDB pod reached the **Running** state, confirming successful deployment.

---

##  Accessing the Application

The NodePort service was exposed using:

```bash
minikube service kaiburr-task2-service -n kaiburr --url
```

The REST API was accessible from the host through the returned URL.
A `GET /tasks` request returned an empty list, confirming application and database connectivity.

---

##  Verifying MongoDB

MongoDB logs confirmed successful initialization.
The StatefulSet used a **persistent volume**, allowing data to remain intact even after pod restarts — satisfying the persistence requirement.

---

##  Updating the TaskExecution Endpoint

The `PUT /tasks/{id}/executions` endpoint was enhanced to integrate with the **Kubernetes API**.

* When triggered, the application now creates a **short-lived BusyBox pod** programmatically.
* The given shell command executes inside that pod.
* Output logs are retrieved and saved as a new `TaskExecution` entry in MongoDB.
* The process uses the `task-runner-sa` service account with appropriate RBAC permissions.

---

##  Final Testing

A new task was created with the command:

```bash
echo Hello from busybox
```

When the TaskExecution endpoint was invoked:

* Kubernetes scheduled a temporary pod (e.g., `task-runner-fef87d85`).
* The BusyBox image executed successfully.
* Logs showed the expected output.

MongoDB displayed two execution entries — the original failed one and a new successful entry containing:

```
"output": "Hello from busybox"
```

This confirmed that the application could create pods, execute commands inside them, capture logs, and persist results in MongoDB.

---

##  Summary

| Aspect               | Description                                                    |
| -------------------- | -------------------------------------------------------------- |
| **Base Application** | Java Spring Boot REST API managing Task objects and executions |
| **Containerization** | Multi-stage Dockerfile (Maven → Temurin 17)                    |
| **Deployment**       | Kubernetes Deployment, Service, StatefulSet, PVC, and RBAC     |
| **Database**         | MongoDB deployed as a StatefulSet with persistent storage      |
| **Cluster**          | Minikube (Docker driver)                                       |
| **Pod Execution**    | Dynamic BusyBox pods created at runtime for TaskExecution      |
| **Persistence**      | MongoDB data retained after restarts                           |
| **Access**           | API reachable via NodePort through Minikube service URL        |

---

## Author

**Pranav Biju Nair**
*October 2025*

---
