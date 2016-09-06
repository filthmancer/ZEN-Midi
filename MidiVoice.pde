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

  public color col;

  private float base_freq = 0;
  private float current_freq = 0;

  public float velvol, velvol_inc;

  public float base_vol = 1.0F;

  private MidiOsc [] osc;

  private Envelope gainEnvelope;

  private float attack_time = 300, decay_time = 0, sustain_time = 100, release_time = 50;
  private float attack_amt = 1.0, sustain_amt = 0.8, release_amt = 0.0;

  PULSEBREAK vol_pulse;
  GATEBREAK vol_gate;

  MidiVoice(MidiOsc [] _osc)
  {
    osc = _osc;
   if(aud_context == null) aud_context = new AudioContext();
    wav_player = new WavePlayer[_osc.length];
    gain = new Gain[_osc.length];

    freqGlide = new Glide[_osc.length];
    oscGainGlide = new Glide[_osc.length];

    gainEnvelope = new Envelope(aud_context, 0.0);
    cutoffFilter = new LPRezFilter[_osc.length];

    vol_gate = new GATEBREAK(aud_context, 2, 0.8, 0, 1.0);
    gainGlide = new Glide(aud_context, 0, 50);

    MasterTime.addMessageListener(vol_gate);

    pan = new Panner(aud_context);

    //vol_pulse = new PULSEBREAK(10, 1.0);
    //vol_pulse.AddMidiObj(MUI.master_vol);

    master_gain = new Gain(aud_context, 1, gainGlide);

    aud_context.out.addInput(master_gain);

    for (int i = 0; i < _osc.length; i++)
    {
      freqGlide[i] = new Glide(aud_context, 0.0, 25);
      oscGainGlide[i] = new Glide(aud_context, osc[i].volume, 25);

      wav_player[i] = new WavePlayer(aud_context, freqGlide[i], osc[i].buffer);

      gain[i] = new Gain(aud_context, 1, oscGainGlide[i]);

      cutoffFilter[i] = new LPRezFilter(aud_context, 200.0, 0.97);
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
    if (MUI.decay_time.value > 0.0) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.decay_time.value);
    if (MUI.sustain_time.value > 0.0) gainEnvelope.addSegment(MUI.sustain_amt.value, MUI.sustain_time.value);
    if (MUI.release_time.value > 0.0) gainEnvelope.addSegment(MUI.release_amt.value, MUI.release_time.value);

    for (int i = 0; i < freqGlide.length; i++)
    {
      if (MUI.vel_as_vol.value != 1) 
      {
        velvol = 1.0F;
      } else 
      {
        velvol_inc = constrain(vel/120, 0.03, 1.0);
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