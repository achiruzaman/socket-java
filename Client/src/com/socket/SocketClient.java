package com.socket;


import com.ui.ChatFrame;
import java.io.*;
import java.net.*;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class SocketClient implements Runnable{
    
    public int port;
    public String serverAddr;
    public Socket socket;
    public ChatFrame ui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    public History hist;
    
    public SocketClient(ChatFrame frame) throws IOException{
        ui = frame; 
        this.serverAddr = ui.serverAddr;
        this.port = ui.port;
       System.out.println("Ini Aksi ketika tombol connect di klik");
         socket = new Socket(InetAddress.getByName(serverAddr), port);
            
        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());
        
        hist = ui.hist;
    }

    @Override
    public void run() {
        boolean keepRunning = true;
        while(keepRunning){
            try {
                Message msg = (Message) In.readObject();
                System.out.println("Incoming : "+msg.toString());
                
                if(msg.type.equals("message")){
                    if(msg.recipient.equals(ui.username)){
                        ui.txt_pesan_client.append("["+msg.sender +" > Saya] : " + msg.content + "\n");
                    }
                    else{
                        ui.txt_pesan_client.append("["+ msg.sender +" > "+ msg.recipient +"] : " + msg.content + "\n");
                    }
                                            
                    if(!msg.content.equals(".bye") && !msg.sender.equals(ui.username)){
                        String msgTime = (new Date()).toString();
                        
                        try{
                            hist.addMessage(msg, msgTime);
                            DefaultTableModel table = (DefaultTableModel) ui.historyFrame.jTable1.getModel();
                            table.addRow(new Object[]{msg.sender, msg.content, "Me", msgTime});
                        }
                        catch(Exception ex){}  
                    }
                }
                else if(msg.type.equals("login")){
                    if(msg.content.equals("TRUE")){
                        ui.btn_login.setEnabled(false); ui.btn_signup.setEnabled(false);                        
                        ui.btn_sendmessage.setEnabled(true); ui.btn_browsefile.setEnabled(true);
                        ui.btn_sendmessage.setEnabled(true); ui.btn_browsefiletxt.setEnabled(true);
                        ui.txt_pesan_client.append("[SERVER > Saya] : Login Berhasil\n");
                        ui.txt_username.setEnabled(false); ui.jPasswordField1.setEnabled(false);
                    }
                    else{
                        ui.txt_pesan_client.append("[SERVER > Saya] : Login Gagal\n");
                    }
                }
                else if(msg.type.equals("test")){
                    ui.btn_connect.setEnabled(false);
                    ui.btn_login.setEnabled(true); ui.btn_signup.setEnabled(true);
                    
                    ui.txt_username.setEnabled(true); ui.jPasswordField1.setEnabled(true);
                    ui.txt_ip.setEditable(false); ui.txt_port.setEditable(false);
                    ui.btn_browsehistory.setEnabled(true);
                }
                else if(msg.type.equals("newuser")){
                    if(!msg.content.equals(ui.username)){
                        boolean exists = false;
                        for(int i = 0; i < ui.model.getSize(); i++){
                            if(ui.model.getElementAt(i).equals(msg.content)){
                                exists = true; break;
                            }
                        }
                        if(!exists){ ui.model.addElement(msg.content); }
                    }
                }
                else if(msg.type.equals("signup")){
                    if(msg.content.equals("TRUE")){
                        ui.btn_login.setEnabled(false); ui.btn_signup.setEnabled(false);
                        ui.btn_sendmessage.setEnabled(true); ui.btn_browsefile.setEnabled(true);
                        ui.txt_pesan_client.append("[SERVER > Saya] : SignUp Berhasil\n");
                    }
                    else{
                        ui.txt_pesan_client.append("[SERVER > Saya] : SignUp Gagal\n");
                    }
                }
                else if(msg.type.equals("signout")){
                    if(msg.content.equals(ui.username)){
                        ui.txt_pesan_client.append("["+ msg.sender +" > Saya] : Bye\n");
                        ui.btn_connect.setEnabled(true); ui.btn_sendmessage.setEnabled(false); 
                        ui.txt_ip.setEditable(true); ui.txt_port.setEditable(true);
                        
                        for(int i = 1; i < ui.model.size(); i++){
                            ui.model.removeElementAt(i);
                        }
                        
                        ui.clientThread.stop();
                    }
                    else{
                        ui.model.removeElement(msg.content);
                        ui.txt_pesan_client.append("["+ msg.sender +" > All] : "+ msg.content +" has signed out\n");
                    }
                }
                else if(msg.type.equals("upload_req")){
                    
                    if(JOptionPane.showConfirmDialog(ui, ("Terima '"+msg.content+"' dari "+msg.sender+" ?")) == 0){
                        
                        JFileChooser jf = new JFileChooser();
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);
                       
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION){
                            Download dwn = new Download(saveTo, ui);
                            Thread t = new Thread(dwn);
                            t.start();
                            //send(new Message("upload_res", (""+InetAddress.getLocalHost().getHostAddress()), (""+dwn.port), msg.sender));
                            send(new Message("upload_res", ui.username, (""+dwn.port), msg.sender));
                        }
                        else{
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }
                    }
                    else{
                        send(new Message("upload_res", ui.username, "NO", msg.sender));
                    }
                }
                else if(msg.type.equals("upload_res")){
                    if(!msg.content.equals("NO")){
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;
                        
                        ui.btn_browsefile.setEnabled(false); ui.btn_sendfile.setEnabled(false);
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                    }
                    else{
                        ui.txt_pesan_client.append("[SERVER > Saya] : "+msg.sender+" rejected file request\n");
                    }
                }
                else{
                    ui.txt_pesan_client.append("[SERVER > Saya] : Tipe pesan diketahui\n");
                }
            }
            catch(Exception ex) {
                keepRunning = false;
                ui.txt_pesan_client.append("[Application > Saya] : Connection Failure\n");
                ui.btn_connect.setEnabled(true); ui.txt_ip.setEditable(true); ui.txt_port.setEditable(true);
                ui.btn_sendmessage.setEnabled(false); ui.btn_browsefile.setEnabled(false); ui.btn_browsefile.setEnabled(false);
                
                for(int i = 1; i < ui.model.size(); i++){
                    ui.model.removeElementAt(i);
                }
                
                ui.clientThread.stop();
                
                System.out.println("Exception SocketClient run()");
                ex.printStackTrace();
            }
        }
    }
    
    public void send(Message msg){
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
            
            if(msg.type.equals("message") && !msg.content.equals(".bye")){
                String msgTime = (new Date()).toString();
                try{
                    hist.addMessage(msg, msgTime);               
                    DefaultTableModel table = (DefaultTableModel) ui.historyFrame.jTable1.getModel();
                    table.addRow(new Object[]{"Me", msg.content, msg.recipient, msgTime});
                }
                catch(Exception ex){}
            }
        } 
        catch (IOException ex) {
            System.out.println("Exception SocketClient send()");
        }
    }
    
    public void closeThread(Thread t){
        t = null;
    }
}
