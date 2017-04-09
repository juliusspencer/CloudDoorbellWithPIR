# CloudDoorbellWithPIR
The Cloud Doorbell demo application for androidthings preview 3 with a PIR.

This is a non-production code example, based on the [Cloud Doorbell](https://developer.android.com/things/training/doorbell/index.html) example
to use a PIR to send an image to a companion application via Firebase.

## Hardware
For this example I used the following:
 - [Raspberry Pi Model 3 B](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)
 - [Camera Module V2](https://www.raspberrypi.org/products/camera-module-v2/)
 - D-SUN PIR (RCW-0506)

![hardware](https://github.com/juliusspencer/CloudDoorbellWithPIR/blob/master/doc_resources/hardware.jpg)

# Setup
Using a breadboard I have used BCM26 (pin 34) to plug the output from the PIR into the Raspberry Pi.

Power is supplied from pin 4 and ground is pin 14.

![diagram](https://github.com/juliusspencer/CloudDoorbellWithPIR/blob/master/doc_resources/androidthings_doorbell_pir_diagram.png)