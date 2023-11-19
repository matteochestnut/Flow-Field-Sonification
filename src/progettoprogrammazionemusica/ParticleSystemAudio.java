package progettoprogrammazionemusica;
import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import java.util.Random;

public class ParticleSystemAudio {
    
    private final Synthesizer synth;
    private final LineOut lineOut;
    private final PingPongDelay ppd;
    private final int[] roots = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private final int root;
    Random rand = new Random();
    Chorus chorus;
    
    public ParticleSystemAudio(int n) {
        
        root = roots[rand.nextInt(12)];
        
        synth = JSyn.createSynthesizer();
        synth.add(lineOut = new LineOut());
        synth.add(ppd = new PingPongDelay());
        synth.add(chorus = new Chorus());

        ppd.output.connect(0, chorus.input, 0);
        ppd.output.connect(1, chorus.input, 1);
            
        chorus.output.connect(0, lineOut.input, 0);
        chorus.output.connect(1, lineOut.input, 1);
        
        //ppdelay output...
    }
    
    public void addParticleAudio(ParticleAudio particle) {
        synth.add(particle);
          particle.getOutput().connect(0, ppd.input, 0);
          particle.getOutput().connect(1, ppd.input, 1);
    }
    
    public void startAudioSystem() {
        synth.start();
        lineOut.start();
    }
    
    public void stopAudioSystem() {
        synth.stop();
        lineOut.stop();
    }
    
    public int getRoot() {
        return root;
    }
    
}
