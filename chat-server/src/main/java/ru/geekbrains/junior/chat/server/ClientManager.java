package ru.geekbrains.junior.chat.server;

import javax.imageio.IIOException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    //region Поля

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public static ArrayList<ClientManager> clients = new ArrayList<>();

    //endregion

    public ClientManager(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Удаление клиента из коллекции
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    @Override
    public void run() {
        String massageFromClient;

        while (socket.isConnected()) {
            try {
                // Чтение данных
                massageFromClient = bufferedReader.readLine();
                if (massageFromClient == null) {
                    // для  macOS
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                // Отправка данных всем слушателям
                privateOrBroadcastMsg(massageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }

    }

    /**
     * Отправка сообщения всем слушателям
     *
     * @param message сообщение
     */
    private void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            try {
                if (!client.name.equals(name) && message != null) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /**
     * Метод для отправки личного сообщения, если адресат не найден, отправитель получит сообщение об ошибке
     */
    private void privateMessage(String[] msg) {
        String sendName = msg[0];
        String name = msg[1].replace("@", "");
        String message = msg[0] + ": " + msg[2];
        for (ClientManager client : clients) {
            try {
                if (client.name.equals(name) && message != null) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    return;
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
        for (ClientManager client : clients) {
            try {
                if (client.name.equals(sendName)) {
                    client.bufferedWriter.write("Клинт с именем: " + name + " не найден.");
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    return;
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /**
     * Проверка является ли сообщение публичным или приватным
     * для отправика личного сообщения используется синтаксис: @nameClient: message
     */
    private void privateOrBroadcastMsg(String message) {
        String[] msg = message.split(": ");
        if (msg[1].startsWith("@")) privateMessage(msg);
        else {
            broadcastMessage(message);
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
