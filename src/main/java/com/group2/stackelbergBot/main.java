package com.group2.stackelbergBot;

import lombok.extern.java.Log;



@Log
public class main {

    public static void main(final String[] args) {

        try {
            SimpleLeader leader = new SimpleLeader();

            leader.checkConnection();
        } catch (Exception e ) {
            log.info("Connection not working:" + e.getMessage());
        }
    }

}
