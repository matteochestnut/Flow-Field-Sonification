package progettoprogrammazionemusica;
import processing.core.*;
import controlP5.*;

public class ProcessingGraphics extends PApplet{
    
    private final int sketchWidth; // width of Processing sketch window
    private final int sketchHeight; // height of proceesing sketch window
    private final int cols; // columns in which the sketch is devided
    private final int rows; // rows in which the sketch is devided
    private final int scl; // dimension of each cell in which the sketch is devided (in pixels, scl x scl)
    private final PVector[] flowField; // array of vectors, each vectors is assign to a cell in which the sketch is devided
    private PVector flowVector;
    private int index; // index of the flow vector
    private float angle; // angle the flow vector
    private final float noiseIncrement; // speed of "exploration" of the x and y axis in the Perlin noise space
    private final float zIncrement; // speed of "exploration" of the z axis in the Perlin noise space
    private float xOffset; // x coordinate to sample a point in the Perlin noise space
    private float yOffset; // y coordinate to sample a point in the Perlin noise space
    private float zOffset; // z coordinate to sample a point in the Perlin noise space
    private final int maxNumParticles; // maximum number of particles
    private int numParticles; // numeber of particles to display
    private ParticleGraphics[] particlesLook; // array containing all the particles graphic instances
    private ParticleAudio[] particlesSound; // array containing all the particles audio instances
    private String mode; // either particle or flow mode, change the way of drawing of the particles (lines or points)
    private String shade; // either "red", "green", "blue", determines the colors that the particles have
    private int seed; // Perline noise seed
    private ParticleSystemAudio audioSystem;
    private ControlP5 p5; // ControlP5 object to manage graphical elements for User Interaction (slider to change numParticles, etc.)
    private boolean draw;
    
    /*
    * ProcessingGraphics constructor:
    * takes two integers that are the width and height of the JPanel and set
    * width and height of the Processing sketch.
    * The sketch cover the entire JPanel
    *
    * Here are initialized parameters relative to the overall app and sketch
    * but not the graphical elements such as the ControlP5 object and the particles
    * that can be redraw and so eventually re-initialized
    */
    public ProcessingGraphics(int w, int h) {
        sketchWidth = w;
        sketchHeight = h;
        maxNumParticles = 100;
        numParticles = 20; // defualt number of particles on start
        mode = "particle"; // default drawing mode on start
        /*
        * Default shade on start is red.
        * - red: particles color hue is between 216 and 360
        * - green: particles color hue is between 30 and 215
        * - blue: particles color hue is between 150 and 270
        */
        shade = "red";
        
        scl = 20; // the sketch is devide in cells of 20 x 20 pixels
        cols = floor(sketchWidth / scl); // number of columns
        rows = floor(sketchHeight / scl); // number of rows
        flowField = new PVector[cols * rows];
        
        noiseIncrement = 0.1f;
        zOffset = 0;
        zIncrement = 0.0003f;
        
    }
    
    // Override Processing settings function. Needed to set the size of the sketch
    @Override
    public void settings() {
        size(sketchWidth,sketchHeight);
    }
    
    /*
    * Override Processing setup function.
    * It is always called after settings.
    */
    @Override
    public void setup() {
        /*
        * HSB color mode:
        * Hue takes values from 0 to 360;
        * Saturation takes value from 0 to 100;
        * Brightness takes values from 0 to 100;
        * Alpha / transparency takes values from 0 to 255.
        */
        colorMode(HSB, 360, 100, 100, 255);
        frameRate(30);
        init(); // initialize the audio manager of the wall application and the particles        
        addParticleSlider();
        addModeButtons();
        addColorButtons();
        addOptionButtons();
        
    }
    
    /*
    * If the user choose to restart the particles by pressing restart then their
    * graphial an audio component need to be reinitialized (shade, mode and
    * the number of particles displayed are note reinitialized to their default
    * values). As the setup function cannot be recalled, the particles are
    * instantiated in the init so that it can be called when the restart button
    * is pressed. Also the audioSystem that contains the JSsyn synthesizer and the
    * LineOut object to which each particle is connect is reinstantiated
    */
    private void init() {            
        background(0, 0, 3);
        draw = true;
        audioSystem = new ParticleSystemAudio(numParticles); // Instantiating the class that manage the audio output
        particlesLook = new ParticleGraphics[maxNumParticles];
        particlesSound = new ParticleAudio[maxNumParticles];
        addParticles();
        audioSystem.startAudioSystem(); // starting the audio
        p5 = new ControlP5(this);
        startNumParticles();
    }
    
