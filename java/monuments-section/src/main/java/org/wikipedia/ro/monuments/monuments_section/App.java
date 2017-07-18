package org.wikipedia.ro.monuments.monuments_section;

import com.mongodb.MongoClient;

/**
 * Aceasta e clasa principală. Ea primește din linia de comandă două argumente: indicativul județului și numele unității
 * administrative, prefixat de o cifră ce reprezintă tipul unității (m - municipiu, c - comună, o - oraș). De exemplu: pentru
 * Brașov, avem primul argument BV și al doilea mBrașov; pentru Mizil avem primul argument PH și al doilea oMizil; pentru
 * Comana (Giurgiu), avem primul argument GR și al doilea cComana.
 * 
 * Va printa pe ieșirea standard o schiță de secțiune despre monumentele istorice, schiță ce aproape sigur va trebui editată
 * înainte de a fi inclusă în articol.
 */

public class App {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Please specify county and commune name");
            System.exit(64);
        }
        
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator(args[0], args[1]);
        generator.setMongoClient(new MongoClient());

        System.out.println(generator.generate());
    }
}
