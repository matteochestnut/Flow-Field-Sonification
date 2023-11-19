package progettoprogrammazionemusica;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.Circuit;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.MultiplyAdd;
import com.jsyn.unitgen.InterpolatingDelay;
import com.jsyn.unitgen.Add;
import com.jsyn.unitgen.Multiply;
import com.jsyn.unitgen.MultiPassThrough;

public class PingPongDelay extends Circuit {
    
    // declaring ports and units
    private final InterpolatingDelay delayLineL; // delay lines
    private final InterpolatingDelay delayLineR;
    private final MultiplyAdd feedbackL;
    private final MultiplyAdd feedbackR;
    private final Multiply wetL;
    private final Multiply dryL;
    private final Multiply wetR;
    private final Multiply dryR;
    private final Add adderL;
    private final Add adderR;
    float timeL; // channel independent delay times
    float timeR;
    int numSamples = 88200;
    UnitOutputPort output;
    UnitInputPort input;
    MultiPassThrough inputPass; // 2 channel input
    MultiPassThrough outputPass; // 2 channel ouput
    
    public PingPongDelay() {
        
        // adding units to the circuit
        add(delayLineL = new InterpolatingDelay());
        add(delayLineR = new InterpolatingDelay());
        add(feedbackL = new MultiplyAdd());
        add(feedbackR = new MultiplyAdd());
        add(wetL = new Multiply());
        add(dryL = new Multiply());
        add(wetR = new Multiply());
        add(dryR = new Multiply());
        add(adderL = new Add());
        add(adderR = new Add());
        add(inputPass = new MultiPassThrough(2)); // 2 channel input
        addPort(input = inputPass.input);
        add(outputPass = new MultiPassThrough(2)); // 2 channel output
        addPort(output = outputPass.output);
        
        // setting random delay times
        timeL = 0.4f + (float)Math.random() * (0.8f - 0.2f);
        timeR = 0.4f + (float)Math.random() * (0.8f - 0.2f);
        
        // manage inputs
        inputPass.output.connect(0, feedbackL.inputC, 0);
        inputPass.output.connect(0, dryL.inputA, 0);
        inputPass.output.connect(1, feedbackR.inputC, 0);
        inputPass.output.connect(1, dryR.inputA, 0);
        
        // delays and feeback connections
        feedbackL.output.connect(delayLineL.input);
        feedbackR.output.connect(delayLineR.input);
        delayLineL.output.connect(feedbackR.inputA); // left delay entering right feedback
        delayLineR.output.connect(feedbackL.inputA); // right delay entering left feedback
        
        // dry / wet mix connections
        delayLineL.output.connect(wetL.inputA);
        delayLineR.output.connect(wetR.inputA);
        wetL.output.connect(adderL.inputA); // mixing left dry and wet signals
        dryL.output.connect(adderL.inputB);
        wetR.output.connect(adderR.inputA); // mixing right dry and we signals
        dryR.output.connect(adderR.inputB);
        
        // manage output
        adderL.output.connect(0, outputPass.input, 0);
        adderR.output.connect(0, outputPass.input, 1);
        
        // setting default values
        input.setup(0.0, 0.0, 1.0);
        delayLineL.delay.set(timeL);
        delayLineR.delay.set(timeR);
        delayLineL.allocate(numSamples);
        delayLineR.allocate(numSamples);
        delayLineL.delay.setMaximum(numSamples / 44100);
        delayLineR.delay.setMaximum(numSamples / 44100);
        feedbackL.inputB.set(0.7);
        feedbackR.inputB.set(0.7);
        setMix(0.5f);  
    }
    
    public final void setMix(float mix) {
        float wet = (float)Math.sqrt(mix);
        float dry = (float)Math.sqrt(1f - mix);
        wetL.inputB.set(wet); // setting wet signal amplitude
        wetR.inputB.set(wet);
        dryL.inputB.set(dry); // setting dey signal amplitude
        dryR.inputB.set(dry);
    }
    
}
