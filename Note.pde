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
    return 6.875 * (pow(2.0, ((3.0 + pitch))/12.0));
  }
}

