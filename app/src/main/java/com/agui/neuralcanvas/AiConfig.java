package com.agui.neuralcanvas;

public class AiConfig {
    private String baseUrl;
    private String apiKey;
    private String model;
    private boolean enabled;

    public AiConfig() {
        this.baseUrl = "";
        this.apiKey = "";
        this.model = "";
        this.enabled = false;
    }

    public String getBaseUrl() {
        return baseUrl == null ? "" : baseUrl.trim();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getModel() {
        return model == null ? "" : model.trim();
    }

    public void setModel(String model) {
        this.model = model == null ? "" : model.trim();
    }

    public boolean isEnabled() {
        return enabled && !getBaseUrl().isEmpty() && !getApiKey().isEmpty() && !getModel().isEmpty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
