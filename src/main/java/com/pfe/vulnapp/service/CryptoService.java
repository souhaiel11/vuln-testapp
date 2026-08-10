package com.pfe.vulnapp.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Random;

/**
 * ============================================================================
 * GROUPE A — BUGS QUE LA PLATEFORME DOIT CORRIGER PAR PR (WF2)
 * ============================================================================
 * Ce fichier CONCENTRE volontairement plusieurs findings SonarQube de
 * severite CRITICAL/MAJOR, de type BUG/VULNERABILITY/CODE_SMELL, NON-BLOCKER
 * et corrigeables dans un seul fichier.
 *
 * Objectif : d'apres l'analyse du code de WF2, le workflow patche "le fichier
 * avec le plus de findings CRITICAL/MAJOR". En concentrant 3 bugs corrigeables
 * ici, on GARANTIT que ce fichier soit celui selectionne -> une vraie PR de
 * correction doit etre generee sur ce fichier.
 *
 * Correction attendue de la PR : MD5->SHA-256, Random->SecureRandom,
 * catch vide -> log de l'exception.
 * ============================================================================
 */
@Service
public class CryptoService {

    // ---- BUG A1 — CRITICAL : hachage MD5, algorithme casse (regle S4790) ----
    // WF2 doit proposer SHA-256 dans la PR.
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // ---- BUG A2 — MAJOR : bloc catch vide, exception avalee (S2486) ----
            // WF2 doit proposer de logger l'exception dans la PR.
            return null;
        }
    }

    // ---- BUG A3 — CRITICAL : generateur pseudo-aleatoire previsible pour
    // un usage de securite (token) — regle S2245. WF2 doit proposer
    // SecureRandom dans la PR. ----
    public String generateToken() {
        Random random = new Random();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            token.append(Integer.toHexString(random.nextInt(16)));
        }
        return token.toString();
    }

    // ---- BUG A4 — MAJOR : comparaison de hash sensible au timing
    // (utilise equals au lieu d'une comparaison constante). Corrigeable. ----
    public boolean verifyHash(String provided, String stored) {
        String computed = hashPassword(provided);
        return computed != null && computed.equals(stored);
    }
}
