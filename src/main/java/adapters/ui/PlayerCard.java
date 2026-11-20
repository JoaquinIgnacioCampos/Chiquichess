package adapters.ui;

import core.side.Side;
import core.game.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.Button;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** Identificación y acciones de presentación de un jugador, sin reglas del juego. */
final class PlayerCard extends StackPane {
    private enum Panel { NORMAL, RESIGNATION, OFFER, RESPONSE }
    private final PanelTheme theme;
    private final Side color;
    private final Text displayName = new Text();
    private final Text nameClub = new Text();
    private final Text side = new Text();
    private final VBox identity = new VBox();
    private final ClockView clock;
    private final StackPane actions = new StackPane();
    private Button resign;
    private Button offerDraw;
    private final VBox content = new VBox(12);
    private final HBox barActions = new HBox(8);
    private final Text waiting = new Text("Esperando respuesta");
    private JavaFxActionConfirmation confirmation;
    private JavaFxActionConfirmation confirmOffer;
    private JavaFxActionConfirmation respondOffer;
    private Panel panelVisible = Panel.NORMAL;
    private final StackPane badge;

    PlayerCard(Side color, PanelTheme theme) {
        this(color, theme, null);
    }

    PlayerCard(Side color, PanelTheme themeBase, ClubPresentation club) {
        PanelTheme theme = club == null ? themeBase : club.theme();
        this.theme = theme;
        this.color = color;
        setId(color == Side.WHITE ? "jugador-blanco" : "jugador-negro");
        displayName.setId(color == Side.WHITE ? "nombre-blanco" : "nombre-negro");
        displayName.setFill(theme.texto());
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER_LEFT);
        content.setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(theme.card(), CornerRadii.EMPTY, Insets.EMPTY)));

        Text rol = new Text(color == Side.WHITE ? "LOCAL" : "VISITANTE");
        rol.setFont(PixelStyle.font(14, true));
        rol.setFill(theme.textSecondary());
        clock = new ClockView(color, theme);
        clock.prefWidthProperty().bind(widthProperty().multiply(0.35));
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox cabecera = new HBox(10, new Rectangle(4, 15, theme.accent()), rol, space, clock);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Polygon crest = new Polygon(32, 0, 62, 10, 60, 46, 51, 63, 32, 76, 13, 63, 4, 46, 2, 10);
        crest.setFill(color == Side.WHITE ? theme.badgeWhite() : theme.badgeBlack());
        crest.setStroke(theme.border());
        crest.setStrokeWidth(2);
        Text pawn = new Text(color == Side.WHITE ? "♙" : "♟");
        pawn.setFont(Font.font("Segoe UI Symbol", 44));
        pawn.setFill(color == Side.WHITE ? theme.badgeBlack() : theme.badgeWhite());
        badge = new StackPane();
        if (club == null) {
            badge.getChildren().addAll(crest, pawn);
        } else {
            ImageView image = new ImageView(club.crest());
            image.setId("escudo-" + suffix());
            image.setAccessibleText("Escudo de " + club.team().displayName());
            image.setPreserveRatio(true);
            image.setFitWidth(64);
            image.setFitHeight(80);
            image.setSmooth(true);
            badge.getChildren().add(image);
            nameClub.setText(club.team().displayName());
        }
        badge.setMinSize(64, 80);
        badge.setPrefSize(64, 80);
        badge.setMaxSize(64, 80);

        side.setText(color == Side.WHITE ? "BLANCAS" : "NEGRAS");
        side.setFill(theme.textSecondary());
        side.setFont(PixelStyle.font(13, true));
        nameClub.setId("club-" + suffix());
        nameClub.setAccessibleText(nameClub.getText());
        nameClub.setFill(theme.accent());
        nameClub.setVisible(club != null);
        nameClub.setManaged(club != null);
        identity.getChildren().addAll(displayName, nameClub, side);
        identity.setMinWidth(0);
        identity.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identity, Priority.ALWAYS);
        HBox body = new HBox(16, new Group(badge), identity);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setMinHeight(0);
        actions.getChildren().add(body);
        VBox.setVgrow(actions, Priority.ALWAYS);
        barActions.setAlignment(Pos.CENTER_RIGHT);
        barActions.setVisible(false);
        barActions.setManaged(false);
        waiting.setId("esperando-tablas-" + suffix());
        waiting.setFill(theme.accent());
        waiting.setFont(PixelStyle.font(12, true));
        waiting.setVisible(false);
        waiting.setManaged(false);
        content.getChildren().addAll(cabecera, actions, barActions);
        getChildren().add(content);
        widthProperty().addListener((property, before, now) -> adjustName());
        heightProperty().addListener((property, before, now) -> adjustDistribution());
        adjustDistribution();
        adjustName();
    }

    private void adjustName() {
        displayName.setFont(PixelStyle.font(Math.max(17,
                Math.min(getHeight() < 200 ? 20 : 32, getWidth() * 0.075)), true));
        displayName.setWrappingWidth(Math.max(1, getWidth() - 120));
    }

    private void adjustDistribution() {
        boolean compact = getHeight() < 200;
        setPadding(new Insets(compact ? 10 : 14, compact ? 16 : 20, compact ? 10 : 14, compact ? 16 : 20));
        content.setSpacing(compact ? 5 : 8);
        identity.setSpacing(compact ? 3 : 5);
        nameClub.setFont(PixelStyle.font(compact ? 11 : 14, true));
        side.setFont(PixelStyle.font(compact ? 11 : 13, true));
        badge.setScaleX(compact ? 0.6 : 1);
        badge.setScaleY(compact ? 0.6 : 1);
        adjustName();
    }

    private String suffix() {
        return color == Side.WHITE ? "blanco" : "negro";
    }

    void update(Player player, boolean activo) {
        displayName.setText(player.displayName());
        displayName.setAccessibleText(player.displayName());
        getStyleClass().remove("jugador-activo");
        if (activo) getStyleClass().add("jugador-activo");
        setBorder(new Border(new BorderStroke(activo ? theme.activo() : theme.border(),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(activo ? 2 : 1))));
        setEffect(activo ? new DropShadow(14, theme.activo().deriveColor(0, 1, 1, 0.2)) : null);
    }

    void updateClock(java.util.Optional<java.time.Duration> remaining, boolean activo) {
        clock.update(remaining, activo);
    }

    /** Las acciones se conectan desde el ensamblado; la tarjeta solo emite el clic. */
    void configureResignation(Runnable request) {
        if (resign != null) throw new IllegalStateException("Rendición ya configurada");
        resign = new Button("Rendirse");
        resign.setId(getId().equals("jugador-blanco") ? "rendirse-blanco" : "rendirse-negro");
        resign.setAccessibleText(getId().equals("jugador-blanco") ? "Rendirse: blancas" : "Rendirse: negras");
        resign.setFont(PixelStyle.font(13, true));
        resign.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        resign.setCursor(javafx.scene.Cursor.HAND);
        resign.setGraphic(PixelIcon.create(PixelIcon.FLAG, theme.alerta(), theme.card(), theme.alerta()));
        resign.setGraphicTextGap(6);
        PixelStyle.paint(resign, theme.card(), theme.alerta());
        confirmation = new JavaFxActionConfirmation(theme, "¿Seguro que querés\nrendirte?", "Rendirse", "Cancelar",
                "rendicion", theme.alerta(), () -> {
            show(Panel.NORMAL);
            request.run();
        }, () -> show(Panel.NORMAL));
        confirmation.setId(getId().equals("jugador-blanco") ? "confirmacion-blanco" : "confirmacion-negro");
        confirmation.setVisible(false);
        confirmation.setManaged(false);
        getChildren().add(confirmation);
        resign.setOnAction(event -> show(Panel.RESIGNATION));
        barActions.getChildren().add(resign);
        barActions.setVisible(true);
        barActions.setManaged(true);
    }

    void enableResignation(boolean enabled) {
        if (resign != null) resign.setDisable(!enabled);
        if (!enabled && panelVisible == Panel.RESIGNATION) show(Panel.NORMAL);
    }

    void configureDraw(Runnable offer, Runnable accept, Runnable reject) {
        if (offerDraw != null) throw new IllegalStateException("Tablas ya configuradas");
        offerDraw = new Button("Ofrecer tablas");
        offerDraw.setId("ofrecer-tablas-" + suffix());
        offerDraw.setAccessibleText("Ofrecer tablas: " + (color == Side.WHITE ? "blancas" : "negras"));
        offerDraw.setFont(PixelStyle.font(13, true));
        offerDraw.setCursor(javafx.scene.Cursor.HAND);
        PixelStyle.paint(offerDraw, theme.card(), theme.accent());
        confirmOffer = new JavaFxActionConfirmation(theme, "¿Querés ofrecer\ntablas al rival?",
                "Ofrecer tablas", "Cancelar", "oferta-tablas", theme.accent(), () -> {
            show(Panel.NORMAL);
            offer.run();
        }, () -> show(Panel.NORMAL));
        confirmOffer.setId("confirmar-oferta-" + suffix());
        respondOffer = new JavaFxActionConfirmation(theme, "Te ofrecieron tablas",
                "Aceptar", "Rechazar", "respuesta-tablas", theme.activo(), accept, reject);
        respondOffer.setId("respuesta-tablas-" + suffix());
        for (var panel : new JavaFxActionConfirmation[]{confirmOffer, respondOffer}) {
            panel.setVisible(false);
            panel.setManaged(false);
            getChildren().add(panel);
        }
        offerDraw.setOnAction(e -> show(Panel.OFFER));
        barActions.getChildren().addAll(0, java.util.List.of(offerDraw, waiting));
        barActions.setVisible(true);
        barActions.setManaged(true);
    }

    /** Los permisos y la oferta vigente se reciben del estado actual del núcleo. */
    void updateDraw(boolean canOffer, boolean ownOffer, boolean offerRival) {
        if (offerDraw == null) return;
        offerDraw.setDisable(!canOffer);
        offerDraw.setVisible(!ownOffer);
        offerDraw.setManaged(!ownOffer);
        waiting.setVisible(ownOffer);
        waiting.setManaged(ownOffer);
        if (offerRival) {
            show(Panel.RESPONSE);
        } else if (panelVisible == Panel.RESPONSE || panelVisible == Panel.OFFER && !canOffer) {
            show(Panel.NORMAL);
        }
    }

    private void show(Panel panel) {
        if (panelVisible == panel) return;
        panelVisible = panel;
        content.setVisible(panel == Panel.NORMAL);
        JavaFxActionConfirmation focus = null;
        if (confirmation != null) {
            confirmation.setVisible(panel == Panel.RESIGNATION);
            confirmation.setManaged(panel == Panel.RESIGNATION);
            if (panel == Panel.RESIGNATION) focus = confirmation;
        }
        if (confirmOffer != null) {
            confirmOffer.setVisible(panel == Panel.OFFER);
            confirmOffer.setManaged(panel == Panel.OFFER);
            respondOffer.setVisible(panel == Panel.RESPONSE);
            respondOffer.setManaged(panel == Panel.RESPONSE);
            if (panel == Panel.OFFER) focus = confirmOffer;
            if (panel == Panel.RESPONSE) focus = respondOffer;
        }
        if (focus != null) focus.focusCancel();
    }
}
