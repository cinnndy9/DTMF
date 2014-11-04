import org.JMathStudio.DataStructure.Vector.Vector;
import org.JMathStudio.DataStructure.Vector.VectorStack;
import org.JMathStudio.Exceptions.IllegalArgumentException;
import org.JMathStudio.SignalToolkit.Utilities.SignalGenerator;


public class DTMF_Demo {
	static int [] freqTableRow = {
			697, 770, 852, 941	
	} ;
	
	static int [] freqTableCol = {
			1209, 1336, 1477
	} ;
	
	static char [][] dialKey = {
		{'1', '2', '3'},
		{'4', '5', '6'},
		{'7', '8', '9'},
		{'*', '0', '#'}
	} ;
	
	static int fs = 4000 ;				// Sampling Rate
	static float noiseLevel = 0.2f ;	// Amplitude of the noise signal before normalization
	
	private String dialKeyString ;		// The dial key sequence generated

	// This is the method that generate the dial key sequence signal
	public float [] dtmfDial() throws IllegalArgumentException {

		VectorStack numberStack = new VectorStack() ;	// A temporary storage for a list of signal vectors
		int totalLength = 0 ;	// Total length of the dial key sequence signal generated
		int row ;				// Row index into the frequency table
		int col ;				// Column index into the frequency table
		int length ;			// Length of a dial key signal
		int numDigit ;			// Number of keys to generate for a dial key sequence
		int pauseLength ;		// Length of pause signal
		int i, j, k ;			// Looping variables
		float normFactor ;		// Normalization factor for dial key sequence
		
		normFactor = 2.0f + noiseLevel ;
		dialKeyString = new String("") ;
		
		numDigit = Math.round((float) (5.0f + Math.random() * 11.0f)) ;
		for(i = 0; i < numDigit; i++) {
			row = Math.round((float) (Math.random() * 3.99 - 0.49)) ;
			col = Math.round((float) (Math.random() * 2.99 - 0.49)) ;
			length = Math.round((float) (fs/2 + Math.random() * 1.5 * fs)) ;
			pauseLength = Math.round((float) (fs/4 + Math.random() * 0.75 * fs)) ;
			Vector tone = SignalGenerator.cosine(freqTableRow[row], fs, 0.0f, length+pauseLength) ;
			Vector tone2 = SignalGenerator.cosine(freqTableCol[col], fs, 0.0f, length+pauseLength) ;
			Vector noise = SignalGenerator.random(length+pauseLength) ;

			dialKeyString += dialKey[row][col] ;	// Look up actual key dialed
			
			totalLength += (length+pauseLength) ;
			
			// Sum the two sinusoids with addition of noise
			for(j = 0; j < length; j++) {
				tone.accessVectorBuffer()[j] += tone2.accessVectorBuffer()[j] ;
				tone.accessVectorBuffer()[j] += ((2.0f * noiseLevel) * noise.accessVectorBuffer()[j] - noiseLevel);
				tone.accessVectorBuffer()[j] /= normFactor ;
			}

			// Inject pause and noise signals
			for(j = length; j < length + pauseLength; j++) {
				tone.accessVectorBuffer()[j] = ((2.0f * noiseLevel) * noise.accessVectorBuffer()[j] - noiseLevel) ;
				tone.accessVectorBuffer()[j] /= normFactor ;
			}
			
			numberStack.addVector(tone) ;	// Add the dial key signal into the stack of signal vectors
			
		}
		
		float [] dtmfSignal = new float[totalLength] ;	// Array of floats to serve as the output dial key sequence signal
		Vector [] numberVectorArray = numberStack.accessVectorArray();

		// Concatenate the dial key signals to form the output dial key sequence signal
		k = 0 ;
		for(i = 0; i < numberStack.size(); i++) {
			for(j = 0; j < numberVectorArray[i].length(); j++) {
				dtmfSignal[k] = numberVectorArray[i].accessVectorBuffer()[j] ;
				k++ ;
			}
		}
		
		return dtmfSignal ;
	}
	
	public Boolean dialKeyStringMatch(String decodedKeyString) {
		return dialKeyString.equals(decodedKeyString) ;
	}
	
	public String getDialString() {
		return dialKeyString ;
	}
	
	public static void main(String[] args) {
		DTMF_Demo demo = new DTMF_Demo() ;
		DTMF_Decode decoder = new DTMF_Decode() ;
		
		try {
			int correct = 0 ;
			int totalCount = 100 ; // XXX
			
			for(int i = 0; i < totalCount; i++) {
				// Generate Dial Key Sequence Signal
				float [] dtmfSignal = demo.dtmfDial() ;
				
				// Call your implemented dtmfAnalyse method and obtain decoded key string
				String decodedKeyString = decoder.dtmfAnalyse(dtmfSignal, fs) ;

				// Compare your decoded key sequence with the ground truth
				if(demo.dialKeyStringMatch(decodedKeyString)) {
					correct++ ;
				} else {
					// Print out incorrectly decoded dial key sequence
					System.out.println(demo.getDialString() + " (original) != " + decodedKeyString + " (decoded)") ;
				}
			}
			
			System.out.println(correct + " out of " + totalCount + " are correct.") ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

}
