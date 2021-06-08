package sample;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class Player {
    public Socket socket;
    public Socket opponentSocket;
    public int uid;
}

public class ServerTicTacToe implements Runnable{

    static final int MAX_PLAYER_CNT = 5;                  // less than this num

    private ServerSocket serverSocket;
    private String ip = "localhost";
    private int gamePort = 7777;
    private int uid1 = 1, uid2 = 2;
    private Socket socket1 = null, socket2= null;
    private Socket next_socket = null;
    private Socket pre_socket = null;

    private Thread thread1, thread2;

    public ServerTicTacToe() {

        try {
            serverSocket = new ServerSocket(gamePort, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        System.out.println("Server Running .... ");
        while (true) {
            try {
                socket1 = serverSocket.accept();
                System.out.println("u1");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                socket2 = serverSocket.accept();
                System.out.println("u2");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Player player1 = new Player();
            player1.socket = socket1;
            player1.opponentSocket = socket2;
            player1.uid = uid1;

            Player player2 = new Player();
            player2.socket = socket2;
            player2.opponentSocket = socket1;
            player2.uid = uid2;


            thread1 = new Thread(new ServerHandler(player1));
            thread2 = new Thread(new ServerHandler(player2));
            thread1.start();
            thread2.start();
        }
    }

    public static void main(String[] args) throws IOException {
        Thread gameThread = new Thread(new ServerTicTacToe());
        gameThread.start();
        Thread chatThread = new Thread(new ChatServer());
        chatThread.start();
    }
}

class ChatServer implements Runnable {

    static ArrayList<String> userName = new ArrayList<String>();
    static ArrayList<PrintWriter> printWriters = new ArrayList<PrintWriter>();      // Send msg to all client

    Socket chatSocket;
    static final int chatPort = 9999;

    static BufferedReader in;
    static PrintWriter out;
    String name = null;

    public ChatServer() throws IOException {
        System.out.println("Chat Server Waiting for clients...");

        ServerSocket chatServerSocket = new ServerSocket(chatPort);
        // Rx incoming conn
        while (true) {
            chatSocket = chatServerSocket.accept();
            System.out.println("Chat Server Connection Established");
            Thread thread = new Thread(this::run);
            thread.start();
        }
    }

    @Override
    public void run() {
        try {

            in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            out = new PrintWriter(chatSocket.getOutputStream(), true);

            int count = 0;
            while (true) {

                if(count > 0)
                    out.println("NAME_ALREADY_EXIST");
                else
                    out.println("NAME_REQUIRED");

                name = in.readLine();
                if (name == null)
                    return;
                if (!ChatServer.userName.contains(name)) {
                    ChatServer.userName.add(name);
                    break;
                }
                ++count;
            }
            out.println("NAME_ACCEPTED"+name);
            ChatServer.printWriters.add(out);

            while (true) {
                String message = in.readLine();

                if (message == null)
                    return;
                for (PrintWriter writer : ChatServer.printWriters)              // For all writer in printWriter
                    writer.println(name + ": " + message);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class ServerHandler implements Runnable{

    Player player;

    Socket socket, opponentSocket;
    int uid;

    private DataOutputStream dos, oppoDos;
    private DataInputStream dis, oppoDis;
    private PrintWriter out, oppoOut;
    private BufferedReader in, oppoIn;

    public ServerHandler(Player player) {
        this.player = player;
        this.socket = player.socket;
        this.opponentSocket = player.opponentSocket;
        this.uid = player.uid;

        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            oppoDos = new DataOutputStream(opponentSocket.getOutputStream());
            oppoDis = new DataInputStream(opponentSocket.getInputStream());
            oppoOut = new PrintWriter(opponentSocket.getOutputStream(), true);
            oppoIn = new BufferedReader(new InputStreamReader(opponentSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("UID " + uid + " has joined.");

        out.println("UID=" + uid);

        int position = 0;

        while (true) {
            try {
                position = dis.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(position);
            // send to opponent
            //oppoOut.println(position);
            try {
                oppoDos.writeInt(position);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
