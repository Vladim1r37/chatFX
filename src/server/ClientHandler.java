package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

public class ClientHandler {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String nick;
    private ArrayList<String> blackList;


    public ClientHandler(Server server, Socket socket) {
        try {
            this.blackList = new ArrayList<>();
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            long startTime = System.currentTimeMillis();


            new Thread(() -> {
                try {
                    //цикл авторизации
                    while (true) {
                        String str = in.readUTF();
                        if (System.currentTimeMillis() - startTime < 120000) {

                            if (str.startsWith("/signup")) {
                                String[] tokens = str.split(" ");
                                if (AuthService.signUp(tokens[1], tokens[2], tokens[3])) {
                                    sendMsg("Регистрация прошла успешно");
                                } else sendMsg("Недопустимый логин/никнэйм");
                            }

                            if (str.startsWith("/auth")) {
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.getNickname(tokens[1], tokens[2]);
                                if (newNick != null) {
                                    if (!server.isNickBusy(newNick)) {
                                    sendMsg("/authok");
                                    nick = newNick;
                                    server.subscribe(this);
                                    server.addByNick(nick, this);
                                    AuthService.getBlacklist(blackList, nick);
                                    break;
                                    } else sendMsg("Учетная запись уже используется!");
                                } else {
                                    sendMsg("Неверный логин/пароль");
                                }
                            }

                        } else {
                            sendMsg("Превышено время ожидания авторизации");
                            sendMsg("/autherror");
                            return;
                        }
                    }
                    //рабочий цикл
                    while (true) {
                        String str = in.readUTF();
                        String time = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
                        if (str.startsWith("/")) {

                            if (str.equals("/end")) {
                                out.writeUTF("/serverclosed");
                                break;
                            }
                            if (str.startsWith("/pm")) {
                                String[] tokens = str.split(" ", 3);
                                ClientHandler pmClient = server.getByNick(tokens[1]);
                                if (pmClient != null) {
                                    pmClient.sendMsg(String.format("%s from %s: %s", time, nick, tokens[2]));
                                    sendMsg(String.format("%s to %s: %s", time, tokens[1], tokens[2]));
                                } else {
                                    sendMsg(String.format("%s is not connected", tokens[1]));
                                }
                            }
                            if (str.startsWith("/blacklist")) {
                                String[] tokens = str.split(" ");
                                blackList.add(tokens[1]);
                                sendMsg("Вы добавили пользователя с ником " + tokens[1] + " в черный список!");
                            }
                            if (str.startsWith("/unblock")) {
                                String[] tokens = str.split(" ");
                                blackList.remove(tokens[1]);
                                sendMsg("Вы убрали пользователя с ником " + tokens[1] + " из черного списка!");
                            }
                        } else {
                            server.broadcastMsg(this, String.format("%s %s: %s", time, nick, str));
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
                    if (!blackList.isEmpty()) {
                        AuthService.saveBlacklist(blackList, nick);
                    }
                    server.unsubscribe(this);
                    server.removeByNick(nick);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkBlackList(String nick) {
        return blackList.contains(nick);
    }

}