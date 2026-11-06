package adaptadores.ui;

import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Proyección del tablero actual. Solo guarda referencias a sus dependencias y nodos gráficos. */
public final class VistaTablero extends Region {
    private final ITablero tablero;
    private final TemaTablero tema;
    private final RepresentacionPiezas representacion;
    private final GridPane grilla = new GridPane();
    private final Consumer<Posicion> alPulsar;

    public VistaTablero(ITablero tablero, TemaTablero tema, RepresentacionPiezas representacion) {
        this(tablero, tema, representacion, posicion -> { });
    }

    public VistaTablero(ITablero tablero, TemaTablero tema, RepresentacionPiezas representacion,
                        Consumer<Posicion> alPulsar) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.tema = Objects.requireNonNull(tema, "tema");
        this.representacion = Objects.requireNonNull(representacion, "representacion");
        this.alPulsar = Objects.requireNonNull(alPulsar, "alPulsar");
        setMinSize(0, 0);
        getChildren().add(grilla);
        redibujar();
    }

    /** Reconstruye la representación consultando el núcleo, sin almacenar piezas por separado. */
    public void redibujar() {
        redibujar(Optional.empty(), Set.of());
    }

    /** Los destinos ya fueron consultados al núcleo; aquí solo se representan. */
    public void redibujar(Optional<Posicion> seleccionada, Set<Posicion> destinos) {
        Objects.requireNonNull(seleccionada, "seleccionada");
        Objects.requireNonNull(destinos, "destinos");
        grilla.getChildren().clear();
        grilla.getColumnConstraints().clear();
        grilla.getRowConstraints().clear();
        for (int columna = 0; columna < tablero.ancho(); columna++) {
            ColumnConstraints restriccion = new ColumnConstraints();
            restriccion.setPercentWidth(100.0 / tablero.ancho());
            grilla.getColumnConstraints().add(restriccion);
        }
        for (int fila = 0; fila < tablero.alto(); fila++) {
            RowConstraints restriccion = new RowConstraints();
            restriccion.setPercentHeight(100.0 / tablero.alto());
            grilla.getRowConstraints().add(restriccion);
            for (int columna = 0; columna < tablero.ancho(); columna++) {
                Posicion posicion = new Posicion(columna, fila);
                StackPane casilla = crearCasilla(posicion,
                        seleccionada.filter(posicion::equals).isPresent(), destinos.contains(posicion));
                grilla.add(casilla, columna, tablero.alto() - 1 - fila);
            }
        }
        requestLayout();
    }

    private StackPane crearCasilla(Posicion posicion, boolean seleccionada, boolean destino) {
        StackPane casilla = new StackPane();
        casilla.setMinSize(0, 0);
        casilla.setId("casilla-" + posicion.columna() + "-" + posicion.fila());
        casilla.getStyleClass().add("casilla");
        boolean oscura = (posicion.columna() + posicion.fila()) % 2 == 0;
        if (seleccionada) {
            casilla.getStyleClass().add("casilla-seleccionada");
        }
        casilla.setBackground(new Background(new BackgroundFill(
                seleccionada ? tema.casillaSeleccionada()
                        : oscura ? tema.casillaOscura() : tema.casillaClara(),
                CornerRadii.EMPTY, Insets.EMPTY)));
        casilla.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY) {
                alPulsar.accept(posicion);
            }
        });
        casilla.setAccessibleText("Columna " + (posicion.columna() + 1) + ", fila " + (posicion.fila() + 1));
        tablero.piezaEn(posicion).ifPresent(pieza -> {
            Node figura = representacion.crear(pieza, tema);
            figura.scaleXProperty().bind(casilla.widthProperty().divide(RepresentacionPiezas.LADO_BASE));
            figura.scaleYProperty().bind(casilla.heightProperty().divide(RepresentacionPiezas.LADO_BASE));
            casilla.getChildren().add(figura);
        });
        if (tablero.pelota().filter(posicion::equals).isPresent()) {
            agregarPelota(casilla, tablero.piezaEn(posicion).isPresent());
        }
        if (destino) {
            agregarIndicador(casilla, tablero.piezaEn(posicion).isPresent());
        }
        return casilla;
    }

    /**
     * Pelota del modo fútbol: suelta, va en el centro de la casilla; si una pieza la lleva, va más
     * chica en la esquina de abajo a la derecha, para que la pieza se siga viendo.
     */
    private void agregarPelota(StackPane casilla, boolean conPieza) {
        Circle pelota = new Circle();
        pelota.setId("pelota");
        pelota.getStyleClass().add("pelota");
        pelota.setManaged(false);
        pelota.setMouseTransparent(true);
        var lado = Bindings.min(casilla.widthProperty(), casilla.heightProperty());
        pelota.radiusProperty().bind(lado.multiply(conPieza ? 0.13 : 0.22));
        double posicion = conPieza ? 0.80 : 0.5;
        pelota.centerXProperty().bind(casilla.widthProperty().multiply(posicion));
        pelota.centerYProperty().bind(casilla.heightProperty().multiply(posicion));
        pelota.setFill(javafx.scene.paint.Color.WHITE);
        pelota.setStroke(javafx.scene.paint.Color.web("#1b1b1b"));
        pelota.strokeWidthProperty().bind(lado.multiply(0.035));
        casilla.getChildren().add(pelota);
    }

    private void agregarIndicador(StackPane casilla, boolean captura) {
        Circle indicador = new Circle();
        indicador.setManaged(false);
        indicador.setMouseTransparent(true);
        indicador.centerXProperty().bind(casilla.widthProperty().divide(2));
        indicador.centerYProperty().bind(casilla.heightProperty().divide(2));
        var lado = Bindings.min(casilla.widthProperty(), casilla.heightProperty());
        indicador.radiusProperty().bind(lado.multiply(captura ? 0.43 : 0.16));
        if (captura) {
            indicador.getStyleClass().add("indicador-captura");
            indicador.setFill(null);
            indicador.setStroke(tema.destinoCaptura());
            indicador.strokeWidthProperty().bind(lado.multiply(0.08));
        } else {
            indicador.getStyleClass().add("indicador-movimiento");
            indicador.setFill(tema.destinoLibre());
        }
        casilla.getChildren().add(indicador);
    }

    @Override
    protected void layoutChildren() {
        double lado = Math.min(getWidth() / tablero.ancho(), getHeight() / tablero.alto());
        double ancho = lado * tablero.ancho();
        double alto = lado * tablero.alto();
        grilla.resizeRelocate((getWidth() - ancho) / 2, (getHeight() - alto) / 2, ancho, alto);
    }

    @Override
    protected double computePrefWidth(double height) {
        return tablero.ancho() * RepresentacionPiezas.LADO_BASE;
    }

    @Override
    protected double computePrefHeight(double width) {
        return tablero.alto() * RepresentacionPiezas.LADO_BASE;
    }
}
