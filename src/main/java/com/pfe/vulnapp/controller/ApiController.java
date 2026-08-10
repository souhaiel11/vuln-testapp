package com.pfe.vulnapp.controller;

import com.pfe.vulnapp.service.CryptoService;
import com.pfe.vulnapp.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * ENDPOINTS HTTP — cible ZAP (DAST) + injection SQL atteignable (Sonar+ZAP)
 * ============================================================================
 * Ces endpoints sont exposes SANS authentification. ZAP scanne l'application
 * EN FONCTIONNEMENT -> l'appli doit reellement demarrer (H2 en memoire).
 * ============================================================================
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CryptoService cryptoService;
    private final DataService dataService;

    public ApiController(CryptoService cryptoService, DataService dataService) {
        this.cryptoService = cryptoService;
        this.dataService = dataService;
    }

    // BUG ZAP Z1 — XSS reflechi : entree renvoyee sans echappement dans du HTML.
    @GetMapping(value = "/greet", produces = "text/html")
    public String greet(@RequestParam(defaultValue = "visiteur") String name) {
        return "<html><body><h1>Bonjour " + name + "</h1></body></html>";
    }

    // BUG Z2 + Sonar CRITICAL : injection SQL atteignable depuis le web.
    // Corrigeable par WF2 SI ce fichier/DataService est le fichier cible.
    @GetMapping("/user")
    public List<String> getUser(@RequestParam String username) {
        return dataService.findUser(username);
    }

    // BUG Z3 — endpoint sensible sans auth : divulgation d'information.
    @GetMapping("/debug/info")
    public String debugInfo() {
        return "OS: " + System.getProperty("os.name")
                + " | Java: " + System.getProperty("java.version")
                + " | User: " + System.getProperty("user.name");
    }

    // Utilise CryptoService (groupe A) — rend les bugs A atteignables/reels.
    @GetMapping("/token")
    public String token() {
        return cryptoService.generateToken();
    }

    // Endpoint sain — health check pour confirmer que l'appli tourne.
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
