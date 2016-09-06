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

