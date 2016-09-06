class BrainFilter
{

  PImage f_img;
  int [] f_array;
  Function f_func;

  public float [] factor = new float[3];

  public color [] output;

  BrainFilter(PImage _img, int [] _pixels)
  {
    f_img = _img;
    f_array = _pixels;
    output = new color [_pixels.length];
  }

  public color[] Update()
  {
    for (int i = 0; i < f_array.length; i++)
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

  public abstract color calculate(color col, int i);
}

