/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author user
 */
public class ClientA {

    private static InetAddress serverIP;
    private static int serverTcpPort;
    private static int serverUdpPort;
    private Socket socket;
    private final BufferedReader in;
    private final BufferedOutputStream out;
    private final DatagramSocket dgSocket;
    private DatagramPacket sendPacket;
    private String resp = "";
    private String[] tokens = null;
    private boolean respRead;

    public ClientA(InetAddress ip, int tcpPort, int udpPort) throws IOException {

        //create a socket to connect to the server
        try {
            socket = new Socket(ip, tcpPort);
        } catch (IOException ex) {
            System.err.println("Exception creating a socket: " + ex);
        }

        //create input and output stream
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());

        //Create a Datagram Socket for UDP messages
        dgSocket = new DatagramSocket();

        //create a byte array to hold the initial message to be sent to the server
        byte[] sendData = "one".getBytes();

        //create a packet to send udp messge 
        sendPacket = new DatagramPacket(sendData, sendData.length, ip, udpPort);

        //create a loop to send the udp packets to the server
        /**
         * IMPORTANT! Using a loop to send the packets is just to ensure that
         * the UDP packets reach the server. There may be a loss of UDP packets
         * for its unreliability. You can change the loop count if required.
         */
        System.out.println("sending initial udp message");
        for (int i = 0; i < 100000; i++) {
            dgSocket.send(sendPacket);
            System.out.println("" + i);
        }
        System.out.println("Sent initial udp messages");
        //create a loop to read the TCP response from the server
        while (respRead != true) {
            resp = in.readLine();

            tokens = resp.split("~~");  //split response into tokens for IP and Port

            System.out.println("****************************************");
            System.out.println("My PUBLIC IP seen by server: " + tokens[0]);
            System.out.println("My PUBLIC UDP PORT seen by server: " + tokens[1]);
            System.out.println("****************************************\n");

            System.out.println("****************************************");
            System.out.println("CLIENT B PUBLIC IP seen by server: " + tokens[2]);
            System.out.println("CLIENT B PUBLIC UDP PORT seen by server: " + tokens[3]);
            System.out.println("****************************************");

            respRead = true;

            //ACK SERVER
            out.write("ackOne".getBytes());
            out.write('\n');
            out.flush();

        }

        //Create thread to receive UDP packets 
        new Thread(new Runnable() {
            private String udpMsg = "";

            @Override
            public void run() {
                //create datagram packet to receive udp messages
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                while (true) {
                    try {
                        dgSocket.receive(receivePacket);    //receiver udp packet

                        udpMsg = new String(receivePacket.getData());   //get data from the received udp packet

                        System.out.println("Received: " + udpMsg.trim() + ", From: IP " + receivePacket.getAddress().getHostAddress().trim() + " Port " + receivePacket.getPort());
                    } catch (IOException ex) {
                        System.err.println("Error " + ex);
                    }

                }
            }

        }).start();

        //create Loop to send udp packets
        int j = 0;
        String msg = "";
        while (true) {
            msg = "I AM CLIENT A " + j;
            sendData = msg.getBytes();
            DatagramPacket sp = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(tokens[2].trim()), Integer.parseInt(tokens[3].trim()));
            dgSocket.send(sp);
            j++;
            try{
                Thread.sleep(2000);
            }catch(Exception e){
                System.err.println("Exception in Thread sleep"+e);
            }
        }

    }

    public static void main(String[] args) throws UnknownHostException, IOException {

        if (args.length > 0) {
            try {
                serverIP = InetAddress.getByName(args[0].trim());
                serverTcpPort = Integer.parseInt(args[1].trim());
                serverUdpPort = Integer.parseInt(args[2].trim());
            } catch (Exception ex) {
                System.err.println("Error in input");
                System.out.println("USAGE: java ClientA serverIp serverTcpPort serverUdpPort");
                System.out.println("Example: java ClientA 127.0.0.1 9000 9001");
                System.exit(0);
            }

        } else {
            System.out.println("ClientA running with default ports 9000 and 9001");
            serverIP = InetAddress.getByName("127.0.0.1");
            serverTcpPort = 9000;
            serverUdpPort = 9001;

        }
        new ClientA(serverIP, serverTcpPort, serverUdpPort);
    }
}
