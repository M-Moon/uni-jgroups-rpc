import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Scanner;

/**
 * The buyer client
 */
public class ClientSeller extends Client
{
    public ClientSeller()
    {
        super();
    }

    @Override
    public void displayClientUsage()
    {
        System.out.println("ClientSeller [Function] [Args, if any]");
        System.out.println("==================================");
        System.out.println("ClientSeller 0 (This help screen)");
        System.out.println("ClientSeller 1 (Display items auctioned on the server)");
        System.out.println("ClientSeller 2 (Put item up for auction)");
        System.out.println("ClientSeller 3 (Close auction)");
        System.out.println("ClientSeller -1 (Exit)");
    }

    private AuctionItem.Condition getConditionFromString(String s)
    {
        if (s.equalsIgnoreCase("new"))
            return AuctionItem.Condition.NEW;
        else if (s.equalsIgnoreCase("used"))
            return AuctionItem.Condition.USED;
        else if (s.equalsIgnoreCase("damaged"))
            return AuctionItem.Condition.DAMAGED;
        else if (s.equalsIgnoreCase("broken"))
            return AuctionItem.Condition.BROKEN;

        return null;
    }

    public void addAuctionItemToServer(ICalc server)
    {
        String itemTitle = getInput("What is the item's name?");
        String itemDesc = getInput("What is the item's description?");
        String itemCondition = getInput("What is the item's condition?");

        AuctionItem.Condition properCondition = getConditionFromString(itemCondition);
        if (properCondition == null)
        {
            System.err.println("Incorrect item condition given. Must be new, used, damaged, or broken. Aborting...");
            return;
        }

        int startingPrice;
        int reservePrice;
        int clientID = this.clientID;
        try
        {
            startingPrice = Integer.parseInt(getInput("What's the starting price?"));
            reservePrice = Integer.parseInt(getInput("What's the reserve price?"));
            if (clientID <= -1) // check if client instance has a saved clientID
                clientID = Integer.parseInt(getInput("Do you have a clientID? -1 if not"));
            else
                System.out.println("Registered clientID used. ClientID: " + clientID);
        } catch (NumberFormatException e)
        {
            System.out.println("Incorrect format given. Must be integer!");
            return;
        }

        if (itemTitle.isBlank() | itemDesc.isBlank() | itemCondition.isBlank() | startingPrice <= 0 | reservePrice <= 0)
        {
            System.err.println("One or more parameters not provided. Aborting...");
            return;
        }

        if (reservePrice < startingPrice)
        {
            System.err.println("Reserve price given as less than starting price. Aborting...");
            return;
        }

        try
        {
            SealedObject clientRequest = createSealedClientRequest(clientID);
            Integer[] returnedIDs = server.addAuctionListing(clientRequest, itemTitle, itemDesc, properCondition, startingPrice, reservePrice);

            if (returnedIDs[1] == -1)
            {
                System.out.println("\nThe given clientID doesn't exist, please give a valid one or provide -1 when prompted");
                return;
            }
            System.out.println("\nAuctionID given to item: " + returnedIDs[0]);
            System.out.println("Attached clientID: " + returnedIDs[1]);

            if (clientID <= -1)
                this.clientID = returnedIDs[1];
        } catch (RemoteException e)
        {
            System.err.println("Exception has occurred adding auction listing!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void closeAuction(ICalc server)
    {
        Integer auctionID = Integer.parseInt(getInput("What auctionID do you wish to close?"));
        Integer clientID = Integer.parseInt(getInput("What's your clientID? Auction cannot be closed without correct clientID"));
        SealedObject clientRequest = createSealedClientRequest(clientID);
        try
        {
            System.out.println("Closing auctionID: " + auctionID);
            System.out.println(server.closeAuctionListing(auctionID, clientRequest));
        } catch (Exception e)
        {
            System.err.println("Exception has occurred closing auction");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        ClientSeller cs = new ClientSeller();

        /*if (args.length < 1)
        {
            System.out.println("Must supply an integer as a command argument!");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);*/

        try
        {
            ICalc server = cs.createServerConnection();

            while (true)
            {
                int n = Integer.parseInt(cs.getInput(""));

                if (n == -1)
                {
                    System.exit(0);
                } else if (n == 0)
                {
                    cs.displayClientUsage();
                } else if (n == 1)
                {
                    cs.viewAuctionedItems(server);
                } else if (n == 2)
                {
                    cs.addAuctionItemToServer(server);
                } else if (n == 3)
                {
                    cs.closeAuction(server);
                } else
                {
                    System.out.println("Invalid parameter given. Use 'ClientSeller 0' for help");
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