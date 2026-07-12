package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
}
