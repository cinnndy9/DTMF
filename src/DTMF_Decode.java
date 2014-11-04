import java.util.ArrayList;

import org.JMathStudio.DataStructure.Vector.CVector;
import org.JMathStudio.DataStructure.Vector.Vector;
import org.JMathStudio.Exceptions.IllegalArgumentException;
import org.JMathStudio.SignalToolkit.TransformTools.FourierSet.FFT1D;

public class DTMF_Decode {

	static char[][] dialKey = { { '1', '2', '3' }, { '4', '5', '6' }, { '7', '8', '9' },
			{ '*', '0', '#' } };

	static int[] HIGH_FREQUENCY_BOUNDS = { 1273, 1407, 1580 };
	static int[] LOW_FREQUENCY_BOUNDS = { 734, 811, 897, 990 };

	static float noiseLevel = 0.2f;

	static float PAUSE_DURATION_MIN = 0.25f;
	static float TONE_DURATION_MIN = 0.5f;

	static int TONE_END = 1;
	static int TONE_START = 0;

	public String dtmfAnalyse(float dtmfSignal[], int fs) throws IllegalArgumentException {

		int N = fs;
		FFT1D fft1d = new FFT1D(N);
		Integer[][] tones = findTones(dtmfSignal, fs); // row0 for tone starts,
														// row1 for tone ends

		String sequence = "";
		String key = null;
		int start, end;
		float[] toneSegment;
		float[] nPointTone;
		CVector dft;

		for (int i = 0; i < tones[TONE_END].length; i++) {
			start = tones[TONE_START][i];
			end = tones[TONE_END][i];

			toneSegment = getSubArrayByStartEnd(dtmfSignal, start, end);
			nPointTone = getNPointSignal(toneSegment, N);
			dft = fft1d.fft1D(new Vector(nPointTone));

			float[] amplitues = dft.accessRealPart().accessVectorBuffer();
			int majorLowF = getIndexOfMaxInSubarray(amplitues, 670, 990);
			int majorHighF = getIndexOfMaxInSubarray(amplitues, 1180, 1580);

			key = getKeyByFreq(majorLowF, majorHighF);
			sequence = sequence.concat(key);
		}

		return sequence;
	}

	protected Integer[][] findTones(float[] signal, int fs) {
		int length = signal.length;
		int pauseNumMin = (int) (PAUSE_DURATION_MIN * fs);

		ArrayList<Integer> toneStarts = new ArrayList<Integer>();
		ArrayList<Integer> toneEnds = new ArrayList<Integer>();

		int noiseCount = 0;
		int i, tempEnd = 0;

		toneStarts.add(0);

		for (i = 0; i < length; i++) {
			if (naiveIsNoise(signal[i], noiseLevel)) {
				noiseCount++;
			} else {
				if (noiseCount >= pauseNumMin) {
					toneStarts.add(i);
					toneEnds.add(tempEnd);
				}
				noiseCount = 0;
				tempEnd = i;
			}
		}

		toneEnds.add(tempEnd);

		Integer[][] tones = new Integer[2][toneStarts.size()];
		tones[TONE_START] = toneStarts.toArray(new Integer[0]);
		tones[TONE_END] = toneEnds.toArray(new Integer[0]);

		return tones;
	}

	protected int getIndexOfMaxInSubarray(float[] array, int indexFrom, int indexTo) {
		float max = 0f;
		int index = 0;

		for (int i = indexFrom; i <= indexTo; i++) {
			if (array[i] > max) {
				max = array[i];
				index = i;
			}
		}

		return index;
	}

	protected String getKeyByFreq(int lowF, int highF) {
		int row = -1, col = -1;
		int i;

		for (i = 0; i < LOW_FREQUENCY_BOUNDS.length; i++) {
			if (lowF < LOW_FREQUENCY_BOUNDS[i]) {
				row = i;
				break;
			}
		}

		for (i = 0; i < HIGH_FREQUENCY_BOUNDS.length; i++) {
			if (highF < HIGH_FREQUENCY_BOUNDS[i]) {
				col = i;
				break;
			}
		}

		if (row < 0) {
			return "[low not found]";
		}

		if (col < 0) {
			return "[high not found]";
		}

		return String.valueOf(dialKey[row][col]);
	}

	protected float[] getNPointSignal(float[] dtmfSignal, int n) {
		int length = dtmfSignal.length;
		int interval = 1;

		if (length >= n) {
			interval = (length - 1) / (n - 1);
		}

		float[] reval = new float[n];

		for (int i = 0, j = 0; i < n && j < length; i++, j += interval) {
			reval[i] = dtmfSignal[j];
		}

		return reval;
	}

	protected float[] getSubArrayByStartEnd(float[] array, int startIndex, int endIndex) {
		float[] segment = new float[endIndex - startIndex + 1];

		for (int i = startIndex; i <= endIndex; i++) {
			segment[i - startIndex] = array[i];
		}

		return segment;
	}

	protected boolean naiveIsNoise(float point, float noiseLevel) {
		if (point <= noiseLevel) {
			return true;
		}
		return false;
	}

}
