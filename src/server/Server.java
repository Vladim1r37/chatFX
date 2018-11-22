package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private HashMap<String, ClientHandler> clientsByNick;

    public Server() {
        AuthService.connect();
        clients = new Vector<>();
        clientsByNick = new HashMap<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            server = new ServerSocket(8686);
            System.out.println("Сервер запущен!");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void addByNick(String nick, ClientHandler clientHandler) {
        clientsByNick.put(nick, clientHandler);
    }

    public void removeByNick(String nick) {
        clientsByNick.remove(nick);
    }

    public ClientHandler getByNick(String nick) {
        return clientsByNick.get(nick);
    }
}