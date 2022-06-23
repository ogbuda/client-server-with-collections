package com.cyrex.client;

import com.cyrex.client.gui.views.LoginView;
import common.ServerRequest;
import common.route.Route;
import javafx.application.Application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Scanner;

public class Client {

    private static Scanner scanner = new Scanner(System.in);
    private static Scanner loginScanner = new Scanner(System.in);
    private static DatagramSocket ds;
    private static String serverName;
    private static int serverPort;
    private static Object[] inputArr;
    private static String command;
    private static Object argument;
    private static String login;
    private static String password;
    private static InetAddress serverAddress;
    private static Properties serverProp = new Properties();
    private static String serverPropPath = "config/client.properties";

    public static void main(String[] args) throws IOException {
        //Загрузка конфигурации
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(serverPropPath)) {
            serverProp.load(inputStream);
        }
        serverPort = Integer.parseInt(serverProp.getProperty("server.port", "1234"));
        serverName = serverProp.getProperty("server.IPv4", "127.0.0.1");
        System.out.println("connection with server " + serverName + ':' + serverPort);

        try {
            serverAddress = InetAddress.getByName(serverName);
            //Создание сокета для отправки команд
            ds = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("connection failed, check serverName");
            throw new RuntimeException(e);
        } catch (UnknownHostException ex) {
            throw new RuntimeException("connection failed, check server name and server port");
        }
        Application.launch(LoginView.class);
//        Запуск общения с сервером
//        start();
//        go();
    }


//    private static void processInput() {
//        argument = null;
//        inputArr = scanner.nextLine().split(" ");
//        command = (String) inputArr[0];
//        if (inputArr.length > 1) {
//            argument = inputArr[1];
//            if (command.equals("update")) {
//                Route routeForUpdating = new Route();
//                try {
//                    routeForUpdating.setId(Integer.parseInt((String) inputArr[1]));
//                } catch (NumberFormatException e) {
//                    System.out.println("invalid id");
//                }
//                argument = routeForUpdating;
//            }
//        }
//        if (command.equals("add")) {
//            argument = new Route();
//        } else if (command.equals("login")) {
//            login(login, password);
//        } else if (command.equals("register")) {
//            register(login, password);
//        }
//    }

    public static void sendRequest() {

        byte[] requestArr;

        //Создание потока вывода
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                baos.flush();
                oos.flush();
                //Запись команды, аргумента и данных о пользователе в этот поток
                ServerRequest request = new ServerRequest(command, argument, login, password);
                oos.writeObject(request);
                oos.flush();
                requestArr = baos.toByteArray();

                //Сначала отправляется размер запроса
                sendRequestSize(requestArr);
                //Теперь отправляется сам запрос
                DatagramPacket dp = new DatagramPacket(requestArr, requestArr.length, serverAddress, serverPort);
                ds.send(dp);

                setArgument(null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getResult() {

        try {
            //Считывание размера ответа от сервера
            int resultSize = getResultSize();

            //Прием датаграммы с результатом
            byte[] resultArr = new byte[resultSize];
            DatagramPacket resultPacket = new DatagramPacket(resultArr, resultArr.length, serverAddress, serverPort);
            ds.receive(resultPacket);
            //Распаковка полученного ответа от сервера из датаграммы
            String resultString = new String(resultPacket.getData());
            System.out.println(resultString);

            if (resultString.equals("exit")) {
                ds.close();
                System.exit(0);
            }

            if (resultString.equals("bye-bye")) {
                login = null;
                password = null;
            }
            return resultString;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getResultSize() {
        try {
            byte[] resultSizeArr = new byte[4];
            DatagramPacket resultSizePacket = new DatagramPacket(resultSizeArr, resultSizeArr.length, serverAddress, serverPort);
            ds.receive(resultSizePacket);
            return ByteBuffer.wrap(resultSizeArr).getInt();
        } catch (IOException e) {
            System.out.println("failed to get result size");
        }
        return -1;
    }

    private static void sendRequestSize(byte[] requestArr) {
        try {
            byte[] requestSize = ByteBuffer.allocate(4).putInt(requestArr.length).array();
            DatagramPacket requestSizePacket = new DatagramPacket(requestSize, requestSize.length, serverAddress, serverPort);
            ds.send(requestSizePacket);
        } catch (IOException e) {
            System.out.println("failed to send result size");
        }
    }

    public static String login(String username, String password) {
        command = "login";
        setLogin(username);
        setPassword(password);
        sendRequest();
        return getResult();
    }

    public static String register(String username, String password) {
        command = "register";
        setLogin(username);
        setPassword(password);
        sendRequest();
        return getResult();
    }

//    private static void go() {
//        while (true) {
//            processInput();
//            sendRequest();
//            getResult();
//        }
//    }

//    private static void start() {
//        System.out.println("login or register to start");
//        command = loginScanner.next();
//        switch (command) {
//            case "login" -> {
//                login(login, password);
//                sendRequest();
//                getResult();
//            }
//            case "register" -> {
//                register(login, password);
//                sendRequest();
//                getResult();
//            }
//            default -> start();
//        }
//    }

    public static String getCommand() {
        return command;
    }

    public static void setCommand(String command) {
        Client.command = command;
    }

    public static Object getArgument() {
        return argument;
    }

    public static void setArgument(Object argument) {
        Client.argument = argument;
    }

    public static String getLogin() {
        return login;
    }

    public static void setLogin(String login) {
        Client.login = login;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        Client.password = password;
    }
}