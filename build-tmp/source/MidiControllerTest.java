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

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MidiControllerTest extends PApplet {








public class PFrame extends Frame{
  public PFrame()
  {
    setBounds(0, 0, 600, 300);
    frameB = new SFrame();
    add(frameB);
    frameB.init();
    show();
  }
}

public class SFrame extends PApplet
{
  public void setup()
  {
    size(600, 300);

    //if (frame != null) {
    //frame.setResizable(true);
    //}
    noLoop();
  }

  public void draw(){
    //background(0);
  }
}

final int VOICES = 3;

public static SynthData Data;
public static ControlP5 control;
public static MidiUI MUI;

PFrame frameA;
SFrame frameB;

MidiBus testBus;
SimpleMidiListener listen;

ImageFilter filter;

boolean keyPlaying = false;
MidiVoice [] voice;

ArrayList<Note> allNotes = new ArrayList<Note>(); 

PerlinNoise pnoise;

public void setup()
{
  size(600, 300);

  frameA = new PFrame();
  Data = new SynthData();

  filter = new ImageFilter();
  filter.Start();

  control = new ControlP5(this);
  pnoise = new PerlinNoise();

  //MidiBus.list();
  testBus = new MidiBus(this, "MPKmini2", "Real Time Sequencer");
  testBus.addMidiListener(listen);

  MUI = new MidiUI();
  MUI.SetupKnobs();

  voice = new MidiVoice[VOICES];
  MidiOsc [] osc = new MidiOsc[]{new MidiOsc(), new MidiOsc(), new MidiOsc(), new MidiOsc()};

  for(int i = 0; i < VOICES; i++)
  {
    voice[i] = new MidiVoice(osc);
    voice[i].col = Data.WaveCol[i];
  }
 
}

public void draw()
{
  background(0); 
  frameB.background(0);
  Data.DebugFrameRate(30);
  Data.DebugController();

  filter.Update();

  if(allNotes.size() > 0)
  {
    for(int n = 0; n < allNotes.size(); n++)
    {
        float freq = allNotes.get(n).pitch;
        float vel = allNotes.get(n).velocity;
        MidiVoice v = FindFreeMidiVoice(freq);
        if(v!= null) 
        {
          v.GetNote(freq, vel);
          allNotes.remove(n);
        }
    }
  }

  for(int i = 0; i < VOICES; i++)
  {
    voice[i].Update();
  }
  MUI.Update();

  frameB.redraw();  
}



public MidiVoice FindFreeMidiVoice(float freq)
{
  for(MidiVoice child : voice)
  {
    if(child.GetPitch() == freq) return child;
  }
  for(MidiVoice child : voice)
  {
    if(!child.isPlaying) return child;
  }
  return voice[0];
}


public void controlEvent(ControlEvent c)
{

  for(MidiRack rack: MUI.allRacks)
  {
    rack.CheckEvent(c);
  }

  if(c.isGroup())
  {
    for(MidiList child : MUI.allLists)
    {
      child.CheckEvent(c);
    }
  }
  if(c.isController())
  {
    for(MidiKnob child : MUI.allKnobs)
    {
      child.CheckEvent(c);
    }
    for(MidiToggle child : MUI.allToggles)
    {
      child.CheckEvent(c);
    }
  }
}

public void controllerChange(int channel, int number, int value)
{
  Data.last_control = Data.new_control;
  Data.new_control = number;
  for(MidiRack child : MUI.allRacks)
  {
    child.CheckController(number, value);
  }
  for(MidiKnob child : MUI.allKnobs)
  {
    child.CheckController(number, value);
  }
  for(MidiToggle child : MUI.allToggles)
  {
    child.CheckController(number, value);
  }
  for(MidiList child : MUI.allLists)
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
  for(MidiVoice child : voice)
  {
    if(child.GetPitch() == Pitch.mtof(pitch)) 
    {
      child.Stop();
    }
  }
  for(int i = 0; i< allNotes.size(); i++)
  {
    if(allNotes.get(i).IsMidiPitch(pitch)) allNotes.remove(i);
  }
}

public void keyPressed()
{
  if(keyPlaying) allNotes.add(new Note(55, 1.0F));
  else release(55);
  keyPlaying = !keyPlaying;
}

public void release(int key)
{
  for(MidiVoice child : voice)
  {
    if(child.GetPitch() == Pitch.mtof(key)) 
    {
      child.Stop();
    }
  }
  for(int i = 0; i< allNotes.size(); i++)
  {
    if(allNotes.get(i).IsMidiPitch(key)) allNotes.remove(i);
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

		for(int i = 0; i < f_array.length; i++)
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
class ImageFilter
{
	PImage img;

	final int pixPerFrame = 200;

	boolean allTrans = false;

	int[] pixArray;

	BrainFilter filter_base, filter_static, filter_wave;

	int [] pixArrayColor;

	float vel = 2;

	public void Start()
	{
		img = loadImage("zam.png");
		img.resize(300,300);
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
				return col * color(random(200),random(10),random(10), 0.1f);
			} 
		}
		);

		filter_wave = new BrainFilter(img, pixArray);
		filter_wave.factor[0] = 50;
		filter_wave.SetFilterFunc(new Function()
		{
			public int calculate(int col, int i) {
				int fWidth = filter_wave.f_img.width;
				int fin = color(0,0,0);
				if(red(pixArrayColor[i]) < filter_wave.factor[0]) fin = color(0,100, 105);
				return fin;
				//return filter_base.output[i] + color(255, 0, 0,0.4) * (i - i % fWidth) * (frameCount*10);
			}
		}
		);

	}


	public void Update()
	{
		background(0);
		img.loadPixels();
		//SetPixels(filter_base.Update());

		vel = (abs(voice[0].aud_context.out.getValue(0, 400))) * 100;
		print(vel);
		filter_wave.factor[0] = lerp(filter_wave.factor[0], vel, frameRate/600);


		SetPixels(filter_static.Update());
		SetPixels(filter_wave.Update());

		img.updatePixels();
		image(img,0,0);
		text(filter_wave.factor[0], 25, 25);
	}

	public void keyPressed()
	{
		filter_wave.factor[0] ++;
	}

	public void SetPixels(int [] c)
	{
		for(int i = 0; i < c.length; i++)
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

		while(!foundPixels)
		{
			if(num >= img.width * img.height) foundPixels = true;
			else 
			{
				int col = img.pixels[num];
				if(alpha(col) != 0) pix.append(num);
				num ++;
			}
		}

		return pix.array();
	}
	public int FindTransparentPixel()
	{
		if(allTrans) return 0;
		int transparentPix = 0;
		boolean foundPixel = false;
		img.loadPixels();
		int num = 0;

		while(!foundPixel)
		{
			if(num >= img.width * img.height) break;
			int col = img.pixels[num];
			if(alpha(col) == 0) 
			{
				transparentPix = num;
				foundPixel = true;
			}
			num ++;
		}
		if(!foundPixel) allTrans = true;
		img.pixels[transparentPix] = color(random(255),random(255),random(255));
		img.updatePixels();
		return transparentPix;
	}

	public int [] StorePixels(int [] pix)
	{
		int [] pCol = new int[pix.length];
		for(int i = 0; i < pix.length; i++)
		{
			pCol[i] = img.pixels[pix[i]];
		}
		return pCol;
	}

}
class MidiObj
{
  

}

