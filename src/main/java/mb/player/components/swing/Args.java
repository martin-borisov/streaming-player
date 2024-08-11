package mb.player.components.swing;

import com.beust.jcommander.Parameter;

public class Args {
    
    @Parameter(names = "--mock-audio")
    private boolean mockAudio;

    public boolean isMockAudio() {
        return mockAudio;
    }

    public void setMockAudio(boolean mockAudio) {
        this.mockAudio = mockAudio;
    } 
}
