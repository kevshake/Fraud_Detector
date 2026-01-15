package com.posgateway.aml.config;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AerospikeConfig {

    @Value("${aerospike.hosts:localhost:3000}")
    private String aerospikeHosts;

    @Value("${aerospike.namespace:test}")
    private String namespace;

    @Bean(destroyMethod = "close")
    public AerospikeClient aerospikeClient() {
        ClientPolicy policy = new ClientPolicy();
        policy.failIfNotConnected = false; // Allow startup even if Aerospike is down (for testing)

        // Parse hosts (format: host:port,host:port)
        String[] hostStrings = aerospikeHosts.split(",");
        Host[] hosts = new Host[hostStrings.length];

        for (int i = 0; i < hostStrings.length; i++) {
            String[] parts = hostStrings[i].split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 3000;
            hosts[i] = new Host(host, port);
        }

        return new AerospikeClient(policy, hosts);
    }

    public String getNamespace() {
        return namespace;
    }
}
