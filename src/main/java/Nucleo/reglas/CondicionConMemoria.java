package Nucleo.reglas;

/**
 * Condición de fin que lleva estado propio para responder rápido (por ejemplo, un contador),
 * con la posibilidad de volver atrás mediante el patrón Memento.
 *
 * Partida llama a {@link #registrar} para todas las condiciones con memoria una vez al crear la
 * partida y una vez después de cada jugada, antes de evaluar ninguna. Así todas anotan cada jugada
 * aunque otra condición anterior termine la partida. Para deshacer, Partida devuelve los recuerdos
 * en orden inverso con {@link #restaurar}.
 *
 * Como guarda estado, una instancia pertenece a una sola partida.
 */
public interface CondicionConMemoria extends CondicionDeFinalizacion {

    /**
     * Actualiza el estado con la situación del contexto (la última jugada, o la posición inicial si
     * todavía no hay jugadas) y devuelve lo necesario para deshacer este registro.
     */
    Recuerdo registrar(ContextoDeJugada contexto);

    /**
     * Vuelve al estado anterior al registro que devolvió ese recuerdo. Los recuerdos se restauran en
     * orden inverso al que se crearon.
     *
     * @throws IllegalArgumentException si el recuerdo no lo creó esta condición
     */
    void restaurar(Recuerdo recuerdo);
}
