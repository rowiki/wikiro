package org.wikipedia.ro.monuments.monuments_section;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        if (args.length < 2) {
            System.err.println("Please specify county and commune name");
            System.exit(64);
        }
        MonumentInCommuneGenerator generator = new MonumentInCommuneGenerator(args[0], args[1]);
        
        System.out.println(generator.generate());
    }
}
