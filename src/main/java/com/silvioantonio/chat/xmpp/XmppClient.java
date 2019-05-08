/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.silvioantonio.chat.xmpp;

import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 *
 * @author SILVIO
 */
public class XmppClient {
    
    private XMPPTCPConnectionConfiguration.Builder builder;
    private XMPPTCPConnection connection;
    private ChatManager chatManager;
    private String userName = "testesilvio001";
    private String password = "01477410";
    private final Scanner scanner;
    private String msg, toJabberId;
    private Roster roster;
    
    public XmppClient() throws XmppStringprepException {
        builder = XMPPTCPConnectionConfiguration.builder();
        builder
                .setXmppDomain("xabber.org")
                .setUsernameAndPassword(userName, password)
                .setHost("xabber.org")
                .setResource("desktop-app");                    

        connection =  new XMPPTCPConnection(builder.build());
        connection.setReplyTimeout(5000);
        scanner = new Scanner(System.in);
    }
    
    public boolean connect(){
        try {
            connection.connect();
        } catch (SmackException | IOException | XMPPException | InterruptedException ex) {
            throw new RuntimeException("Nao foi possivel conectar ao servidor"+ex);
        }
        
        chatManager = ChatManager.getInstanceFor(connection);
        try {
            connection.login();
        } catch (XMPPException | SmackException | IOException | InterruptedException ex) {
            throw new RuntimeException("Nao foi possivel logar no servidor",ex);
        }
        
        return true;
    }
    
    private void start(){
        if(!connect()){
            return;
        }

        listContacts();

        do{
            System.out.print("Digite uma mensagem no formato destinatario@dominio msg (ou apenas msg pra enviar pro destinatário anterior): ");
            msg = scanner.nextLine();

            if(validateMsg()) {
                try {
                    sendMessage(msg, toJabberId);
                    System.out.println("Mensagem enviada para "+toJabberId);
                } catch (RuntimeException e) {
                    System.err.println("Erro ao enviar mensagem: " + e.getMessage());
                } catch (XmppStringprepException ex) {
                    Logger.getLogger(XmppClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }while(!msg.equalsIgnoreCase("sair"));
    }
    
    public static void main(String[] args) throws XmppStringprepException {
        try{
            XmppClient client = new XmppClient();
            client.start();
     
        }catch(RuntimeException e){
            System.err.println("Erro ao iniciar a aplicaçao: "+e.getMessage());
        }
    }

    private boolean validateMsg() {
        if("sair".equalsIgnoreCase(msg))
            return false;

        if(msg.matches(".*@.* .*")) {
            int i = msg.indexOf(' ');
            toJabberId = msg.substring(0, i);
            msg = msg.substring(i+1);
            return true;
        }

        if(toJabberId.isEmpty()){
            System.err.println("Mensagem em formato inválido!");
            return false;
        }

        return true;
    }

    private void listContacts() {
        try {
            roster = Roster.getInstanceFor(connection);
            if (!roster.isLoaded())
                roster.reloadAndWait();

            Set<RosterEntry> contacts = roster.getEntries();
            if(!contacts.isEmpty()) {
                System.out.println("\nLista de contatos");
                
                contacts.forEach(contact -> System.out.println("\t"+contact));
                
            } else System.out.println("Lista de contatos vazia");
            System.out.println();
        } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
            System.err.println("Não foi possível obter a lista de contatos: " + e.getMessage());
        }
    }

    private void sendMessage(String msg, String toJabberId) throws XmppStringprepException {
        if (connection.isConnected()) {
            //Cria um objeto que representa o usuário destinatário da mensagem.
            EntityBareJid jid = JidCreate.entityBareFrom(toJabberId);
            try {
                //Envia a mensagem para tal usuário
                chatManager.chatWith(jid).send(msg);
            } catch (SmackException.NotConnectedException | InterruptedException ex) {
                Logger.getLogger(XmppClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }
    
}