class MidiKnob extends MidiObj
{
  int number;
  String knobName;

  public float value;
  public float min, max, start;
  public float threshold;
  
  private Knob knob;
  private float value_hard;

  MidiKnob(int num, String name, float v_min, float v_max, float v_start, float v_threshold)
  {
    number = num;
    min = v_min;
    max = v_max;
    start = v_start;
    value = start;
    threshold = v_threshold;
    
    knobName = name;

    PVector offset = new PVector(10 + (num-1) * 70,50);
    if(num >= 5)
    {
      offset.x = 10 + (num - 5) * 70;
      offset.y = 120;
    }
    knob = control.addKnob(name, min, max, start,(int)offset.x, (int)offset.y, 50);

    MUI.allKnobs.add(this);
  }

  public void SetPosition(PVector pos, float size)
  {
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

  public void Update()
  {
    value_hard = value;
  }


  public void CheckEvent(ControlEvent c)
  {
    if(c.getName().matches(knobName)) 
    {
      value = c.getValue();
      if(value > -threshold && value < threshold){
        value = 0;
      } 
    }
  }
  
  public void CheckController(float num, float val)
  {
    if(num == number) 
    {
      value = constrain(min + (val/127 * (max-min)), min, max);

      value = ClosestStep(value);
      
      //if(value > -threshold && value < threshold) value = 0;
      knob.setValue(value);
    }
  }

  public float ClosestStep(float val)
  {
    if(threshold == 0.0F) return val;

    float remainder = val % threshold;

    if(remainder == 0.0F) return val;
    else if(remainder > threshold/2) return val + threshold - remainder;
    else return val - remainder;
  }

  public boolean ValueChange()
  {
    if(value_hard != value)
    {
      return true;
    }
    else return false;
  }
}

class MidiToggle extends MidiObj
{
  int number;
  String toggleName;

