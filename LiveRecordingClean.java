package liverecordingclean;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import ddf.minim.analysis.FFT;
import liverecordingclean.AudioProcessing;
import liverecordingclean.Band;
import liverecordingclean.Buttons;
import processing.core.PApplet;
import processing.core.PImage;


public class LiveRecordingClean extends PApplet {

	//======================== Define Window Properties ===================================
	public final static int WINDOW_WIDTH = 800;
	public final static int WINDOW_HEIGHT = 350;
	//======================== Define FFT Related Properties ==============================
	public final static int NUM_PERFRAME = 4096;
	
	public final static int NUM_PEROCTAVE = 10;
	public final static int FREQUENCY_MIN = 22;
	//======================== Define Visual Related Properties ===========================
	public final static int WINDOW_BASELINE = WINDOW_HEIGHT - 50;
	
	public final static float STATICOVAL_CENTER_X = (float)WINDOW_WIDTH * 0.618f / 1.618f;
	public final static float STATICOVAL_CENTER_Y = (float)WINDOW_HEIGHT / 2;
	public final static float STATICOVAL_RAD_1 = (float)WINDOW_HEIGHT / 10;
	public final static float STATICOVAL_RAD_2 = STATICOVAL_RAD_1 * 0.6f;
	public final static float MOVINGOVAL_RAD = STATICOVAL_RAD_1 * 0.2f;
	
	public final static float TEXT_X = STATICOVAL_CENTER_X + STATICOVAL_RAD_1 * 1.2f;
	public final static float TEXT_Y = STATICOVAL_CENTER_Y;
	public final static int TEXT_SIZE = 26;
	
	public final static int BUTTON_RATIO = 8;
	//========================= Define Check File Length In Bytes ===============================
	public final static int FILE_LENGTH = 100;
	
	//======================== Define All Variables =======================================
	private Band[] bands = new Band[NUM_PERFRAME];
	
	public AudioProcessing audio;
	
	private SourceDataLine sourceline;
	private TargetDataLine targetline;
	private AudioFormat targetFormat = new AudioFormat(44100, 16, 1, true, false);
	private File targetFile;
	
	private FFT audioFFT;
	private byte bytesbuffer[] = new byte[NUM_PERFRAME];
	private float floatbuffer[] = new float[NUM_PERFRAME];
	private int logSize;
	private float logbuffer[];
	private float previous[];
	
	private Buttons recordbutton;
	private Buttons cutbutton;
	private Buttons replaybutton;
	private Buttons playbutton;
	private Buttons stopbutton;
	private Buttons resetbutton;
	private float rotateAngle;
	//========================= Define Flags Important =====================================
	private boolean RecordFlag = false;
	private boolean PlayFlag = false;
	
	private boolean record_THREADFLAG = true;
	//========================= Main Threads: Setup, SetupPlayer, Draw, Stop =====================
	public void setup()
	{
		size(800, 350);
		
		try {
			 targetline = AudioSystem.getTargetDataLine(targetFormat);
			 targetline.open();
		}
		catch(LineUnavailableException lue) { 
			lue.printStackTrace();
			System.err.println("TargetLine is not available!");
		}
		targetline.start();
		
		targetFile = new File("record.wav");
		
		drawRecordButtons();
		rotateAngle = 0.0f;
		
		recordbutton.enable = true;
		cutbutton.enable = false;
		replaybutton.enable = false;
		recordbutton.pressedcounter = 0;
		
		delay(1000);
	}
	
	public void setupPlayer()
	{
		for(int i = 0; i < bands.length; i++) {
			bands[i] = new Band(this);
		}
		
		try {
			URL audiourl = new URL(getDocumentBase(), "record.wav");
			audio = new AudioProcessing(audiourl);
			
			sourceline = AudioSystem.getSourceDataLine(audio.audioFormat);
			sourceline.open();
		}
		catch(MalformedURLException mre) {
			System.err.println("URL not available! File does not exist!");
			mre.printStackTrace();
		}
		catch(LineUnavailableException lue) { 
			System.err.println("SourceLine not available!");
			lue.printStackTrace();
		}
		sourceline.start();
		System.out.println("Starting Sound...");
		
		drawPlayButtons();
		
		float sampleRate = audio.audioFormat.getSampleRate();
		audioFFT = new FFT(NUM_PERFRAME, sampleRate);
		//audioFFT.window(FFT.HAMMING);
		audioFFT.logAverages(FREQUENCY_MIN, NUM_PEROCTAVE);
		logSize = audioFFT.avgSize();
		logbuffer = new float[logSize];
		previous = new float[logSize];
		
		playbutton.enable = true;
		stopbutton.enable = false;
		resetbutton.enable = true;
		
		int frameSize = audio.audioFormat.getFrameSize();
		int chunksPerFrame = (int)(sampleRate / NUM_PERFRAME * frameSize) + 1;
		frameRate(chunksPerFrame);
	}

