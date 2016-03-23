package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;

/**
 * This filter uses the Frequency Modified Fourier Transform algorithm 
 * (Sidlichovsky and Nesvorny 1997, Cel. Mech. 65, 137) to estimate frequencies,
 * amplitudes, and phase angles in the decomposition of a given equally-spaced
 * time series of the form x(t) + i*y(t).
 * 
 * The code for the algorithm has been copied from a version written in C,
 * which is located at David Nesvorny's
 * <a href="http://www.boulder.swri.edu/~davidn/fmft/fmft.html">website</a>.
 * 
 * @author Matt Maly
 *
 */
public class FrequencyAnalysisFilter extends AbstractMultipleSourceFilter
{
	private static final long serialVersionUID = 8341947040778001832L;
	
	private static final double GOLD_R = 0.61803399;
	private static final double GOLD_C = (1.0 - GOLD_R);
	private static final double TWOPI = 2.0 * Math.PI;
	
	private static final double MFT_NOMINAL_PRECISION = 1e-10;
	private static final double MFT_OVERLAP_EXCLUSION = 0.0;
	
	private static final double DEFAULT_MIN_FREQUENCY = -100;
	private static final double DEFAULT_MAX_FREQUENCY = 100;
	private static final int DEFAULT_NUM_FREQUENCIES = 10;
	private static final int DEFAULT_NUM_ELEMENTS_EXPONENT = 13;
		
	//Sorts vectors in 'rest' according to 'rules' without actually sorting 'rules'.
	private static void sortVectors(final double[] rules, double[]...rest) {
		Integer[] indices = new Integer[rules.length];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;
		
		Comparator<Integer> comp = new Comparator<Integer>() {
			@Override
            public int compare(Integer i, Integer j) {
				return Double.compare(rules[i], rules[j]);
			}
		};
		Arrays.sort(indices, comp);
		
		double[] copy = new double[rules.length];
		for (double[] arr : rest) {
			for (int i = 0; i < arr.length; i++)
				copy[i] = arr[i];
			for (int i = 0; i < arr.length; i++)
				arr[i] = copy[indices[arr.length - i - 1]];
		}
	}
	
	private static void swap(float[] data, int i, int j) {
		float ival = data[i];
		data[i] = data[j];
		data[j] = ival;
	}
	
	private EditableDouble minFrequency, maxFrequency;
	private EditableInt numFrequencies;
	private EditableInt numDataElements;
	
	private List<String> radioChoices;
	private int flag;
	private transient JPanel mainPanel;
	private transient ButtonGroup radioGroup;
	
	public FrequencyAnalysisFilter() {
		minFrequency = new EditableDouble(DEFAULT_MIN_FREQUENCY);
		maxFrequency = new EditableDouble(DEFAULT_MAX_FREQUENCY);
		numFrequencies = new EditableInt(DEFAULT_NUM_FREQUENCIES);
		numDataElements = new EditableInt(DEFAULT_NUM_ELEMENTS_EXPONENT);
		setupRadioChoices();
	}
	
	private void setupRadioChoices() {
		radioChoices = new ArrayList<String>();
		//radioChoices.add("Basic Fourier Transform"); (Not Implemented)
		radioChoices.add("Modified Fourier Transform");
		radioChoices.add("Frequency Modified Fourier Transform");
		radioChoices.add("Frequency Modified Fourier Transform with Additional Non-Linear Correction");
		flag = 1;
	}

	@Override
    protected boolean doingInThreads() {
		return false;
	}
	
	/**
	 * When a new input is added, the Frequency Analysis Filter sets the
	 * number of data elements used to be as large as possible.
	 */
	@Override
	public void addInput(DataSource input) {
		if (inputVector.isEmpty()) {
			int ndata = 1, exp;
			for (exp = 0; ndata <= input.getNumElements(0); exp++)
				ndata <<= 1;
			numDataElements.setValue(exp - 1);
		}
		super.addInput(input);
	}
	
