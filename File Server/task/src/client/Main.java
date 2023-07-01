package client;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    static String path =  System.getProperty("user.dir") + "/src/client/data/";
    //static String path = System.getProperty("user.dir") + "/File Server/task/src/client/data/";
    public static void save(DataInputStream input, DataOutputStream output) throws IOException {
        output.writeUTF("save");

        System.out.println("Enter filename:");
        String name = scanner.nextLine();
        output.writeUTF(name);

        String filepath = path + name;
        byte[] data = Files.readAllBytes(Path.of(filepath));
        output.writeInt(data.length);
        output.write(data);

        System.out.println("Enter name of the file to be saved on the server:");
        String savedName = scanner.nextLine();
        output.writeUTF(savedName);

        System.out.println("The request was sent");
        String result = input.readUTF();
        if (result.equals("200")){
            String id = input.readUTF();
            System.out.printf("Response says that file is saved! ID = %s\n",id);
        } else{
            System.out.println("The response says that creating the file was forbidden!");
        }
    }

    public static void get(DataInputStream input, DataOutputStream output) throws IOException {
        output.writeUTF("get");

        System.out.println("Do you want to get the file by name or by id (1 - name, 2 - id):");
        String getBy = scanner.nextLine();
        if (Objects.equals(getBy, "1")){
            output.writeUTF("name");
            System.out.println("Enter name:");
            String name = scanner.nextLine();
            output.writeUTF(name);
        } else if (Objects.equals(getBy, "2")) {
            output.writeUTF("id");
            System.out.println("Enter id:");
            String id = scanner.nextLine();
            output.writeUTF(id);
        }
        System.out.println("The request was sent.");
        String result = input.readUTF();
        if (result.equals("200")){
            int length = input.readInt();
            byte[] fileData = new byte[length];
            input.read(fileData, 0, fileData.length);
            System.out.println("The file was downloaded! Specify a name for it:");
            String newName = scanner.nextLine();
            try (FileOutputStream stream = new FileOutputStream(path + newName)) {
                stream.write(fileData);
            } catch (IOException e) {
                System.out.println("The response says that creating the file was forbidden!");
            }
        } else{
            System.out.println("The response says that this file is not found!");
        }
    }

    public static void delete(DataInputStream input, DataOutputStream output) throws IOException {
        output.writeUTF("delete");
        System.out.println("Do you want to delete the file by name or by id (1 - name, 2 - id):");
        String getBy = scanner.nextLine();
        if (Objects.equals(getBy, "1")){
            output.writeUTF("name");
            System.out.println("Enter name:");
            String name = scanner.nextLine();
            output.writeUTF(name);
        } else if (Objects.equals(getBy, "2")) {
            output.writeUTF("id");
            System.out.println("Enter id:");
            String id = scanner.nextLine();
            output.writeUTF(id);
        }
        //output.writeUTF(name);
        String result = input.readUTF();
        if (result.equals("200")){
            System.out.println("The response says that this file was deleted succesfully!");
        } else{
            System.out.println("The response says that the file was not found!");
        }
    }

    public static void operator(DataInputStream input, DataOutputStream output) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter action (1 - get a file, 2 - save a file, 3 - delete a file):");
        String action = scanner.nextLine();
        switch (action) {
            case "1" -> get(input,output);
            case "2" -> save(input, output);
            case "3" -> delete(input,output);
            case "exit" -> {
                output.writeUTF("exit");
                System.out.println("The request was sent");
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread.sleep(1000);
        String address = "127.0.0.1";
        int port = 23456;
        try(
            Socket socket = new Socket(InetAddress.getByName(address), port);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        ) {
            operator(input, output);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}

