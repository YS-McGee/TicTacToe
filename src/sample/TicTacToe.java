package sample;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class TicTacToe {

    private String ip = "localhost";
    private int gamePort = 7777;

    private Scanner scanner = new Scanner(System.in);
    private JFrame gameFrame;
    private final int WIDTH = 506;
    private final int HEIGHT = 527;
    private final int FRAME_WIDTH = 900, FRAME_HEIGHT = 600;

    private Thread thread, chatThread;

    private Painter painter;

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private PrintWriter out;
    private BufferedReader in;

    private ServerSocket serverSocket;

    private BufferedImage board;
    private BufferedImage redX;
    private BufferedImage blueX;
    private BufferedImage redCircle;
    private BufferedImage blueCircle;

    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean unableToCommunicateWithOpponent = false;
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean tie = false;

    private int lengthOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;

    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    private String waitingString = "Waiting for another player";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
    private String wonString = "You won!";
    private String enemyWonString = "Opponent won!";
    private String tieString = "Game ended in a tie.";

    private int[][] wins = new int[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    /**
     * <pre>
     * 0, 1, 2
     * 3, 4, 5
     * 6, 7, 8
     * </pre>
     */

    public TicTacToe() {

        // Loads Images
        try {
            board = ImageIO.read(getClass().getResourceAsStream("board.png"));
            redX = ImageIO.read(getClass().getResourceAsStream("redX.png"));
            redCircle = ImageIO.read(getClass().getResourceAsStream("redCircle.png"));
            blueX = ImageIO.read(getClass().getResourceAsStream("blueX.png"));
            blueCircle = ImageIO.read(getClass().getResourceAsStream("blueCircle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        gameFrame = new JFrame("Tic-Tac-Toe");
        gameFrame.setContentPane(painter);
        gameFrame.setSize(WIDTH, HEIGHT);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setResizable(false);

//        sendButton.addActionListener(new Listener());                   // Bind sendBtn with the Listener
//        textField.addActionListener(new Listener());                    // Bind ENTER to Listener

        try {
            socket = new Socket(ip, gamePort);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            accepted = true;
        } catch (IOException e) {
            System.out.println("Unable to connect to the address: " + ip + ":" + gamePort);
            return;
        }

        Thread chatThread = new Thread(new ChatHandler());
        chatThread.start();

        gameFrame.setVisible(true);

        System.out.println("Successfully connected to the server.");

        Run();
    }


    public void Run() {

        String s = null;

        try {
            s = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (s.startsWith("UID"))
//            System.out.println(s);
        String uidString = s.substring(4);
        //System.out.println(uidString);
        int uid = Integer.parseInt(uidString);
        //System.out.println(uid);
        if ((uid % 2) != 0) {
            yourTurn = true;
            circle = false;
        }

        while (true) {
            tick();
            painter.repaint();

            if (!circle && !accepted) {
                listenForServerRequest();
            }

        }
    }

//    public void run() {
//        while (true) {
//            tick();
//            painter.repaint();
//
//            if (!circle && !accepted) {
//                listenForServerRequest();
//            }
//
//        }
//    }

    private void render(Graphics g) {
        g.drawImage(board, 0, 0, null);
        if (unableToCommunicateWithOpponent) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
            g.drawString(unableToCommunicateWithOpponentString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }

        if (accepted) {
            for (int i = 0; i < spaces.length; i++) {
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (won || enemyWon) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);

                g.setColor(Color.RED);
                g.setFont(largerFont);
                if (won) {
                    int stringWidth = g2.getFontMetrics().stringWidth(wonString);
                    g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                } else if (enemyWon) {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                    g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                }
            }
            if (tie) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(tieString);
                g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }

    }

    private void tick() {
        if (errors >= 10) unableToCommunicateWithOpponent = true;

        if (!yourTurn && !unableToCommunicateWithOpponent) {
            try {
                int space = dis.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForTie();
                yourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
                errors++;
            }
        }
    }

    private void checkForWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            }
        }
    }

    private void checkForEnemyWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            }
        }
    }

    private void checkForTie() {
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        tie = true;
    }

    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            render(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
                    int x = e.getX() / lengthOfSpace;
                    int y = e.getY() / lengthOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();                 // synchronizes the graphics state

                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("DATA WAS SENT");
                        checkForWin();
                        checkForTie();

                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

    }

}

class ChatHandler implements Runnable {

    static JFrame chatFrame;

    static JTextArea chatArea = new JTextArea(22, 40);
    static JTextField textField = new JTextField(40);           // para means columns
    static JLabel blankLabel = new JLabel("         ");         // between chat area and textfield
    static JLabel nameLabel = new JLabel("        ");
    static JButton sendButton = new JButton("Send");            // Send Btn

    static Socket chatSocket= null;
    static String ip;
    static final int chatPort = 9999;

    static BufferedReader in;
    static PrintWriter out;

    public ChatHandler() {
        chatFrame = new JFrame("Chat Application");
        chatFrame.setLayout(new FlowLayout());                         // arrange component
        chatFrame.add(nameLabel);
        chatFrame.add(new JScrollPane(chatArea));                      // scroller
        chatFrame.add(blankLabel);
        chatFrame.add(textField);
        chatFrame.add(sendButton);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);      // enable close action to close the application
        chatFrame.setSize(475, 500);

        textField.setEditable(false);                                   // set true once server connection established
        chatArea.setEditable(false);                                    // chatArea is only for displaying

        sendButton.addActionListener(new Listener());                   // Bind sendBtn with the Listener
        textField.addActionListener(new Listener());                    // Bind ENTER to Listener

        chatFrame.setVisible(false);
    }

    @Override
    public void run() {

        ip = (String) JOptionPane.showInputDialog(chatFrame,
                "Enter IP Address:",
                "IP Address Required!!",
                JOptionPane.PLAIN_MESSAGE, null, null,
                "localhost"
        );
        chatFrame.setVisible(true);                                    // Display these things on screen

        try {
            chatSocket = new Socket(ip, chatPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            out = new PrintWriter(chatSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            String str = null;
            try {
                str = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (str.equals("NAME_REQUIRED")) {
                String name = JOptionPane.showInputDialog(chatFrame,
                        "Enter a unique name:",
                        "Name Required!",
                        JOptionPane.PLAIN_MESSAGE);
                out.println(name);
            } else if (str.equals("NAME_ALREADY_EXIST")) {
                String name = JOptionPane.showInputDialog(chatFrame,
                        "Try another name:",
                        "Name Already Exists!!",
                        JOptionPane.WARNING_MESSAGE);
                out.println(name);
            } else if (str.startsWith("NAME_ACCEPTED")) {
                textField.setEditable(true);
                textField.requestFocusInWindow();
                nameLabel.setText("You are logged in as: " + str.substring(13));
            } else
                chatArea.append(str + "\n");                                            // msg from other clients
        }

    }
}

class Listener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ChatHandler.out.println(ChatHandler.textField.getText());                 // send whatever inside textField
        ChatHandler.textField.setText("");                                       // Clear textField
    }
}