    /*
    * Override Processing draw function. Called continuosly at the rate defined
    * by frameRate()
    */
    @Override
    public void draw() { 
        if (draw){
        
        noiseSeed(seed);  
        
        if (mode.equals("particle")) {
            background(0, 0, 3);
        }   
        
        // Defining the flow field
        yOffset = 0;
        for (int y = 0; y < rows; y++) {
            xOffset = 0;
            for (int x = 0; x < cols; x++) {
                index = x + y * cols;
                // sampling Perlin noise space to have flow vectotors with a coherent direction one another
                angle = noise(xOffset, yOffset, zOffset) * TWO_PI * 4;
                flowVector = PVector.fromAngle(angle);
                flowVector.setMag(1); // normalizing the magnitude of the floe vector
                flowField[index] = flowVector; // updating the flow field
                xOffset += noiseIncrement; // moving through Perlin noise space over the x axis
            }
            yOffset += noiseIncrement;
            // with zOffset each vector cahnge direction sligthly frame by frame making the flow field dynamic
            zOffset += zIncrement;
        }
        
        /*
        * Updating particles look and audio.
        * Despite all the particles are created and stored by addParticles(),
        * only the first "numParticles" are displayed and sounding
        */
        for (int i = 0; i < numParticles; i++) {
            particlesLook[i].followFlowField(flowField);
            particlesLook[i].update();
            particlesLook[i].edgesCollision();
            particlesLook[i].show(mode);
            particlesSound[i].setAmplitude(particlesLook[i].getY(), maxNumParticles);
            particlesSound[i].setPan(particlesLook[i].getX());
            particlesSound[i].boing(mode, particlesLook[i].getX(), particlesLook[i].getY());
        }  
        } // end draw
    }
    
    // Instantiating the graphical and audio component for all the 100 particles
    public void addParticles(){
        for (int i = 0; i < maxNumParticles; i++) {
            particlesLook[i] = new ParticleGraphics();
            particlesLook[i].setColor(shade, particlesLook[i].getHue());
            particlesSound[i] = new ParticleAudio(
                    particlesLook[i].color,
                    shade,
                    particlesLook[i].getX(),
                    particlesLook[i].getY(),
                    numParticles,
                    audioSystem.getRoot(),
                    mode);
            audioSystem.addParticleAudio(particlesSound[i]);
        }
    }
    
    /*
    * Retrieving the object holding the Processing sketch in order to use it
    * as Swing component and displaying it in the JPanel
    */
    public static PSurface getPSurface(ProcessingGraphics processingObject) {
        return processingObject.initSurface();
    }
    
