# Algorithms-NetworkTopologyDetector
Project for Algorithms and Data Structures-- network topology detector.

## Problem
Provide a RESTful service which accepts as a POST of JSON as a representation of a network of computers.  Assume each connection between computers is bidirectional.
<br />Output a JSON message indicating if the topology is a bus (1), a ring (2), or a star (3).  If the topology is none of these, then output the type as “irregular”.
<br />Example input:
<br />{ “inList” : [ { “connected” : [ “A”, “B” ] },
<br />{ “connected” : [ “B”, “C” ] },
<br />{ “connected” : [ “C”, “D” ] },
<br />{ “connected” : [ “D”, “E” ] }] }
<br />Example output:
<br />{ “type” : “bus” }
<br />Erroneous input (e.g. malformed JSON) should be handled gracefully.  

## Deliverable
An HTTP URL was available for the class project yet was destroyed upon completion. Users invoked a RESTful service with a tool such as curl or Postman.
