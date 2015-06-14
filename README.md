# Wallee IO

A fledgling [MQTT 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) server written in Scala,
using the upcoming [akka streams](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-RC3/scala.html?_ga=1.213223377.452692600.1432054632)
package for implementing the IO layer.

## Motivation

As of today, this project is mainly aimed at getting familiar with *akka streams* and its viability for implementing
custom protocols. It remains to be seen whether this code will one day turn into a fully developed MQTT server.

Moreover this code is part of my ongoing efforts to implement one and the same networked application - I chose an MQTT
server - using different programming languages. A C++11 version - even more bare-bones than this one! - is about to
follow.

## Status

This is pretty much pre-alpha software, i.e. it does not even fully implement MQTT's wire protocol, let alone any
serious functionality built on top of that. It might be useful, however, as sample code demonstrating how to use *akka
streams* for network IO.

