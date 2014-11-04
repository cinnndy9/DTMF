import java.util.Arrays;

import org.JMathStudio.DataStructure.Vector.Vector;
import org.JMathStudio.Exceptions.IllegalArgumentException;
import org.JMathStudio.SignalToolkit.TransformTools.FourierSet.FFT1D;

public class DTMF_Decode {
	
	private static final double AMP_THRESHOLD = 0.8; // ratio of amplitude as threshold to distinguish a pause from a tone
	private static final double FREQ_THRESHOLD = 0.4; // limit for frequency differ from frequency resolution
	
	private static final float MIN_TIME = 0.25f; // pause can be as short as this
	
	private static int [] freqTableRow = {
		697, 770, 852, 941	
	} ;
	
	private static int [] freqTableCol = {
		1209, 1336, 1477
	} ;
	
	private static char [][] dialKey = {
		{'1', '2', '3'},
		{'4', '5', '6'},
		{'7', '8', '9'},
		{'*', '0', '#'}
	} ;
	
	// Cache for windows size and padding size
	private int lastFs = 1;
	private int lastWindowSize = 1;
	private int lastPaddingSize = 1;
	
	public String dtmfAnalyse (float dtmfSignal[], int fs) throws IllegalArgumentException  {
		
		// determine best window size
		int freqs [] = { freqTableRow[0],freqTableRow[1],freqTableRow[2],freqTableRow[3],
						 freqTableCol[0],freqTableCol[1],freqTableCol[2] };

		int windowSize = 1;
		int paddingSize = 1;
		if(lastFs == fs) {
			windowSize = lastWindowSize;
			paddingSize = lastPaddingSize;
		} else {
			// maximum windows size should not exceeding minimum period
			// best to be power of 2 according to the API
			windowSize = (int)Math.pow(2,Math.floor(Math.log(MIN_TIME * fs)/Math.log(2)));
			
			// pad the sample with zeros 
			// so that the target frequencies are not mid-way between
			paddingSize = windowSize;
			double maxDistToFreq = 0;
			do {
				maxDistToFreq = 0;
				for(int f : freqs) {
					double n = f*paddingSize/(double)fs;
					if(Math.abs(n-Math.round(n)) > maxDistToFreq)
						maxDistToFreq = Math.abs(n-Math.round(n));
				}
				paddingSize *= 2;
			} while(maxDistToFreq >= FREQ_THRESHOLD);
			paddingSize /= 2;

			lastFs = fs;
			lastWindowSize = windowSize;
			lastPaddingSize = paddingSize;
		}
		float threshold = (float)(Math.sqrt(windowSize)*AMP_THRESHOLD);
		
		char last = '\0';
		StringBuffer result = new StringBuffer();
		
		// moving window moves half windowSize every time
		for(int i=0; i<dtmfSignal.length; i+=windowSize/2) {
			 // collect samples
			 Vector samples = new Vector(Arrays.copyOfRange(dtmfSignal, i, i+windowSize));
			 
			 // do DFT with padding
			 float [] buffer = FFT1D.fft(samples, paddingSize).getMagnitude().accessVectorBuffer();
			 
			 float rowmax = 0;
			 float colmax = 0;
			 int row = 0;
			 int col = 0;
			 // find row frequency
			 for(int r=0;r<freqTableRow.length;++r) {
				 float magnitude = buffer[(int)Math.round(freqTableRow[r]*paddingSize/(double)fs)];
				 if(magnitude > rowmax) {
					 row = r;
					 rowmax = magnitude;
				 }
			 }
			 // find col frequency
			 for(int c=0;c<freqTableCol.length;++c) {
				 float magnitude = buffer[(int)Math.round(freqTableCol[c]*paddingSize/(double)fs)];
				 if(magnitude > colmax) {
					 col = c;
					 colmax = magnitude;
				 }
			 }
			 if(Math.max(rowmax, colmax) > threshold) {
				 // save dial key
				 last = dialKey[row][col];
			 } else {
				 // output dial key when pause is encountered
				 if(last != '\0') {
					 result.append(last);
					 last = '\0';
				 }
			 }			 
		}
		// output last dial key if exists
		if(last != '\0') {
			 result.append(last);
		}
		return result.toString();
	}
}