  Toggle toggle;

  public boolean value;

  private boolean value_hard;

  MidiToggle(int num, String name, boolean start)
  {
    number = num;
    toggleName = name;
    toggle = control.addToggle(name, 10 + (num-20) * 35, 10, 30, 20);
    MUI.allToggles.add(this);
    value_hard = !value;
  }

  public void SetPosition(PVector pos, PVector size)
  {
    toggle.setPosition(pos.x, pos.y);
    toggle.setSize((int)size.x, (int)size.y);
  }

    public void Update()
  {
    value_hard = value;
  }

  public void SetValue(boolean num)
  {
    value = num;
    value_hard = num;
    toggle.setValue(num);
  }

  public void SetValueUI(boolean num)
  {
    toggle.setValue(num);
  }

  public void CheckEvent(ControlEvent c)
  {
    if(c.getName().matches(toggleName)) 
    {
      value = c.getValue() == 1;
    }
  }

  public boolean ValueChange()
  {
    if(value_hard != value)
    {
      return true;
    }
    else return false;
  }
  
  public void CheckController(float num, float val)
  {
    if(num == number) 
    {
      value = val != 0;
      toggle.setValue(value);
    }
  }
}


class MidiRack
{
  MidiToggle toggle;
  MidiKnob [] knobs;

  PVector pos;
  boolean value;
  int rows = 2;
  float knob_radius = 25;
  MidiRack(MidiToggle _toggle, boolean _value, MidiKnob [] _knobs)
  {
      toggle = _toggle;
      knobs = _knobs;
      pos = new PVector(0,0);
      value = _value;

      for(int i = 0; i < knobs.length; i++)
      {
        MUI.allKnobs.remove(knobs[i]);
      }

      MUI.allRacks.add(this);
  }

  public void SetPosition(PVector _pos, int _rows)
  {
    pos = _pos;
    rows = _rows;

    int i = 0;
    int offset_x = 5, offset_y = 15;
    for(int x = 0; x < knobs.length/rows; x++)
    {
      for(int y = 0; y < rows; y++)
      {
        knobs[i].SetPosition(new PVector(pos.x + x * (knob_radius*2+offset_x), pos.y + y * (knob_radius*2+offset_y)), knob_radius);

        i++;
      }
    }
  }

  public void Update()
  {
    for(MidiKnob child : knobs) child.Update();
      toggle.Update();
  }

  public void CheckEvent(ControlEvent c)
  {
    if(c.isController())
    {
      toggle.CheckEvent(c);
      if(toggle.value != value) return;

      for(MidiKnob child : knobs)
      {
        child.CheckEvent(c);
      } 
    }
  }

  public void CheckController(float num, float val)
  {
    toggle.CheckController(num, val);
    if(toggle.value != value) return;
    for(MidiKnob child : knobs)
    {
      child.CheckController(num,val);
    }
  }
}

class MidiList extends MidiObj
{
  int number;
  String cycleName;

  DropdownList list;

  public float value;

  private float value_hard, value_max;

  MidiList(int num, String name, String [] values)
  {
    number = num;
    cycleName = name;
    value_max = values.length;
    list = control.addDropdownList(name)
                  .setPosition(10 + (num-20), 200);

    value = 0;
    value_hard = value;

    for(int i = 0; i < value_max; i++)
    {
      list.addItem(values[i], i);
    }
    MUI.allLists.add(this);
  }

  public void SetPosition(PVector pos, PVector size)
  {
    list.setPosition(pos.x, pos.y);
    list.setSize((int)size.x, (int)size.y);
  }

  public void SetValue(int num)
  {
    value = num;
    value_hard = num;
    list.setIndex(num);
  }

