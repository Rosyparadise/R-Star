import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Scanner; // Import the Scanner class to read text files

public class RStarTree
{


    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static byte[] doubleToBytes(double x) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(x);
        return buffer.array();
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException
    {

        //open file
        try
        {
            File datafile = new File("datafile.txt");
            if (datafile.createNewFile()) {
                System.out.println("File created: " + datafile.getName());
            }
        }
        catch (IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try
        {
            //stream to write bytes to text file
            FileOutputStream  dataWriter = new FileOutputStream("datafile.txt");

            //link xml parser to .osm file
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File("map.osm"));
            //get rid of white spaces
            doc.getDocumentElement().normalize();

            //node block represents the <node> we are currently processing
            Node block;
            //noOfNodes is the number of <nodes> in the .osm file
            int noOfNodes = doc.getElementsByTagName("node").getLength();
            //data to save
            long id;
            String name=null;
            double lat, lon;

            //where the data will be saved after being converted into byte arrays
            byte[] b_id, b_lat, b_lon, b_name=null;

            //for every <node>
            for (int i = 0; i < noOfNodes; i++)
            {

                block = doc.getElementsByTagName("node").item(i);
                //get its attributes
                NamedNodeMap attrList = block.getAttributes();

                //save the ones we want
                id = Long.parseLong(attrList.getNamedItem("id").getNodeValue().toString());
                lat = Double.parseDouble(attrList.getNamedItem("lat").getNodeValue());
                lon = Double.parseDouble(attrList.getNamedItem("lon").getNodeValue());
                //and into byte[] form
                b_id = longToBytes(Long.parseLong(attrList.getNamedItem("id").getNodeValue().toString()));
                b_lat = doubleToBytes(Double.parseDouble(attrList.getNamedItem("lat").getNodeValue()));
                b_lon = doubleToBytes(Double.parseDouble(attrList.getNamedItem("lon").getNodeValue()));

                //if <node> has children>
                if (block.getChildNodes().getLength() > 0)
                {
                    NodeList children = block.getChildNodes();

                    //for every child
                    for (int j=1; j < children.getLength(); j+=2)
                    {
                        //get its attributes and check if there is one called k with the value name
                        if (children.item(j).getAttributes().getNamedItem("k").getNodeValue().equals("name"))
                        {
                            //if there is, save the value of attribute v as the name
                            name = children.item(j).getAttributes().getNamedItem("v").getNodeValue();
                            b_name = children.item(j).getAttributes().getNamedItem("v").getNodeValue().getBytes();
                            break;
                        }
                    }
                }
                if (b_name==null) {
                    //dataWriter.write(id + " " + lat + " " + lon);
                    dataWriter.write(b_id);dataWriter.write(b_lat);dataWriter.write(b_lon);
                    System.out.println(id + " "+ lat +" "+lon);
                    //dataWriter.write(id + " " + lat + " " + lon);
                    //bytecounter+=24;
                }
                else {
                    //dataWriter.write(id + " " + lat + " " + lon + " " + name);
                    dataWriter.write(b_id);dataWriter.write(b_lat);dataWriter.write(b_lon);dataWriter.write(b_name);
                    System.out.println(id + " "+ lat +" "+lon);
                    //dataWriter.write(id + " " + lat + " " + lon + " " + name);
                    //bytecounter+=(24+b_name.length);
                }
                dataWriter.write("\n".getBytes());

                name=null;
                b_name=null;


            }
            dataWriter.close();


        }

        catch (IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            File file = new File("datafile.txt");
            byte[] bytes = Files.readAllBytes(file.toPath());

            byte[] NodeIdArray = new byte[8];
            byte[] LatArray = new byte[8];
            byte[] LonArray = new byte[8];

            System.arraycopy(bytes, 0, NodeIdArray, 0, 8);
            System.arraycopy(bytes, 8, LatArray, 0, 8);
            System.arraycopy(bytes, 16, LonArray, 0, 8);

            long tempNodeId = ByteBuffer.wrap(NodeIdArray).getLong();
            double tempLat = ByteBuffer.wrap(LatArray).getDouble();
            double tempLon = ByteBuffer.wrap(LonArray).getDouble();

            System.out.println("Node id: " + tempNodeId);
            System.out.println("LAT: " + tempLat);
            System.out.println("LON: " + tempLon);

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

}

