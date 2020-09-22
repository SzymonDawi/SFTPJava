package Server;// File Name GreetingServer.java
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

public class Server extends Thread {
    private ServerSocket serverSocket;

    private int currentAccLine = -1;

    private String responseCode;
    private String response;
    private String sendMode = "A";
    private String workingDir = System.getProperty("user.dir");
    private String tempDIR;
    private String oldFileSpec;
    private String currentFileSpec;

    private boolean loggedIn = false;
    private boolean cdirFlag = false;
    private boolean cdirFlagAcct = false;
    private boolean cdirFlagPass = false;
    private boolean running = true;
    private boolean tobeFlag = false;
    private boolean retrFlag = false;

    private DataOutputStream out;
    private DataInputStream in;


    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(10000);
    }

    public void run() {
        while(true) {
            try {
                Socket server = serverSocket.accept();

                in = new DataInputStream(server.getInputStream());
                System.out.println(in.readUTF());
                out = new DataOutputStream(server.getOutputStream());
                out.writeUTF("Welcome to Sbud159s SFTP Server. You are connected to " + server.getLocalSocketAddress());

                while(running){
                    //recieves the command from the client
                    String fromClient = in.readUTF();
                    String[] clientCommand = fromClient.split("\\s+");

                    //checks what command the client sent
                    CheckCommand(clientCommand);

                    //responds to the client
                    out.writeUTF(responseCode + "" + response);
                }

                server.close();

            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String [] args) {
        int port = Integer.parseInt("115");
        try {
            Thread t = new Server(port);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CheckCommand(String[] command) throws IOException {
        responseCode = "";
        response = "";
        //Switch Case statement checking what command is sent
        switch(command[0]){
            case "USER":
                //System.out.print("user");
                USER(command[1]);
                break;
            case "ACCT":
                //System.out.print("acct");
                ACCT(command[1]);
                break;
            case "PASS":
                //System.out.print("pass");
                PASS(command[1]);
                break;
            case "TYPE":
                //System.out.print("type");
                TYPE(command[1]);
                break;
            case "LIST":
                //System.out.print("list");
                if (command.length > 1) {
                    if (command.length == 2) {
                        LIST(command[1], null);
                    } else {
                        LIST(command[1], command[2]);
                    }
                }
                break;
            case "CDIR":
                //System.out.print("cdir");
                CDIR(command[1]);
                break;
            case "KILL":
                //System.out.print("kill");
                KILL(command[1]);
                break;
            case "NAME":
                //System.out.print("name");
                NAME(command[1]);
                break;
            case "TOBE":
                //System.out.print("tobe");
                TOBE(command[1]);
                break;
            case "DONE":
                //System.out.print("done");
                DONE();
                break;

        }
    }

    public void USER(String userID){
        //finds the file and pulls all the lines from it
        Path file_path = Paths.get("src/Server/users.txt");
        List <String> lines = getData(file_path);
        int i = 0;

        //compares the data from the file to the input ID
        for (String line : lines){
            if(userID.equals(line)){
                if(currentAccLine == i){
                    if(loggedIn){
                        responseCode = "!";
                        response = "Logged in";
                        return;
                    }
                }

                responseCode = "+";
                response = "User-id valid, send account and password";
                return;
            }
            i++;
        }

        responseCode = "-";
        response = "Invalid user-id, try again";
    }

    public void ACCT(String acctName){
        int i = 0;
        Path file_path = Paths.get("src/Server/accounts.txt");

        //if already loggind in
        if(loggedIn){
            responseCode = "!";
            response = acctName + " Logged in";
            return;
        }

        List <String> lines = getData(file_path);

        //compares the data from the file with in given account name
        for (String line : lines){
            if(acctName.equals(line)){
                this.currentAccLine = i;
                responseCode = "+";
                response = "Account valid, send password";

                if(cdirFlag){
                    //checks if the CDIR command was used before this.
                    if(!cdirFlagAcct){
                        responseCode = "+";
                        response = "Account ok, send password";
                        cdirFlagAcct = true;
                    }

                    if(cdirFlagAcct && cdirFlagPass){
                        //if both account flag and password flags have been set true then the new DIR is set
                        responseCode = "!";
                        response = "Changed working dir to "+ tempDIR;
                        workingDir = tempDIR;

                    }
                }
                return;
            }
            i++;
        }

        responseCode = "-";
        response = "Invalid account, try again";
    }

    public void PASS(String password){
        Path file_path = Paths.get("src/Server/passwords.txt");

        List <String> lines = getData(file_path);

        //if the password is sent first it asks for them to send the account first
        if (currentAccLine == -1){
            responseCode = "+";
            response = "Send account first";
            return;
        }

        //checks if the password line and account lines match
        if(password.equals(lines.get(this.currentAccLine))){
            responseCode = "!";
            response = " Logged in";
            loggedIn = true;

            if(cdirFlag){
                //checks if the CDIR command was used first
                if(!cdirFlagPass){
                    responseCode = "+";
                    response = "Password ok, send account";
                    cdirFlagPass = true;
                }

                if(cdirFlagAcct && cdirFlagPass){
                    //if both account flag and password flags have been set true then the new DIR is set
                    responseCode = "!";
                    response = "Changed working dir to "+ tempDIR;
                    workingDir = tempDIR;
                }
            }
            return;
        }

        responseCode = "-";
        response = "Invalid Password, try again";
    }

    public void TYPE(String type){
        //checks the command input is either A, B, or C
        if (type.equals("A")){
            responseCode = "+";
            response = "Using Ascii mode";
            sendMode = "A";
            return;
        }else if(type.equals("B")){
            responseCode = "+";
            response = "Using Binary mode";
            sendMode = "B";
            return;
        }else if(type.equals("C")){
            responseCode = "+";
            response = "Using Continuous mode";
            sendMode = "C";
            return;
        }

        responseCode = "-";
        response = "Type not valid";
    }

    public void LIST(String listType, String path){
        String currentDir;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        //checks if a path is given
        if(path != null){
            currentDir = path;
        }else{
            currentDir = workingDir;
        }

        File file = new File(currentDir);

        File[] filesList = file.listFiles();

        //checks if the client want short or long response
        if(listType.equals("F")){
            responseCode = "+";
            response = currentDir + "\r\n";

            for(File f: filesList){
                response += f.getName() + "\r\n";
            }
            return;
        }else if(listType.equals("V")){
            responseCode = "+";
            response = currentDir + "\r\n";

            for(File f: filesList){
                String WR = "N/A";

                //check the permissions of the file
                if(f.canWrite()){
                    WR = "w";
                    if (f.canRead()){
                        WR += "r";
                    }
                }else{
                    if (f.canRead()){
                        WR = "r";
                    }
                }

                response += "File name: "+f.getName()
                            +" | Length: "+f.length()
                            +" Bytes | Permission: "+WR
                            +" | Last Modified: "+ sdf.format(file.lastModified())
                            +"\r\n";
            }
            return;
        }

        responseCode = "-";
        response = "Type not valid";
    }

    public void CDIR(String newDir){
        String currentDir;

        File file = new File(newDir);

        //checks if the directory exists
        if(!(file.exists() && file.isDirectory())){
            responseCode = "-";
            response = "Can't connect to directory because: Directory does not exists.";
            return;
        }

        //checks if user is loggedIn
        if (!loggedIn){
            responseCode = "+";
            response = "directory ok, send account/password";
            cdirFlag = true;
            tempDIR = newDir;
            return;
        }

        responseCode = "!";
        response = "Changed working dir to "+ newDir;
        workingDir = newDir;
    }

    public void KILL(String fileSpec){
       File f= new File(workingDir + "/" +fileSpec);

       //tries to delete file
       if(f.delete()){
           responseCode = "+";
           response = fileSpec+" has been deleted";
       }else{
           responseCode = "-";
           response = "Not deleted";
       }

    }

    public void NAME(String fileSpec){
        File f= new File(workingDir+"/"+fileSpec);

        //checks if file exist and save the file name
        if(f.exists()){
            this.oldFileSpec = fileSpec;
            tobeFlag = true;
            responseCode = "+";
            response = "File exists";
        }else{
            responseCode = "-";
            response = "Can't find " + fileSpec;
        }
    }

    public void TOBE(String newFileSpec){
        //checks if NAME was called first
        if(tobeFlag){
            File oldFile = new File(workingDir+"/"+oldFileSpec);
            File newFile = new File(workingDir+"/"+newFileSpec);

            tobeFlag = false;
            //tries to rename the file
            if(oldFile.renameTo(newFile)){
                responseCode = "+";
                response = oldFileSpec+" renamed to "+newFileSpec;
            }else{
                responseCode = "-";
                response = "File was not renamed";
            }
        }
    }

    public void RETR(String fileSpec){
        File f = new File(workingDir+"/"+fileSpec);

        if(f.exists()){
            responseCode = "";
            response = "ok, file length "+f.length();
            currentFileSpec = fileSpec;
            retrFlag = true;
        }else{
            responseCode = "-";
            response = "File doesn't exist";
        }
    }

    public void SEND() throws IOException {
        File f = new File(workingDir+"/"+currentFileSpec);
        byte[] bytes = new byte[(int) f.length()];

        if(sendMode.equals("A")){
            try{
                BufferedInputStream buffStream = new BufferedInputStream(new FileInputStream(f));
                out.flush();

                int b = 0;
                while((b = buffStream.read(bytes)) >= 0){
                    out.write(bytes, 0, b);
                }

                buffStream.close();
                out.flush();
            }catch(IOException e){
                serverSocket.close();
                running = false;
            }
        }else if(sendMode.equals("B")){

        }else if(sendMode.equals("C")){

        }
    }

    public void STOP(String fileSpec){
        responseCode = "+";
        response = "ok, RETR aborted";
        currentFileSpec = "";
        retrFlag = false;
    }

    public void DONE() throws IOException {
        //closes the socket
        responseCode = "+";
        response = "Goodbye";
        running = false;
        serverSocket.close();
    }

    private List<String> getData(Path filePath){
        //reads all lines of a file and returns a list of strings.
        int i = 0;
        List<String> lines = null;

        try{
            lines = Files.readAllLines(filePath);
        }
        catch(FileNotFoundException e){
            System.out.print("FileNotFoundException");
        }catch(IOException e){
            System.out.print(e);
        }
        return lines;
    }
}