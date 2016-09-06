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
    attack_time = new MidiKnob(20, "attack time", 0.0, 400, 0.0, 0.0);
    decay_time = new MidiKnob(21, "decay time", 0.0, 400, 0.0, 0.0);
    sustain_time = new MidiKnob(22, "sustain time", 0.0, 400, 0.0, 0.0);
    release_time = new MidiKnob(23, "release time", 0.0, 400, 0.0, 0.0);

    attack_amt = new MidiKnob(24, "attack amt", 0.0, 1, 1.0, 0.0);
    decay_amt = new MidiKnob(25, "decay amt", 0.0, 1, 0.0, 0.0);
    sustain_amt = new MidiKnob(26, "sustain amt", 0.0, 1, 0.0, 0.0);
    release_amt = new MidiKnob(27, "release amt", 0.0, 1, 1.0, 0.0);

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
      vol[i] = new MidiKnob(24, "volume " + i, 0, 1.0, 1.0, 0.0);
      pitch[i] = new MidiKnob(25, "pitch " + i, -1.05, 1, 0.0, 0.05);
      oct[i] = new MidiKnob(26, "octave " + i, -2, 2, 0.0, 1.0);
      lfo[i] = new MidiKnob(27, "lfo " + i, 0.0, 20, 0.0, 0.0);
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
    master_vol = new MidiKnob(21, "m. gain", 0, 1.0, 0.2, 0.0);
    master_pitch = new MidiKnob(20, "m. pitch", -2, 5, 0.0, 0.05);
    master_attack = new MidiKnob(22, "m. velocity", 0, 100, 0.0, 0.0);
    master_lfo = new MidiKnob(23, "m. lfo", 0.0, 20, 0.0, 0.0);
    master_hfo = new MidiKnob(24, "m hfo", 0.0, 20, 0.0, 0.0);

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
    MidiKnob gate_rate = new MidiKnob(24, "rate", 1, 10, 3, 1.0);
    MidiKnob gate_thresh = new MidiKnob(25, "threshold", 0.0, 1.0, 1.0, 0.0);
    MidiKnob gate_time = new MidiKnob(26, "time", 10, 100, 10, 5.0);

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