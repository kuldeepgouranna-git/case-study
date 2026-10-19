package com.nimbus.reeferguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReeferGuardApplication {
    public static void main(String[] args) { SpringApplication.run(ReeferGuardApplication.class, args); }
}