	public void draw()
	{
		if(!PlayFlag) {
			drawRecorder();
			
			if(recordbutton.pressedcounter == 1) {
				RecordFlag = true;
				delay(10);
				if(record_THREADFLAG) {
					thread("recordAudio");
					record_THREADFLAG = false;
					System.out.println("Are you kidding me?");
				}
				System.out.println("Recording...");
			}
			if(recordbutton.pressedcounter > 1) {
				recordbutton.enable = false;
				cutbutton.enable = true;
				replaybutton.enable = false;
				drawRecordingOval();
			}
			if(cutbutton.pressed) {
				RecordFlag = false;
				recordbutton.pressedcounter = 0;
				stopRecorder();
				
				recordbutton.enable = true;
				cutbutton.enable = false;
				replaybutton.enable = true;
				
			}
			if(replaybutton.pressed) {
				RecordFlag = false;
				recordbutton.pressedcounter = 0;
				closeRecorder();
				setupPlayer();
				
				recordbutton.enable = false;
				cutbutton.enable = false;
				replaybutton.enable = true;
				
				// TODO: Check availability of file here
				if(targetFile.exists() && (targetFile.length() > FILE_LENGTH)) {
					PlayFlag = true;
					delay(1000);
				}
			}
		}
		else {
			drawPlayer();
			
			if(playbutton.pressedonce) {
				playbutton.enable = false;
				stopbutton.enable = true;
				resetbutton.enable = true;
				
				playAudio();
				convertAudio();
				computeFFT();
				drawLogarithm();
				storePreFrame();
			}
			if(stopbutton.pressed) {
				playbutton.pressedonce = false;
				stopPlayer();
				
				playbutton.enable = true;
				stopbutton.enable = false;
				resetbutton.enable = true;
			}
			if(resetbutton.pressed) {
				playbutton.pressedonce = false;
				PlayFlag = false;
				resetProgram();
				
				playbutton.enable = false;
				stopbutton.enable = false;
				resetbutton.enable = true;
			}
		}
	}
	
	public void stop()
	{
		RecordFlag = false;
		PlayFlag = false;
		
		if(sourceline.isActive()) {
			sourceline.stop();
			sourceline.drain();
			sourceline.close();
		}
		if(targetline.isActive()) {
			targetline.stop();
			targetline.drain();
			targetline.close();
		}
		
		if(targetFile.exists())
			targetFile.delete();
	}
	
	//=========================================== Threads Being Used ===============================
	public void recordAudio()
	{	
		while(RecordFlag) {
			try {
				byte[] data = new byte[targetline.getBufferSize()/5];
				targetline.read(data, 0, data.length);
				AudioInputStream stream = new AudioInputStream(targetline);
				AudioSystem.write(stream, AudioFileFormat.Type.WAVE, targetFile);
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
				System.err.println("Writing to TargetLine failed!");
			}
		}
	}
	
