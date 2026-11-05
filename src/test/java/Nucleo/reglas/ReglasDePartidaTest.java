package Nucleo.reglas;

import dobles.StubDetectorDeJaque;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReglasDePartidaTest {

    @Test
    void lasClasicasIncluyenLasCuatroCondicionesEnOrden() {
        // Act
        List<CondicionDeFinalizacion> condiciones = ReglasDePartida.clasicas().condiciones();

        // Assert: mate y ahogado primero, como pide Partida
        assertEquals(4, condiciones.size());
        assertInstanceOf(JaqueMate.class, condiciones.get(0));
        assertInstanceOf(Ahogado.class, condiciones.get(1));
        assertInstanceOf(TablasPorRepeticion.class, condiciones.get(2));
        assertInstanceOf(TablasPorReglaDeLos50Movimientos.class, condiciones.get(3));
    }

    @Test
    void cadaLlamadaAClasicasCreaInstanciasNuevas() {
        // Act
        ReglasDePartida primera = ReglasDePartida.clasicas();
        ReglasDePartida segunda = ReglasDePartida.clasicas();

        // Assert: las condiciones con memoria no se comparten entre partidas
        assertNotSame(primera.condiciones().get(2), segunda.condiciones().get(2));
        assertNotSame(primera.condiciones().get(3), segunda.condiciones().get(3));
        assertNotSame(primera.movimientosLegales(), segunda.movimientosLegales());
    }

    @Test
    void elDetectorEsElQueUsanLosMovimientosLegales() {
        // Arrange
        IDetectorDeJaque detector = StubDetectorDeJaque.siempreEnJaque();

        // Act
        ReglasDePartida reglas = new ReglasDePartida(detector, List.of());

        // Assert
        assertSame(detector, reglas.detector());
        assertSame(detector, reglas.movimientosLegales().detector());
    }

    @Test
    void guardaUnaCopiaInmodificableDeLasCondiciones() {
        // Arrange
        List<CondicionDeFinalizacion> original = new ArrayList<>(List.of(new JaqueMate()));
        ReglasDePartida reglas = new ReglasDePartida(new DetectorDeJaque(), original);

        // Act
        original.add(new Ahogado());

        // Assert
        assertEquals(1, reglas.condiciones().size());
        assertThrows(UnsupportedOperationException.class, () -> reglas.condiciones().add(new Ahogado()));
    }

    @Test
    void rechazaDependenciasNulas() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new ReglasDePartida((MovimientosLegales) null, List.of()));
        assertThrows(NullPointerException.class, () -> new ReglasDePartida((IDetectorDeJaque) null, List.of()));
        assertThrows(NullPointerException.class, () -> new ReglasDePartida(new DetectorDeJaque(), null));
    }
}
