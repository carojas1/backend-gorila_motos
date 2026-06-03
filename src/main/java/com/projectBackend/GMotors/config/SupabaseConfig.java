package com.projectBackend.GMotors.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import okhttp3.OkHttpClient;

@Configuration
public class SupabaseConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public String supabaseUrl() {
        return supabaseUrl;
    }

    @Bean
    public String supabaseAnonKey() {
        return supabaseAnonKey;
    }

    @Bean
    public String serviceRoleKey() {
        return serviceRoleKey;
    }
}