	@Override
    protected void setupSpecificPanelProperties() {
		JPanel optionsPanel = new JPanel(new GridLayout(4 + radioChoices.size(), 1));
		radioGroup = new ButtonGroup();
		
		for (int i = 0; i < radioChoices.size(); i++) {
			JRadioButton choice;
			String s = radioChoices.get(i);
			if (i == 0)
				choice = new JRadioButton(s, true);
			else
				choice = new JRadioButton(s);
			choice.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					flag = radioChoices.indexOf(e.getActionCommand()) + 1;
				}
			});
			radioGroup.add(choice);
			optionsPanel.add(choice);
		}
		
		final JPanel numElementsPanel = new JPanel(new BorderLayout());
		final JLabel numElementsLabel = new JLabel("=" + (1 << numDataElements.getValue()));
		numElementsPanel.add(numDataElements.getLabeledTextField("Number of Data Elements 2^", new EditableInt.Listener() {
			@Override
            public void valueChanged() {
				if (numDataElements.getValue() < 1) {
					JOptionPane.showMessageDialog(numDataElements.getTextField(this),
							"You have to enter a positive integer here.");
					numDataElements.getTextField(this).requestFocus();
				}
				else {
					numElementsLabel.setText("=" + (1 << numDataElements.getValue()));
					numElementsPanel.validate();
					numElementsPanel.repaint();
				}
			}
		}), BorderLayout.CENTER);
		numElementsPanel.add(numElementsLabel, BorderLayout.EAST);
		
		optionsPanel.add(numElementsPanel);
		optionsPanel.add(minFrequency.getLabeledTextField("Minimum Frequency", null));
		optionsPanel.add(maxFrequency.getLabeledTextField("Maximum Frequency", null));
		optionsPanel.add(numFrequencies.getLabeledTextField("Number of Frequencies", new EditableInt.Listener() {
			@Override
            public void valueChanged() {
				if (numFrequencies.getValue() < 1) {
					JOptionPane.showMessageDialog(numFrequencies.getTextField(this),
							"You have to enter a positive integer here.");
					numFrequencies.getTextField(this).requestFocus();
				}
			}
		}));
		
		JButton propButton = new JButton("Propagate Changes");
		propButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				abstractRedoAllElements();
			}
		});
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(optionsPanel, BorderLayout.NORTH);
		mainPanel.add(propButton, BorderLayout.SOUTH);
		propPanel.add("Options", mainPanel);
	}

	@Override
    protected void redoAllElements() {
		dataVect.get(0).clear();
		DataSource ds = inputVector.get(0);
		if (ds.getNumValues(0) != 3)
			return;
		int ndata = 1 << numDataElements.getValue();
		int nfreq = numFrequencies.getValue();
		double dataSpacing = ds.getElement(1, 0).getValue(0) - ds.getElement(2, 0).getValue(1);
		double minfreq = minFrequency.getValue() * Math.PI * dataSpacing / (180.0 * 3600.0); 
		double maxfreq = maxFrequency.getValue() * Math.PI * dataSpacing / (180.0 * 3600.0);
		
		double leftf, centerf, rightf, fac;
		
		double[] f = new double[nfreq];
		double[] A = new double[nfreq];
		double[] psi = new double[nfreq];
		
		double[] xdata = new double[ndata];
		double[] ydata = new double[ndata];
		double[] x = new double[ndata];
		double[] y = new double[ndata];
		float[] powsd = new float[ndata];
		double[][] freq = new double[3*flag][nfreq];
		double[][] amp = new double[3*flag][nfreq];
		double[][] phase = new double[3*flag][nfreq];
		
		double[][] Q = new double[nfreq][nfreq];
		double[][] alpha = new double[nfreq][nfreq];
		double[] B = new double[nfreq];
		
		double[][] output = new double[3*flag][nfreq];
		
		for (int i = 0; i < flag; i++) {
			if (i == 0) {
				for (int j = 0; j < ndata; j++) {
					DataElement e = ds.getElement(j, 0);
					xdata[j] = e.getValue(1);
					ydata[j] = e.getValue(2);
				}
			}
			
			else {
				for (int j = 1; j <= ndata; j++) {
					xdata[j-1] = 0;
					ydata[j-1] = 0;
					for (int k = 1; k <= nfreq; k++) {
						xdata[j-1] += amp[i-1][k-1] * Math.cos(freq[i-1][k-1]*(j-1) + phase[i-1][k-1]);
						ydata[j-1] += amp[i-1][k-1] * Math.sin(freq[i-1][k-1]*(j-1) + phase[i-1][k-1]);
					}
				}
			}
			
			windowPosition(x, y, xdata, ydata);
			setPowers(powsd, x, y);
			
			if (i == 0) {
				while ( ((centerf = bracket(powsd)) < minfreq) || (centerf > maxfreq)) {
					leftf = centerf - TWOPI / ndata;
					rightf = centerf + TWOPI / ndata;
					f[0] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
					double[] temp = amph(f[0], x, y);
					A[0] = temp[0];
					psi[0] = temp[1];
					
					for (int j = 0; j < ndata; j++) {
						xdata[j] -= A[0] * Math.cos(f[0]*j + psi[0]);
						ydata[j] -= A[0] * Math.sin(f[0]*j + psi[0]);
					}
					
					windowPosition(x, y, xdata, ydata);
					setPowers(powsd, x, y);
				}
			}
			else
				centerf = freq[0][0];
			
			leftf = centerf - TWOPI / ndata;
			rightf = centerf + TWOPI / ndata;
			f[0] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
			double[] ampPhase = amph(f[0], x, y);
			A[0] = ampPhase[0];
			psi[0] = ampPhase[1];
			
			for (int j = 0; j < ndata; j++) {
				xdata[j] -= A[0] * Math.cos(f[0]*j + psi[0]);
				ydata[j] -= A[0] * Math.sin(f[0]*j + psi[0]);
			}
			
			Q[0][0] = 1;
			alpha[0][0] = 1;
			for (int m = 2; m <= nfreq; m++) {
				windowPosition(x, y, xdata, ydata);
				setPowers(powsd, x, y);
				
				if (i == 0) {
					centerf = bracket(powsd);
					leftf = centerf - TWOPI / ndata;
					rightf = centerf + TWOPI / ndata;
					f[m-1] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
					
					boolean nearfreq = false;
					for (int k = 1; k <= m-1; k++) {
						if (Math.abs(f[m-1] - f[k-1]) < MFT_OVERLAP_EXCLUSION*TWOPI/ndata)
							nearfreq = true;
					}
					
					while (f[m-1] < minfreq || f[m-1] > maxfreq || nearfreq) {
						leftf = centerf - TWOPI / ndata;
						rightf = centerf + TWOPI / ndata;
						f[m-1] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
						ampPhase = amph(f[m-1], x, y);
						A[m-1] = ampPhase[0];
						psi[m-1] = ampPhase[1];
						
						for (int j = 0; j < ndata; j++) {
							xdata[j] -= A[m-1] * Math.cos(f[m-1]*j + psi[m-1]);
							ydata[j] -= A[m-1] * Math.sin(f[m-1]*j + psi[m-1]);
						}
						
						windowPosition(x, y, xdata, ydata);
						setPowers(powsd, x, y);
						centerf = bracket(powsd);
						leftf = centerf - TWOPI / ndata;
						rightf = centerf + TWOPI / ndata;
						f[m-1] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
						
						nearfreq = false;
						for (int k = 1; k <= m-1; k++)
							if (Math.abs(f[m-1] - f[k-1]) < MFT_OVERLAP_EXCLUSION*TWOPI/ndata)
								nearfreq = true;	
					}
				}
				
				else {
					centerf = freq[0][m-1];
					leftf = centerf - TWOPI / ndata;
					rightf = centerf + TWOPI / ndata;
					f[m-1] = goldenMean(new PhiSquarePower(), leftf, centerf, rightf, x, y);
				}
				
				ampPhase = amph(f[m-1], x, y);
				A[m-1] = ampPhase[0];
				psi[m-1] = ampPhase[1];
				
				//Equation (3)
				Q[m-1][m-1] = 1;
				for (int j = 0; j < m-1; j++) {
					fac = (f[m-1] - f[j]) * (ndata - 1.0) / 2.0;
					Q[m-1][j] = Math.sin(fac)/fac * Math.PI * Math.PI / (Math.PI*Math.PI - fac*fac);
					Q[j][m-1] = Q[m-1][j];
				}
				
				//Equation (17)
				for (int k = 0; k < m-1; k++) {
					B[k] = 0;
					for (int j = 0; j <= k; j++) {
						B[k] += -alpha[k][j] * Q[m-1][j];
					}
				}
				
				//Equation (18)
				alpha[m-1][m-1] = 1;
				for (int j = 0; j < m-1; j++)
					alpha[m-1][m-1] -= B[j]*B[j];
				alpha[m-1][m-1] = 1.0 / Math.sqrt(alpha[m-1][m-1]);
				
				//Equation (19)
				for (int k = 0; k < m-1; k++) {
					alpha[m-1][k] = 0;
					for (int j = k; j < m-1; j++)
						alpha[m-1][k] += B[j]*alpha[j][k];
					alpha[m-1][k] = alpha[m-1][m-1] * alpha[m-1][k];
				}
				
				//Equation (22)
				double xsum, ysum;
				for (int j = 1; j <= ndata; j++) {
					xsum = 0.0;
					ysum = 0.0;
					for (int k = 1; k <= m; k++) {
						fac = f[k-1] * (j-1) + (f[m-1] - f[k-1])*(ndata-1.0)/2.0 + psi[m-1];
						xsum += alpha[m-1][k-1] * Math.cos(fac);
						ysum += alpha[m-1][k-1] * Math.sin(fac);
					}
					xdata[j-1] -= alpha[m-1][m-1] * A[m-1] * xsum;
					ydata[j-1] -= alpha[m-1][m-1] * A[m-1] * ysum;
				}
			} //End of main loop.
			
			//Equation (26)
			double xsum, ysum;
			for (int k = 1; k <= nfreq; k++) {
				xsum = 0.0;
				ysum = 0.0;
				for (int j = k; j <= nfreq; j++) {
					fac = (f[j-1] - f[k-1]) * (ndata-1.0)/2.0 + psi[j-1];
					xsum += alpha[j-1][j-1] * alpha[j-1][k-1] * A[j-1] * Math.cos(fac);
					ysum += alpha[j-1][j-1] * alpha[j-1][k-1] * A[j-1] * Math.sin(fac);
				}
				A[k-1] = Math.sqrt(xsum*xsum + ysum*ysum);
				psi[k-1] = Math.atan2(ysum, xsum);
			}
			
			for (int k = 0; k < nfreq; k++) {
				freq[i][k] = f[k];
				amp[i][k] = A[k];
				phase[i][k] = psi[k];
			}
		}
		
		for (int k = 0; k < nfreq; k++) {
			output[0][k] = freq[0][k];
			output[1][k] = amp[0][k];
			output[2][k] = phase[0][k];
			
			if (output[2][k] < -Math.PI)
				output[2][k] += TWOPI;
			if (output[2][k] >= Math.PI)
				output[2][k] -= TWOPI;
		}
		
		if (flag == 2 || flag == 3) {
			for (int k = 0; k < nfreq; k++) {
				output[3][k] = freq[0][k] + (freq[0][k] - freq[1][k]);
				output[4][k] = amp[0][k] + (amp[0][k] - amp[1][k]);
				output[5][k] = phase[0][k] + (phase[0][k] - phase[1][k]);
				
				if (output[5][k] < -Math.PI)
					output[5][k] += TWOPI;
				if (output[5][k] >= Math.PI)
					output[5][k] -= TWOPI;
			}
		}
		
		if (flag == 3) {
			for (int k = 0; k < nfreq; k++) {
				output[6][k] = freq[0][k];
				if (Math.abs((fac = freq[1][k] - freq[2][k])/freq[1][k]) > MFT_NOMINAL_PRECISION)
					output[6][k] += (freq[0][k] - freq[1][k])*(freq[0][k] - freq[1][k]) / fac;
				else
					output[6][k] += freq[0][k] - freq[1][k];
				
				output[7][k] = amp[0][k];
				if (Math.abs((fac = amp[1][k] - amp[2][k])/amp[1][k]) > MFT_NOMINAL_PRECISION)
					output[7][k] += (amp[0][k] - amp[1][k])*(amp[0][k] - amp[1][k]) / fac;
				else
					output[7][k] += amp[0][k] - amp[1][k];
				
				output[8][k] = phase[0][k];
				if (Math.abs((fac = phase[1][k] - phase[2][k])/phase[1][k]) > MFT_NOMINAL_PRECISION)
					output[8][k] += (phase[0][k] - phase[1][k])*(phase[0][k] - phase[1][k]) / fac;
				else
					output[8][k] += phase[0][k] - phase[1][k];
				
				if (output[8][k] < -Math.PI)
					output[8][k] += TWOPI;
				if (output[8][k] >= Math.PI)
					output[8][k] -= TWOPI;
			}
		}
		
		//Sort the frequencies in decreasing order of amplitude.
		if (flag == 1)
			sortVectors(output[1], output[0], output[1], output[2]);
		
		else if (flag == 2) {
			sortVectors(output[4], output[0], output[1], output[2]);
			sortVectors(output[4], output[3], output[4], output[5]);
		}
		
		else if (flag == 3) {
			sortVectors(output[7], output[0], output[1], output[2]);
			sortVectors(output[7], output[3], output[4], output[5]);
			sortVectors(output[7], output[6], output[7], output[8]);
		}
		
		for (int i = 0; i < nfreq; i++) {
			double frequency = output[3*flag - 3][i] * 180.0 * 3600.0 / (Math.PI * dataSpacing);
			double amplitude = output[3*flag - 2][i];
			double phs = output[3*flag - 1][i] * 180.0 / Math.PI;
			if (phs < 0)
				phs += 360.0;
			float[] resultValues = { (float) frequency, (float) amplitude, (float) phs };
			dataVect.get(0).add(new DataElement(new int[0], resultValues));
		}
	}
	
	private void windowPosition(double[] x, double[] y, double[] xdata, double[] ydata) {
		int ndata = x.length;
		double window;
		for (int j = 0; j < ndata; j++) {
			window = TWOPI * j / (ndata - 1);
			window = (1.0 - Math.cos(window)) / 2.0;
			x[j] = window * xdata[j];
			y[j] = window * ydata[j];
		}
	}
			
	private void setPowers(float[] powsd, double[] x, double[] y) {
		int ndata = x.length;
		float[] z = new float[2 * ndata];
		for (int i = 0; i < ndata; i++) {
			z[2*i] = (float) x[i];
			z[2*i + 1] = (float) y[i];
		}
		dff(z, ndata, 1);
		for (int i = 0; i < ndata; i++)
			powsd[i] = z[2*i]*z[2*i] + z[2*i + 1]*z[2*i + 1];
	}
	
	private void dff(float[] data, int nn, int isign) {
		int n, mmax, m, istep, j;
		double wtemp, wr, wpr, wpi, wi, theta;
		float tempr, tempi;
		
		n = nn << 1;
		j = 1;
		for (int i = 1; i < n; i += 2) {
			if (j > i) {
				swap(data, j-1, i-1);
				swap(data, j, i);
			}
			
			m = n >> 1;
			while (m >= 2 && j > m) {
				j -= m;
				m >>= 1;
			}
			j += m;
		}
		
		mmax = 2;
		while (n > mmax) {
			istep = mmax << 1;
			theta = isign * (TWOPI / mmax);
			wtemp = Math.sin(0.5 * theta);
			wpr = -2.0 * wtemp * wtemp;
			wpi = Math.sin(theta);
			wr = 1.0;
			wi = 0.0;
			for (m = 1; m < mmax; m += 2) {
				for (int i = m; i <= n; i += istep) {
					j = i + mmax;
					tempr = (float) (wr * data[j-1] - wi * data[j]);
					tempi = (float) (wr * data[j] + wi * data[j-1]);
					data[j-1] = data[i-1] - tempr;
					data[j] = data[i] - tempi;
					data[i-1] += tempr;
					data[i] += tempi;
				}
				wtemp = wr;
				wr = wr*wpr - wi*wpi + wr;
				wi = wi*wpr + wtemp*wpi + wi;
			}
			mmax = istep;
		}
	}
	
	private double goldenMean(PhiFunctor phi, double ax, double bx, double cx,
			double[] xdata, double ydata[]) {
		double f1, f2, x0, x1, x2, x3;
		
		x0 = ax;
		x3 = cx;
		
		if (Math.abs(cx-bx) > Math.abs(bx-ax)) {
			x1 = bx;
			x2 = bx + GOLD_C * (cx - bx);
		}
		else {
			x2 = bx;
			x1 = bx - GOLD_C * (bx - ax);
		}
		
		f1 = phi.compute(x1, xdata, ydata);
		f2 = phi.compute(x2, xdata, ydata);
		
		while (Math.abs(x3-x0) > MFT_NOMINAL_PRECISION*(Math.abs(x1) + Math.abs(x2))) {
			if (f2 > f1) {
				x0 = x1;
				x1 = x2;
				x2 = GOLD_R * x1 + GOLD_C * x3;
				f1 = f2;
				f2 = phi.compute(x2, xdata, ydata);
			}
			else {
				x3 = x2;
				x2 = x1;
				x1 = GOLD_R * x2 + GOLD_C * x0;
				f2 = f1;
				f1 = phi.compute(x1, xdata, ydata);
			}
		}
		
		if (f1 > f2)
			return x1;
		else
			return x2;
	}
	
	private double[] phiFunction(double freq, double[] xdata, double[] ydata) {
		int n = xdata.length, nn, j;
		double c, s;
		double[] xdata2 = new double[n];
		double[] ydata2 = new double[n];
		
		xdata2[0] = xdata[0] / 2.0;
		ydata2[0] = ydata[0] / 2.0;
		xdata[n-1] = xdata[n-1] / 2.0;
		ydata[n-1] = ydata[n-1] / 2.0;
		
		for (int i = 2; i<= n-1; i++) {
			xdata2[i-1] = xdata[i-1];
			ydata2[i-1] = ydata[i-1];
		}
		
		nn = n;
		while (nn != 1) {
			nn /= 2;
			c = Math.cos(-nn * freq);
			s = Math.sin(-nn * freq);
			for (int i = 0; i < nn; i++) {
				j = i + nn;
				xdata2[i] += c*xdata2[j] - s*ydata2[j];
				ydata2[i] += c*ydata2[j] + s*xdata2[j];
			}
		}
		
		return new double[] {
				2.0 * xdata2[0] / (n-1),
				2.0 * ydata2[0] / (n-1)
		};
	}
	
	private double bracket(float[] powsd) {
		int maxj = 0, ndata = powsd.length;
		double freq = 0.0, maxpow = 0.0;
		
		for (int j = 2; j <= ndata/2 - 2; j++) {
			if (powsd[j-1] > powsd[j-2] && powsd[j-1] > powsd[j]) {
				if (powsd[j-1] > maxpow) {
					maxj = j-1;
					maxpow = powsd[j-1];
				}
			}
		}
		
		for (int j = ndata/2 + 2; j <= ndata - 1; j++) {
			if (powsd[j-1] > powsd[j-2] && powsd[j-1] > powsd[j]) {
				if (powsd[j-1] > maxpow) {
					maxj = j-1;
					maxpow = powsd[j-1];
				}
			}
		}
		
		if (powsd[0] > powsd[1] && powsd[0] > powsd[ndata-1]) {
			if (powsd[0] > maxpow) {
				maxj = 0;
				maxpow = powsd[0];
			}
		}
		
		if (maxpow == 0.0)
			JOptionPane.showMessageDialog(mainPanel, "DFT has no maximum.");
		
		if (maxj + 1 < ndata/2)
			freq = -maxj;
		if (maxj + 1 > ndata/2)
			freq = -(maxj-ndata);
		
		return (TWOPI * freq / ndata);
	}
	
	private double[] amph(double freq, double[] xdata, double[] ydata) {
		double[] ret = new double[2];
		double[] phixy = phiFunction(freq, xdata, ydata);
		ret[0] = Math.sqrt(phixy[0]*phixy[0] + phixy[1]*phixy[1]);
		ret[1] = Math.atan2(phixy[1], phixy[0]);
		return ret;
	}

	@Override
    public String getParameterDescription(int stream, int which) {
		return inputVector.get(0).getParameterDescription(stream, which);
	}

	@Override
    public String getValueDescription(int stream, int which) {
		return inputVector.get(0).getValueDescription(stream, which);
	}

	@Override
    public GraphElement copy(List<GraphElement> l) {
		return null;
	}

	@Override
    public String getDescription() {
		return "Frequency Analysis Filter";
	}
	
	public static String getTypeDescription() {
		return "Frequency Analysis Filter";
	}
	
	private interface PhiFunctor {
		double compute(double freq, double[] xdata, double[] ydata);
	}
	
	private class PhiSquarePower implements PhiFunctor {
		@Override
        public double compute(double freq, double[] xdata, double[] ydata) {
			double[] phixy = phiFunction(freq, xdata, ydata);
			return phixy[0]*phixy[0] + phixy[1]*phixy[1];
		}
	}
}