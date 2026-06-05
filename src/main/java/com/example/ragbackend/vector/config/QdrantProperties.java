package com.example.ragbackend.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qdrant")
public class QdrantProperties {

    private String host = "localhost";
    private int httpPort = 6333;
    private String collectionName = "rag_chunks";
    private int vectorSize = 384;
    private String distance = "COSINE";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public void setVectorSize(int vectorSize) {
        this.vectorSize = vectorSize;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String resolveBaseUrl() {
        String normalizedHost = host == null || host.isBlank() ? "localhost" : host.trim();
        String hostWithScheme = normalizedHost.startsWith("http://") || normalizedHost.startsWith("https://")
                ? normalizedHost
                : "http://" + normalizedHost;
        if (hostWithScheme.matches(".*:\\d+$")) {
            return hostWithScheme;
        }
        return hostWithScheme + ":" + httpPort;
    }

    public String resolveDistance() {
        String normalizedDistance = distance == null ? "COSINE" : distance.trim().toUpperCase();
        return switch (normalizedDistance) {
            case "COSINE" -> "Cosine";
            case "EUCLID" -> "Euclid";
            case "DOT" -> "Dot";
            case "MANHATTAN" -> "Manhattan";
            default -> normalizedDistance;
        };
    }
}
