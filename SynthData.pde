class SynthData
{
  public int frametext = 0;
  public int last_control = 0;
  public int new_control = 0;
  SynthData() {
  }
  public color [] WaveCol = new color [] 
  {
    color(100, 0, 0), 
    color(0, 100, 0), 
    color(0, 0, 100), 
    color(0, 50, 60), 
    color(80, 40, 0)
  };

  public color RandomWaveCol ()
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