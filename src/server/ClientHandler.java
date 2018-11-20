package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ClientHandler {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String nick;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //цикл авторизации
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/auth")) {
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.getNickname(tokens[1], tokens[2]);
                                if (newNick != null) {
                                    sendMsg("/authok");
                                    nick = newNick;
                                    server.subscribe(ClientHandler.this);
                                    server.addByNick(nick, ClientHandler.this);
                                    break;
                                } else {
                                    sendMsg("Неверный логин/пароль");
                                }
                            }
                        }
                        //рабочий цикл
                        while (true) {
                            String str = in.readUTF();
                            String time = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
                            if (str.equals("/end")) {
                                out.writeUTF("/serverclosed");
                                break;
                            }
                            if (str.startsWith("/pm")) {
                                String[] tokens = str.split(" ");
                                ClientHandler pmClient = server.getByNick(tokens[1]);
                                if ( pmClient != null) {
                                    String pm = "";
                                    for (int i = 2; i < tokens.length; i++) {
                                        pm += tokens[i] + " ";
                                    }
                                    pmClient.sendMsg(String.format("%s from %s: %s", time, nick, pm));
                                    sendMsg(String.format("%s to %s: %s", time, tokens[1], pm));
                                } else {
                                    sendMsg(String.format("%s is not connected", tokens[1]));
                                }
                            } else {
                                server.broadcastMsg(String.format("%s %s: %s", time, nick, str));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                        server.removeByNick(nick);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}