package net.ejth.psketches.pingradar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.DisplayMetrics;

import com.hoho.android.usbserial.driver.*;

import controlP5.CallbackEvent;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.DropdownList;
import controlP5.Textlabel;
import processing.core.*;


public class RadarActivity extends PApplet {

	/* GUI COLORS */
	int BACKGROUND_COLOR = 31;
	int LINE_FILL_COLOR  = 0;
	int OUTLINE_COLOR    = 0;
	
	/* GUI DIMENSIONS */
	static int GUI_WIDTH   = 250;
	static int GUI_HEIGHT  = 30;
	static int GUI_PADDING = 40;
	static int GUI_RPAD    = 100;
	
	/* CP5 GUI Library */
	ControlP5 cp5;
	
	Textlabel consoleLbl; 		// Debug message label
	DropdownList lst;     		// Dropdown for device selection
	
	
	
	/* USB driver variables */
	UsbManager manager; 				// Get UsbManager from Android.
	UsbSerialDriver myPort; 			// Driver for communication
	HashMap<String, UsbDevice> devices; // Device list

	
	
	/* SONAR SETTINGS VARIABLES */
	
	int resolution  = 24; 		// Resolution / detail level you desire
	
	int readDelay   = 200; 		// Delay before we read from the radar (time for the servo to move)
	
	int echoTimeout = 160000; 	// Timeout for ultrasonic ping-pong
	
	int noEchoValue = 750; 		// The translation of bad readings.
	
	int zoom = 250; 			// Zoom value in millimeters
	
	boolean RADAR_ON = false;
	
	/* Contains the current readings */
	ArrayList<int[]> readings;
	
	
	/**
	 * Boiler plating for processing.
	 * @param args
	 */
	public static void main(String args[]) {
        PApplet.main(new String[] { "–present", "net.ejth.psketches.arduinoradar" });
    }
	
	/**
	 * Setup the applicaton GUI, initialize variables etc.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void setup() 
	{
	  orientation(LANDSCAPE);

	  
	  
	  /* GUI */
	  setGUIProperties();
	  
	  /* RADAR COLORS */
	  BACKGROUND_COLOR = 30;
      LINE_FILL_COLOR  = color(0,120,0);
	  OUTLINE_COLOR    = color(0,255,0);
	  
	  /* CP5 GUI Controls */
	  ControlP5 cp5 = new ControlP5(this);
	  
	  //Zoom slider
	  cp5.addSlider("zoom"      , 50, 500  , zoom, width-GUI_WIDTH-GUI_RPAD, 10, GUI_WIDTH, GUI_HEIGHT).setId(1);
	  
	  int i=1;
	  
	  consoleLbl = cp5.addTextlabel("console");
	  consoleLbl.setText("Welcome press start.");
	  
	  lst = cp5.addDropdownList("comport", 10, 20, GUI_WIDTH,400).setId(2);
	  
	  cp5.addSlider("delay"     , 20,   800   , readDelay  , width-GUI_WIDTH-GUI_RPAD, GUI_HEIGHT+(GUI_PADDING*i++), GUI_WIDTH, GUI_HEIGHT).setId(3);
	  cp5.addSlider("timeout"   , 50, 300000 , echoTimeout , width-GUI_WIDTH-GUI_RPAD, GUI_HEIGHT+(GUI_PADDING*i++), GUI_WIDTH, GUI_HEIGHT).setId(4);
	  cp5.addSlider("resolution", 6 , 180     , resolution , width-GUI_WIDTH-GUI_RPAD, GUI_HEIGHT+(GUI_PADDING*i++), GUI_WIDTH, GUI_HEIGHT).setId(5);
	  cp5.addSlider("noecho"    , 0 , 750    , noEchoValue , width-GUI_WIDTH-GUI_RPAD, GUI_HEIGHT+(GUI_PADDING*i++), GUI_WIDTH, GUI_HEIGHT).setId(7);  
	  cp5.addButton("startBtn"  , 0                        , width-GUI_WIDTH-GUI_RPAD, GUI_HEIGHT+(GUI_PADDING*i++), GUI_WIDTH, GUI_HEIGHT).setId(6).setLabel("Start");
	  