  public void SetValueUI(int num)
  {
    list.setIndex(num);
  }

    public void Update()
  {
    value_hard = value;
  }

  public void CheckEvent(ControlEvent c)
  {
    if(c.getName().matches(cycleName)) 
    {
      value = c.getValue();
    }
  }

  public boolean ValueChange()
  {
    if(value_hard != value)
    {
      return true;
    }
    else return false;
  }

  
  public void CheckController(float num, float val)
  {
    if(num == number) 
    {
      value++;
      if(value >= value_max) value = 0;
      list.setValue(value);
    }
  }
}
class MidiOsc
{
	public float pitch, volume;
	public float pitch_init;
	public Buffer buffer;

	public int bufferUI;
	public float pitchUI, octUI, volumeUI;

	MidiOsc()
	{
		pitch = 0.0F;
		volume = 1.0F;
		buffer = Buffer.SINE;
	}

	public void Update(int i)
	{
		if(MUI.EditingOSC(i))
		{
			MUI.SetOscUIValues(this);
			if(MUI.osc_vol.ValueChange())
			{
				//print(" - ", i, true);
			 	volumeUI = MUI.osc_vol.value;	
			 	volume = volumeUI;
			}
			if(MUI.osc_pitch.ValueChange() || MUI.osc_oct.ValueChange())
			{
				pitchUI = MUI.osc_pitch.value;
				octUI = MUI.osc_oct.value;
				float pitch = pitchUI/2;
				if(pitch > 0) pitch*=pitchUI+1;
				float oct = octUI/2;
				if(oct > 0) oct*= octUI+1;
				pitch_init = pitch + oct;
			}
			if(MUI.osc_list.ValueChange())
			{
				bufferUI = (int)(MUI.osc_list.value);
				buffer = GetBuffer(bufferUI);	
			}	
		}
		//else print(" - ", i, false);
		float m_pitch = (MUI.master_pitch.value / 2);
		if(m_pitch > 0) m_pitch *= MUI.master_pitch.value+1;
		pitch = m_pitch + pitch_init;
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
			return pnoise.generateBuffer(700);
		}
		return Buffer.SINE;
	}
}
class MidiUI
{
	MidiKnob master_vol, master_pitch, master_attack, master_lfo;
	MidiKnob osc_vol, osc_pitch, osc_oct;

	MidiKnob attack_time, decay_time, sustain_time,release_time;
	MidiKnob attack_amt, decay_amt, sustain_amt,release_amt;

	MidiList osc_list;

	MidiToggle waveform, vel_as_vol;

	MidiRack wave_rack, osc_rack, base_rack;

	MidiToggle [] edit_osc = new MidiToggle[4];

	ArrayList<MidiKnob> allKnobs = new ArrayList<MidiKnob>();
	ArrayList<MidiToggle> allToggles = new ArrayList<MidiToggle>();
	ArrayList<MidiList> allLists = new ArrayList<MidiList>();

	ArrayList<MidiRack> allRacks = new ArrayList<MidiRack>();

	MidiOsc current_osc;

	ArrayList<MidiOsc> osc_current = new ArrayList<MidiOsc>();

	MidiUI()
	{

	}

	public void SetupKnobs()
	{
  		waveform = new MidiToggle(5, "waveform", false);
  		vel_as_vol = new MidiToggle(7, "vel 2 vol", false);

  		vel_as_vol.SetPosition(new PVector(50, 10), new PVector(30, 15));
  		waveform.SetPosition(new PVector(400, 10), new PVector(30, 15));

  		WaveformRack();
  		OscRack();
  		BasicRack();
		
	}

	public void Update()
	{
		for(MidiKnob child : allKnobs) child.Update();
		for(MidiList child : allLists) child.Update();
		for(MidiToggle child : allToggles) child.Update();
		for(MidiRack child : allRacks) child.Update();
	}

	public boolean EditingOSC(int num)
	{
		if(edit_osc.length <= num) return false;

		return edit_osc[num].value;
	}

	public void SetOscUIValues(MidiOsc osc)
	{
		for(MidiOsc child : osc_current)
		{
			if(child == osc) return;
		}

		osc_current.add(osc);

		osc_list.SetValueUI(osc.bufferUI);
		osc_vol.SetValueUI(osc.volumeUI);
		osc_pitch.SetValueUI(osc.pitchUI);
		osc_oct.SetValueUI(osc.octUI);
	}

