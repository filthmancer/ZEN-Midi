import beads.*;
import org.jaudiolibs.beads.*;
import themidibus.*;
import controlP5.*;
import java.util.Arrays;
import java.awt.Frame;

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
void setup()
{
  size(800, 600);
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

  vis = new WaveVisual();
  vis.Start();
}

void draw()
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
  vis.Update(); 

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



MidiVoice FindFreeMidiVoice(float freq)
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


void controlEvent(ControlEvent c)
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

void controllerChange(int channel, int number, int value)
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

void noteOn(int channel, int pitch, int velocity)
{
  allNotes.add(new Note(pitch, velocity));
}

void noteOff(int channel, int pitch, int velocity)
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

void keyPressed()
{
  if (keyPlaying) allNotes.add(new Note(55, 1.0F));
  else release(55);
  keyPlaying = !keyPlaying;
}

void release(int key)
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