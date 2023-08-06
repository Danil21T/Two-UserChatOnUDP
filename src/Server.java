import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

public class Server {


    private static DatagramSocket datagramSocket;
    private static byte[] buffer = new byte[256];
    private static String name;
    private static int port;
    private static InetAddress inetAddress;
    private static boolean dialogEnd = false;
    private static boolean kill = false;

    private static String isName(String word) {
        String what = "@name";
        StringBuilder name = new StringBuilder("");
        int i;
        if (what.length() < word.length()) {
            i = word.lastIndexOf(what);
            i += 6;
            if (i >= 6) {
                for (; i < word.length(); i++) {
                    name.append(word.charAt(i));
                }
            }
        }
        return name.toString();
    }

    private static boolean isEmpty(byte[] bytes, int s) {
        for (byte symbol : bytes) {
            if (symbol != s) {
                return false;
            }
        }
        return true;
    }

    public static void send(DatagramPacket datagramPacket, String message) throws IOException {
        buffer = message.getBytes();
        datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, port);
        datagramSocket.send(datagramPacket);
    }

    public Server(DatagramSocket datagramSocket) {
        Server.datagramSocket = datagramSocket;
        name = "No_name";
        dialogEnd = false;
    }

    public static class SendMes extends Thread {
        @Override
        public void run() {
            do {
                try {
                    DatagramPacket datagramPacket = null;
                    Scanner scanner = new Scanner(System.in);
                    String messageToSend;
                    messageToSend = scanner.nextLine();
                    if (!isName(messageToSend).isEmpty()) {
                        name = isName(messageToSend);
                    } else if (messageToSend.equals("@quit")) {
                        send(datagramPacket, messageToSend);
                        dialogEnd = true;
                        break;
                    } else if (messageToSend.contains("@kill")) {
                        System.out.println("The opponent is disable");
                        send(datagramPacket, messageToSend);
                        dialogEnd = true;
                        break;
                    } else {
                        messageToSend = name + ": " + messageToSend;
                        send(datagramPacket, messageToSend);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (!dialogEnd);
        }
    }

    public static class WaitMes extends Thread {
        @Override
        public void run() {
            boolean connect = false;
            do {
                try {
                    buffer = new byte[256];
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(datagramPacket);
                    inetAddress = datagramPacket.getAddress();
                    port = datagramPacket.getPort();
                    if (connect) {
                        String messageFromClient = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        if (messageFromClient.contains("@quit")) {
                            System.out.println("The dialog is finished");
                            send(datagramPacket,messageFromClient);
                            System.exit(0);
                        } else if (messageFromClient.contains("@kill")) {
                            System.out.println("You've been disconnected");
                            byte[] go = new byte[256];
                            Arrays.fill(go, (byte) 1);
                            datagramSocket.send(new DatagramPacket(go, go.length, inetAddress, port));
                            System.exit(0);
                        } else if (isEmpty(datagramPacket.getData(), 1)) {
                            connect = false;
                            dialogEnd = true;
                            kill = true;
                            break;
                        } else {
                            System.out.println(messageFromClient);
                        }
                    }
                    if (isEmpty(datagramPacket.getData(), 0)) {
                        System.out.println("Client connected");
                        connect = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (!dialogEnd);
        }
    }

    public void start() {
        System.out.println("Command:\n1)@name - enter your name.\n2)@quit - exit program.\n" +
                "3)Enter message(if name don't chosen, your name: No_name)\n" +
                "4)@kill - closing the program at the option.");
        SendMes sendMes = new SendMes();
        WaitMes waitMes = new WaitMes();
        sendMes.start();
        waitMes.start();
        Thread.yield();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        do {
            Scanner read = new Scanner(System.in);
            System.out.println("Enter the number of port: ");
            int port = read.nextInt();
            DatagramSocket datagramSocket = new DatagramSocket(port);
            Server server = new Server(datagramSocket);
            server.start();
            while (!dialogEnd) {
                Thread.sleep(100);
            }
        } while (kill);
    }
}
