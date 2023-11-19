package progettoprogrammazionemusica;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.RangeConverter;
import com.jsyn.unitgen.MultiPassThrough;
import com.jsyn.unitgen.MultiplyAdd;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.Multiply;
import com.jsyn.unitgen.SineOscillator;
import com.jsyn.unitgen.InterpolatingDelay;
import com.jsyn.unitgen.Circuit;

public class Chorus extends Circuit {
    // Declare units and ports.
    InterpolatingDelay delayLineL;
    InterpolatingDelay delayLineR;
    MultiPassThrough inputPass;
    public UnitInputPort input;
    Multiply wetL;
    MultiplyAdd mixL;
    Multiply wetR;
    MultiplyAdd mixR;
    MultiPassThrough outputPass;
    public UnitOutputPort output;
    SineOscillator timeModL;
    RangeConverter modDepthL;
    SineOscillator timeModR;
    RangeConverter modeDepthR;

    public Chorus() {
        
        // Create unit generators.
        add(delayLineL = new InterpolatingDelay());
        add(delayLineR = new InterpolatingDelay());       
        add(wetL = new Multiply());
        add(mixL = new MultiplyAdd());
        add(wetR = new Multiply());
        add(mixR = new MultiplyAdd());        
        add(timeModL = new SineOscillator());
        add(modDepthL = new RangeConverter());
        add(timeModR = new SineOscillator());
        add(modeDepthR = new RangeConverter());        
        add(inputPass = new MultiPassThrough(2)); // 2 channel input
        addPort(input = inputPass.input);
        add(outputPass = new MultiPassThrough(2)); // 2 channel output
        addPort(output = outputPass.output);
        
        // manage inputs
        inputPass.output.connect(0, delayLineL.input, 0);
        inputPass.output.connect(0, mixL.inputA, 0);
        inputPass.output.connect(1, delayLineR.input, 0);
        inputPass.output.connect(1, mixR.inputA, 0);
        
        // Connect units and ports.
        delayLineL.output.connect(wetL.inputA);
        delayLineR.output.connect(wetR.inputA);
        
        wetL.output.connect(mixL.inputC);
        mixL.output.connect(outputPass.input);
        wetR.output.connect(mixR.inputC);
        mixR.output.connect(0, outputPass.input, 1);
        timeModL.output.connect(modDepthL.input); // left delay time modulator
        modDepthL.output.connect(delayLineL.delay); // scaling time modulation
        timeModR.output.connect(modeDepthR.input);
        modeDepthR.output.connect(delayLineR.delay);
        
        // Setup
        input.setup(0.0, 0.0, 1.0);
        delayLineL.allocate(44100);
        delayLineR.allocate(44100);        
        timeModL.frequency.set(0.2);
        timeModL.amplitude.set(1.0);
        modDepthL.min.set(0.01); // minimum delay time
        modDepthL.max.set(0.05); // maximum delay time
        timeModR.frequency.set(0.2);
        timeModR.amplitude.set(1.0);
        timeModR.phase.set(Math.random());
        modeDepthR.min.set(0.01);
        modeDepthR.max.set(0.05);                
        setMix(0.5f);
    }
    
    public final void setMix(float mix) {
        float wet = (float)Math.sqrt(mix);
        float dry = (float)Math.sqrt(1f - mix);
        wetL.inputB.set(Math.sqrt(wet)); // dry
        mixL.inputB.set(Math.sqrt(dry)); // wet
        wetR.inputB.set(Math.sqrt(wet));
        mixR.inputB.set(Math.sqrt(dry));        
    }
    

}