    /*
    * ControlP5 slider to change the number of particles to display
    * when the slider is moved the value of the varialbe numParticles is
    * automatically updated with the value of the slider
    */
    private void addParticleSlider() {   
        p5.addSlider("numParticles")
          .setPosition(440, height - 60)
          .setSize(400,15)
          .setRange(1,100)
          .setValue(20)
          .getCaptionLabel().align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE).setPaddingX(0)
          ;
    }
    
    // ControlP5 buttons to change the display mode and Perlin noise seed
    private void addModeButtons() {        
        p5.addButton("particleMode")
          .setPosition(320, height - 60)
          .setSize(80,15)
          .setLabel("Particle")
          ;
        
        p5.addButton("flowMode")
          .setPosition(200, height - 60)
          .setSize(80,15)
          .setLabel("Flow")
          .setId(2)
          ;
        
        p5.addButton("newSeed")
          .setPosition(1150, height - 60) // rimangono 30 pxl a destra
          .setSize(100,15)
          .setLabel("new seed")
          .setId(2)
          ;
    }
    
    // ControlP5 buttons to change the range of colors that the particles can assume
    private void addColorButtons() {        
        p5.addButton("redColor")
          .setPosition(880, height - 60)
          .setSize(50,15)
          .setLabel("R")
          ;
        
        p5.addButton("greenColor")
          .setPosition(970, height - 60)
          .setSize(50,15)
          .setLabel("G")
          ;
        
        p5.addButton("blueColor")
          .setPosition(1060, height - 60)
          .setSize(50,15)
          .setLabel("B")
          ;
    }
    
    // ControlP5 buttons to restart, stop and conntinue particles display and audio
    public void addOptionButtons() {        
        p5.addButton("start")
          .setPosition(30, height - 60)
          .setSize(45,15)
          .setLabel("restart")
          ;
        
        p5.addButton("stopping")
          .setPosition(115, height - 60)
          .setSize(45,15)
          .setLabel("stopping")
          ;
        
        p5.addButton("continue")
          .setPosition(115, height - 90)
          .setSize(45,15)
          .setLabel("continue")
          ;
        
        p5.addButton("fps30")
          .setPosition(220, height - 90)
          .setSize(40,15)
          .setLabel("30")
          ;
       
        p5.addButton("fps60")
          .setPosition(340, height - 90)
          .setSize(40,15)
          .setLabel("60")
          ;
        
    }
    
    // ControlP5 event handler
    public void controlEvent(ControlEvent theEvent) {        
        switch (theEvent.getController().getName()) {
            case "particleMode":
                mode = "particle";
                background(0, 0, 3);
                manageParticleEnvelope("particle");
                break;
            case "flowMode":
                mode = "flow";
                background(0, 0, 3);
                manageParticleEnvelope("flow");
                break;
            case "redColor":
                shade = "red";
                changeParticleColor(shade);
                background(0, 0, 3);
                break;
            case "greenColor":
                shade = "green";
                changeParticleColor(shade);
                background(0, 0, 3);
                break;
            case "blueColor":
                shade = "blue";
                changeParticleColor(shade);
                background(0, 0, 3);
                break;
            case "newSeed":
                seed++;
                break;
            case "numParticles":
                startNumParticles();
                break;
            case "start":
                audioSystem.stopAudioSystem();
                noLoop();
                init();
                loop();
                break;
            case "stopping":
                draw = false;
                audioSystem.stopAudioSystem();
                break;
            case "continue":
                audioSystem.startAudioSystem();
                draw = true;
                break;
            case "fps30":
                frameRate(30);
                break;
            case "fps60":
                frameRate(60);
                break;
            default:
                break;
        }
    }
    
    /*
    * addParticles() function instantiated all the 100 particles but only
    * "numParticles" are active.
    * startNumParticles turn on the audio for the first numParticles and turn of
    * the audio for the remaining.
    * startNumParticles is called in the init() function (to start the defualt
    * 20 particles) and each time "numParticles" is changed with the ControlP5
    * slider
    */
    public void startNumParticles() {
        for (int i = 0; i < numParticles; i++) {
            particlesSound[i].turnOn();
        }
        for (int i = numParticles; i < maxNumParticles; i++) {
            particlesSound[i].turnOff();
        }
    }
    
    /*
    * Mode:
    * - particle: particles amplidtude have an envelope which decays really fast
    * - flow: particles amplitude if continuosly modulated by a sine wave but
    *   it has no envelope controlling the overall amplitude
    */
    public void manageParticleEnvelope(String mode) {
        if (mode.equals("particle")) {
            for (ParticleAudio particle : particlesSound) {
                particle.startEnvelope();
            }
        } else if (mode.equals("flow")) {
            for (ParticleAudio particle : particlesSound) {
                particle.stopEnvelope();
            }
        }
    }
    
    // change particle color and pitch when varialbe "shade" is changed
    // with the ControlP5 buttons
    public void changeParticleColor(String shade) {
        for (int i = 0; i < maxNumParticles; i++) {
            particlesLook[i].setColor(shade, particlesLook[i].getHue());
            particlesSound[i].setPitch(particlesLook[i].getColor(), shade, audioSystem.getRoot());
        }
    }
    
    //=========================================================================================================
    //=========================================================================================================
    //=========================================================================================================
    
    /*
    * The ParticleGraphics inner class makes the graphical part of each particle
    * moving in between the flow field in the Processing sketch
    */
    public class ParticleGraphics {
        
        private final PVector location; // particle location on the processing sketch
        private final PVector previousLocation; // particle location at previous frame
        private final PVector speed; // speed of the particle
        private final PVector acceleration; // acceleration imprinted by flow field vectors
        private final int maxspeed; // maximum particle speed
        private final float hue; // particle color hue between 0 and 1
        private float color; // particle color hue between 0 and 360
        private float transparency; // particolor alpha / transparency
        private final float transparencyIncrement;
        
        public ParticleGraphics() {
            location = new PVector(random(width), random(height)); // random initial location
            previousLocation = location.copy(); // previous location initialized with the location
            speed = new PVector(0, 0);
            acceleration = new PVector(0, 0);
            maxspeed = 4;
            hue = random(1); // initial random hue
            transparency = random(1); // intial random alpha
            transparencyIncrement = 0.01f;
        }
        

        private void update() {
            /*
            * updating the speed of the particle using the acceleration that is
            * the vector corresponding to the cell where the particle is
            */
            speed.add(acceleration);
            speed.limit(maxspeed); // bounding the speed to the maximum speed
            location.add(speed); // updating location
            acceleration.mult(0);
        }
        
        // updating acceleration
        private void applyForce(PVector force) {
            acceleration.add(force); // updating acceleration
        }
        
        // updating the previous locaiton of the particle when it moves
        private void updatePreviousLocation() {
            previousLocation.x = location.x;
            previousLocation.y = location.y;
        }
        
        private void updateTransparency() {
            transparency += transparencyIncrement;
        }
        
        /*
        * drawing the particle
        * Mode:
        * - flow: the particle is drawn as a line the connect the current location
        *   to the previous creating a path for each particle
        * - particle: eahc particle is drawn as a point
        *
        * additionally to each particle a countor made of several layers with 
        * lower alpha is drawn for more visual candy
        */
        private void show(String mode) {
            if (mode.equals("flow")) {
                // particle body
                stroke(color, 100, 100, noise(transparency) * 255);
                strokeWeight(1);
                line(location.x, location.y, previousLocation.x, previousLocation.y);

                // particle first contour
                stroke(color, 100, 100, noise(transparency) * 75);
                strokeWeight(2);
                line(location.x, location.y, previousLocation.x, previousLocation.y);

                // particle second contour
                stroke(color, 100, 100, noise(transparency) * 50);
                strokeWeight(3);
                line(location.x, location.y, previousLocation.x, previousLocation.y);

                // particle third contour
                stroke(color, 100, 100, noise(transparency) * 20);
                strokeWeight(4);
                line(location.x, location.y, previousLocation.x, previousLocation.y);

                // particle fourth contour
                stroke(color, 100, 100, noise(transparency) * 10);
                strokeWeight(5);
                line(location.x, location.y, previousLocation.x, previousLocation.y);
            } else if (mode.equals("particle")) {
                // particle body
                stroke(color, 100, 100, noise(transparency) * 255);
                strokeWeight(7);
                point(location.x, location.y);

                // particle first contour
                stroke(color, 100, 100, noise(transparency) * 75);
                strokeWeight(14);
                point(location.x, location.y);

                // particle second contour
                stroke(color, 100, 100, noise(transparency) * 50);
                strokeWeight(28);
                point(location.x, location.y);

                // particle third contour
                stroke(color, 100, 100, noise(transparency) * 20);
                strokeWeight(56);
                point(location.x, location.y);

                // particle fourth contour
                stroke(color, 100, 100, noise(transparency) * 10);
                strokeWeight(112);
                point(location.x, location.y);
            }
            
            updatePreviousLocation();
            updateTransparency();
        }
        
        /*
        * Defining the behaviour of the particle when it collides on an edge
        * of the sketch. When the particle find an edge the it reappears on the
        * other hand of the sketch. Additionally each time the particle cross
        * an edge, its previous location is updated.
        */
        private void edgesCollision() {
            if (location.x > width) {
                location.x = 0;
                updatePreviousLocation();
            }
            if (location.x < 0) {
                location.x = width;
                updatePreviousLocation();
            }
            if (location.y > height) {
                location.y = 0;
                updatePreviousLocation();
            }
            if (location.y < 0) {
                location.y = height;
                updatePreviousLocation();
            }
        }
        
        /*
        * followFlowField takes in input the array of vectors of the flow field
        * then, given the coordinates of the particle, associates the
        * particle to the flow vector of the cell it falls into ad update the
        * acceleration vector of the particle with the values of the flow vector.
        */
        private void followFlowField(PVector[] flowVectors) {
            // mapping pixel coordinates to the cell (row, column) the particle fall into
            int x  = (int)( 0 + ( (location.x - 0) * (cols - 1 - 0) ) / (1280 - 0) );
            int y  = (int)( 0 + ( (location.y - 0) * (rows - 1 - 0) ) / (720 - 0) );
            int index = x + y * cols;
            PVector flowVector = flowVectors[index];
            applyForce(flowVector);
        }
        
        // Return particle x coordinate
        public float getX() {
            return location.x;
        }
        
        // Return particle y coordinate
        public float getY() {
            return location.y;
        }
        
        // Return particle color
        public float getColor() {
            return color;
        }
        
        // return particle hue
        public float getHue() {
            return hue;
        }
        
        // Map the hue from (0, 1) to a range of colors defined by the shade parameter
        public void setColor(String shade, float hue) {
            // mapping hue to color ranges
            if (shade.equals("red")) {
                color =  map(hue, 0, 1, 216, 360);
            } else if (shade.equals("blue")) {
                color = map(hue, 0, 1, 150, 270);
            } else if (shade.equals("green")) {
                color = map(hue, 0, 1, 30, 215);
            }
        }
        
    }
    
}
