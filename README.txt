title: SmartWatch
author: daniel
date: 13.03.2014

# Motivation

	> Künftige Nissan-Autofahrer sollen eine Smartwatch tragen, die
	> ihren Körper, aber auch das Auto überwacht. So könnte
	> beispielsweise der Stress während bestimmter Fahrsituationen
	> ermittelt werden.

In diesem Projekt sollen Sie einen ersten Prototypen entwicklen,
welcher aehnlich zu Nissan's Nismo Smartwatch das System ueberwacht
und die gesamelten Daten drahtlos an die Uhr uebertraegt.

Hierbei kommt ein Standard Linux-Rechner zum Einsatz, dessen Systemwerte
auf der Uhr angezeigt werden sollen. 

# Anforderungen

Die Uhr ist frei programmierbar, nutzen Sie dies um eine eigene GUI zu
entwickeln, die moeglichst sinnvoll die gesendeten Daten
anzeigt. Erweitern Sie den Code des LCD4Linux-Projekts um die
Moeglichkeit drahtlose Displays via Bluetooth ansteuern zu koennen
(Socket-Plugin/Driver etc.).

# Ziele

- Entwicklung einer offenen GUI fuer die SmartWatch
- Implementierung eines Display-Treibers zur Ansteuerung drahtloser
  Displays (LCD4Linux)
- Auslesen und Aufbereiten der gesendeten Daten auf der Uhr

# Setup

[Sony SmartWatch] -- BLUETHOOTH -- [PC/LCD4Linux]

# Bonus

Steuerung des Rechners (z.B. Display Helligkeit)
