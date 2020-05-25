
//==============================================================================

///------------------------------ LIBRARIES ------------------------------------
#include "BluetoothSerial.h"
#include "Arduino.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

///------------------------------ VARIABLES -------------------------------------

BluetoothSerial SerialBT;
int curs = 0;
int parse_count = 0;
String buff = ""; 
bool started = false;                             //is the intervalometer started
int count_value = 0;                              //number of pictures to take
int duration_value = 0;                           //duration of each picture
int delay_value = 0;                              //delay between each picture
char current_char;                                //bluetooth parsing buffer char
int photo_taken = 0;                              //count the number of taken photos

unsigned long gpioLoopStartTimestamp = 0;         //timestamp for gpio control. Activate at each gpio start
unsigned long gpioLoopStopTimestamp = 0;          //timestamp for gpio control. Activate at each gpio stop
bool gpioActivated = false;

int bluetoothBlinkIntervalOn = 200;
int bluetoothBlinkIntervalOff = 50;
bool bluetoothLedStatus = false;
unsigned long bluetoothLoopStartTimestamp = 0;    //timestamp for bluetooth led control. Activate at each bluetooth led start
unsigned long bluetoothLoopStopTimestamp = 0;     //timestamp for bluetooth led control. Activate at each bluetooth led stop
unsigned long bluetoothDevicesDisplayTimestamp = 0;

bool debugMode = false;                           //enable or disable debug display



///------------------------------ CONSTANTS ------------------------------------

const int INTERVALOMETER_PIN = 4;

//==============================================================================

void setup() {
  Serial.begin(115200);
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(INTERVALOMETER_PIN, OUTPUT);
  SerialBT.register_callback(bluetoothConnectionCallback);
  
  if (!SerialBT.begin("ESP32")) {
    Serial.println("An error occurred initializing Bluetooth");
  } else {
    Serial.println("Bluetooth initialized");
  }
  gpioLoopStartTimestamp = millis();
  gpioLoopStopTimestamp = millis();
}

//==============================================================================

void loop() {
  if (SerialBT.available()) {
    readBluetooth();
  }
  commandGPIO();
  bluetoothBlink();
  checkConnectedDevices();
  delay(20);
}

//==============================================================================

void bluetoothConnectionCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param) {  
  if (event == ESP_SPP_SRV_OPEN_EVT) {
    Serial.println("Client Connected has address:");
    for (int i = 0; i < 6; i++) {
      Serial.printf("%02X", param->srv_open.rem_bda[i]);
      if (i < 5) {
        Serial.print(":");
      }
    }
    Serial.println("");
    bluetoothBlinkIntervalOn = 1000;
  }
}

//==============================================================================

void checkConnectedDevices(){
  if (bluetoothDevicesDisplayTimestamp + 10000  < millis() && debugMode){
    Serial.println(SerialBT.hasClient());
    bluetoothDevicesDisplayTimestamp = millis();    
  }
  if(!SerialBT.hasClient()){
    bluetoothBlinkIntervalOn = 200;
  }
}

//==============================================================================

void readBluetooth(){
  current_char = SerialBT.read();
  if ( current_char == ',' || current_char == '\n'){  
    if(debugMode){
      Serial.print("buff : ");
      Serial.println(buff);
      Serial.print("parse_count : ");
      Serial.println(parse_count);
    }
    if (buff == "S"){
      started = true;
      Serial.println("Started");
    }else if(buff == "P"){
      Serial.println("Paused");
      started = false;
    }else if(buff == "R"){
      Serial.println("Reseted");
      digitalWrite(LED_BUILTIN, LOW);
      started = false;
      count_value = 0;
      duration_value = 0;
      delay_value = 0;
      photo_taken = 0;
    }else{
      if (parse_count == 0){
        count_value = buff.toInt();
        if(debugMode){
          Serial.println(buff);
        }
      }
      if (parse_count == 1){
        duration_value = buff.toInt();
        if(debugMode){
          Serial.println(buff);
        }
      }
      if (parse_count == 2){
        delay_value = buff.toInt();
        if(debugMode){
          Serial.println(buff);
        }
      }
    }
    buff = "";
    parse_count++;
    if (current_char == '\n'){
      buff = "";
      parse_count = 0;
    }
  }else {
    buff += current_char;
  }
  if(debugMode){
    Serial.print("is started");
    Serial.println(started);
  }
}

//==============================================================================

void sendBluetoothData(){
  /*uint_8* buff = new int[10];
  buff[0] = count_value;
  buff[1] = duration_value;
  buff[2] = delay_value;
  buff[3] = photo_taken;
  SerialBT.write(buff, 10);*/
  /*SerialBT.flush();
  SerialBT.write(count_value);
  SerialBT.write(duration_value);
  SerialBT.write(delay_value);
  SerialBT.write(photo_taken);
  SerialBT.flush();*/
  SerialBT.print(String(count_value) + "," + String(duration_value) + "," + String(delay_value) + "," + String(photo_taken));
  //SerialBT.flush();
}

//==============================================================================

void commandGPIO() {
  if (!gpioActivated && started){
    if (gpioLoopStopTimestamp + (delay_value * 1000) < millis()){
      Serial.print("count:");
      Serial.println(photo_taken);
      photo_taken += 1;
      gpioActivated = true;
      digitalWrite(INTERVALOMETER_PIN, HIGH);
      digitalWrite(LED_BUILTIN, LOW);
      gpioLoopStartTimestamp = millis();
      sendBluetoothData();
    }
    
  } else if (gpioActivated) {
    if (gpioLoopStartTimestamp + (duration_value * 1000) < millis()){
      gpioActivated = false;
      digitalWrite(INTERVALOMETER_PIN, LOW);
      digitalWrite(LED_BUILTIN, HIGH);
      gpioLoopStopTimestamp = millis();
    }
  }
  
  if (photo_taken == count_value && photo_taken > 0){
    Serial.println("end");
    started = false;
    photo_taken = 0;
  }
}

//==============================================================================

void bluetoothBlink() {
  if (!started){
    if (!bluetoothLedStatus){
      if (bluetoothLoopStartTimestamp + bluetoothBlinkIntervalOn < millis()){
        digitalWrite(LED_BUILTIN, HIGH);
        bluetoothLedStatus = true;
        bluetoothLoopStartTimestamp = millis();
      }
    } else {
      if (bluetoothLoopStartTimestamp + bluetoothBlinkIntervalOff < millis()){
        digitalWrite(LED_BUILTIN, LOW);
        bluetoothLedStatus = false;
        //bluetoothLoopStopTimestamp = millis();
      }
    }
  }  
}
