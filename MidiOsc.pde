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
      if (rack.GetObj("volume " + i).ValueChange())
      {
        volume = rack.GetObj("volume " + i).value;
      }
      if (rack.GetObj("pitch " + i).ValueChange())
      {
        float p = rack.GetObj("pitch " + i).value/2;
        if (p > 0) p *= rack.GetObj("pitch " + i).value + 1;

        pitch_init = p;
      }
      if (rack.GetObj("octave " + i).ValueChange())
      {
        float oct = rack.GetObj("octave " + i).value/2;
        if (oct > 0) oct*= rack.GetObj("octave " + i).value+1;
        oct_init = oct;
      }
      if (rack.GetObj("wave " + i).ValueChange())
      {
        buffer = GetBuffer((int)(rack.GetObj("wave " + i).value));
      }	
      if (rack.GetObj("lfo " + i).ValueChange())
      {
        lfo = (rack.GetObj("lfo " + i).value);
      }
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

