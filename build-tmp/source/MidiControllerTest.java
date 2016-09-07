import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import beads.*; 
import org.jaudiolibs.beads.*; 
import themidibus.*; 
import controlP5.*; 
import java.util.Arrays; 
import java.awt.Frame; 
import beads.Buffer; 
import beads.BufferFactory; 
import beads.Buffer; 
import beads.BufferFactory; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MidiControllerTest extends PApplet {








final int VOICES = 1;

public static SynthData Data;
public static ControlP5 UIControl;
public static MidiUI MUI;

PFrame primary_frame;

MidiBus midi;
SimpleMidiListener midi_listener;

BeachVisual vis;

Buffer pnoise_gen;
Buffer bwhip_gen;

MidiVoice [] voice;
ArrayList<Note> allNotes = new ArrayList<Note>(); 

boolean keyPlaying;
int hits = 0;

Clock MasterTime;
AudioContext MasterTimeAud;
public void setup()
{
  
  frameRate(60);

  Data = new SynthData();
  UIControl = new ControlP5(this);
  pnoise_gen = new PerlinNoise().generateBuffer(100);
  bwhip_gen = new BeachWave().generateBuffer(10);

  //MidiBus.list();
  midi = new MidiBus(this, "MPKmini2", "Real Time Sequencer");
  midi.addMidiListener(midi_listener);

  MUI = new MidiUI();
  MUI.SetupKnobs();


  MasterTimeAud = new AudioContext();
  MasterTime = new Clock(MasterTimeAud, 512);
  MasterTimeAud.out.addDependent(MasterTime);
  MasterTimeAud.start();

  voice = new MidiVoice[VOICES];
  MidiOsc [] osc = new MidiOsc[] {
    new MidiOsc(1.0F), new MidiOsc(0.0F), new MidiOsc(0.0F), new MidiOsc(0.0F)
  };

  for (int i = 0; i < VOICES; i++)
  {
    voice[i] = new MidiVoice(osc);
    voice[i].col = Data.WaveCol[i];
  }

  Data = new SynthData();

 // vis = new WaveVisual();
 // vis.Start();
}

public void draw()
{
  background(0); 

  Data.DebugFrameRate(30);
  Data.DebugController();

  if(MUI.saveSynth.ValueChange()) Data.SaveSynth(voice, vis);
  if(MUI.loadSynth.ValueChange())  
    {
      for(int i = 0; i < voice.length; i++)
      {
        voice[i].Destroy();
      }
      voice = Data.LoadSynth();
      MUI.Update();
      return;
    }

  MUI.Update();
 // vis.Update(); 

  if (allNotes.size() > 0)
  {
    for (int n = 0; n < allNotes.size (); n++)
    {
      float freq = allNotes.get(n).pitch;
      float vel = allNotes.get(n).velocity;
      MidiVoice v = FindFreeMidiVoice(freq);
      if (v!= null) 
      {
        v.GetNote(freq, vel);
        allNotes.remove(n);
      }
    }
  }

  for (int i = 0; i < VOICES; i++)
  {
    voice[i].Update();
  }
}



public MidiVoice FindFreeMidiVoice(float freq)
{
  for (MidiVoice child : voice)
  {
    if (child.GetPitch() == freq) return child;
  }
  for (MidiVoice child : voice)
  {
    if (!child.isPlaying) return child;
  }
  return voice[0];
}


public void controlEvent(ControlEvent c)
{
  for (MidiRack rack : MUI.allRacks)
  {
    rack.CheckEvent(c);
  }

  if (c.isGroup())
  {
    for (MidiList child : MUI.allLists)
    {
      child.CheckEvent(c);
    }
  }
  if (c.isController())
  {
    for (MidiKnob child : MUI.allKnobs)
    {
      child.CheckEvent(c);
    }
    for (MidiToggle child : MUI.allToggles)
    {
      child.CheckEvent(c);
    }
  }
}

public void controllerChange(int channel, int number, int value)
{
  Data.last_control = Data.new_control;
  Data.new_control = number;
  
  for (MidiRack child : MUI.allRacks)
  {
    child.CheckController(number, value);
  }
  for (MidiKnob child : MUI.allKnobs)
  {
    child.CheckController(number, value);
  }
  for (MidiToggle child : MUI.allToggles)
  {
    child.CheckController(number, value);
  }
  for (MidiList child : MUI.allLists)
  {
    child.CheckController(number, value);
  }
}

public void noteOn(int channel, int pitch, int velocity)
{
  allNotes.add(new Note(pitch, velocity));
}

public void noteOff(int channel, int pitch, int velocity)
{
  // if(sustain.value) return;
  for (MidiVoice child : voice)
  {
    if (child.GetPitch() == Pitch.mtof(pitch)) 
    {
      child.Stop();
    }
  }
  for (int i = 0; i< allNotes.size (); i++)
  {
    if (allNotes.get(i).IsMidiPitch(pitch)) allNotes.remove(i);
  }
}

public void keyPressed()
{
  
  if (keyPlaying) allNotes.add(new Note(55, 1.0F));
  else release(55);
  keyPlaying = !keyPlaying;
}

public void release(int key)
{
  for (MidiVoice child : voice)
  {
    if (child.GetPitch() == Pitch.mtof(key)) 
    {
      child.Stop();
    }
  }
  for (int i = 0; i< allNotes.size (); i++)
  {
    if (allNotes.get(i).IsMidiPitch(key)) allNotes.remove(i);
  }
}
class BREAK
{
  float value;
  public ArrayList<MidiObj> out = new ArrayList<MidiObj>();
  public ArrayList<UGen> outUG = new ArrayList<UGen>();
  BREAK()
  {
  }

  public void AddMidiObj(MidiObj _obj)
  {
    out.add(_obj);
  }

  public void AddUGen(UGen ug)
  {
    outUG.add(ug);
  }

  public void CheckOuts()
  {
    if (out.size() > 0)
    {
      for (int i = 0; i<out.size (); i++)
      {
        out.get(i).SetValue(value);
      }
    }

    if (outUG.size() > 0)
    {
      for (int i = 0; i<outUG.size (); i++)
      {
        outUG.get(i).setValue(value);
      }
    }
  }
}

class PULSEBREAK extends BREAK
{
  float time;
  PVector range;

  float velocity;

  PULSEBREAK(float _time, float _range)
  {
    this(_time, new PVector(0, _range));
  }

  PULSEBREAK(float _time, PVector _range)
  {
    time = _time;
    range = _range;
    value = _range.x + (range.y - range.x)/2;

    velocity = abs(range.y - range.x) / (time * frameRate);
  }

  public void Update()
  {
    if (value == range.y || value == range.x) velocity = -velocity;
    float new_val = value + velocity;
    value = constrain(new_val, range.x, range.y);

    super.CheckOuts();
  }

  public void SetVelocity(float vel) {
    velocity = vel;
  }
}

class GATEBREAK extends UGen
{
  int rate;
  int threshold;
  float valueOn, valueOff;

  public float gateValue;
  int mercy_value = -1;

  GATEBREAK(AudioContext ac, int _rate, float _thresh, float _valueOff, float _valueOn)
  {
    super(ac, 1, 1);
    this.outputInitializationRegime = OutputInitializationRegime.RETAIN;
    bufOut = bufIn;
    threshold = round(_thresh * (MasterTime.getTicksPerBeat() * rate));
    rate = _rate;
    valueOn = _valueOn;
    valueOff = _valueOff;
    gateValue = valueOn;

    //mercy_value = MasterTime.getTicksPerBeat() - mercy_value;
  }

  public void SetThreshold(float _thresh)
  {
    threshold = round(_thresh * (MasterTime.getTicksPerBeat() * rate));
    //println(threshold, (MasterTime.getTicksPerBeat() * rate));
  }

  public void SetGateRate(int val)
  {
    rate = val;
  }

  protected void messageReceived(Bead b)
  {
    Clock c = (Clock) b;

    float tick = c.getCount() - (c.getBeatCount() * c.getTicksPerBeat());
    if (c.isBeat(rate))
    {
      //print(tick, "ed", threshold);
      gateValue = valueOn;
    } else if (c.getBeatCount() % rate == 0 && tick <= threshold)
    {
      //println(tick, "simple", threshold);
      gateValue = valueOn;
    } else if (c.getBeatCount() % rate == rate - 1 && tick - c.getTicksPerBeat() > mercy_value)
    {
      //println(tick, "mercy", threshold);
      gateValue = valueOn;
    } else if (c.getBeatCount() % rate < (threshold/c.getTicksPerBeat()) && tick <= threshold)
    {
      //println(tick, "newy", threshold);
      gateValue = valueOn;
    } else 
    {
      //println(tick, "off");
      gateValue = valueOff;
    }

    //if(gateValue == valueOn) println("ON");
  }

  public void calculateBuffer() {
  }
}

public class PFrame extends Frame {
  public PFrame(String name)
  {
    super(name);
    setBounds(0, 0, 600, 300);
    show();
  }
}

public class SFrame extends PApplet
{
  public void setup()
  {
    noLoop();
  }

  public void draw() {
    // background(0);
  }
}

class BeachVisual
{
  PVector window_size = new PVector(600, 600);
  //SFrame frame;
  PFrame vis;
  int back;

  public int frame_rate = 120;

  public float [] value;

  public BeachVisual(String _name, PVector _size)
  {
    back = color(0);
    value = new float[1];
    //frame = new SFrame();

    window_size = _size;

    vis = new PFrame(_name);
    vis.setSize((int)_size.x, (int)_size.y);
    //vis.add(frame);
    vis.show();
    /*frame.init();
    frame.size((int)_size.x, (int)_size.y);
    frame.show();
    frame.frameRate(240);
    frame.colorMode(HSB);*/
  }

  public void Start()
  {
  }

  public void Update()
  {
    //frame.background(back);
  }
}

class WaveVisual extends BeachVisual
{
  PImage waveImg;
  int fin_col;
  public WaveVisual()
  {

    super("WAVVVV", new PVector(600, 600));

    value[0] = 0;
    fin_col = color(255);
  }
  public void Start()
  {
    super.Start();
   // waveImg = frame.createImage(frame.width, frame.height, HSB);
  }

  public void Update()
  {
    super.Update();
    float vert_offset = 0.5F;
    float scale = 1.0F;
    colorMode(HSB);

  /*  for (int im = 0; im < waveImg.pixels.length; im++)
    {
      waveImg.pixels[im] = color(0);
    }
    waveImg.loadPixels();
    for (int a = 0; a < voice.length; a++)
    {
      for (int i = 0; i < waveImg.width; i++)
      {	

        int buffIndex = i * voice[a].aud_context.getBufferSize() / waveImg.width;
        int vOffset = (int) ((1+voice[a].aud_context.out.getValue(0, buffIndex)) * (waveImg.height * vert_offset));
        //vOffset = min(vOffset, waveImg.height);
        //vOffset = max(vOffset, 0);

        int fin = constrain((int)(vOffset) * (int)(waveImg.height * vert_offset) + i, 0, waveImg.pixels.length - 1);

        value[0] = vOffset*0.6F;
        if (value[0] > 255) value[0] = 0;

        fin_col = color((int) value[0], 255, 255);
        waveImg.pixels[fin] = fin_col;
      }
    }
    waveImg.updatePixels();
    frame.image(waveImg, 0, 0);

    frame.redraw();*/
  }
}

class ImageVisual extends BeachVisual
{
  final int pixPerFrame = 200;

  boolean allTrans = false;
  PImage img;
  int[] pixArray;
  int [] pixArrayColor;

  BrainFilter filter_base, filter_static, filter_wave;

  float vel = 2;

  public ImageVisual()
  {
    super("BRAIN", new PVector(300, 300));
  }

  public void Start()
  {
    super.Start();
    img = loadImage("Images/ham.png");
    img.resize((int)window_size.x, (int)window_size.y);
    pixArray = FindNonTransparentPixels();
    pixArrayColor = StorePixels(pixArray);

    filter_base = new BrainFilter(img, pixArray);
    filter_base.SetFilterFunc(new Function()
    {
      public int calculate(int col, int i)
      {
        return pixArrayColor[i];
      }
    }
    );

    filter_static = new BrainFilter(img, pixArray);
    filter_static.SetFilterFunc(new Function()
    {
      public int calculate(int col, int i) {
        int fin = col;
        if (hue(fin) > 255) fin = color(0, 100, 105);
        else fin += color(random(10));
        return fin;
      }
    }
    );

    filter_wave = new BrainFilter(img, pixArray);
    filter_wave.factor[0] = 50;
    filter_wave.SetFilterFunc(new Function()
    {
      public int calculate(int col, int i) {
        int fWidth = filter_wave.f_img.width;
        int fin = color(0, 0, 0);
        if (red(pixArrayColor[i]) < filter_wave.factor[0]) fin = color(filter_wave.factor[1], 255, 255);
        return fin;
        //return filter_base.output[i] + color(255, 0, 0,0.4) * (i - i % fWidth) * (frameCount*10);
      }
    }
    );
  }

  public void Update()
  {
    super.Update();
    colorMode(HSB);
    img.loadPixels();
    //SetPixels(filter_base.Update());

    vel = (abs(voice[0].aud_context.out.getValue(0, 400))) * 60;
    filter_wave.factor[0] = lerp(filter_wave.factor[0], vel, frameRate/9000);
    //SetPixels(filter_static.Update());
    SetPixels(filter_wave.Update());

    img.updatePixels();

    if (filter_wave.factor[1] > 255) filter_wave.factor[1] = 0;
    else filter_wave.factor[1]++;

   // frame.image(img, 0, 0);
   // frame.text(filter_wave.factor[0], 25, 25);
   // frame.redraw();
  }

  public void SetPixels(int [] c)
  {
    for (int i = 0; i < c.length; i++)
    {
      img.pixels[pixArray[i]] = c[i];
    }
  }

  public int [] FindNonTransparentPixels()
  {
    IntList pix = new IntList();
    boolean foundPixels = false;
    int num = 0;

    img.loadPixels();

    while (!foundPixels)
    {
      if (num >= img.width * img.height) foundPixels = true;
      else 
      {
        int col = img.pixels[num];
        if (alpha(col) != 0) pix.append(num);
        num ++;
      }
    }

    return pix.array();
  }
  public int FindTransparentPixel()
  {
    if (allTrans) return 0;
    int transparentPix = 0;
    boolean foundPixel = false;
    img.loadPixels();
    int num = 0;

    while (!foundPixel)
    {
      if (num >= img.width * img.height) break;
      int col = img.pixels[num];
      if (alpha(col) == 0) 
      {
        transparentPix = num;
        foundPixel = true;
      }
      num ++;
    }
    if (!foundPixel) allTrans = true;
    img.pixels[transparentPix] = color(random(255), random(255), random(255));
    img.updatePixels();
    return transparentPix;
  }

  public int [] StorePixels(int [] pix)
  {
    int [] pCol = new int[pix.length];
    for (int i = 0; i < pix.length; i++)
    {
      pCol[i] = img.pixels[pix[i]];
    }
    return pCol;
  }
}



public class BeachWave extends BufferFactory
{
	public Buffer generateBuffer(int bufferSize)
	{
		Buffer b = new Buffer(bufferSize);
		int mod = 10;

		for(int i = 0; i < bufferSize; i++)
		{
			//float fract = (float)i / (float)(bufferSize - 1);
			//b.buf[i] = 1f / (1f - (float)Math.log(fract));
			if(i % mod != 0) b.buf[i] = 1.0F - (1.0F / ((i % mod)));
			else b.buf[i] = 0.0F;
		}

		return b;
	}

	public String getName()
	{
		return "BeachWave";
	}
}
class BrainFilter
{

  PImage f_img;
  int [] f_array;
  Function f_func;

  public float [] factor = new float[3];

  public int [] output;

  BrainFilter(PImage _img, int [] _pixels)
  {
    f_img = _img;
    f_array = _pixels;
    output = new int [_pixels.length];
  }

  public int[] Update()
  {
    for (int i = 0; i < f_array.length; i++)
    {
      output[i] = f_func.calculate(f_img.pixels[f_array[i]], i);
    }

    return output;
  }

  public void SetFilterFunc(Function f)
  {
    f_func = f;
  }
}

public abstract class Function
{
  public Function()
  {
  }

  public abstract int calculate(int col, int i);
}

class MidiObj
{
  public PVector pos, local_pos;
  public MidiObj parent;
  public String name;

  public float value;
  public int number;
  protected float value_hard;

  public void SetPosition(PVector _pos)
  {
    local_pos = _pos;
    pos = local_pos;
    if (parent != null)   pos.add(parent.pos);
  }

  public void SetPosition(PVector _pos, float f)
  {
    local_pos = _pos;
    pos = local_pos;
    if (parent != null)   pos.add(parent.pos);
  }

  public void SetPosition(PVector _pos, PVector _size)
  {
    local_pos = _pos;
    pos = local_pos;
    if (parent != null)   pos.add(parent.pos);
  }

  public boolean ValueChange() { 
    return value_hard != value;
  }

  public void SetValue(float num)
  {
    value = num;
    value_hard = num;
  }

  public void Update()
  {
    value_hard = value;
  }

  public void CheckEvent(ControlEvent c)
  {
    if (c.getName().matches(name)) 
    {
      value = c.getValue();
    }
  }

  public void CheckController(float num, float val)
  {
    if (num == number) 
    {
      value = val;
    }
  }

  public void SetNumber(int num) { 
    number = num;
  }
}

class MidiRack extends MidiObj
{
  MidiToggle [] toggle;
  MidiObj [] obj;

  boolean [] value;
  int rows = 2;
  float knob_radius = 25;

  boolean all = false;

  MidiRack(MidiToggle _toggle, boolean _value, MidiObj [] _obj)
  {
    this(new MidiToggle[] {
      _toggle
    }
    , new boolean[] {
      _value
    }
    , _obj);
  }

  MidiRack(MidiToggle [] _toggle, boolean _value, MidiObj [] _obj)
  {
    this(_toggle, new boolean[] {_value}, _obj);
  }


  MidiRack(MidiToggle [] _toggle, boolean [] _value, MidiObj [] _obj)
  {
    toggle = _toggle;
    obj = _obj;
    pos = new PVector(0, 0);
    value = _value;

    for (int i = 0; i < obj.length; i++)
    {
      MUI.allKnobs.remove(obj[i]);
      MUI.allToggles.remove(obj[i]);
      MUI.allLists.remove(obj[i]);
      _obj[i].parent = this;
    }
    for (int i = 0; i < toggle.length; i++)
    {
      MUI.allToggles.remove(_toggle[i]);
      toggle[i].parent = this;
    }

    MUI.allRacks.add(this);
  }

  public void SetPosition(PVector _pos, int _rows)
  {
    super.SetPosition(_pos);
    rows = _rows;

    int i = 0;
    int offset_x = 5, offset_y = 15;
    for (int x = 0; x < obj.length/rows; x++)
    {
      for (int y = 0; y < rows; y++)
      {
        obj[i].SetPosition(new PVector(x * (knob_radius*2+offset_x), y * (knob_radius*2+offset_y)), knob_radius);
        i++;
      }
    }
  }

  public void Update()
  {
    for (MidiObj child : obj) child.Update();
      for (MidiToggle child : toggle) child.Update();
    }

  public void CheckEvent(ControlEvent c)
  {
    if (c.isController())
    {
      for (MidiToggle child : toggle)
      {
        child.CheckEvent(c);
      } 
     //if (!all && !GetAnyTrue()) return;
     //else if(all && !GetAllTrue()) return;

      for (MidiObj child : obj)
      {
        child.CheckEvent(c);
      }
    }
  }

  public void CheckController(float num, float val)
  {
    for (MidiToggle child : toggle)
    {
      child.CheckController(num, val);
    }
    if (!all && !GetAnyTrue()) return;
    else if(all && !GetAllTrue()) return;

    for (MidiObj child : obj)
    {
      child.CheckController(num, val);
    }
  }

  public MidiObj GetObj(String _name)
  {
    for (int i = 0; i < obj.length; i++)
    {
      if (obj[i].name.equals(_name)) return obj[i];
    }
    for (int i = 0; i < toggle.length; i++)
    {
      if (toggle[i].name.equals(_name)) return toggle[i];
    }
    return null;
  }

  public boolean GetAnyTrue()
  {
    boolean active = false;
    for (int i = 0; i < toggle.length; i++)
    {
      int v = value.length > i ? i : 0;
      
      if ((toggle[i].value == 1) == value[v]) active = true;
    }
    return active;
  }

  public boolean GetAllTrue()
  {
    int num = 0;
    for (int i = 0; i < toggle.length; i++)
    {
      int v = value.length > i ? i : 0;
      if ((toggle[i].value == 1) == value[v]) num++;
    }
    return num == toggle.length;
  }
}

class MidiKnob extends MidiObj
{
  public float min, max, start;
  public float threshold;

  private Knob knob;

  MidiKnob(int num, String _name, float v_min, float v_max, float v_start, float v_threshold)
  {
    number = num;
    min = v_min;
    max = v_max;
    start = v_start;
    value = start;
    value_hard = value;
    threshold = v_threshold;

    name = _name;

    PVector offset = new PVector(10 + (num-1) * 70, 50);
    if (num >= 5)
    {
      offset.x = 10 + (num - 5) * 70;
      offset.y = 120;
    }
    knob = UIControl.addKnob(name, min, max, start, (int)offset.x, (int)offset.y, 50);

    MUI.allKnobs.add(this);
  }

  public void SetPosition(PVector _pos, float size)
  {
    super.SetPosition(_pos);
    knob.setPosition(pos.x, pos.y);
    knob.setRadius(size);
  }

  public void SetValue(float num)
  {
    value = num;
    value_hard = num;
    knob.setValue(num);
  }

  public void SetValueUI(float num)
  {
    knob.setValue(num);
  }

  public boolean ValueChange()
  {
    return value_hard != value;
  }

  public void CheckEvent(ControlEvent c)
  {
    if (c.getName().matches(name)) 
    {
      value = c.getValue();
      if (value > -threshold && value < threshold) {
        value = 0;
      }
    }
  }

  public void CheckController(float num, float val)
  {
    if (num == number) 
    {
      value = constrain(min + (val/127 * (max-min)), min, max);

      value = ClosestStep(value);

      //if(value > -threshold && value < threshold) value = 0;
      knob.setValue(value);
    }
  }

  public float ClosestStep(float val)
  {
    if (threshold == 0.0F) return val;

    float remainder = val % threshold;

    if (remainder == 0.0F) return val;
    else if (remainder > threshold/2) return val + threshold - remainder;
    else return val - remainder;
  }
}

class MidiToggle extends MidiObj
{
  Toggle toggle;

  MidiToggle(int num, String _name, boolean start)
  {
    number = num;
    name = _name;
    toggle = UIControl.addToggle(name, 10 + (num-20) * 35, 10, 30, 20);
    MUI.allToggles.add(this);

    value = start ? 1 : 0;
    value_hard = start ? 0 : 1;
  }

  public void SetPosition(PVector _pos, PVector size)
  {
    super.SetPosition(_pos);
    toggle.setPosition(pos.x, pos.y);
    toggle.setSize((int)size.x, (int)size.y);
  }



  public void SetValue(boolean num)
  {
    value = num ? 1 : 0;
    value_hard = num ? 1 : 0;
    toggle.setValue(num);
  }

  public void SetValueUI(boolean num)
  {
    toggle.setValue(num);
  }

  public void CheckEvent(ControlEvent c)
  {
    if (c.getName().matches(name)) 
    {
      value = c.getValue();
    }
  }

  public void CheckController(float num, float val)
  {
    if (num == number) 
    {
      value = val;
      toggle.setValue(value);
    }
  }
}

class MidiList extends MidiObj
{
  DropdownList list;

  private float value_max;

  MidiList(int num, String _name, String [] values)
  {
    number = num;
    name = _name;
    value_max = values.length;
    list = UIControl.addDropdownList(name)
    .setPosition(10 + (num-20), 200);

    value = 0;
    value_hard = value;

    for (int i = 0; i < value_max; i++)
    {
      list.addItem(values[i], i);
    }
    MUI.allLists.add(this);
    pos = new PVector(0, 0);
  }

  public void SetPosition(PVector pos, PVector size)
  {
    super.SetPosition(pos);
    list.setPosition( pos.x, pos.y);
    list.setSize((int)size.x, (int)size.y);
  }

  public void SetValue(float num)
  {
    super.SetValue(num);
   // list.setIndex((int)num);
  }

  public void SetValueUI(int num)
  {
  //  list.setIndex(num);
  }

  public void CheckEvent(ControlEvent c)
  {
    if (c.getName().matches(name)) 
    {
      value = c.getValue();
    }
  }

  public void CheckController(float num, float val)
  {
    if (num == number) 
    {
      value++;
      if (value >= value_max) value = 0;
      list.setValue(value);
    }
  }
}
class MidiOsc
{
  public float pitch, volume, lfo;
  public float pitch_init, oct_init;
  public Buffer buffer;

  private MidiRack rack;

  MidiOsc(float vol) { 
    this(vol, 0.0F, Buffer.SINE);
  }

  MidiOsc(float vol, float pit) {	
    this(vol, pit, Buffer.SINE);
  }

  MidiOsc(float vol, float pit, Buffer buf)
  {
    volume = vol;

    pitch_init = pit;

    oct_init = 0.0F;

    buffer = buf;
  }

  MidiOsc(float vol, float pit, float oct, float lfo, Buffer buf)
  {
    volume = vol;

    pitch_init = pit;

    oct_init = oct;

    lfo = lfo;

    buffer = buf;
  }

  MidiOsc(MidiOsc m)
  {
    volume = m.volume;

    pitch_init = m.pitch_init;

    oct_init = m.oct_init;

    lfo = m.lfo;

    buffer = m.buffer;
  }

  public void Update(int i)
  {
    if (rack == null) 
    {
      rack = MUI.osc_rack[i];
      return;
    }
    if (MUI.EditingOSC(i))
    {
      //
      //if (rack.GetObj("volume " + i).ValueChange())
      //{
        print(rack.GetObj("volume " + i).value, volume);
        volume = rack.GetObj("volume " + i).value;
      //}
     // if (rack.GetObj("pitch " + i).ValueChange())
     // {
        float p = rack.GetObj("pitch " + i).value/2;
        if (p > 0) p *= rack.GetObj("pitch " + i).value + 1;

        pitch_init = p;
      //}
     // if (rack.GetObj("octave " + i).ValueChange())
     // {
        float oct = rack.GetObj("octave " + i).value/2;
        if (oct > 0) oct*= rack.GetObj("octave " + i).value+1;
        oct_init = oct;
     /// }
     // if (rack.GetObj("wave " + i).ValueChange())
     // {
        buffer = GetBuffer((int)(rack.GetObj("wave " + i).value));
     // }	
     // if (rack.GetObj("lfo " + i).ValueChange())
     // {
        lfo = (rack.GetObj("lfo " + i).value);
     // }
    }

    float m_pitch = (MUI.master_pitch.value / 2);
    if (m_pitch > 0) m_pitch *= MUI.master_pitch.value+1;
    pitch = m_pitch + pitch_init + oct_init;
  }

  public Buffer GetBuffer(int num)
  {
    switch(num)
    {
    case 0:
      return Buffer.SINE;
    case 1:
      return Buffer.TRIANGLE;
    case 2:
      return Buffer.SAW;
    case 3:
      return pnoise_gen;
    case 4:
      return bwhip_gen;
    }
    return Buffer.SINE;
  }
}

class MidiUI
{
  MidiKnob master_vol, master_pitch, master_attack, master_lfo, master_hfo;

  MidiKnob attack_time, decay_time, sustain_time, release_time;
  MidiKnob attack_amt, decay_amt, sustain_amt, release_amt;

  MidiList osc_list;

 	MidiToggle waveform, vel_as_vol, gate;
	MidiToggle [] edit_osc = new MidiToggle[4];
  MidiRack wave_rack, base_rack, gate_rack;

  MidiRack [] osc_rack = new MidiRack[4];


  ArrayList<MidiKnob> allKnobs = new ArrayList<MidiKnob>();
  ArrayList<MidiToggle> allToggles = new ArrayList<MidiToggle>();
  ArrayList<MidiList> allLists = new ArrayList<MidiList>();

  ArrayList<MidiRack> allRacks = new ArrayList<MidiRack>();

  MidiToggle saveSynth, loadSynth;


  MidiUI()
  {
  }

  public void SetupKnobs()
  {
  	gate = new MidiToggle(5, "gate arp", false);
    waveform = new MidiToggle(6, "waveform", false);
    vel_as_vol = new MidiToggle(4, "vel 2 vol", false);

    saveSynth = new MidiToggle(10, "save", false);
    loadSynth = new MidiToggle(11, "load", false);

    vel_as_vol.SetPosition(new PVector(50, 10), new PVector(30, 15));
    waveform.SetPosition(new PVector(400, 10), new PVector(30, 15));
     saveSynth.SetPosition(new PVector(700, 10), new PVector(30, 15));
     loadSynth.SetPosition(new PVector(750, 10), new PVector(30, 15));

    WaveformRack();
    OscRack();
    BasicRack();
    GateRack();
  }

  public void Update()
  {
    for (MidiKnob child : allKnobs) child.Update();
    for (MidiList child : allLists) child.Update();
    for (MidiToggle child : allToggles) child.Update();
    for (MidiRack child : allRacks) child.Update();
    saveSynth.Update();
    loadSynth.Update();
  }

  public boolean EditingOSC(int num)
  {
    if (edit_osc.length <= num) return false;

    return edit_osc[num].value == 1;
  }

  public void WaveformRack()
  {
    attack_time = new MidiKnob(20, "attack time", 0.0f, 400, 0.0f, 0.0f);
    decay_time = new MidiKnob(21, "decay time", 0.0f, 400, 0.0f, 0.0f);
    sustain_time = new MidiKnob(22, "sustain time", 0.0f, 400, 0.0f, 0.0f);
    release_time = new MidiKnob(23, "release time", 0.0f, 400, 0.0f, 0.0f);

    attack_amt = new MidiKnob(24, "attack amt", 0.0f, 1, 1.0f, 0.0f);
    decay_amt = new MidiKnob(25, "decay amt", 0.0f, 1, 0.0f, 0.0f);
    sustain_amt = new MidiKnob(26, "sustain amt", 0.0f, 1, 0.0f, 0.0f);
    release_amt = new MidiKnob(27, "release amt", 0.0f, 1, 1.0f, 0.0f);

    wave_rack = new MidiRack(waveform, true, new MidiKnob[] {
      attack_time, attack_amt, decay_time, decay_amt, sustain_time, sustain_amt, release_time, release_amt
    }
    );
    wave_rack.SetPosition(new PVector(450, 50), 2);
    waveform.SetPosition(new PVector(0, -40), new PVector(40, 25));
  }

  public void OscRack()
  {
    edit_osc[0] = new MidiToggle(0, "OSC A", false);
    edit_osc[1] = new MidiToggle(1, "OSC B", false);
    edit_osc[2] = new MidiToggle(2, "OSC C", false);
    edit_osc[3] = new MidiToggle(3, "OSC D", false);

    MidiKnob [] vol = new MidiKnob[4], pitch = new MidiKnob[4], oct = new MidiKnob[4], lfo = new MidiKnob[4];
    MidiList [] waves = new MidiList[4];

    for (int i = 0; i < 4; i++)
    {
      vol[i] = new MidiKnob(24, "volume " + i, 0, 1.0f, 1.0f, 0.0f);
      pitch[i] = new MidiKnob(25, "pitch " + i, -1.05f, 1, 0.0f, 0.05f);
      oct[i] = new MidiKnob(26, "octave " + i, -2, 2, 0.0f, 1.0f);
      lfo[i] = new MidiKnob(27, "lfo " + i, 0.0f, 20, 0.0f, 0.0f);
      waves[i] = new MidiList(4, "wave " + i, new String[] {
        "SINE", "TRI", "SAW", "PERLIN", "B. WHIP"
      }
      );

      osc_rack[i] = new MidiRack(edit_osc[i], true, new MidiObj[] {
        vol[i], pitch[i], oct[i], lfo[i], waves[i]
      }
      );
      osc_rack[i].SetPosition(new PVector(10 + (60 * i), 250), 4);

      waves[i].SetPosition(new PVector(0, -5), new PVector(50, 100));

      edit_osc[i].SetPosition(new PVector(0, -50), new PVector(50, 15));
    }
  }

  public void BasicRack()
  {
    master_vol = new MidiKnob(21, "m. gain", 0, 1.0f, 0.2f, 0.0f);
    master_pitch = new MidiKnob(20, "m. pitch", -2, 5, 0.0f, 0.05f);
    master_attack = new MidiKnob(22, "m. velocity", 0, 100, 0.0f, 0.0f);
    master_lfo = new MidiKnob(23, "m. lfo", 0.0f, 20, 0.0f, 0.0f);
    master_hfo = new MidiKnob(24, "m hfo", 0.0f, 20, 0.0f, 0.0f);

    MidiToggle[] t = new MidiToggle[edit_osc.length+2];
    for (int i = 0; i < edit_osc.length; i++)
    {
      t[i] = edit_osc[i];
    }
    t[t.length-2] = gate;
    t[t.length-1] = waveform;

    base_rack = new MidiRack(t, false, new MidiKnob[] {
      master_pitch, master_vol, master_attack, master_lfo, master_hfo
    }
    );
    base_rack.SetPosition(new PVector(10, 40), 1);
    base_rack.all = true;
  }

  public void GateRack()
  {
    MidiKnob gate_rate = new MidiKnob(24, "rate", 1, 10, 3, 1.0f);
    MidiKnob gate_thresh = new MidiKnob(25, "threshold", 0.0f, 1.0f, 1.0f, 0.0f);
    MidiKnob gate_time = new MidiKnob(26, "time", 10, 100, 10, 5.0f);

    gate_rack = new MidiRack(gate, true, new MidiKnob[] {
      gate_rate, gate_thresh, gate_time
    }
    );
    gate_rack.SetPosition(new PVector(260, 40), 1);
    gate.SetPosition(new PVector(0, -30), new PVector(40, 20));
  }

  public void NumberSetUI()
  {
  }
}
class MidiVoice
{
  public boolean isPlaying;

  public AudioContext aud_context;
  public WavePlayer [] wav_player;
  public Gain [] gain;
  public Gain master_gain;

  public Panner pan;

  public Glide [] freqGlide;
  public Glide [] oscGainGlide;
  public Glide gainGlide, cutoffGlide;

  public LPRezFilter [] cutoffFilter;

  public int col;

  private float base_freq = 0;
  private float current_freq = 0;

  public float velvol, velvol_inc;

  public float base_vol = 1.0F;

  private MidiOsc [] osc;

  private Envelope gainEnvelope;

  private float attack_time = 300, decay_time = 0, sustain_time = 100, release_time = 50;
  private float attack_amt = 1.0f, sustain_amt = 0.8f, release_amt = 0.0f;

  PULSEBREAK vol_pulse;
  GATEBREAK vol_gate;

  MidiVoice(MidiOsc [] _osc)
  {
    osc = _osc;
   if(aud_context == null) aud_context = new AudioContext();
    wav_player = new WavePlayer[osc.length];
    gain = new Gain[osc.length];

    freqGlide = new Glide[osc.length];
    oscGainGlide = new Glide[osc.length];

    gainEnvelope = new Envelope(aud_context, 0.0f);
    cutoffFilter = new LPRezFilter[osc.length];

    vol_gate = new GATEBREAK(aud_context, 2, 0.8f, 0, 1.0f);
    gainGlide = new Glide(aud_context, 0, 50);

    MasterTime.addMessageListener(vol_gate);

    pan = new Panner(aud_context);

    //vol_pulse = new PULSEBREAK(10, 1.0);
    //vol_pulse.AddMidiObj(MUI.master_vol);

    master_gain = new Gain(aud_context, 1, gainGlide);

    aud_context.out.addInput(master_gain);

    for (int i = 0; i < osc.length; i++)
    {
      freqGlide[i] = new Glide(aud_context, 0.0f, 25);
      oscGainGlide[i] = new Glide(aud_context, osc[i].volume, 25);

      wav_player[i] = new WavePlayer(aud_context, freqGlide[i], osc[i].buffer);

      gain[i] = new Gain(aud_context, 1, oscGainGlide[i]);

      cutoffFilter[i] = new LPRezFilter(aud_context, 200.0f, 0.97f);
      cutoffFilter[i].addInput(wav_player[i]);

      gain[i].addInput(cutoffFilter[i]);

      master_gain.addInput(gain[i]);
    }

    aud_context.start();
  }

  public void Update()
  {
    isPlaying = GetPitch() != 0;	
    velvol = constrain(velvol + velvol_inc, 0.1F, 1.0F);

    vol_gate.SetThreshold(MUI.gate_rack.GetObj("threshold").value);
    vol_gate.SetGateRate((int)(MUI.gate_rack.GetObj("rate").value));

    gainEnvelope.update();

    gainGlide.setValue(vol_gate.gateValue * MUI.master_vol.value * gainEnvelope.getCurrentValue());

    gainGlide.setGlideTime(MUI.gate_rack.GetObj("time").value);
    for (int i = 0; i < osc.length; i++)
    {
      osc[i].Update(i);
      oscGainGlide[i].setValue(osc[i].volume);
      cutoffFilter[i].setFrequency(1 + (MUI.master_lfo.value + osc[i].lfo) * 200);
      wav_player[i].setBuffer(osc[i].buffer);
    }
    println();

    if (isPlaying) {
      Play(current_freq);
    }
  }

  public void Play(float freq)
  {
    current_freq = freq;

    for (int i = 0; i < freqGlide.length; i++)
    {
      freqGlide[i].setValue(freq + (freq*osc[i].pitch));
    }

  }

  public void GetNote(float freq, float vel)
  {
    current_freq = freq;

    gainEnvelope.clear();
    gainEnvelope.addSegment(MUI.attack_amt.value, MUI.attack_time.value);
    if (MUI.decay_time.value > 0.0f) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.decay_time.value);
    if (MUI.sustain_time.value > 0.0f) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.sustain_time.value);
    if (MUI.release_time.value > 0.0f) gainEnvelope.addSegment(MUI.release_amt.value, MUI.release_time.value);

    for (int i = 0; i < freqGlide.length; i++)
    {
      if (MUI.vel_as_vol.value != 1) 
      {
        velvol = 1.0F;
      } else 
      {
        velvol_inc = constrain(vel/120, 0.03f, 1.0f);
        velvol = 0.0F;
      }
      freqGlide[i].setValue(freq + (freq*osc[i].pitch));
      freqGlide[i].setGlideTime(MUI.master_attack.value);
    }
  }


  public void Stop()
  {
    current_freq = base_freq;

    gainEnvelope.clear();

    for (int i = 0; i < freqGlide.length; i++)
    {
      freqGlide[i].setValue(base_freq);
    }
  }

  public void SetBaseFreq(float freq)
  {
    base_freq = freq;
  }

  public float GetPitch() {
    return current_freq;
  }

  public void Destroy()
  {
    // aud_context.kill();

    gainEnvelope.kill();

    vol_gate.kill();
    gainGlide.kill();

    pan.kill();

    for (int i = 0; i < osc.length; i++)
    {
      freqGlide[i].kill();
      oscGainGlide[i].kill();

      wav_player[i].kill();

      gain[i].kill();

      cutoffFilter[i].kill();
    }
  }
}
static class Note
{
  public float pitch;
  public float velocity;
  public float channel;


