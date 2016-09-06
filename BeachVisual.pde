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
  SFrame frame;
  PFrame vis;
  color back;

  public int frame_rate = 120;

  public float [] value;

  public BeachVisual(String _name, PVector _size)
  {
    back = color(0);
    value = new float[1];
    frame = new SFrame();

    window_size = _size;

    vis = new PFrame(_name);
    vis.setSize((int)_size.x, (int)_size.y);
    vis.add(frame);
    vis.show();
    frame.init();
    frame.size((int)_size.x, (int)_size.y);
    frame.show();
    frame.frameRate(240);
    frame.colorMode(HSB);
  }

  public void Start()
  {
  }

  public void Update()
  {
    frame.background(back);
  }
}

class WaveVisual extends BeachVisual
{
  PImage waveImg;
  color fin_col;
  public WaveVisual()
  {

    super("WAVVVV", new PVector(600, 600));

    value[0] = 0;
    fin_col = color(255);
  }
  public void Start()
  {
    super.Start();
    waveImg = frame.createImage(frame.width, frame.height, HSB);
  }

  public void Update()
  {
    super.Update();
    float vert_offset = 0.5F;
    float scale = 1.0F;
    colorMode(HSB);

    for (int im = 0; im < waveImg.pixels.length; im++)
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

    frame.redraw();
  }
}

class ImageVisual extends BeachVisual
{
  final int pixPerFrame = 200;

  boolean allTrans = false;
  PImage img;
  int[] pixArray;
  color [] pixArrayColor;

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
      public color calculate(color col, int i)
      {
        return pixArrayColor[i];
      }
    }
    );

    filter_static = new BrainFilter(img, pixArray);
    filter_static.SetFilterFunc(new Function()
    {
      public color calculate(color col, int i) {
        color fin = col;
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
      public color calculate(color col, int i) {
        int fWidth = filter_wave.f_img.width;
        color fin = color(0, 0, 0);
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

    frame.image(img, 0, 0);
    frame.text(filter_wave.factor[0], 25, 25);
    frame.redraw();
  }

  void SetPixels(color [] c)
  {
    for (int i = 0; i < c.length; i++)
    {
      img.pixels[pixArray[i]] = c[i];
    }
  }

  int [] FindNonTransparentPixels()
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
        color col = img.pixels[num];
        if (alpha(col) != 0) pix.append(num);
        num ++;
      }
    }

    return pix.array();
  }
  int FindTransparentPixel()
  {
    if (allTrans) return 0;
    int transparentPix = 0;
    boolean foundPixel = false;
    img.loadPixels();
    int num = 0;

    while (!foundPixel)
    {
      if (num >= img.width * img.height) break;
      color col = img.pixels[num];
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

  color [] StorePixels(int [] pix)
  {
    color [] pCol = new color[pix.length];
    for (int i = 0; i < pix.length; i++)
    {
      pCol[i] = img.pixels[pix[i]];
    }
    return pCol;
  }
}