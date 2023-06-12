
import java.io.*;
import java.net.*;
import java.util.Random;


public class Server {
    
        private String name;
        boolean available;
        Random rd = new Random();
        private boolean staticOrDynamic;
        private static int connections = 0;

        public Server(String name) {
            this.name = name;
            this.available = true;
            staticOrDynamic = rd.nextBoolean();
        }

        public String getName() {
        return name;
        }
        
        public boolean getStaticOrDynamic(){
            return staticOrDynamic;
        }
        
        public int getConnections(){
            return connections;
        }
        
        public void incrementConnections() {
        connections++;
    }

    public void decrementConnections() {
        connections--;
    }
        
     public static void main(String[] args) throws IOException{
         
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        ServerSocket server = null;
        
        server = new ServerSocket(8000);
        
        while(true){
            
            try{
                socket = server.accept();
                
                inputStreamReader = new InputStreamReader(socket.getInputStream());
                outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                
                bufferedReader = new BufferedReader(inputStreamReader);
                bufferedWriter = new BufferedWriter(outputStreamWriter);
                
                while(true){
                    
                    String msgFromClient = bufferedReader.readLine();
                    
                    System.out.println("Client: "+msgFromClient);
                                     
                    bufferedWriter.write("Message received.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    
                                        
                   if(msgFromClient.equalsIgnoreCase("Goodbye")){
                       break;
                   }
                     
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
                
        
     }
     
     public boolean isAvailable() {
            return available;
        }
     
     
    
     
    
}