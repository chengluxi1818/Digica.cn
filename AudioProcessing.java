package digicaapplet;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.*;

public class AudioProcessing
{
	public static URL audioFile;
	protected AudioInputStream audioStream;
	protected AudioFileFormat fileFormat;
	protected AudioFormat audioFormat;
	
	public AudioProcessing() { }
	
	public AudioProcessing(URL audioFile)
	{
		this.audioFile = audioFile;
		try {
			this.fileFormat = AudioSystem.getAudioFileFormat(audioFile);
			// Report the file format here
			System.out.println("Audio Format: " + fileFormat.getFormat());
			System.out.println("Number of Frames: " + fileFormat.getFrameLength());
			System.out.println("File Type: " + fileFormat.getType());
			
			this.audioFormat = fileFormat.getFormat();
			
			this.audioStream = AudioSystem.getAudioInputStream(audioFile);
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
		catch(UnsupportedAudioFileException uafe) { uafe.printStackTrace(); }
		
		
	}
}