	public void RemoveOscUIValues(MidiOsc osc, int a)
	{
		if(edit_osc[a].value) return;

		for(int i = 0; i < osc_current.size(); i++)
		{
			if(osc_current.get(i) == osc)
			{
				osc_current.remove(i);
			}
		}
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

		wave_rack = new MidiRack(waveform, true, new MidiKnob[]{attack_time, attack_amt, decay_time,decay_amt, sustain_time, sustain_amt, release_time, release_amt});
		wave_rack.SetPosition(new PVector(400, 50), 2);
	}

	public void OscRack()
	{
  		edit_osc[0] = new MidiToggle(0, "OSC A", false);
  		edit_osc[1] = new MidiToggle(1, "OSC B", false);
  		edit_osc[2] = new MidiToggle(2, "OSC C", false);
  		edit_osc[3] = new MidiToggle(3, "OSC D", false);
  		osc_list = new MidiList(4, "osc type", new String[] {"SINE", "TRI", "SAW", "PERLIN"});
		osc_vol = new MidiKnob(24, "osc volume", 0, 1.0f, 0.5f, 0.0f);
  		osc_pitch = new MidiKnob(25, "osc pitch", -1.05f, 1, 0.0f, 0.05f);
  		osc_oct = new MidiKnob(26, "osc_octave", -2, 4, 0.0f, 1.0f);

		edit_osc[0].SetPosition(new PVector(10, 150), new PVector(30, 15));  		
		edit_osc[1].SetPosition(new PVector(42, 150), new PVector(30, 15));
		edit_osc[2].SetPosition(new PVector(74, 150), new PVector(30, 15));
		edit_osc[3].SetPosition(new PVector(106, 150), new PVector(30, 15));
		osc_list.SetPosition(new PVector(10, 200), new PVector(70, 100));
  		osc_vol.SetPosition(new PVector(140, 150), 25);
  		osc_pitch.SetPosition(new PVector(200, 150), 25);
  		osc_oct.SetPosition(new PVector(260, 150), 25);

  		osc_rack = new MidiRack(waveform, false, new MidiKnob[]{osc_vol, osc_pitch, osc_oct});
	}

	public void BasicRack()
	{
		master_vol = new MidiKnob(21, "m. gain", 0, 1.0f, 0.1f, 0.0f);
  		master_pitch = new MidiKnob(20, "m. pitch", -2, 5, 0.0f, 0.05f);
  		master_attack = new MidiKnob(22, "m. velocity", 0, 100, 0.0f, 0.0f);
  		master_lfo = new MidiKnob(23, "m. lfo", 0.0f, 20, 0.0f, 0.0f);

		//master_vol.SetPosition(new PVector(70, 50), 25);
		//master_pitch.SetPosition(new PVector(10, 50), 25);
		//master_attack.SetPosition(new PVector(130, 50), 25);
		//master_lfo.SetPosition(new PVector(190,50), 25);
		base_rack = new MidiRack(waveform, false, new MidiKnob[]{master_pitch, master_vol, master_attack, master_lfo});
		base_rack.SetPosition(new PVector(10, 40), 1);
	}
}
class MidiVoice
{
	public boolean isPlaying;

	public AudioContext aud_context;
	public WavePlayer [] wav_player;
	public Gain [] gain;
	public Gain master_gain;
	public UGenChain [] vol;
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

	MidiVoice(MidiOsc [] _osc)
	{
		osc = _osc;
		aud_context = new AudioContext();
		wav_player = new WavePlayer[_osc.length];
		gain = new Gain[_osc.length];

		freqGlide = new Glide[_osc.length];
		oscGainGlide = new Glide[_osc.length];

		gainEnvelope = new Envelope(aud_context, 0.0f);
		cutoffFilter = new LPRezFilter[_osc.length];
		gainGlide = new Glide(aud_context, 0, 0.0f);		

		pan = new Panner(aud_context);

		master_gain = new Gain(aud_context, 1, gainGlide);
		aud_context.out.addInput(master_gain);

		for(int i = 0; i < _osc.length;i++)
		{
			freqGlide[i] = new Glide(aud_context, 0.0f, 25);
			oscGainGlide[i] = new Glide(aud_context, osc[i].volume, 25);

			wav_player[i] = new WavePlayer(aud_context, freqGlide[i], osc[i].buffer);

			gain[i] = new Gain(aud_context, 1, oscGainGlide[i]);
			cutoffFilter[i] = new LPRezFilter(aud_context, 200.0f, 0.97f);
			cutoffFilter[i].addInput(wav_player[i]);
			//gain[i].addInput(wav_player[i]);
			gain[i].addInput(cutoffFilter[i]);

			master_gain.addInput(gain[i]);
		}
	
		aud_context.start();
	}

