package com.pfe.vulnapp.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * INJECTION SQL — detectee par Sonar (CRITICAL, S3649) ET ZAP (DAST)
 * ============================================================================
 * Corrigeable par WF2 (CRITICAL, type VULNERABILITY, non-BLOCKER) : la
 * correction attendue est une requete parametree (PreparedStatement).
 * ============================================================================
 */
@Service
public class DataService {

    // BUG — CRITICAL : injection SQL par concatenation directe de l'entree.
    public List<String> findUser(String username) {
        List<String> results = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
            Statement stmt = conn.createStatement();
            String query = "SELECT name FROM users WHERE username = '" + username + "'";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            results.add("erreur");
        }
        return results;
    }
}
