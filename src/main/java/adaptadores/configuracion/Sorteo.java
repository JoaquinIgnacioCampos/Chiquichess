package adaptadores.configuracion;

/**
 * Puerto del sorteo de colores del menú. Separarlo del ensamblado permite reemplazar el azar
 * por una respuesta fija en los tests, igual que FuenteDeTiempo reemplaza al reloj del sistema.
 */
@FunctionalInterface
public interface Sorteo {

    /** true si el Jugador 1 juega con blancas. */
    boolean jugador1JuegaConBlancas();
}
