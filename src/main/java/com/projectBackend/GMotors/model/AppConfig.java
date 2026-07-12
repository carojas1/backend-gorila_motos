package com.projectBackend.GMotors.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_config")
public class AppConfig {

    @Id
    @Column(name = "config_key", length = 80)
    private String key;

    @Column(name = "config_value", length = 255, nullable = false)
    private String value;

    public AppConfig() {}

    public AppConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
