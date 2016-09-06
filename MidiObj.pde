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

  float ClosestStep(float val)
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
    list.setIndex((int)num);
  }

  public void SetValueUI(int num)
  {
    list.setIndex(num);
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