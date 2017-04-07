package digicaapplet;

import processing.core.PApplet;

public class Band
{
	public PApplet parent;
	public float x;
	public float y;
	public float width;
	public float height;
	
	public Band(PApplet p)
	{
		this.parent = p;
	}
	
	public void display()
	{
		parent.fill(210, 93, 0);
		parent.noStroke();
		parent.rect(x, y, width, height);
	}
	
	public void setrect(float x, float y, float width, float height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public void displaystroke()
	{
		parent.fill(12, 200, 171);
		parent.noStroke();
		parent.rect(x, y, width, height, width/2, width/2, 0, 0);
	}
}
