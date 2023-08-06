import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static DatagramSocket datagramSocket;
    private static InetAddress inetAddress;
    private static int port;
    private static String name;
    private static byte[] buffer;
    private static Scanner scanner;
    private static boolean dialogEnd = false;
    private static boolean kill = false;

    public Client(DatagramSocket datagramSocket, InetAddress inetAddress, int port) {
        Client.datagramSocket = datagramSocket;
        Client.inetAddress = inetAddress;
        Client.port = port;
        name = "No_name";
        buffer = new byte[256];
        dialogEnd = false;
    }

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

    public static class SendMes extends Thread {

        @Override
        public void run() {
            do {
                try {
                    DatagramPacket datagramPacket = null;
                    String messageToSend;
                    messageToSend = scanner.nextLine();
                    if (!isName(messageToSend).isEmpty()) {
                        name = isName(messageToSend);
                    } else if (messageToSend.contains("@quit")) {
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
            do {
                try {
                    buffer = new byte[256];
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(datagramPacket);
                    String messageFromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    if (messageFromServer.contains("@quit")) {
                        System.out.println("The dialog is finished");
                        send(datagramPacket, messageFromServer);
                        dialogEnd = true;
                        System.exit(0);
                    } else if (messageFromServer.contains("@kill")) {
                        System.out.println("You've been disconnected");
                        byte[] go = new byte[256];
                        Arrays.fill(go, (byte) 1);
                        datagramSocket.send(new DatagramPacket(go, go.length, inetAddress, port));
                        System.exit(0);
                    } else if (isEmpty(datagramPacket.getData(), 1)) {
                        dialogEnd = true;
                        kill = true;
                        break;
                    }
                    System.out.println(messageFromServer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (!dialogEnd);
        }
    }

    public void start() throws IOException {
        scanner = new Scanner(System.in);
        System.out.println("Command:\n1)@name - enter your name.\n2)@quit - exit program.\n" +
                "3)Enter message(if name don't chosen, your name: No_name)\n" +
                "4)@kill - closing the program at the option.");
        datagramSocket.send(new DatagramPacket(buffer, buffer.length, inetAddress, port));
        SendMes sendMes = new SendMes();
        WaitMes waitMes = new WaitMes();
        sendMes.start();
        waitMes.start();

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        do {
            DatagramSocket datagramSocket = new DatagramSocket();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            Scanner read = new Scanner(System.in);
            System.out.println("Enter the number of port: ");
            int port = read.nextInt();
            Client client = new Client(datagramSocket, inetAddress, port);
            client.start();
            while (!dialogEnd) {
                Thread.sleep(100);
            }
        } while (kill);
    }

}
