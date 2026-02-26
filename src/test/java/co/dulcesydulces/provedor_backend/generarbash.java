package co.dulcesydulces.provedor_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class generarbash {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("admi26"));
    }
}