  private float midi_pitch;

  Note(float _p, float _v, float _c)
  {
    midi_pitch = _p;
    pitch = Pitch.mtof(_p);
    velocity = _v;
    channel = _c;
  }

  Note(float _p, float _v)
  {
    midi_pitch = _p;
    pitch = Pitch.mtof(_p);
    velocity = _v;
  }

  Note(float _p)
  {
    midi_pitch = _p;
    pitch = Pitch.mtof(_p);
  }

  public boolean IsMidiPitch(float midi) {
    return midi_pitch == midi;
  }

  public static float MidiToWave(float pitch)
  {
    return 6.875f * (pow(2.0f, ((3.0f + pitch))/12.0f));
  }
}


//package net.beadsproject.beads.data.buffers;
//package evansbeads;

//import net.beadsproject.beads.data.Buffer;
//import net.beadsproject.beads.data.BufferFactory;




// this class fakes up some perlin noise
// all it does is sum up a bunch of envelopes of different scales
// in other words, 1 envelope that only has a few points, plus another envelope with a few more points, ... and so on
// it's a simplified Perlin Noise generator that doesn't comply with the reproducibility of the actual Perlin Noise equation
public class PerlinNoise extends BufferFactory
{

  public Buffer generateBuffer(int bufferSize)
  {
    return generateBuffer(bufferSize, 7, 0.5f);
  }
  public Buffer generateBuffer(int bufferSize, int numberOfLayers, float persistence)
  {
    Buffer b = new Buffer(bufferSize);

    int nextPosition = 0;
    int skipSize = 0;
    float currentValue = 0.0f;
    float nextValue = 0.0f;
    float increment = 0.0f;
    float amplitude = 1.0f;

    float amplitudeSum = 0.0f;
    // store the sum of the amplitudes, so that we can properly scale the end result
    for ( int i = numberOfLayers; i >= 0; i-- )
    {
      amplitudeSum += amplitude;
      amplitude *= persistence;
    }

    amplitude = 1.0f;
    for ( int i = numberOfLayers; i >= 0; i-- )
    {
      skipSize = (int)pow(2, i);
      currentValue = 1.0f - random(2.0f);
      nextValue = 1.0f - random(2.0f);
      increment = (nextValue - currentValue) / skipSize;
      nextPosition = skipSize;

      for ( int j = 0; j < bufferSize; j++ )
      {
        // set the value for the new buffer
        currentValue += increment;
        b.buf[j] += (amplitude * currentValue);

        // if we get to a point in this envelope, generate the next point, and set up linear interpolation
        if ( j >= nextPosition )
        {
          nextPosition = j + skipSize;
          nextValue = 1.0f - random(2.0f);
          increment = (nextValue - currentValue) / skipSize;
        }
      }

      // set the amplitude for the next buffer
      amplitude *= persistence;
    }

    // scale the noise to fit within -1.0 to 1.0
    for ( int j = 0; j < bufferSize; j++ )
    {
      b.buf[j] /= amplitudeSum;
    }

    return b;
  }