	public void Update()
	{
		isPlaying = GetPitch() != 0;	
		velvol = constrain(velvol + velvol_inc, 0.1F, 1.0F);

		gainGlide.setValue(MUI.master_vol.value * gainEnvelope.getCurrentValue());
		
		gainEnvelope.update();

		for(int i = 0; i < osc.length; i++)
		{
			osc[i].Update(i);
			oscGainGlide[i].setValue(osc[i].volume);
			cutoffFilter[i].setFrequency(MUI.master_lfo.value * 200);
			wav_player[i].setBuffer(osc[i].buffer);
		}

		if(isPlaying) {
			Play(current_freq);
			
		}
		
		DrawWave();
	}

	public void Play(float freq)
	{
		current_freq = freq;
		
		for(int i = 0; i < freqGlide.length; i++)
		{
			freqGlide[i].setValue(freq + (freq*osc[i].pitch));
		}
	}

	public void GetNote(float freq, float vel)
	{
		current_freq = freq;

		gainEnvelope.clear();
		gainEnvelope.addSegment(MUI.attack_amt.value, MUI.attack_time.value);
		if(MUI.decay_time.value > 0.0f) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.decay_time.value);
		if(MUI.sustain_time.value > 0.0f) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.sustain_time.value);
		if(MUI.release_time.value > 0.0f) gainEnvelope.addSegment(MUI.release_amt.value, MUI.release_time.value);

		for(int i = 0; i < freqGlide.length; i++)
		{
			if(!MUI.vel_as_vol.value) 
			{
				velvol = 1.0F;
			}
			else 
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
	
		for(int i = 0; i < freqGlide.length; i++)
		{
			freqGlide[i].setValue(base_freq);
		}
	}

	public void SetBaseFreq(float freq)
	{
		base_freq = freq;
	}

	public float GetPitch() {return current_freq;}

	public void DrawWave()
	{
		float vert_offset = 1.0F;
		float scale = 1.0F;

	  	frameB.loadPixels(); 

	  	//print(frameB.height);
	  	//for(int a = 0; a < aud_context.length; a++)
	  	//{
	  		for (int i = 0; i < frameB.width; i++)
	  		{
	  		  int buffIndex = i * aud_context.getBufferSize() / frameB.width;
	  		  int vOffset = (int) ((1+aud_context.out.getValue(0, buffIndex)) * frameB.height/2);
	  		  vOffset = min(vOffset, frameB.height);
	  		  vOffset = max(vOffset, 0);
	  		  int fin = constrain((int)(vOffset) * frameB.height + i, 0, frameB.pixels.length - 1);
	  		  frameB.pixels[fin] = this.col;
	  		}
	  	//}
	  	frameB.updatePixels();
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
   
   public boolean IsMidiPitch(float midi) {return midi_pitch == midi;}
   
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
      
      for( int j = 0; j < bufferSize; j++ )
      {
        // set the value for the new buffer
        currentValue += increment;
        b.buf[j] += (amplitude * currentValue);
        
        // if we get to a point in this envelope, generate the next point, and set up linear interpolation
        if( j >= nextPosition )
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
    for( int j = 0; j < bufferSize; j++ )
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
	SynthData() {}
	public int [] WaveCol = new int [] 
	{
		color(255,0,0),
		color(0,255,0),
		color(0,0,255),
		color(0,50,120),
		color(80,2,0)
	};

	public int RandomWaveCol ()
	{
		return WaveCol[(int)random(0, WaveCol.length)];
	}

	//Draws a framerate counter that updates on 'rate'
	public void DebugFrameRate(int rate)
	{
	  if(frameCount % rate == 0) frametext = (int)frameRate;
	  frameB.text(frametext, width-20, 10);
	}

	public void DebugController()
	{
		frameB.text(last_control, width - 20, 40);
	}
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MidiControllerTest" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
