import beads.Buffer;
import beads.BufferFactory;

public class BeachWave extends BufferFactory
{
	public Buffer generateBuffer(int bufferSize)
	{
		Buffer b = new Buffer(bufferSize);
		int mod = 10;

		for(int i = 0; i < bufferSize; i++)
		{
			//float fract = (float)i / (float)(bufferSize - 1);
			//b.buf[i] = 1f / (1f - (float)Math.log(fract));
			if(i % mod != 0) b.buf[i] = 1.0F - (1.0F / ((i % mod)));
			else b.buf[i] = 0.0F;
		}

		return b;
	}

	public String getName()
	{
		return "BeachWave";
	}
}