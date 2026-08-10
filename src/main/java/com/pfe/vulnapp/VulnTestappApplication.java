package com.pfe.vulnapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application de test DevSecOps — vulnerabilites DELIBEREES.
 * Concue pour valider la chaine complete de la plateforme :
 * detection (4 scanners) -> analyse (agents IA) -> decision (Judge)
 * -> correction (WF2/WF4/WF5). NE JAMAIS deployer en production.
 */
@SpringBootApplication
public class VulnTestappApplication {
    public static void main(String[] args) {
        SpringApplication.run(VulnTestappApplication.class, args);
    }
}
