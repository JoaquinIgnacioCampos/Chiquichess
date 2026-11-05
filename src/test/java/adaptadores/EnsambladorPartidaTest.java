package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.juego.Partida;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.MotivoFinalizacion;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.ConfiguracionPartida.Colores;
import adaptadores.configuracion.ConfiguracionPartida.Equipo;
import adaptadores.configuracion.ConfiguracionPartida.Modo;
import adaptadores.configuracion.ConfiguracionPartida.Rival;
import adaptadores.configuracion.ConfiguracionPartida.Tiempo;
import adaptadores.configuracion.PartidaConfigurada;
import dobles.FuenteDeTiempoFalsa;
import dobles.SorteoFijo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class EnsambladorPartidaTest {
    private final EnsambladorPartida ensamblador = new EnsambladorPartida();

    @Test
    void ensamblaPosicionEstandarYRelojConLaFuenteInyectada() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = ensamblador.crearConReloj(Duration.ofMinutes(5), fuente);
        assertEquals(8, partida.tablero().ancho());
        assertEquals(8, partida.tablero().alto());
        assertEquals(32, partida.tablero().piezas().size());
        assertEquals(Bando.BLANCO, partida.turno());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        fuente.avanzar(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), partida.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofMinutes(5)), partida.tiempoRestante(Bando.NEGRO));
    }

    @Test
    void elRelojEnsambladoFinalizaLaPartidaRealPorTiempo() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = ensamblador.crearConReloj(Duration.ofSeconds(10), fuente);
        fuente.avanzar(Duration.ofSeconds(10));
        partida.verificarTiempo();
        assertEquals(MotivoFinalizacion.TIEMPO_AGOTADO, partida.resultado().orElseThrow().motivo());
        assertEquals(Optional.of(Bando.NEGRO), partida.resultado().orElseThrow().ganador());
    }

    @Test
    void consolaConservaLasReglasRealesYSigueSinReloj() {
        Partida partida = ensamblador.crearSinReloj();
        assertTrue(partida.tiempoRestante(Bando.BLANCO).isEmpty());
        assertTrue(partida.tiempoRestante(Bando.NEGRO).isEmpty());
        partida.mover(mov("f2", "f3"));
        partida.mover(mov("e7", "e5"));
        partida.mover(mov("g2", "g4"));
        partida.mover(mov("d8", "h4"));
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertEquals(Bando.NEGRO, partida.ganador().orElseThrow().color());
    }

    @Test
    void partidasNuevasNoCompartenLaMemoriaDeRepeticion() {
        Partida primera = ensamblador.crearSinReloj();
        cicloDeCaballos(primera);
        Partida segunda = ensamblador.crearSinReloj();
        cicloDeCaballos(segunda);
        assertEquals(EstadoPartida.EN_CURSO, segunda.estado());
        cicloDeCaballos(primera);
        assertEquals(EstadoPartida.TABLAS_REPETICION, primera.estado());
        assertEquals(EstadoPartida.EN_CURSO, segunda.estado());
    }

    @Test
    void seUsanRendicionYAcuerdoReales() {
        Partida rendicion = ensamblador.crearSinReloj();
        rendicion.rendirse(Bando.BLANCO);
        assertEquals(MotivoFinalizacion.RENDICION, rendicion.resultado().orElseThrow().motivo());
        assertEquals(Bando.NEGRO, rendicion.ganador().orElseThrow().color());
        Partida acuerdo = ensamblador.crearSinReloj();
        acuerdo.ofrecerTablas(Bando.BLANCO);
        acuerdo.aceptarTablas(Bando.NEGRO);
        assertEquals(MotivoFinalizacion.ACUERDO, acuerdo.resultado().orElseThrow().motivo());
        assertTrue(acuerdo.ganador().isEmpty());
    }

    @Test
    void enModoAleatorioElSorteoInyectadoDecideColoresYEquipos() {
        SorteoFijo sorteo = SorteoFijo.jugador1ConNegras();
        EnsambladorPartida conSorteo = new EnsambladorPartida(sorteo, new FuenteDeTiempoFalsa());
        PartidaConfigurada preparada = conSorteo.prepararConfigurada(configuracion(Tiempo.SIN_TIEMPO, Colores.ALEATORIO));
        assertEquals(1, sorteo.consultas());
        assertEquals("Jugador 2", preparada.partida().jugador(Bando.BLANCO).nombre());
        assertEquals("Jugador 1", preparada.partida().jugador(Bando.NEGRO).nombre());
        assertEquals(Equipo.RIVER, preparada.equipoBlancas());
        assertEquals(Equipo.BOCA, preparada.equipoNegras());
    }

    @Test
    void conColoresElegidosNoSeConsultaElSorteo() {
        SorteoFijo sorteo = SorteoFijo.jugador1ConNegras();
        EnsambladorPartida conSorteo = new EnsambladorPartida(sorteo, new FuenteDeTiempoFalsa());
        PartidaConfigurada preparada = conSorteo.prepararConfigurada(
                configuracion(Tiempo.SIN_TIEMPO, Colores.JUGADOR_1_BLANCAS));
        assertEquals(0, sorteo.consultas());
        assertEquals("Jugador 1", preparada.partida().jugador(Bando.BLANCO).nombre());
        assertEquals(Equipo.BOCA, preparada.equipoBlancas());
    }

    @Test
    void elRelojDelMenuUsaLaFuenteInyectada() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        EnsambladorPartida conFuente = new EnsambladorPartida(SorteoFijo.jugador1ConBlancas(), fuente);
        Partida partida = conFuente.prepararConfigurada(
                configuracion(Tiempo.TRES_MINUTOS, Colores.JUGADOR_1_BLANCAS)).partida();
        fuente.avanzar(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(150)), partida.tiempoRestante(Bando.BLANCO));
        assertEquals(Optional.of(Duration.ofMinutes(3)), partida.tiempoRestante(Bando.NEGRO));
    }

    @Test
    void crearConRelojSinFuenteExplicitaUsaLaInyectada() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = new EnsambladorPartida(SorteoFijo.jugador1ConBlancas(), fuente)
                .crearConReloj(Duration.ofMinutes(5));
        fuente.avanzar(Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(270)), partida.tiempoRestante(Bando.BLANCO));
    }

    @Test
    void rechazaSorteoOFuenteNulos() {
        assertThrows(NullPointerException.class, () -> new EnsambladorPartida(null, new FuenteDeTiempoFalsa()));
        assertThrows(NullPointerException.class, () -> new EnsambladorPartida(SorteoFijo.jugador1ConBlancas(), null));
    }

    private ConfiguracionPartida configuracion(Tiempo tiempo, Colores colores) {
        return new ConfiguracionPartida(Modo.CLASICO, tiempo, Rival.AMIGO, Equipo.BOCA, Equipo.RIVER, colores);
    }

    private void cicloDeCaballos(Partida partida) {
        partida.mover(mov("g1", "f3"));
        partida.mover(mov("g8", "f6"));
        partida.mover(mov("f3", "g1"));
        partida.mover(mov("f6", "g8"));
    }
}
