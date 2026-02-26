package co.dulcesydulces.provedor_backend.domain.dto;

public class LoginResponse {
    private boolean ok;
    private String codigo;
    private String nombreUsuario;
    private String rol;
    private String message;

    public static LoginResponse ok(String codigo, String nombreUsuario, String rol) {
        LoginResponse r = new LoginResponse();
        r.ok = true;
        r.codigo = codigo;
        r.nombreUsuario = nombreUsuario;
        r.rol = rol;
        r.message = "Login OK";
        return r;
    }

    public static LoginResponse fail(String message) {
        LoginResponse r = new LoginResponse();
        r.ok = false;
        r.message = message;
        return r;
    }

    public boolean isOk() { return ok; }
    public String getCodigo() { return codigo; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getRol() { return rol; }
    public String getMessage() { return message; }
}
