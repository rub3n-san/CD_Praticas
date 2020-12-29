package com.isel.cd.server;

import io.grpc.ServerBuilder;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.io.IOException;
import java.net.InetAddress;

public class SpreadServer implements Runnable{


    private final int port;
    private final int grpcPort;
    private final String groupId;
    private final String hostname;
    private final String name;
    private final boolean local;
    private final DatabaseRepository database;
    private SpreadConnection connection;

    public SpreadServer(int port, int grpcPort, String groupId, String hostname, String name, boolean local, final DatabaseRepository database) {
        this.port = port;
        this.grpcPort = grpcPort;
        this.groupId = groupId;
        this.hostname = hostname;
        this.name = name;
        this.local = local;
        this.database = database;
    }

    @Override
    public void run() {
        System.out.println();
        System.out.println();
        try
        {

            //Criar Conexaão e Juntar
            connection = new SpreadConnection();
            connection.connect(this.local ? InetAddress.getLocalHost() : InetAddress.getByName(hostname),
                    port, name, false, true);

            LeaderManager leaderManager = new LeaderManager(connection, groupId, database);

            //Listener das mensagens multicast
            SpreadMessageListener msgHandling = new SpreadMessageListener(leaderManager);
            connection.add(msgHandling);

            //Criar Grupo e Juntar
            SpreadGroup group = new SpreadGroup();
            group.join(connection, this.groupId);


            //GRPC Clientes
            io.grpc.Server svc = ServerBuilder
                    .forPort(this.grpcPort)
                    .addService(new ClientService(leaderManager, this.database) )
                    .build();

            svc.start();

        } catch (SpreadException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop()  {
        try{
            System.out.println("Stop server:  " + this.name);
            connection.disconnect();
        }catch (SpreadException spreadException){
            System.out.println(spreadException);
        }
    }

}