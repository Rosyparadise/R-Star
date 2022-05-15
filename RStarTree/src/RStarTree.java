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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner; // Import the Scanner class to read text files

public class RStarTree
{
    private static long noOfNodes;
    private static long noOfBlocks;
    private final static char delimiter = '$';
    private final static char blockSeperator = '#';



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

    public static byte[] charToBytes(Character x) {
        ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES);
        buffer.putChar(x);
        return buffer.array();
    }



    public static void main(String[] args) throws ParserConfigurationException, SAXException
    {

        //open file
        try
        {
            File datafile = new File("datafile.dat");
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
            FileOutputStream  dataWriter = new FileOutputStream("datafile.dat");

            //link xml parser to .osm file
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File("map.osm"));
            //get rid of white spaces
            doc.getDocumentElement().normalize();

            //node block represents the <node> we are currently processing
            Node block;
            //noOfNodes is the number of <nodes> in the .osm file
            noOfNodes = doc.getElementsByTagName("node").getLength();


            //data to save
            int bytecounter=0;
            long id;
            String name=null;
            double lat, lon;

            //where the data will be saved after being converted into byte arrays
            byte[] b_id, b_lat, b_lon, b_name=null;
            //first block will overwrite this with data
            dataWriter.write("xxxxxxxxxxxxxxxx".getBytes());
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
                            b_name = children.item(j).getAttributes().getNamedItem("v").getNodeValue().getBytes(StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                if (b_name==null) {
                    //dataWriter.write(id + " " + lat + " " + lon);
                    dataWriter.write(b_id);dataWriter.write(b_lat);dataWriter.write(b_lon);
                    System.out.println(id + " "+ lat +" "+lon);
                    //dataWriter.write(id + " " + lat + " " + lon);
                    bytecounter+=26;
                }
                else {
                    //dataWriter.write(id + " " + lat + " " + lon + " " + name);
                    dataWriter.write(b_id);dataWriter.write(b_lat);dataWriter.write(b_lon);dataWriter.write(b_name);
                    System.out.println(id + " "+ lat +" "+lon + " " + name);
                    //dataWriter.write(id + " " + lat + " " + lon + " " + name);
                    bytecounter+=(26+b_name.length);
                }
                dataWriter.write(charToBytes(delimiter));
                if (bytecounter+26>32000)
                {
                    dataWriter.write(charToBytes(blockSeperator));
                    noOfBlocks++;
                    bytecounter=0;
                }

                name=null;
                b_name=null;


            }
            dataWriter.close();


            //first block
            RandomAccessFile datafile = new RandomAccessFile("datafile.dat", "rw");
            datafile.seek(0);
            datafile.write(longToBytes(noOfBlocks));
            datafile.write(longToBytes(noOfNodes));
            datafile.close();
        }

        catch (IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        try {
            File file = new File("datafile.dat");
            //byte arrays to save the byte representation of the variables
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] nodesArray = new byte[8],BlocksArray = new byte[8];
            byte[] delimiterArray = new byte[2],blockSeperatorArray = new byte[2];
            byte[] NodeIdArray = new byte[8];
            byte[] LatArray = new byte[8],LonArray = new byte[8];
            //current byte
            int bytecounter=0;
            //actual variables
            long tempNodeId;
            double tempLat,tempLon;
            String tempName="";
            char newlinestr;

            //reading first block
            System.arraycopy(bytes, 0, BlocksArray, 0, 8);
            System.arraycopy(bytes, 8, nodesArray, 0, 8);
            System.out.println("Number of blocks= " + ByteBuffer.wrap(BlocksArray).getLong());
            System.out.println("Number of nodes= " + ByteBuffer.wrap(nodesArray).getLong());
            bytecounter+=16;



            for (int i=0;i<noOfNodes;i++)
            {

                //byte[] nameArray = new byte[18];
                System.arraycopy(bytes, bytecounter, NodeIdArray, 0, 8);
                System.arraycopy(bytes, bytecounter+8, LatArray, 0, 8);
                System.arraycopy(bytes, bytecounter+16, LonArray, 0, 8);

                System.arraycopy(bytes, bytecounter+24, delimiterArray,0,2);

                tempNodeId = ByteBuffer.wrap(NodeIdArray).getLong();
                tempLat = ByteBuffer.wrap(LatArray).getDouble();
                tempLon = ByteBuffer.wrap(LonArray).getDouble();
                newlinestr = ByteBuffer.wrap(delimiterArray).getChar();

                int tempcounter=0;

                //count bytes until you meet the delimiter if not already met above
                while (newlinestr!=delimiter)
                {
                    tempcounter+=1;
                    System.arraycopy(bytes, bytecounter+24+tempcounter, delimiterArray,0,2);
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();


                }

                //write name
                if (tempcounter!=0)
                {
                    byte[] nameArray = new byte[tempcounter];
                    System.arraycopy(bytes, bytecounter+24, nameArray,0,tempcounter);
                    tempName = new String(nameArray);

                }


                System.out.println("Node id: " + tempNodeId);
                System.out.println("LAT: " + tempLat);
                System.out.println("LON: " + tempLon);
                System.out.println("nl: "+ newlinestr);


                if (tempcounter!=0)
                {
                    System.out.println("Name: "+ tempName);
                    bytecounter+=26+tempcounter;

                }
                else
                    bytecounter+=26;



                if (i+1<noOfNodes) {
                    System.arraycopy(bytes, bytecounter, blockSeperatorArray, 0, 2); // DOESNT ADD BYTES IF FILE ENDS WITH #
                    if (ByteBuffer.wrap(blockSeperatorArray).getChar()==blockSeperator)
                        bytecounter+=2;
                }
                /*
                else
                {
                    if (bytes.length>bytecounter)
                        bytecounter+=2;
                }
                tempcounter=0;
                */
            }
            System.out.print(bytecounter);


        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}