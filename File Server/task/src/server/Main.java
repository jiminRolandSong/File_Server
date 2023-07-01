package server;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
public class Main {
    static String path =  System.getProperty("user.dir") + "/src/server/data/";
    static String mapPath = System.getProperty("user.dir") + "/src/" + "map.txt";
    static Map<String, String> fileIdMap = new HashMap<>(); //<fileName, id>
    static int lastGeneratedId = new Random().nextInt(10000);
    static volatile boolean status = true;

    public static void saveMap(Map<String, String> map) {
        try (FileWriter writer = new FileWriter(mapPath)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String fileName = entry.getKey();
                String id = entry.getValue();
                writer.write(fileName+ " " + id + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> loadMap(){
        Map<String, String> map = new HashMap<>();
        File file = new File(mapPath);
        try (Scanner scanner = new Scanner(file)){
            while (scanner.hasNext()) {
                String[] parts = scanner.nextLine().split(" ");
                if (parts.length == 2) {
                    String fileName = parts[0];
                    String id = parts[1];
                    map.put(fileName, id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }


    public static void save(DataInputStream input, DataOutputStream output) throws IOException{
        String fileName = input.readUTF();

        int length = input.readInt();
        byte[] fileData = new byte[length];
        input.readFully(fileData, 0, fileData.length);

        String id = input.readUTF();
        if (id.equals("")) {
            id = String.valueOf(lastGeneratedId);
            lastGeneratedId += 1;
        } else{
            fileName = id;
        }

        File file = new File(path + id);
        if (file.exists()){
            output.writeUTF("403");
        } else{
            try (FileOutputStream stream = new FileOutputStream(path + id)) {
                stream.write(fileData);
                output.writeUTF("200");
                output.writeUTF(id);
                fileIdMap.put(fileName, id);
            } catch (IOException e) {
                output.writeUTF("403");
            }
        }
    }

    public static void get(DataInputStream input, DataOutputStream output) throws IOException{
        String getBy = input.readUTF();
        String dir = "";
        if (Objects.equals(getBy, "name")){
            String name = input.readUTF();
            dir = fileIdMap.get(name);
        } else if (Objects.equals(getBy, "id")) {
            dir = input.readUTF();
        }

        File filePath = new File(path + dir);
        try {
            if (filePath.exists() && filePath.isFile()) {
                output.writeUTF("200");
                byte[] data = Files.readAllBytes(filePath.toPath());
                byte[] dataCopy = data.clone();

                output.writeInt(dataCopy.length);
                output.write(dataCopy);
            } else {
                output.writeUTF("404");
            }
        } catch (IOException e) {
            output.writeUTF("404");
        }
    }
    public static void delete(DataInputStream input, DataOutputStream output) throws IOException{
        String getBy = input.readUTF();
        String dir = "";
        if (Objects.equals(getBy, "name")){
            String name = input.readUTF();
            dir = fileIdMap.get(name);
        } else if (Objects.equals(getBy, "id")) {
            dir = input.readUTF();
        }
        File filePath = new File(path + dir);
        if (filePath.exists() && filePath.isFile()){
            if(filePath.delete()){
                output.writeUTF("200");
            } else{
                output.writeUTF("403");
            }
        } else{
            output.writeUTF("403");
        }
    }

    public static void operator(ServerSocket server) {
        ExecutorService executorService = Executors.newCachedThreadPool();

        while (status) {
            try {
                Socket socket = server.accept();
                executorService.submit(() -> {
                    try (
                            DataInputStream input = new DataInputStream(socket.getInputStream());
                            DataOutputStream output = new DataOutputStream(socket.getOutputStream())
                    ) {
                        String action = input.readUTF();
                        switch (action) {
                            case "save":
                                save(input, output);
                                break;
                            case "get":
                                get(input, output);
                                break;
                            case "delete":
                                delete(input, output);
                                break;
                            case "exit":
                                saveMap(fileIdMap);
                                status = false;
                                break;
                        }

                        if (action.equals("exit")) {
                            socket.close();
                            server.close();
                            executorService.shutdown();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        if (new File(mapPath).exists()) {
            fileIdMap = loadMap();
        }
        System.out.println("Server started!");
        String address = "127.0.0.1";
        int port = 23456;
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
            operator(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}