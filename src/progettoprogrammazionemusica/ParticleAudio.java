package progettoprogrammazionemusica;
import com.jsyn.data.SegmentedEnvelope;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.Circuit;
import com.jsyn.unitgen.SineOscillator;
import com.jsyn.unitgen.UnitOscillator;
import com.jsyn.unitgen.MixerStereoRamped;
import com.jsyn.unitgen.UnitSource;
import com.softsynth.math.AudioMath;
import java.util.Random;
import static processing.core.PApplet.map;
import com.jsyn.unitgen.VariableRateMonoReader;

public class ParticleAudio  extends Circuit implements UnitSource{
    
    private final float max_amp = 0.7f;
    private final float min_amp = 0.2f;
    private final int width = 1280;
    private final int height = 720;
    private final UnitOscillator osc; // pure sine oscillator for audio
    private final UnitOscillator mod; // pure sine oscillator for amplitude modulation
    private final MixerStereoRamped smoother; // pain, gain and amp are smoothed to avoid glitches
    Random rand = new Random();
    double[] envelopeData = {
        0.02, 1,
        0.5, 0.0
    };
    SegmentedEnvelope envelope = new SegmentedEnvelope(envelopeData);
    VariableRateMonoReader envPlayer;
    
    /*
    * ParticleAudio creates the audio component of a particle using its color,
    * the shade paramter of ProcessingGraphcis, its location (x, y), the total
    * number of particles (to normalize the amplitude of the particle), a note
    * given by ParticleSystemAudio and the mode to control the envelope
    */
    public ParticleAudio(float color, String shade, float x, float y, int numParticles, int scaleRoot, String mode){
        
        add(osc = new SineOscillator());
        add(mod = new SineOscillator());
        add(smoother = new MixerStereoRamped(1));
        add(envPlayer = new VariableRateMonoReader());
        
        setPitch(color, shade, scaleRoot);
        setAmplitude(y, numParticles);
        setPan(x);
        resetEnvelope();
        
        mod.frequency.set((Math.random() * 2 + 0.01));
        mod.phase.set(Math.random());
        mod.amplitude.set(1);
        
        if (mode.equals("particle")){
            startEnvelope();
        } else if (mode.equals("flow")) {
            stopEnvelope();
        }
        
        //osc.amplitude.set(1);
        osc.output.connect(smoother.input);
    }
    
    /*
    * The frequency of the oscillator is based on three values:
    * ParticleGraphics shade:
    *   - red: diatonic scale;
    *   - blue: pentatonic scale;
    *   - green: whole tone scale
    * ParticleSystemAudio scaleRoot:
    *   determines the root note of the scale. It is a midi pitch from
    *   36 to 47
    * ParticleGraphics color:
    *   is the hue scaled to a range of values between 0 and 360, in particular
    *   given the shade, color assumes values only in a certain range in order
    *   to produce shades of red, blue or green.
    */ 
    public void setPitch(float color, String shade, int scaleRoot) {        
        int min = 0; // initializing the ranges for the color parameter
        int max = 0;
        int note = 0;
        int index = 0;
        int[] diatonic = {0, 2, 4, 5, 7, 9, 11}; // degrees of each scale in terms of pitch classes
        int[] pentatonic = {0, 2, 4, 7, 9};
        int[] wholeTone = {0, 2, 4, 6, 8, 10};
        switch (shade) {
            case "red":
                min = 216;
                max = 360;
                break;
            case "blue":
                min = 150;
                max = 270;
                break;
            case "green":
                min = 30;
                max = 215;
                break;
            default:
                break;
        }
        
        // the color is mapped onto a degree of a scale
        if (shade.equals("red")) {
            index = (int) ( 0 + ( (color - min) * (6.99 - 0) ) / (max - min) );
            note = diatonic[index];
        } else if (shade.equals("blue")){
            index = (int) ( 0 + ( (color - min) * (4.99 - 0) ) / (max - min) );
            note = pentatonic[index];
        } else if (shade.equals("green")) {
            index = (int) ( 0 + ( (color - min) * (5.99 - 0) ) / (max - min) );
            note = wholeTone[index];
        }
        
        note += scaleRoot; // getting the midi pitch of the note
        double frequency = AudioMath.pitchToFrequency(note); // convert the pitch to frequency
        // harmonics contain the harmonics of the note
        double[] harmonics = {frequency, frequency * 2, frequency * 3, frequency * 4, frequency * 5, frequency * 6, frequency * 7, frequency * 8, frequency * 9, frequency * 10};
        int indexHarmonic = rand.nextInt(10);
        double harmonic = harmonics[indexHarmonic];
        // osc frequency is define as a random harmonic of the note
        osc.frequency.set(harmonic);
        // lowering the amplitude of higher harmonics
        osc.amplitude.set(1 / (indexHarmonic / 5 + 1));
    }
    
    /*
    * scaling the y coordinate of the particle between max_amp and min_amp to
    * modulate the particle amplitude.
    * maximum amplitude is 0.7, minimum amplitude is 0.2
    */
    public void setAmplitude(float y, int numParticles) {
        float mix = Math.abs( map(y, 0, height, max_amp, min_amp));
        smoother.amplitude.set((mix / (numParticles * max_amp)));
    }
    
    // scaling the x coordinate of the particle between -1 and +1 to modulate the pan
    public void setPan(float x) {
        float pan = map(x / width, 0, 1, -1, 1);
        smoother.pan.set(pan);
    }
    
    // when the particle mode button is pushed then the gain modulator
    // is disconnected and the envelope is used instead
    public void startEnvelope() {
        mod.output.disconnect(smoother.gain);
        if (!envPlayer.output.isConnected()) {
            envPlayer.output.connect(smoother.gain);
        }
    }
    
    // when the flow mode button is pushed then the gain envelope is
    // disconnected and the sine wave modulator is used instead
    public void stopEnvelope() {
        envPlayer.output.disconnect(smoother.gain);
        if (!mod.output.isConnected()) {
            mod.output.connect(smoother.gain);
        }
    }
    
    // resetting the envelope
    public void resetEnvelope() {
        envPlayer.dataQueue.clear();
        envPlayer.dataQueue.queue(envelope);
    }
    
    // resetting the envelope when a particle (when in particle mode) hit an edge
    public void boing(String mode, float x, float y) {
        if (mode.equals("particle")) {
            if (x >= 1280 || x <= 0 || y >= 720 || y <= 0) {
                resetEnvelope();
            }
        }
    }
    
    // start the oscillator
    public void turnOn() {
        osc.start();
        osc.setEnabled(true);
    }
    
    // stop the oscillator and disable it to consume less cpu
    public void turnOff() {
        osc.stop();
        osc.setEnabled(false);
    }
    
    // returning the stereo output of the smoother
    @Override
    public UnitOutputPort getOutput() {
        return smoother.output;
    }
    
}