  public String getName() {
    return "PinkNoise";
  }
};

class SynthData
{
  public int frametext = 0;
  public int last_control = 0;
  public int new_control = 0;
  SynthData() {
  }
  public int [] WaveCol = new int [] 
  {
    color(100, 0, 0), 
    color(0, 100, 0), 
    color(0, 0, 100), 
    color(0, 50, 60), 
    color(80, 40, 0)
  };

  public int RandomWaveCol ()
  {
    return WaveCol[(int)random(0, WaveCol.length)];
  }

  //Draws a framerate counter that updates on 'rate'
  public void DebugFrameRate(int rate)
  {
    if (frameCount % rate == 0) frametext = (int)frameRate;
    text("FPS: " + frametext, 10, height - 10);
  }

  public void DebugController()
  {
    text("LAST MIDI: " + new_control, width - 90, height - 10);
  }

  private MidiOsc [] Osc;
  private MidiVoice [] Voice;
  private BeachVisual Visual;

  public void SaveSynth(MidiVoice [] _voices, BeachVisual _vis)
  {
    Voice = new MidiVoice [_voices.length];
    Osc = new MidiOsc[_voices[0].osc.length];
    Visual = _vis;
    for(int i = 0; i < Osc.length; i++)
    {
      Osc[i] = new MidiOsc(_voices[0].osc[i]);
    }

    for(int i = 0; i < Voice.length; i++)
    {
      Voice[i] = new MidiVoice(Osc);

      Voice[i].attack_time = _voices[i].attack_time;
      Voice[i].attack_amt = _voices[i].attack_amt;
      Voice[i].decay_time = _voices[i].decay_time;
      Voice[i].sustain_time = _voices[i].sustain_time;
      Voice[i].sustain_amt = _voices[i].sustain_amt;
      Voice[i].release_time = _voices[i].release_time;
      Voice[i].release_amt = _voices[i].release_amt;

      for(int o = 0; o < Voice[i].osc.length; o++)
      {
        Voice[i].cutoffFilter[o].setFrequency(_voices[i].cutoffFilter[o].getFrequency());
      }
    }

    println("saved synth");
  }

  public MidiVoice []  LoadSynth()
  {
    MidiVoice [] _voices = new MidiVoice[Voice.length];
    MidiOsc [] _osc = new MidiOsc[Osc.length];

    for(int i = 0; i < Osc.length; i++)
    {
      _osc[i] = new MidiOsc(Osc[i]);
    }

    for(int i = 0; i < Voice.length; i++)
    {
      _voices[i] = new MidiVoice(_osc);

      _voices[i].attack_time = Voice[i].attack_time;
      _voices[i].attack_amt = Voice[i].attack_amt;
      _voices[i].decay_time = Voice[i].decay_time;
      _voices[i].sustain_time = Voice[i].sustain_time;
      _voices[i].sustain_amt = Voice[i].sustain_amt;
      _voices[i].release_time = Voice[i].release_time;
      _voices[i].release_amt = Voice[i].release_amt;

      for(int o = 0; o < Voice[i].osc.length; o++)
      {
        _voices[i].cutoffFilter[o].setFrequency(Voice[i].cutoffFilter[o].getFrequency());
      }
    }
    return _voices;
  }
}
  public void settings() {  size(800, 600); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MidiControllerTest" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
