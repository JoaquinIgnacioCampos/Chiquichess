package adapters.audio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VolumeControlTest {
    private final VolumeControl control = new VolumeControl();
    private final List<String> notices = new ArrayList<>();

    VolumeControlTest() {
        control.onChange(() -> notices.add(control.muted() + " " + control.effective()));
    }

    @Test
    void carryVolumeToZeroMutesAndKeepsPrevious() {
        control.changeVolume(0.7);
        control.changeVolume(0);
        assertTrue(control.muted());
        assertEquals(0, control.effective());
        assertEquals(0.7, control.volume(), 1e-9);
    }

    @Test
    void raiseVolumeFromSilenceReactivatesSound() {
        control.mute(true);
        control.changeVolume(0.3);
        assertFalse(control.muted());
        assertEquals(0.3, control.effective(), 1e-9);
    }

    @Test
    void reactivateSoundRecoversLastVolume() {
        control.changeVolume(0.6);
        control.mute(true);
        control.mute(false);
        assertEquals(0.6, control.effective(), 1e-9);
    }

    @Test
    void onlyNotifiesWhenChangesSomething() {
        control.mute(true);
        control.mute(true);
        control.changeVolume(0);
        assertEquals(List.of("true 0.0"), notices);
    }
}
