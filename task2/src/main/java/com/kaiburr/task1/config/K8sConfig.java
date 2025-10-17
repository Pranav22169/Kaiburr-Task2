package com.kaiburr.task1.config;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a KubernetesClient bean.
 * When the app runs inside the cluster, it automatically uses the in-cluster config.
 * When running locally, it uses your ~/.kube/config.
 */
@Configuration
public class K8sConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }
}