	  findDevices();
	  
	  readings = new ArrayList<int[]>();
	  background(BACKGROUND_COLOR);
	}
	
	/**
	 * Processing draw function, serial data is read in draw thread too currently.
	 */
	@Override
	public void draw()
	{
	  background(BACKGROUND_COLOR);
	  
	  /* Capture data */
	  if ( RADAR_ON ) {  // If data is available,
		String val;
	    val = readStringUntil(10);         // read it and store it in val
	    if(val != null){
	      String[] pinfo = split(val," ");
	      
	      if(pinfo.length == 2){
		      try {
		    	  int[] reading = {Integer.parseInt(trim(pinfo[0])),Integer.parseInt(trim(pinfo[1]))};
		      
			      readings.add(reading);
			      
			      if(readings.size() > (resolution*2)){
			        readings.remove(0);
			      }
		      } catch (NumberFormatException e){
		    	  consoleLbl.setText("Invalid data : "+val);
		      }
	      }
		      	
	    }
	  }
	  drawRadar();
	}
	
	
	/**
	 * Lists devices in the CP5 control lst.
	 */
	void findDevices(){
		  /* Search for usb devices */
		  consoleLbl.setText("Searching for USB devices..");

		  UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		  devices = manager.getDeviceList();
		  
		  consoleLbl.setText("Devices found: "+devices.size());
		  
		  /* POPULATE LIST */
		  lst.clear();
		  lst.beginItems();
		  
		  int i=0;
		  for (String key : devices.keySet()) {
			  lst.addItem(key,i);
			  i++;
		  }
		  
		  lst.endItems();
		  lst.setValue(0);
	}
	
	/**
	 * Sets GUI dimensions depending on screen size.
	 */
	void setGUIProperties(){
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		
		if(screenWidth >= 1200){
			/* big resolution */
			GUI_WIDTH   = 300;
			GUI_HEIGHT  = 40;
			GUI_PADDING = 50;
			GUI_RPAD    = 100;
		} else 
		if(screenWidth >= 800){
			/* medium resolution */
			GUI_WIDTH   = 200;
			GUI_HEIGHT  = 25;
			GUI_PADDING = 35;
			GUI_RPAD    = 80;
		} else {
			/* Low resolution */

			GUI_WIDTH   = 80;
			GUI_HEIGHT  = 14;
			GUI_PADDING = 20;
			GUI_RPAD    = 80;
		}
	}

	/* 
	 * GUI CONTROL EVENTHANDLERS / SETTERS 
	 */
	public void startBtn(){
	  
	}
	public void noecho(int n){
	  noEchoValue = n;  
	}
	@Override
	public void delay(int d){
	  readDelay = d; 
	}
	public void timeout(int t){
	  echoTimeout = t; 
	}
	public void resolution(int r){
	  resolution = r;
	}
	public void zoom(int z){
	  zoom = z;
	  drawRadar();
	}
	
	/* 
	 * GUI CONTROL EVENT HANDLING 
	 */
	@SuppressWarnings("deprecation")
	public void controlEvent(CallbackEvent theEvent) {
	    int id = theEvent.getController().getId();
	    
	    switch(theEvent.getAction()) {
	      case(ControlP5Constants.ACTION_RELEASEDOUTSIDE): 
	      case(ControlP5Constants.ACTION_RELEASED):
	        if(id == 6){
	    
	          if(RADAR_ON){
	            resetRadar();
	            theEvent.getController().setLabel("START");
	          } else {
	            configure();
	            theEvent.getController().setLabel("STOP");        
	          }
	          return; 
	        }
	        if(id >= 3 && id <=5 && RADAR_ON){
	          
	          configure();
	        }
	      break;
	      
	    }
	  
	}
	
	/**
	 * Stop the radar
	 */
	public void resetRadar(){
	  if(RADAR_ON){
		RADAR_ON = false;
	    try {
			myPort.write("2STOP".getBytes(), 1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			consoleLbl.setText("Something went wrong:"+e.toString());
		}
	    
	    
	  }
	}
	
	/**
	 * Start the radar and send new settings.
	 */
	public void configure(){
		background(BACKGROUND_COLOR);
		readings.clear(); 
		if(myPort == null){
			try {
				manager = (UsbManager) getSystemService(Context.USB_SERVICE);
				
				String deviceName = lst.getItem((int)lst.getValue()).getText();
				consoleLbl.setText("Selected device :"+deviceName);
				// Find the first available driver.
				myPort = UsbSerialProber.acquire(manager, devices.get(deviceName));
				
				if(myPort == null){
					consoleLbl.setText("Something went wrong: myPort is null");
					return;
				}
				
				myPort.open();
				myPort.setBaudRate(9600);
				
				
			} catch (IOException e1) {
				consoleLbl.setText("Something went wrong:"+e1.toString());
				return;
			}
		}
		
		
 
  
		String str = null;
		/* Wait for device to become ready */
		while(str == null){
			str = readStringUntil(10); 
		}
		consoleLbl.setText("DEVICE READY: "+str);
		
		try {

			myPort.write(("1S"+Integer.toString(resolution)+"r"+
					Integer.toString(readDelay)+"d"+
					Integer.toString(echoTimeout)+"t").getBytes(),100);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			consoleLbl.setText("CONFIG ERR:"+e.toString());
			return;
		}
		consoleLbl.setText("Config sent.");
		
		RADAR_ON = true;
  	
	}

	
	/**
	 * Read from serial till char
	 * @param c
	 * @return
	 */
	public String readStringUntil(int c){
		String theChar = Character.toString((char)c);
		String ret = null;
		String readBuffer;
		
		if (myPort != null) {
			try {
				byte buffer[] = new byte[512];
				int nb = myPort.read(buffer, 10);
				
				//If there is nothing to read, then carry on..
				if(nb==0) return null;
				readBuffer = new String(buffer);
				
				while(!readBuffer.contains(theChar)){
					    buffer = new byte[512];
			    		nb = myPort.read(buffer, 5000);
			    		if(nb>0){
				    		readBuffer += new String(buffer);
				    	}
			    	
			    	
			    }
				
				String[] split = readBuffer.split(theChar,2);
			    ret = split[0];
			    consoleLbl.setText("RECIEVED: "+ret);
			    readBuffer = split.length == 1 ? "" : split[1];
			    
			} catch (IOException e) {
			    // Deal with error.
				consoleLbl.setText("Something went wrong:"+e.toString());
			}
			    
			    
		}
		
		return ret;
		
		
	}
	
	/**
	 * Draw radar.
	 */
	public void drawRadar(){
	  float r,radians,d;
	  float alpha;
	  int[] pinfo;
	  
	  float x = 0,y = 0,ox = 0,oy = 0;
	  
	  
	  for(int i = 0; i<readings.size(); i++){
	    alpha = ((float)i / (float) readings.size()) * 255f;
	    pinfo = (int[]) readings.get(i);
	    r = pinfo[0];
	    radians = ((( (r+1) / resolution)*175)+180) * PI / 180;
	    d = pinfo[1];
	    if(d==0)d=noEchoValue;
	    if(d!=0){
	      x = ((float)width/2)+cos(radians) * ((d/zoom)*((float)height/2));
	      y = ((float)height/2)+sin(radians) * ((d/zoom)*((float)height/2));
	      if(ox != 0 && oy != 0){
	         stroke(OUTLINE_COLOR,alpha);
	         line(ox, oy, x, y);
	         stroke(LINE_FILL_COLOR,alpha);
	         line(width/2,height/2,x,y);
	         
	      }
	      ox = x;
	      oy = y;
	    }    
	  }
	  
	}

	
	

}
