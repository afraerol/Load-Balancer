import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class LoadBalancer {
    private static List<Server> servers;
    private static int currentIndex;
    static List<Integer> numofConnections;

    public LoadBalancer() {
        servers = new ArrayList<>();
        currentIndex = 0;
    }
    
     public static void main(String[] args) throws IOException, InterruptedException{
         
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        Socket loadBalancerSocket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
       
        loadBalancerSocket = new Socket("localhost", 8000);
        
        registerServer("Server 1", "localhost", 8000);
        registerServer("Server 2", "localhost", 8000);
        registerServer("Server 3", "localhost", 8000);
       
        while(true){
            
            try{ 
              
                serverSocket = new ServerSocket(8000);
                
                clientSocket = serverSocket.accept();
            
                inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                outputStreamWriter = new OutputStreamWriter(loadBalancerSocket.getOutputStream());
                
                bufferedReader = new BufferedReader(inputStreamReader);
                bufferedWriter = new BufferedWriter(outputStreamWriter);
                
                while (true) {
                try {
                    String request = bufferedReader.readLine();
                    
                    System.out.println("Request: "+request);
                    bufferedWriter.write("Message received.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    
                    routeRequest(request, numofConnections);
                    
                    
                } catch(IOException i) {
                    System.out.println(i);
                }
            }
                
            }catch(IOException i) {
                    System.out.println(i);
                }
            
           
        finally{
            if(loadBalancerSocket!=null)//
                loadBalancerSocket.close();
            if(inputStreamReader!=null)
                inputStreamReader.close();
            if(outputStreamWriter!=null)
                outputStreamWriter.close();
            if(bufferedReader!=null)
                bufferedReader.close();
            if(bufferedWriter!=null){
                bufferedWriter.close();
            }
        }
            
            
        }  
     }
     
    private static String routeRequest(String request, List<Integer> numofConnections) throws InterruptedException {
        String response;

        if (servers.isEmpty()) {
            response = "No active servers available";
        } 
        else {
            if (servers.size() == 1) {
                // Only one server available, no need for load balancing
                Server server = servers.get(0);
                response = sendRequestToServer(server, request);
            } else {
                // Load balancing logic based on server type
                Server server = null;
                if (request.equals("directory") || request.startsWith("file")) {
                    server = getServerForStaticLoadBalancing(numofConnections, request);
                } else if (request.equals("computation") || request.equals("video")) {
                    server = getServerForDynamicLoadBalancing();
                }

                if (server != null) {
                    response = sendRequestToServer(server, request);
                } else {
                    response = "No active servers available for handling the request";
                }
            }
        }

        return response;
    }
    
    private static String sendRequestToServer(Server server, String request) throws InterruptedException {
        
          
        try {
            Socket serverSocket = new Socket("localhost", 8000);
            PrintWriter writer = new PrintWriter(serverSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            writer.println(request);
            String response = reader.readLine();
            
            processRequest(request, server);
            
            reader.close();
            writer.close();
            serverSocket.close();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error occurred while communicating with the server";
        }
    }

    
  
    
    public static Server getServerForStaticLoadBalancing(List<Integer> numofConnections, String request) throws InterruptedException {
        if (numofConnections == null || numofConnections.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty.");
        }

        int leastNumber = numofConnections.get(0);

        for (int i = 1; i < numofConnections.size(); i++) {
            int currentNumber = numofConnections.get(i);
            if (currentNumber < leastNumber) {
                leastNumber = currentNumber;
            }
        }

        servers.get(leastNumber).available=false;
        return servers.get(leastNumber);
        
    }
 
          
    private static Server getServerForDynamicLoadBalancing() {
        synchronized (servers) {
            Server leastConnServer = null;
            int leastConnections = Integer.MAX_VALUE;

            for (Server server : servers) {
                if (server.getStaticOrDynamic()==true && server.isAvailable() && server.getConnections() < leastConnections) {
                    leastConnServer = server;
                    leastConnections = server.getConnections();
                }
            }

            if (leastConnServer != null) {
                leastConnServer.incrementConnections();
            }

            return leastConnServer;
        }
    }
    
          
    public static void registerServer(String serverName, String serverHost, int serverPort) {
        Server server = new Server(serverName);
        servers.add(server);
        System.out.println("Registered server: " + serverName + " (" + serverHost + ":" + serverPort + ")");
    }

    public void removeServer(Server server) {
        servers.remove(server);
    }

   
     private Server getAvailableServer() {
        for (Server server : servers) {
            if (server.isAvailable()) {
                return server;
            }
        }
        return null;
    }

    public void handleJoinMessage(Server server) {
        System.out.println("Server " + server.getName() + " sent a join message.");
        if (!servers.contains(server)) {
            servers.add(server);
        }
    }

    public void handleGoodbyeMessage(Server server) {
        System.out.println("Server " + server.getName() + " sent a goodbye message.");
        servers.remove(server);
    }
    public Server getNextServer() {
        if (servers.isEmpty()) {
            return null;
        }

        Server server = servers.get(currentIndex);
        currentIndex = (currentIndex + 1) % servers.size();
        return server;
    }
    
      public static void processRequest(String request, Server s) throws InterruptedException {
            System.out.println("Request received by " + s.getName() + ": " + request);
            Scanner scn = new Scanner(System.in);

            if (request.equals("DirectoryListingRequest")) {
                handleDirectoryListingRequest();
            } else if (request.equals("FileTransferRequest")) {
                System.out.println("Please enter the name of the file: ");
                String name = scn.nextLine();
                handleFileTransferRequest(name);
            } else if (request.equals("ComputationRequest")) {
                handleComputationRequest(s,150); 
            } else if (request.equals("VideoStreamingRequest")) {
                OutputStream outputStream = new ByteArrayOutputStream();;
                handleVideoStreamingRequest(400,outputStream);
            }

            System.out.println("Request processed by " + s.getName());
            s.available = true;
        }
     
     private static void handleDirectoryListingRequest() {
            String directoryPath = "/path/to/server/directory";
            File directory = new File(directoryPath);
            
            if(directory.exists() && directory.isDirectory()){
                File[] files = directory.listFiles();
                
                if(files != null && files.length>0){
                    
                   String[] fileNames = Arrays.stream(files).map(File::getName).toArray(String[]::new);
                   for(int i=0;i<fileNames.length;i++){
                     System.out.println(String.join(", ", fileNames)); 
                   } 
                }
            }
            
            System.out.println("Handling directory listing request...");
        }
     
     private static void handleFileTransferRequest(String fileName) {
            System.out.println("Handling file transfer request...");
            fileTransfer(fileName);
            
        }
     private static void handleComputationRequest(Server server, int duration) throws InterruptedException {
            server.available = false;
            
            try{
                for (int i = 1; i <= duration; i++) {
                   Thread.sleep(1000);
                }                
     }
            catch(InterruptedException e){
                   e.printStackTrace();
        
            }
           
            System.out.println("Handling computation request...");
        }
     
     private static void handleVideoStreamingRequest(int duration, OutputStream output) throws InterruptedException {
            try{
                System.out.println("Handling video streaming request...");
                for(int i=0; i<duration; i++){
                    output.write("Video frame".getBytes());
                    Thread.sleep(1000);
                    
                }
            }catch(IOException e){
                e.printStackTrace();
            }
           
        }
     
     private static byte[] fileTransfer(String fileName){
            try{
                System.out.println("Content of file '" + fileName + "'");
                byte[] content = Files.readAllBytes(Paths.get(fileName));
                return content;
            }
            catch(Exception e){
                e.printStackTrace();
                return new byte[0];
            }
        }
    
    
}