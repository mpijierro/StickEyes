/**
* Creado por: @mpijierro
*/

#include <./Ultrasonic.h> // Cambiar la ruta a la correcta.

Ultrasonic ultrasonic(9,8,17400);   // (Trig PIN,Echo PIN)


float detectedDistance = 0;         //distancia detectada por hc-06
int detectedDistanceInt = 0;        //redondeo a un enteor de la distancia detectada

//Valores que se reciben por bluetooth
int receivedDistance = 100;         //valor numerico de la distancia recibida
int timeToSleep = 200;              //tiemp por defecto en el que Arduino se para en ms

char receivedChar;                 //Caracter recibido por bluetooth
char receivedString[10];          //Cadena con todos los caracteres recibidos
String Data = "";

int isReceivedTime = 0;
int isReceivedDistance = 0;

int recalculate = 0;


void setup()
{
  Serial.begin(9600);
}

void loop()
{

  detectedDistance = ultrasonic.Ranging(CM);
  
  
  /**
  * Bucle que obtiene la informacin desde el bluetooth del telefono movil.
  * El modulo bluetooth adosado al Arduino va recibiendo caracter a caracter.
  * Esta preparado para recibir dos patrones: t@@@# d@@@#   , siendo @@@ valores escalares
  * que representan el tiempo (t) en segundos para 'dormir' el arduino y la distancia (d)
  * minima en centimetros para que el sensor de distancias y el bluetooth envien datos
  * al telefono movil
  */
  
  while (Serial.available()){
    
    receivedChar = Serial.read();
    
    //Detectamos el tipo de informacion que nos llega tiempo (t) o distancia (d)
    if (receivedChar == 't'){
       isReceivedTime = 1; 
    }
    else if (receivedChar == 'd'){
       isReceivedDistance = 1; 
    }
    else{  
      //Caracter fin de cadena enviada
      if (receivedChar == '#'){
        recalculate = 1;
        break;
      }
      else{
        //Cualquier otro caracter normalmente un numero
        Data.concat(receivedChar);
        
      }
    }
    
  }
  
  /* Recalculamos la configuracion de Arduino cuando recibimos datos
   * de configuracion desde el telefono. Se recalculan el tiempo en
   * el que Arduino tiene que estar 'dormido' y la distancia minima
   * de deteccion para enviarla por bluetooth 
   */
  if (recalculate){
      
    Data.toCharArray(receivedString,10);
    
    if (isReceivedDistance){
      receivedDistance = atoi(receivedString);
    }
    else if (isReceivedTime){
      timeToSleep = atoi (receivedString) * 1000;
    }
    
    //Una vez recibida la configuracion, reseteamos los valores
    isReceivedTime =  0;
    isReceivedDistance = 0;
    recalculate = 0;
    Data = "";
    
  }
  
  detectedDistanceInt = (int) detectedDistance;

  // Enviar datos al bluetooth->android solo cuando sea necesario
  if (detectedDistanceInt < receivedDistance) {
    Serial.println(detectedDistanceInt);
  }

  //En vez de un delay, se deberia poder 'suspender' la actividad del arduino y los sensores
  //para ahorrar energia hasta que sean despertados nuevamente para medir la distancia.
  delay(timeToSleep);


}




