
package co.dulcesydulces.provedor_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHash {
    public static void main(String[] args) {
        String password = "987654";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("Hash para '987654': " + hash);
    }
}