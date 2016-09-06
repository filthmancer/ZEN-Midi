
//package net.beadsproject.beads.data.buffers;
//package evansbeads;

//import net.beadsproject.beads.data.Buffer;
//import net.beadsproject.beads.data.BufferFactory;

import beads.Buffer;
import beads.BufferFactory;

// this class fakes up some perlin noise
// all it does is sum up a bunch of envelopes of different scales
// in other words, 1 envelope that only has a few points, plus another envelope with a few more points, ... and so on
// it's a simplified Perlin Noise generator that doesn't comply with the reproducibility of the actual Perlin Noise equation
public class PerlinNoise extends BufferFactory
{

  public Buffer generateBuffer(int bufferSize)
  {
    return generateBuffer(bufferSize, 7, 0.5);
  }
  public Buffer generateBuffer(int bufferSize, int numberOfLayers, float persistence)
  {
    Buffer b = new Buffer(bufferSize);

    int nextPosition = 0;
    int skipSize = 0;
    float currentValue = 0.0;
    float nextValue = 0.0;
    float increment = 0.0;
    float amplitude = 1.0;

    float amplitudeSum = 0.0;
    // store the sum of the amplitudes, so that we can properly scale the end result
    for ( int i = numberOfLayers; i >= 0; i-- )
    {
      amplitudeSum += amplitude;
      amplitude *= persistence;
    }

    amplitude = 1.0;
    for ( int i = numberOfLayers; i >= 0; i-- )
    {
      skipSize = (int)pow(2, i);
      currentValue = 1.0 - random(2.0);
      nextValue = 1.0 - random(2.0);
      increment = (nextValue - currentValue) / skipSize;
      nextPosition = skipSize;

      for ( int j = 0; j < bufferSize; j++ )
      {
        // set the value for the new buffer
        currentValue += increment;
        b.buf[j] += (amplitude * currentValue);

        // if we get to a point in this envelope, generate the next point, and set up linear interpolation
        if ( j >= nextPosition )
        {
          nextPosition = j + skipSize;
          nextValue = 1.0 - random(2.0);
          increment = (nextValue - currentValue) / skipSize;
        }
      }

      // set the amplitude for the next buffer
      amplitude *= persistence;
    }

    // scale the noise to fit within -1.0 to 1.0
    for ( int j = 0; j < bufferSize; j++ )
    {
      b.buf[j] /= amplitudeSum;
    }

    return b;
  }




  public String getName() {
    return "PinkNoise";
  }
};

