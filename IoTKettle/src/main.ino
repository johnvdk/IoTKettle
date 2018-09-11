#include <Arduino.h>


#define controlPin PB11
void setup() {
  pinMode(controlPin, OUTPUT);
  digitalWrite(controlPin, HIGH); //Make sure kettle is turned off
  Serial1.begin(9600);
  Serial.begin(9600);
}
char nextChar;
String colon = ":";

void loop() {
  Serial.println("started");
  String str;
  String temp = "";
  if(Serial1.available()){
    str = "";
  }
  while (Serial1.available()){
    nextChar = (byte)Serial1.read();
    temp += nextChar;
    if(temp.equals(colon)){
      break;
    }
    else{str += nextChar;}
    delay(1);
  }
  if(str.equals("TO")){
    digitalWrite(controlPin, LOW);
    delay(10);
  }
  if(str.equals("TF")){
    digitalWrite(controlPin, HIGH
    
    
    
    );
    delay(10);
  }
}