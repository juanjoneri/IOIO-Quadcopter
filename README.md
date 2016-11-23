# Android Powered Quadcopter
![Logo](http://juanjoneri.me/img/Drone/Drone-Logo.png)<br>

## Tallerine
> El Tallerine (Taller de Introducción a la Ingeniería Eléctrica) es una nueva iniciativa de la Facultad de Ingeniería que apunta a introducir a los estudiantes a la Ingeniería Eléctrica de una manera más práctica, activa y creativa.

## Project
The objective of this project was to use the rich set of sensors that we have in our phones to stablish a closed loop control system for a drone. The phone communicates with the rotors using the IOIO OTG board and updates the specific speed of each one 60 times a second using a PID model with the data collected from the gyroscope and accelerometer.

## Instructions
The project uses 2 phones running the android operating system. In particular, two phones that can stablish a Bluetooth communication and have gyroscope and accelerometer built in.
## Brain
<img src="http://juanjoneri.me/img/Drone/brain_icon_app.png" width="150"/><br>
One phone serves as the *"brains"* of the drone and therefore has to be mounted to the Quadcopter.<br>
<img src="http://juanjoneri.me/img/Drone/Screen_brain.png" width="300"/>
<p></p>
<img src="http://juanjoneri.me/img/Drone/Screen_brain_2.png" width="300"/>

## Remote
<img src="http://juanjoneri.me/img/Drone/controller_icon_app.png" width="150"/>

The other phone is used as a remote. The user can use the remote to tweak the parameters of the PID model, and change the altitude of the drone with a slider as shown in the screens below.<br>
<img src="http://juanjoneri.me/img/Drone/Screen_Controller.png" width="300"/>
<p></p>
<img src="http://juanjoneri.me/img/Drone/Screen_Controller_2.png" width="300"/>

## Hardware

### This is what our drone looks like
<img src="http://juanjoneri.me/img/Drone/Drone.jpg"/>

### components
The project is hardware independent as we were most interested in the system itself, and having all the parts interacting together. We got our specific components from [hobbyking](https://hobbyking.com/en_us)<br>
<img src="http://juanjoneri.me/img/Drone/Hardware.jpg"/>
