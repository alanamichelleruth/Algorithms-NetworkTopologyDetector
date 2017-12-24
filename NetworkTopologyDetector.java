package csi403;

// Import required java libraries
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.json.*;
import java.util.*;
import java.lang.*;

// Extend HttpServlet class
public class NetworkTopologyDetector extends HttpServlet {

  public PrintWriter out;

  // Standard servlet method 
  public void init() throws ServletException
  {
      // Do any required initialization here - likely none
  }

  // Standard servlet method - handles a POST operation
  public void doPost(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      response.setContentType("application/json");
      out = response.getWriter();

      try {
          doService(request, response);
      } catch (Exception e){
          e.printStackTrace();
          out.println("{ \"message\" : \"Malformed JSON\"}");
      }
  }

  // Standard servlet method - does not respond to GET
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      // Set response content type and return an error message
      response.setContentType("application/json");
      out = response.getWriter();
      out.println("{ \"message\" : \"Use POST!\"}");
  }


  // Our main worker method
  private void doService(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      // Get received JSON data from HTTP request
      BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
      String jsonStr = "";
      if(br != null){
          jsonStr = br.readLine();
      }

      // Create JsonReader object
      StringReader strReader = new StringReader(jsonStr);
      JsonReader reader = Json.createReader(strReader);

      // Get the singular JSON object (name:value pair) in this message.    
      JsonObject obj = reader.readObject();

      //If more than one key:value pair (not only "inList" present), send an error message
      if(obj.size() > 1){
        out.println("{ \"message\" : \"Invalid number of key:value pairs\" }");
        return;
      }

      // From the object get the array named "inList"
      JsonArray inArray = obj.getJsonArray("inList");
      //Declare variable to hold each element of the inArray
      JsonObject element;
      //Temp string for each node name
      String tempName;
      //ArrayList of String for the nodes
      ArrayList<String> nodeNames = new ArrayList<String>();
      //ArrayList of Linked List<String> for adjacency representations
      ArrayList<ArrayList<String>> nodeConnections = new ArrayList<ArrayList<String>>();
      //Temporary ArrayList to aid in adding new nodeConnections
      ArrayList<String> tempList;

      JsonArrayBuilder outArrayBuilder = Json.createArrayBuilder();

      //Access and execute the commands from "inList"
      for(int i = 0; i < inArray.size(); i++) {
          element = inArray.getJsonObject(i);

          //If more than one key:value pair (not only "connected" present), send an error message
          if (element.size() > 1) {
              out.println("{ \"message\" : \"Invalid number of key:value pairs\" }");
              return;
          }

          //If element does not have "connected", send an error message
          if (!element.containsKey("connected")) {
              out.println("{ \"message\" : \"No \"connected\" present\" }");
              return;
          }

          //If "connected" array is not of size 2, send an error message
          if (element.getJsonArray("connected").size() != 2) {
              out.println("{ \"message\" : \"\"connected\" array must be of size 2\" }");
              return;
          }

          //Initialize nodes array to be the elements
          for (int j = 0; j < 2; j++) {
              //Set temp name to the current node name
              tempName = element.getJsonArray("connected").getString(j);

              //If it is the first appearance of the node name, add it to the array list of node names
              if (!nodeNames.contains(tempName)) {
                  nodeNames.add(tempName);
                  tempList = new ArrayList<String>();

                  //Add the appropriate bidirectional connection
                  if (j == 0) {
                      tempList.add(element.getJsonArray("connected").getString(1));
                  } else {
                      tempList.add(element.getJsonArray("connected").getString(0));
                  }

                  nodeConnections.add(tempList);
              }
              //Otherwise, add the approriate bidirectional connection to the end of the existing connections
              else {
                  tempList = nodeConnections.get(nodeNames.indexOf(tempName));

                  if (j == 0) {
                      tempList.add(element.getJsonArray("connected").getString(1));
                  } else {
                      tempList.add(element.getJsonArray("connected").getString(0));
                  }
                  nodeConnections.set(nodeNames.indexOf(tempName), tempList);
              }
          }
      }

      ////////////////////////////////////////////////////////////////////////////////////////
      //Check to see if the network topology is a BUS
      boolean isBus = true;
      int numEnds = 0;
      for(int l = 0; l < nodeConnections.size(); l++) {
          //If there is one connection, it is an end, so increment numEnds
          if (nodeConnections.get(l).size() == 1)
              numEnds++;
          //Else if there are not two connections, it is not a bus and break out of loop
          else if (nodeConnections.get(l).size() != 2) {
              isBus = false;
              break;
          }
      }
      //At the end, there must be 2 ends, otherwise it is not a bus
      if(numEnds != 2)
          isBus = false;
      //If it is a bus, print so and quit
      if(isBus){
          outArrayBuilder.add("bus");
          out.println("{ \"outList\" : " + outArrayBuilder.build().toString() + " }");
          return;
      }

      ////////////////////////////////////////////////////////////////////////////////////////
      //Check to see if the network topology is a STAR
      boolean isStar = true;
      int numOrigins = 0;
      int originLocation = -1;
      for(int m = 0; m < nodeConnections.size(); m++) {
          //If there is more than 1 connection, it is an origin, so increment numOrigins
          if (nodeConnections.get(m).size() > 1) {
              numOrigins++;
              originLocation = m;
          }
          //Else if there is not one connections, it is not a star and break out of loop
          else if (nodeConnections.get(m).size() != 1) {
              isStar = false;
              break;
          }
      }
      //At the end, there can only be one origin, otherwise it is not a star
      if(numOrigins != 1)
          isStar = false;
      //Make sure the origin has the right amount of rays (nodeNames' size - 1), otherwise it is not a star
      else if(originLocation != -1 && nodeConnections.get(originLocation).size() != (nodeNames.size()-1))
          isStar = false;
      //If it is a star, print so and quit
      if(isStar){
          outArrayBuilder.add("star");
          out.println("{ \"outList\" : " + outArrayBuilder.build().toString() + " }");
          return;
      }

      ///////////////////////////////////////////////////////////////////////////////////////
      //Check to see if the network topology is a RING
      boolean isRing = true;
      for(int k = 0; k < nodeConnections.size(); k++) {
          //If there are not 2 connections, it is not a ring and break out of loop
          if (nodeConnections.get(k).size() != 2) {
              isRing = false;
              break;
          }
      }
      //If it is a ring, print so and quit
      if(isRing) {
          outArrayBuilder.add("ring");
          out.println("{ \"outList\" : " + outArrayBuilder.build().toString() + " }");
          return;
      }


      ////////////////////////////////////////////////////////////////////////////////////////
      //If none of the above happened (not a ring, bus, or star), this will execute, so say the topology is irregular
      outArrayBuilder.add("irregular");
      out.println("{ \"outList\" : " + outArrayBuilder.build().toString() + " }");
      return;
  }

  //Method to add the appropriate node to the end of the existing connections at that place, and return the new connections array list
  private ArrayList<ArrayList<String>> addConnection(ArrayList<ArrayList<String>> connections, String nodeName, ArrayList<String> existing, int place) {
      existing.add(nodeName);
      //If place < 0, add to the end of connections
      if (place < 0)
          connections.add(existing);
      //Otherwise, add it to given place
      else
          connections.set(place, existing);

      return connections;
  }

  // Standard Servlet method
  public void destroy() {
      // Do any required tear-down here, likely nothing.
  }
}