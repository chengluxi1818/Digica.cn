package digicaapplet;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PImage;

public class Buttons
{
	public PApplet parent;
	
	public int x, y;
	public int width, height;
	public boolean enable = true;
	
	private PImage base;
	private PImage roll;
	private PImage down;
	private PImage currentimage;
	private PImage lockimage;
	
	
	private Color basecolor, rollcolor, downcolor;
	private Color currentcolor;
	
	protected boolean over = false;
	protected boolean pressed = false;
	protected boolean pressedonce = false;
	protected int pressedcounter = 0;
	
	public Buttons(PApplet p)
	{
		this.parent = p;
	}
	
	public void pressed()
	{
		if(over && parent.mousePressed) {
			pressed = true;
		}
		else {
			pressed = false;
		}
	}
	
	public void over()
	{
		if(overRect(x, y, width, height)) {
			over = true;
		}
		else {
			over = false;
		}
	}
	  
	public boolean overRect(int x, int y, int width, int height)
	{
		if(parent.mouseX >= x && parent.mouseX <= x+width &&
				parent.mouseY >= y && parent.mouseY <= y+height) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public void setCoordinate(int ix, int iy, int iw, int ih)
	{
		this.x = ix;
		this.y = iy;
		this.width = iw;
		this.height = ih;
	}
	
	public void setImages(PImage ibase, PImage iroll, PImage idown, PImage ilock)
	{
		this.base = ibase;
		this.roll = iroll;
		this.down = idown;
		this.currentimage = base;
		this.lockimage = ilock;
	}
	
	public void setColors(Color ibasecolor, Color irollcolor, Color idowncolor)
	{
		this.basecolor = ibasecolor;
		this.rollcolor = irollcolor;
		this.downcolor = idowncolor;
		this.currentcolor = basecolor;
	}

	public void update()
	{
		if(enable) {	
			over();
			pressed();
			
			if(pressed) {
				currentimage = down;
				currentcolor = downcolor;
				parent.delay(10);
				pressedonce = true;
				pressedcounter++;
				System.out.println("Button Pressing...To Just Prove that the mouse is not clicked once" + pressedcounter);
			}
			else if(over) {
				currentimage = roll;
				currentcolor = rollcolor;
			}
			else {
				currentimage = base;
				currentcolor = basecolor;
			}
		}
		else {
			currentimage = lockimage;
		}
	}
	
	public void display()
	{
		parent.image(currentimage, x, y);
	}
}
