import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * The seller client
 */
public class ClientBuyer extends Client
{
    public ClientBuyer()
    {
        super();
    }

    @Override
    public void displayClientUsage()
    {
        System.out.println("ClientBuyer [Function] [Args, if any]");
        System.out.println("==================================");
        System.out.println("ClientBuyer 0 (This help screen)");
        System.out.println("ClientBuyer 1 (Display items auctioned on the server)");
        System.out.println("ClientBuyer 2 (Place bid)");
        System.out.println("ClientBuyer -1 (Exit)");
    }

    public void placeBid(ICalc server)
    {
        Integer auctionID = -1;
        Integer itemBid = -1;
        Integer clientID = -1;
        String userName = "";
        String userEmail = "";
        try
        {
            auctionID = Integer.parseInt(getInput("What auctionID do you wish to bid on?"));
            itemBid = Integer.parseInt(getInput("How much do you wish to bid??"));
            clientID = Integer.parseInt(getInput("Do you have a clientID? -1 if not"));
        } catch (Exception e)
        {
            System.out.println("Incorrect input given. Must be integers. Aborting...");
            return;
        }

        if (clientID <= 0)
        {
            userName = getInput("What's your username?");
            userEmail = getInput("What's your email?");
        }

        try
        {
            String n = server.addBid(auctionID, itemBid, createSealedClientRequest(clientID), userName, userEmail);
            System.out.println(n);
        } catch (Exception e)
        {
            System.err.println("Exception has occurred placing a bid on the server. Exiting...");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        ClientBuyer cb = new ClientBuyer();

        /*if (args.length < 1)
        {
            System.out.println("Must supply an integer as a command argument!");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);*/

        try
        {
            ICalc server = cb.createServerConnection();

            while (true) // persistent program
            {
                int n = Integer.parseInt(cb.getInput(""));

                if (n == -1)
                {
                    System.exit(0);
                } else if (n == 0)
                {
                    cb.displayClientUsage();
                } else if (n == 1)
                {
                    cb.viewAuctionedItems(server);
                } else if (n == 2)
                {
                    cb.placeBid(server);
                } else
                {
                    System.out.println("Invalid parameter given. Use 'ClientBuyer 0' for help");
                }
            }
        } catch (Exception e)
        {
            System.err.println("🆘 exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}