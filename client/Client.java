import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Scanner;

public abstract class Client
{
    protected static String SERVER_NAME = "auction_server";
    protected static String ENCRYPTION_METHOD = "AES/CBC/PKCS5Padding";

    protected int clientID = -1;

    protected SecretKey secretKey;
    protected Cipher cipher;

    public Client()
    {
        this.secretKey = getSecretKeyFromStorage();
        this.cipher = createCipher(secretKey);
    }

    public void setClientID(int cid)
    {
        this.clientID = cid;
    }

    public int getClientID()
    {
        return this.clientID;
    }

    public String getInput(String prompt)
    {
        Scanner scanner = new Scanner(System.in);
        String line = "";
        try
        {
            System.out.println(prompt);
            line = scanner.nextLine();
        } catch (Exception e)
        {
            System.out.println("Exception has occurred getting input from user");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println();
        return line;
    }

    protected ICalc createServerConnection()
    {
        ICalc server = null;
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            server = (ICalc) registry.lookup(SERVER_NAME);
        } catch (Exception e)
        {
            System.out.println("Error occurred connecting with server.");
            e.printStackTrace();
            System.exit(1);
        }
        return server;
    }

    public abstract void displayClientUsage();

    public void printItemDetails(AuctionItem item)
    {
        System.out.println("The item's details are as follows:");
        System.out.println("ItemID: " + item.getItemID());
        System.out.println("Item Name: " + item.getItemTitle());
        System.out.println("Item Description: " + item.getItemDescription());
        System.out.println("Item Condition: " + item.getItemCondition().toString());
    }

    public SecretKey getSecretKeyFromStorage()
    {
        SecretKey secretKey = null;
        try
        {
            // Get secret key from storage
            KeyStore keyStore = KeyStore.getInstance("jceks");
            keyStore.load(new FileInputStream("../keys/aeskeys.jceks"), null);
            secretKey = (SecretKey) keyStore.getKey("AES-Session-Key","".toCharArray());
            //System.out.println("Secret key: " + secretKey);
        } catch (Exception e)
        {
            System.err.println("Exception has occurred while getting secret key from storage");
            e.printStackTrace();
            System.exit(1);
        }
        return secretKey;
    }

    public Cipher createCipher(SecretKey secretKey)
    {
        Cipher cipher = null;
        try
        {
            // Creating the cipher
            cipher = Cipher.getInstance(ENCRYPTION_METHOD);
            cipher.init(cipher.ENCRYPT_MODE, secretKey);
        } catch (Exception e)
        {
            System.err.println("Exception has occurred creating cipher with secret key");
            e.printStackTrace();
            System.exit(1);
        }
        return cipher;
    }

    public ClientRequest createClientRequest(int cid)
    {
        ClientRequest cr = null;
        try
        {
            // Creating the client request
            cr = new ClientRequest(cid); // generating client request to get auction item
        } catch (Exception e)
        {
            System.err.println("Exception has occurred creating client request");
            e.printStackTrace();
            System.exit(1);
        }
        return cr;
    }

    public SealedObject createSealedClientRequest(int cid)
    {
        SealedObject sealedClientRequest = null;
        try
        {
            // Creating the sealed client request
            ClientRequest cr = createClientRequest(cid);
            sealedClientRequest = new SealedObject(cr,cipher);
        } catch (Exception e)
        {
            System.err.println("Exception has occurred creating client request");
            e.printStackTrace();
            System.exit(1);
        }
        return sealedClientRequest;
    }

    public void viewAuctionedItems(ICalc server)
    {
        try
        {
            AuctionListingWithDetails auctionListingWithDetails = server.getAuctionListings();
            HashMap<Integer,AuctionListing> auctionListings = auctionListingWithDetails.getAuctionListingHashMap();
            HashMap<Integer,Integer> auctionListingsHighestBids = auctionListingWithDetails.getAuctionListingHighestBid();

            for (Integer auctionID : auctionListings.keySet())
            {
                System.out.println("AuctionID: " + auctionID);
                System.out.println("Highest bid: " + auctionListingsHighestBids.get(auctionID));
                printItemDetails(auctionListings.get(auctionID).getItemListed());
                System.out.println();
            }
        } catch (Exception e)
        {
            System.err.println("Exception has occurred viewing the auctioned items");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
