package core.game;

/**
 * Puerto de salida (Observer): lo implementa un adaptador que quiere enterarse de lo que pasa en la
 * partida, como el de sonido. Es el camino inverso a {@code TimeSource}: ahí el núcleo le pide
 * algo al mundo exterior; acá le avisa.
 *
 * Se llama en el mismo hilo que hizo la orden, después de que la partida ya cambió. Un observador no
 * debería lanzar excepciones: si su trabajo falla (por ejemplo, no hay parlantes), lo resuelve él.
 */
@FunctionalInterface
public interface GameObserver {
    void notify(GameEvent event);
}
