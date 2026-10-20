package ar.com.zyasoc.sighs.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza la apertura y cierre de conexiones JDBC con MySQL.
 *
 * Configuracion por variables de entorno (sobrescriben los defaults):
 *   SIGHS_DB_HOST    default: 127.0.0.1
 *   SIGHS_DB_PORT    default: 3306
 *   SIGHS_DB_NAME    default: sighs_db
 *   SIGHS_DB_USER    default: root
 *   SIGHS_DB_PASS    default: pipo14
 *
 * Para TP3 no usamos TLS (es MySQL local en docker); en el TP4 cuando se
 * despliegue en la oficina se agrega useSSL=true y el truststore.
 */
public final class ConexionBD {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_NAME = "sighs_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "pipo14";

    private static final String HOST = System.getenv().getOrDefault("SIGHS_DB_HOST", DEFAULT_HOST);
    private static final String PORT = System.getenv().getOrDefault("SIGHS_DB_PORT", DEFAULT_PORT);
    private static final String NAME = System.getenv().getOrDefault("SIGHS_DB_NAME", DEFAULT_NAME);
    private static final String USER = System.getenv().getOrDefault("SIGHS_DB_USER", DEFAULT_USER);
    private static final String PASS = System.getenv().getOrDefault("SIGHS_DB_PASS", DEFAULT_PASS);

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + NAME
                    + "?useUnicode=true&characterEncoding=UTF-8"
                    + "&serverTimezone=America/Argentina/Buenos_Aires"
                    + "&allowPublicKeyRetrieval=true&useSSL=false";

    private ConexionBD() {}

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "No se encontro el driver mysql-connector-j. Revisar pom.xml.");
        }
    }

    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void cerrar(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // el cierre silencioso es aceptable: la conexion ya no sirve.
            }
        }
    }

    public static String infoConexion() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + NAME + " como " + USER;
    }
}