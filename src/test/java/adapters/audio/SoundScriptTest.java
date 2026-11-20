package adapters.audio;

import core.side.Side;
import core.game.GameEvent;
import core.game.GameEvent.MoveUndone;
import core.game.GameEvent.MoveMade;
import core.game.GameEvent.MoveRedone;
import core.game.GameEvent.FinishedGame;
import core.game.GameEvent.Trait;
import core.game.GameResult;
import core.rules.GameState;
import core.rules.FinishReason;
import adapters.audio.SoundScript.Cue;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SoundScriptTest {

    private final SoundScript classic = new SoundScript(false);
    private final SoundScript football = new SoundScript(true);

    private static MoveMade move(GameState state, Trait... traits) {
        return new MoveMade(Side.WHITE, traits.length == 0 ? Set.of() : EnumSet.of(traits[0], traits), state);
    }

    private static List<Sound> sounds(List<Cue> cues) {
        return cues.stream().map(Cue::sound).toList();
    }

    @Test
    void eachMoveSoundsWithItsTrait() {
        assertEquals(List.of(Sound.MOVEMENT), sounds(classic.para(move(GameState.IN_PROGRESS))));
        assertEquals(List.of(Sound.CAPTURE), sounds(classic.para(move(GameState.IN_PROGRESS, Trait.CAPTURE))));
        assertEquals(List.of(Sound.CASTLING), sounds(classic.para(move(GameState.IN_PROGRESS, Trait.CASTLING))));
        assertEquals(List.of(Sound.PASS), sounds(football.para(move(GameState.IN_PROGRESS, Trait.PASS))));
    }

    @Test
    void aMoveSoundsAAloneTimeWithItsTraitMoreImportant() {
        // El jaque tapa a la captura y a la promoción; el gol tapa a todo
        assertEquals(List.of(Sound.CHECK),
                sounds(classic.para(move(GameState.CHECK, Trait.CAPTURE, Trait.PROMOTION))));
        assertEquals(List.of(Sound.PROMOTION),
                sounds(classic.para(move(GameState.IN_PROGRESS, Trait.CAPTURE, Trait.PROMOTION))));
        assertEquals(List.of(Sound.GOAL),
                sounds(football.para(move(GameState.CHECK, Trait.PASS, Trait.GOAL))));
    }

    @Test
    void goalIsOfCrowdOfSideThatItDid() {
        // Act
        List<Cue> cues = football.para(new MoveMade(Side.BLACK, Set.of(Trait.GOAL), GameState.IN_PROGRESS));

        // Assert
        assertEquals(List.of(new Cue(Sound.GOAL, Optional.of(Side.BLACK))), cues);
    }

    @Test
    void victoryIsOfWinnerAndInFootballPrecedesWhistle() {
        // Arrange
        FinishedGame mate = new FinishedGame(
                new GameResult(Optional.of(Side.BLACK), FinishReason.CHECK_MATE));

        // Act + Assert
        assertEquals(List.of(new Cue(Sound.VICTORIA, Optional.of(Side.BLACK)), new Cue(Sound.GAME_END, Optional.empty())),
                classic.para(mate));
        assertEquals(List.of(new Cue(Sound.WHISTLE_FINAL, Optional.empty()),
                new Cue(Sound.VICTORIA, Optional.of(Side.BLACK)), new Cue(Sound.GAME_END, Optional.empty())),
                football.para(mate));
    }

    @Test
    void theGameEndSoundPlaysWhoeverWinsInEveryMode() {
        // Gane quien gane, en el clásico y en el fútbol: la victoria del que ganó (propia de su club) y, además, el fin
        for (Side winner : Side.values()) {
            FinishedGame finished = new FinishedGame(new GameResult(Optional.of(winner), FinishReason.CHECK_MATE));
            for (SoundScript script : List.of(classic, football)) {
                List<Cue> cues = script.para(finished);
                assertTrue(cues.contains(new Cue(Sound.VICTORIA, Optional.of(winner))), cues.toString());
                assertTrue(cues.contains(new Cue(Sound.GAME_END, Optional.empty())), cues.toString());
            }
        }
    }

    @Test
    void aDrawAlsoEndsWithTheGameEndSound() {
        FinishedGame draw = new FinishedGame(new GameResult(Optional.empty(), FinishReason.AGREEMENT));
        assertEquals(List.of(Sound.DRAW, Sound.GAME_END), sounds(classic.para(draw)));
        assertEquals(List.of(Sound.WHISTLE_FINAL, Sound.DRAW, Sound.GAME_END), sounds(football.para(draw)));
    }

    @Test
    void drawNotAreOfNoTeam() {
        // Arrange
        FinishedGame draw = new FinishedGame(new GameResult(Optional.empty(), FinishReason.AGREEMENT));

        // Act + Assert
        assertEquals(List.of(new Cue(Sound.DRAW, Optional.empty()), new Cue(Sound.GAME_END, Optional.empty())),
                classic.para(draw));
    }

    @Test
    void undoAndRedoRewind() {
        assertEquals(List.of(Sound.REWIND), sounds(classic.para(new MoveUndone(GameState.IN_PROGRESS))));
        assertEquals(List.of(Sound.REWIND),
                sounds(classic.para(new MoveRedone(Side.WHITE, Set.of(), GameState.IN_PROGRESS))));
    }

    @Test
    void allGamesStartWithWhistle() {
        assertEquals(List.of(Sound.WHISTLE_INITIAL), sounds(classic.onStart()));
        assertEquals(List.of(Sound.WHISTLE_INITIAL), sounds(football.onStart()));
    }

    @Test
    void allSoundsLeaveOfSomeEvent() {
        // Arrange: un evento de cada tipo y rasgo; la hinchada no sale del guion porque suena en bucle
        List<GameEvent> events = Stream.concat(
                Stream.of(Trait.values()).map(trait -> (GameEvent) move(GameState.IN_PROGRESS, trait)),
                Stream.of(move(GameState.IN_PROGRESS), move(GameState.CHECK),
                        new MoveUndone(GameState.IN_PROGRESS),
                        new FinishedGame(new GameResult(Optional.empty(), FinishReason.AGREEMENT)),
                        new FinishedGame(new GameResult(Optional.of(Side.WHITE), FinishReason.GOALS))))
                .toList();

        // Act
        Set<Sound> used = EnumSet.noneOf(Sound.class);
        events.forEach(event -> used.addAll(sounds(football.para(event))));
        used.addAll(sounds(football.onStart()));

        // Assert
        // La hinchada suena en bucle, la selección la avisa la vista, y la risa y el ambiente son del menú.
        assertEquals(EnumSet.complementOf(EnumSet.of(Sound.CROWD, Sound.SELECTION, Sound.LAUGH,
                Sound.AMBIENT_MENU)), used);
    }
}