	//=========================================== Methods To Be Included ===========================
	public void drawRecordButtons()
	{
		try {
			// Draw Record Button
			URL buttonurl = new URL(getDocumentBase(), "recordbutton_base.png");
			PImage recordbutton_base = loadImage(buttonurl.toString());
			recordbutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "recordbutton_roll.png");
			PImage recordbutton_roll = loadImage(buttonurl.toString());
			recordbutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "recordbutton_lock.png");
			PImage recordbutton_lock = loadImage(buttonurl.toString());
			recordbutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			// Draw Cut Button
			buttonurl = new URL(getDocumentBase(), "cutbutton_base.png");
			PImage stoprecordbutton_base = loadImage(buttonurl.toString());
			stoprecordbutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "cutbutton_roll.png");
			PImage stoprecordbutton_roll = loadImage(buttonurl.toString());
			stoprecordbutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "cutbutton_lock.png");
			PImage stoprecordbutton_lock = loadImage(buttonurl.toString());
			stoprecordbutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			// Draw Replay Button
			buttonurl = new URL(getDocumentBase(), "replaybutton_base.png");
			PImage replaybutton_base = loadImage(buttonurl.toString());
			replaybutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "replaybutton_roll.png");
			PImage replaybutton_roll = loadImage(buttonurl.toString());
			replaybutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "replaybutton_lock.png");
			PImage replaybutton_lock = loadImage(buttonurl.toString());
			replaybutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			
			int buttonWidth = recordbutton_base.width;
			int buttonHeight = recordbutton_base.height;
			
			recordbutton = new Buttons(this);
			recordbutton.setCoordinate(WINDOW_WIDTH*1/6, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			recordbutton.setImages(recordbutton_base, recordbutton_roll, recordbutton_base, recordbutton_lock);
				
			cutbutton = new Buttons(this);
			cutbutton.setCoordinate(WINDOW_WIDTH*2/6, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			cutbutton.setImages(stoprecordbutton_base, stoprecordbutton_roll, stoprecordbutton_base, stoprecordbutton_lock);
			
			replaybutton = new Buttons(this);
			replaybutton.setCoordinate(WINDOW_WIDTH*4/5, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			replaybutton.setImages(replaybutton_base, replaybutton_roll, replaybutton_base, replaybutton_lock);
		}
		catch(MalformedURLException mre) { mre.printStackTrace(); }
	}
	
	public void drawRecorder()
	{
		background(51, 0);
		noStroke();
		
	    recordbutton.update();
	    recordbutton.display();
		cutbutton.update();
		cutbutton.display();
		replaybutton.update();
		replaybutton.display();
	}
	
	public void drawRecordingOval()
	{
	    noStroke();
		smooth();
		rotateAngle += .1;
	    float x = cos(rotateAngle)*(STATICOVAL_RAD_1+STATICOVAL_RAD_2)/2;
	    float y = sin(rotateAngle)*(STATICOVAL_RAD_1+STATICOVAL_RAD_2)/2;
	    ellipseMode(RADIUS);
	    fill(12, 200, 171);
	    ellipse(STATICOVAL_CENTER_X, STATICOVAL_CENTER_Y, STATICOVAL_RAD_1, STATICOVAL_RAD_1);
	    fill(51);
	    ellipse(STATICOVAL_CENTER_X, STATICOVAL_CENTER_Y, STATICOVAL_RAD_2, STATICOVAL_RAD_2);
	    fill(51);
	    ellipse(STATICOVAL_CENTER_X + x, STATICOVAL_CENTER_Y + y, MOVINGOVAL_RAD, MOVINGOVAL_RAD);
	    fill(51);
	    ellipse(STATICOVAL_CENTER_X - x, STATICOVAL_CENTER_Y - y, MOVINGOVAL_RAD, MOVINGOVAL_RAD);
	    
	    fill(255);
	    textAlign(LEFT, CENTER);
	    textSize(TEXT_SIZE);
	    text("Recording...", TEXT_X, TEXT_Y);
	}
	
	public void stopRecorder()
	{
		targetline.stop();
		targetline.drain();
		targetline.close();
		
		setup();
		loop();
	}
	
	public void drawPlayButtons()
	{
		try {
			// Draw The Play Button
			URL buttonurl = new URL(getDocumentBase(), "playbutton_base.png");
			PImage playbutton_base = loadImage(buttonurl.toString());
			playbutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "playbutton_roll.png");
			PImage playbutton_roll = loadImage(buttonurl.toString());
			playbutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "playbutton_lock.png");
			PImage playbutton_lock = loadImage(buttonurl.toString());
			playbutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			// Draw The Stop Button
			buttonurl = new URL(getDocumentBase(), "stopbutton_base.png");
			PImage stopbutton_base = loadImage(buttonurl.toString());
			stopbutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "stopbutton_roll.png");
			PImage stopbutton_roll = loadImage(buttonurl.toString());
			stopbutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "stopbutton_lock.png");
			PImage stopbutton_lock = loadImage(buttonurl.toString());
			stopbutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			// Draw The Reset Button
			buttonurl = new URL(getDocumentBase(), "resetbutton_base.png");
			PImage resetbutton_base = loadImage(buttonurl.toString());
			resetbutton_base.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "resetbutton_roll.png");
			PImage resetbutton_roll = loadImage(buttonurl.toString());
			resetbutton_roll.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			buttonurl = new URL(getDocumentBase(), "resetbutton_lock.png");
			PImage resetbutton_lock = loadImage(buttonurl.toString());
			resetbutton_lock.resize(WINDOW_WIDTH/BUTTON_RATIO, 0);
			
			int buttonWidth = playbutton_base.width;
			int buttonHeight = playbutton_base.height;
			
			playbutton = new Buttons(this);
			playbutton.setCoordinate(WINDOW_WIDTH*1/6, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			playbutton.setImages(playbutton_base, playbutton_roll, playbutton_base, playbutton_lock);
				
			stopbutton = new Buttons(this);
			stopbutton.setCoordinate(WINDOW_WIDTH*2/6, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			stopbutton.setImages(stopbutton_base, stopbutton_roll, stopbutton_base, stopbutton_lock);
			
			resetbutton = new Buttons(this);
			resetbutton.setCoordinate(WINDOW_WIDTH*4/5, WINDOW_HEIGHT-buttonHeight, buttonWidth, buttonHeight);
			resetbutton.setImages(resetbutton_base, resetbutton_roll, resetbutton_base, resetbutton_lock);
		}
		catch(MalformedURLException mre) { mre.printStackTrace(); }
	}
	
	public void drawPlayer()
	{
		background(51, 0);
		noStroke();
		
		stroke(210, 93, 0);
		strokeWeight(2);
		line(0, WINDOW_BASELINE, WINDOW_WIDTH, WINDOW_BASELINE);
		
		playbutton.update();
		playbutton.display();
		stopbutton.update();
		stopbutton.display();
		resetbutton.update();
		resetbutton.display();
	}
	
	public void playAudio()
	{
		int readcnt = 0;
		
		try {
			readcnt = audio.audioStream.read(bytesbuffer, 0, NUM_PERFRAME);
		}
		catch(IOException ioe) { 
			System.err.println("Audio Reading Unsuccessul!");
			ioe.printStackTrace();
		}
		
		if(readcnt == NUM_PERFRAME) {
			if(readcnt > 0) {
				sourceline.write(bytesbuffer, 0, readcnt);
			}
			else {
				System.out.println("Audio Stream Drained");
				setupPlayer();
			}	
		}
		else {
			System.out.println("Unbalanced Read and Write Happens");
			setupPlayer();
		}
	}
	
	public void stopPlayer()
	{
		sourceline.stop();
		sourceline.drain();
		sourceline.close();
		
		setupPlayer();
		loop();
	}
	
	public void convertAudio()
	{
		for(int i = 0; i < floatbuffer.length; i++)		
			floatbuffer[i] = (float)(bytesbuffer[i]) / (float)255;
	}
	
	public void computeFFT()
	{
		audioFFT.forward(floatbuffer);
	}
	
	public void drawLogarithm()
	{
		int LogBand_Offset = WINDOW_WIDTH / logSize;
		int LogBand_Width = LogBand_Offset - 2;
		float maxAmplitude = 0;
		int maxIndex = 0;
		
		for(int i = 0; i < logbuffer.length; i++) {
			//float centerFreq = audioFFT.getAverageCenterFrequency(i);
			//floatbuffer[i] = audioFFT.freqToIndex(centerFreq);
			logbuffer[i] = audioFFT.getAvg(i);
			
			if(logbuffer[i] > maxAmplitude) { 
				maxAmplitude = logbuffer[i];
				maxIndex = i;
			}
			
			bands[i].setrect(i*LogBand_Offset, WINDOW_BASELINE, LogBand_Width, -logbuffer[i]);
			bands[i].display();
			
			if(logbuffer[i] < previous[i]) {
				bands[i].setrect(i*LogBand_Offset, WINDOW_BASELINE-logbuffer[i]-1, LogBand_Width, logbuffer[i]-previous[i]);
				bands[i].displaystroke();
			}
		}
		
		fill(234, 190, 0);
		rect(maxIndex*LogBand_Offset, WINDOW_BASELINE, LogBand_Width, -maxAmplitude);
	}
	
	public void storePreFrame()
	{
		for(int i = 0; i < previous.length; i++) {
			previous = (float[]) logbuffer.clone();
		}
	}
	
	public void closeRecorder()
	{
		targetline.stop();
		targetline.drain();
		targetline.close();
	}
	
	public void resetProgram()
	{
		sourceline.stop();
		sourceline.drain();
		sourceline.close();
		
		setup();
		loop();
	}
	
	public static void main(String args[])
	{
		PApplet.main(new String[] { "--present", "liverecordingclean.LiveRecordingClean" });
	}
}
