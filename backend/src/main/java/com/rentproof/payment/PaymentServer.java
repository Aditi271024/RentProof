package com.rentproof.payment;

import com.rentproof.RentProofServer;

import java.io.IOException;

/**
 * PaymentServer - Dedicated Payment & Rent Management Server Bridge
 * Module: Payment & Rent Management
 * Author: Aanvi
 * 
 * Provides HTTP server capability on port 8080 by delegating to RentProofServer.
 * Ensures compatibility whether invoked as com.rentproof.RentProofServer
 * or com.rentproof.payment.PaymentServer.
 */
public class PaymentServer {

    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        RentProofServer.main(args);
    }
}
