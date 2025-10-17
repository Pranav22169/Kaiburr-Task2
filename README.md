Kaiburr – Task 2 : Kubernetes Deployment and Pod Execution

This task was completed as a continuation of Task 1.
The application from Task 1, a Java Spring Boot REST API that manages “Task” objects and executions, was extended to run inside containers and then deployed to a Kubernetes cluster along with a MongoDB database.

The project directory used for this task was
C:\Users\prabi\OneDrive\Desktop\PranavBijuNair\Kaiburr\Task2\task2.
All source code from Task 1 was copied here so that Task 1 remains unaffected.

Dockerization

A Dockerfile was created in the root of the project.
It uses a multi-stage build: the first stage builds the Spring Boot application using Maven, and the second stage copies the packaged JAR file into a smaller runtime image based on Eclipse Temurin 17.
The image was built with the command:
docker build -t pranav/task2:latest .
The build completed successfully, producing the image pranav/task2:latest.
This verified that the Java application could be compiled, packaged, and run inside a Docker container.

Kubernetes Configuration

A set of Kubernetes YAML manifests was created inside a folder named k8s.
These include:

app-deployment.yaml – defines the deployment and NodePort service for the Spring Boot application.
The deployment uses one replica and exposes port 8080 through NodePort 30080.
The environment variable SPRING_DATA_MONGODB_URI points to the MongoDB service inside the cluster.
The deployment also specifies a custom service account named task-runner-sa.

mongo-statefulset.yaml – deploys MongoDB as a StatefulSet with one replica.
It uses a PersistentVolumeClaim so that database data is preserved even if the Mongo pod is deleted.

rbac.yaml – creates the service account task-runner-sa together with a Role and RoleBinding.
The role grants permission to create pods and retrieve pod logs, which is required for the TaskExecution feature.

After creating these manifests, they were applied to the cluster using:
kubectl apply -f k8s/

This successfully created all necessary resources: the namespace kaiburr, the service account, the application deployment and service, and the MongoDB StatefulSet and PVC.

Cluster Setup

Minikube was used to run a single-node local Kubernetes cluster with Docker as the driver.
The cluster was started with:
minikube start --driver=docker
Once running, the nodes and pods were checked using kubectl get nodes and kubectl get pods -n kaiburr.
Both the application pod and the MongoDB pod appeared in the Running state, showing that the deployment was successful.

Connecting the Application
The NodePort service for the application was exposed through Minikube and the URL retrieved with:
minikube service kaiburr-task2-service -n kaiburr --url

The REST API could then be accessed from the host machine through this URL.
A simple Invoke-RestMethod call to /tasks returned an empty list, confirming that the application and database were reachable.

Verifying MongoDB

MongoDB logs were viewed to confirm successful initialization.
The StatefulSet used a persistent volume, and the database continued to hold data even after the pod was recreated, meeting the persistence requirement.

Updating the TaskExecution Endpoint

The implementation of the “PUT TaskExecution” endpoint was modified.
Instead of running shell commands locally, the application now connects to the Kubernetes API and programmatically creates a short-lived pod using the BusyBox image.
The provided shell command is executed inside this pod, and the output is then read back and stored as a new TaskExecution entry in MongoDB.

inal Testing

A new task was created with the command echo Hello from busybox.
When the TaskExecution endpoint was called, Kubernetes scheduled a temporary pod named similar to task-runner-fef87d85.
Cluster events confirmed that the pod was scheduled, the BusyBox image was pulled, and the container started and completed successfully.

After the fix, querying the task again showed two executions in MongoDB – the earlier failed one and the new successful one that contained the output “Hello from busybox”.
This verified that the application could create pods, execute commands inside them, capture their output, and store the result in the database.


Summary:
The 2nd task begins with a Java Spring Boot REST API that models and manages Task objects. Each Task contains identifying fields, a shell command to run, and a list of TaskExecution records that capture when a command was run and what its output was. To prepare this application for deployment on Kubernetes, the application was containerized. A Dockerfile was created to produce a compact runtime image. The Dockerfile uses a build stage that compiles and packages the Spring Boot jar with Maven, followed by a runtime stage that copies the packaged jar into a small Java runtime image. Building that Dockerfile produces a local image that can be run or pushed to a registry.

Kubernetes manifests were written to describe the production deployment. The manifests include a Deployment and a Service for the Java application, and a StatefulSet with a PersistentVolumeClaim for MongoDB. The application Deployment is configured to expose port 8080 and the Service is exposed as a NodePort so the API is reachable from the host machine. The application pod is also given an environment variable that contains the MongoDB connection string, allowing the application to discover and connect to the Mongo service inside the cluster without hardcoding hostnames. The MongoDB StatefulSet mounts persistent storage through a PVC so that database files survive pod restarts and node reboots.

A local single-node Kubernetes cluster was used to host these resources. Minikube was selected for the cluster and started using the Docker driver. Minikube provides the Kubernetes control plane on the local machine and integrates with the same Docker daemon used to build the application image. The local Docker image is loaded into Minikube so that the Deployment can use the image without requiring a remote registry push. Applying the YAML manifests creates the namespace, the StatefulSet and PVC for MongoDB, the Deployment and Service for the application, and any required RBAC and service account resources.

Because the application needs to create short-lived pods at runtime to execute shell commands, the application was modified so that the TaskExecution endpoint does not run commands locally. Instead, when a request arrives to create a TaskExecution, the application programmatically calls the Kubernetes API, creates a pod definition based on a minimal busybox container, instructs that pod to run the requested command, waits for the pod to complete, reads the pod logs for the command output, and then stores the collected output inside a new TaskExecution record in MongoDB. To make this possible inside Kubernetes, a dedicated service account and corresponding RBAC role and role binding were created. The role grants the application permission to create pods and read pod logs in the application namespace. The application’s Deployment is configured to use that service account so the runtime code can talk to the Kubernetes API with the required privileges.

During verification the application and MongoDB pods were confirmed to be running inside the cluster. The service was exposed via NodePort and the host machine was able to reach the REST API using the URL returned by Minikube service. Creating a Task via the REST API and then calling the TaskExecution endpoint resulted in a new busybox pod being scheduled, the command being executed inside that pod, the pod logs being read by the application, and the resulting output being saved into MongoDB as part of the TaskExecution entry. The MongoDB StatefulSet used a persistent volume, and deleting the MongoDB pod and letting Kubernetes recreate it did not remove the stored records, proving that persistence is functioning as required.

The final result is an architecture where the Java REST API runs inside Kubernetes, MongoDB runs as a separate stateful pod with persistent storage, and the REST API can request dynamic command execution by creating short-lived pods through the Kubernetes API. The application sources, the Dockerfile, and all Kubernetes manifests are organized in the project directory so they can be applied to bring up the whole system reproducibly. The deployment is reachable from the host, the application reads MongoDB connection details from environment variables, and TaskExecution records are produced by running commands inside ephemeral busybox pods and saving the output in MongoDB.
