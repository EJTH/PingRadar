#include <Servo.h> 
#include <string.h>


/* Ping Radar - Ultrasonic sonar project for arduino, processing and android.
 * --------------------------------------------------------------------------
 * Guide:
 * --------------------------------------------------------------------------
 * You will need: 
 *    - PING))) or similar ultrasonic distance sensor readable by pulseIn
 *    - A servo preferably with 180 degree movement
 *    - Wires
 *    - PC/Mac with processing OR a android device with USB Host support running android 3.0.1 or higher
 *
 *
 * Connect the ultrasonic sensor (most models should work okay, it has to be a sensor readable by PulseIn())
 * to gnd, vcc, (7) ping/trig and (8) echo pins (which may be the same if you are using a 3 pinned PING)) sensor or similar, in that case just change the PINOUT variables below)
 *
 */




/* PIN OUT */
#define TRIG_PIN 7
#define ECHO_PIN 8
#define SERVO_PIN 10





/* Inches or CM ? */
boolean INCHES = false;     //true: CM, false: INCHES




int resolution = 180;
int waitForServo = 50;
long pingTimeout = 350000;
int resFrom = 0;
int resTo = 180;
Servo myservo;
int pos = 0;
int stopFlag = false;


void setup() {
  
  while(!Serial){
    delay(100); //Wait for serial
  }
  
  // initialize serial communication
  Serial.begin(9600);

  Serial.write("Waiting for options...\n");

    
  Serial.parseInt(); // Wait for configuration flag
  
  /* Parse settings from the application in the format of 1:int;int;int; */
  resolution  = Serial.parseInt();
  waitForServo = Serial.parseInt();
  pingTimeout  = Serial.parseInt();  
  
  /* Attach servo */
  myservo.attach(SERVO_PIN);
  
}

void loop() 
{ 
  /* If stop signal is send, then wait a little and return */
  if(stopFlag){ 
    delay(1000); 
    return;
  }
  
  /* Sweep the servo back and forth, and take an amount of readings equal to resolution */
  for(pos = 0; pos < resolution; pos += 1 )
  {
    myservo.write((pos*((float)180/(float)resolution)));            
    delay(waitForServo);
    measureDistance();

    serialEvent(); // Check for signals
    
    if(stopFlag) return;  
  } 
  for(pos = resolution; pos > 0; pos-=1 )
  {                                
    myservo.write((pos*((float)180/(float)resolution)));
    delay(waitForServo);
    measureDistance(); 

    serialEvent(); // Check for signals
    
    if(stopFlag) return;    
  }
} 

/* Measures distance with the ultrasonic sensor sends the response through serial connection to the display app */
boolean measureDistance()
{
  // establish variables for duration of the ping, 
  // and the distance result in inches and centimeters:
  long duration, inches, cm;

  // The PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(TRIG_PIN, OUTPUT);
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(5);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  // The same pin is used to read the signal from the PING))): a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(ECHO_PIN, INPUT);
  duration = pulseIn(ECHO_PIN, HIGH,pingTimeout);

 

  inches = microsecondsToInches(duration);
  cm = microsecondsToCentimeters(duration);
 
  Serial.print(pos);
  Serial.print(' ');
  Serial.print(INCHES ? inches : cm);

  Serial.println();
  
  return true;
}

/* checks the serial connection for stop or reconfigure signals. */
void serialEvent() {
  while (Serial.available()) {
    int action = Serial.parseInt();
    switch(action){
      case 1:
        resolution  = Serial.parseInt();
        waitForServo = Serial.parseInt();
        pingTimeout  = Serial.parseInt(); 
        stopFlag = false; 
      break;
      case 2:
        stopFlag = true;
      break;
    }
      
    } 
 
}


/*
 *  Ultrasonic distance measure methods courtesy of the arduino example pages
 */
 
long microsecondsToInches(long microseconds)
{
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  return microseconds / 74 / 2;
}

long microsecondsToCentimeters(long microseconds)
{
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}