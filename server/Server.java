import org.jgroups.*;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.RspList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

// part 3

public class Server implements ICalc
{
    private static String REGISTRY_ENTRY_NAME = "auction_server";
    private static String ENCRYPTION_METHOD = "AES/CBC/PKCS5Padding";

    private JChannel ch;
    private RpcDispatcher rpc;
    private final int timeout = 1200;

    private Integer auctionIDCount = 1;
    private Integer itemIDCount = 1;
    private Integer clientIDCount = 1;

    private ArrayList<AuctionItem> itemList = new ArrayList<>(); // a list to store individual items if desired

    private HashMap<Integer,AuctionListing> auctionListings = new HashMap<>(); // maps auctionID to an auctionListing
    private HashMap<Integer,Integer> auctionHighestBids = new HashMap<>(); // maps auctionID to its corresponding highest bid
    private HashMap<Integer,ItemBid> auctionHighestBidDetails = new HashMap<>(); // maps auctionID to its corresponding highest bid's details

    private HashMap<Integer,ClientDetails> clientDetails = new HashMap<>(); // maps clientID to its corresponding client details

    private SecretKey secretKey = null;

    public Server() throws RemoteException
    {
        super();

        this.secretKey = createSecretKey();
        storeSecretKey(this.secretKey);
    }

    /////// REPLICA SECTION ///////

    public void setupChannel()
    {
        try
        {
            createChannel();
            setupProtocolStack();
            connectToGroup();
            createDispatcher();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void createChannel()
    {
        ch = new JChannel(false);

        ch.setReceiver(new ReceiverAdapter() {

            public void receive(Message msg) {

                System.out.println("received message " + msg);

            }

            public void viewAccepted(View view) {

                System.out.println("received view " + view);

            }
        });
    }

    private void createDispatcher()
    {
        rpc = new RpcDispatcher(ch, this);
    }

    private void setupProtocolStack()
    {
        ProtocolStack stack = new ProtocolStack();

        ch.setProtocolStack(stack); // set stack to channel

        try
        {
            stack.addProtocol(new UDP().setValue("bind_addr",

                    InetAddress.getByName("127.0.0.1")))

                .addProtocol(new PING())

                .addProtocol(new MERGE2())

                .addProtocol(new FD_SOCK())

                .addProtocol(new FD_ALL().setValue("timeout", 12000)

                    .setValue("interval", 3000))

                .addProtocol(new VERIFY_SUSPECT())

                .addProtocol(new BARRIER())

                .addProtocol(new NAKACK())

                .addProtocol(new UNICAST2())

                .addProtocol(new STABLE())

                .addProtocol(new GMS())

                .addProtocol(new UFC())

                .addProtocol(new MFC())

                .addProtocol(new FRAG2());

            stack.init(); // initialise stack
        } catch (Exception e)
        {
            System.err.println("Something went wrong setting up the protocol stack");
            e.printStackTrace();
        }
    }

    private void connectToGroup() throws Exception
    {
        ch.connect(REGISTRY_ENTRY_NAME);
    }

    public void viewAccepted()
    {
        View view = ch.getView();
        List<Address> members = view.getMembers();
        System.out.println("Members of group/view:");
        view.forEach(System.out::println);
    }

    public void sendMessage(String msg) throws Exception
    {
        Message message = new Message(null, msg.getBytes(StandardCharsets.UTF_8));
    }

    public void receiveMessage()
    {
        ch.getReceivedMessages();
    }

    public int getListingState()
    {
        RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_ALL, timeout);
        int state = 0;
        try
        {
            RspList <List<AuctionListing>> stateList = rpc.callRemoteMethods(null, "getAuctionListingsForReplicas", null, null, requestOptions);
            state = checkListingVotes(stateList);
        } catch (Exception e)
        {
            System.err.println("Error getting auction listings from replicas. Possibly no response");
            e.printStackTrace();
        }
        return state;
    }

    private int checkListingVotes(RspList<List<AuctionListing>> responseList)
    {
        for (List<AuctionListing> response : responseList.getResults())
        {
            for (List<AuctionListing> otherResponse : responseList.getResults())
            {
                if (response.equals(otherResponse))
                    continue;
                else
                {
                    System.out.println("Replica error: Responses did not match");
                    return -1;
                }
            }
        }
        return 0;
    }

    public int getItemState()
    {
        RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_ALL, timeout);
        int state = 0;
        try
        {
            RspList <List<AuctionItem>> stateList = rpc.callRemoteMethods(null, "getAuctionItemsForReplicas", null, null, requestOptions);
            state = checkItemVotes(stateList);
        } catch (Exception e)
        {
            System.err.println("Error getting auction listings from replicas. Possibly no response");
            e.printStackTrace();
        }
        return state;
    }

    private int checkItemVotes(RspList<List<AuctionItem>> responseList)
    {
        for (List<AuctionItem> response : responseList.getResults())
        {
            for (List<AuctionItem> otherResponse : responseList.getResults())
            {
                if (response.equals(otherResponse))
                    continue;
                else
                {
                    System.out.println("Replica error: Responses did not match");
                    return -1;
                }
            }
        }
        return 0;
    }

    /////// FUNCTIONAL SECTION ///////

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

    public void displayServerUsage()
    {
        System.out.println("Command-line inputs:");
        System.out.println("0: This help screen");
        System.out.println("1: Send message");
        System.out.println("2: View group");
        System.out.println("3: Get item list state");
        System.out.println("4: Print all item lists");
        System.out.println("-1: Exit");
    }

    public void sendMessage()
    {
        String s = getInput("Type your message:");
        try
        {
            sendMessage(s);
        } catch (Exception e)
        {
            System.err.println("Exception occurred sending message");
            e.printStackTrace();
        }
        System.out.println(ch.getReceivedMessages());
    }

    private void addToClientDetails(int clientID, String userName, String userEmail)
    {
        if (!clientDetails.containsKey(clientID)) // if client details don't exist in hashmap
        {
            ClientDetails tempDetails = new ClientDetails(clientID, userName, userEmail);
            clientDetails.put(clientID, tempDetails);
        }
    }

    private void printItemDetails(AuctionItem item)
    {
        System.out.println("The item's details are as follows:");
        System.out.println("ItemID: " + item.getItemID());
        System.out.println("Item Name: " + item.getItemTitle());
        System.out.println("Item Description: " + item.getItemDescription());
        System.out.println("Item Condition: " + item.getItemCondition().toString());
    }

    private String printWinnerDetails(Integer auctionID)
    {
        ItemBid winnerBid = auctionHighestBidDetails.get(auctionID);

        String s = "";
        s += "Winner of auctionID " + auctionID + " is:\n";
        s += "User name: " + winnerBid.getUserName() + "\n";
        s += "User email: " + winnerBid.getUserEmail() + "\n";

        return s;
    }

    private void addAuctionItem(AuctionItem item)
    {
        itemList.add(item);
    }

    public List<Integer> getActiveAuctionIDs()
    {
        List<Integer> IDs = new ArrayList<>();

        for (Integer auctionID : auctionListings.keySet())
        {
            IDs.add(auctionID);
        }

        return IDs;
    }

    public void displayCurrentAuctions()
    {
        for (Integer auctionID : auctionListings.keySet())
        {
            AuctionListing tempListing = auctionListings.get(auctionID);
            AuctionItem tempItem = tempListing.getItemListed();

            // printing details of the auction, and then printing details of the item being auctioned
            System.out.println("Auction ID: " + auctionID);
            if (auctionHighestBids.containsKey(auctionID))
            {
                System.out.println("Highest bid: " + auctionHighestBids.get(auctionID));
            } else
            {
                System.out.println("Highest bid: " + tempListing.getStartingPrice());
            }

            printItemDetails(tempItem);
        }
    }

    public synchronized Integer[] addAuctionListing(SealedObject clientRequest, String itemTitle, String itemDesc, AuctionItem.Condition itemCondition, int startingPrice, int reservePrice)
    {
        ClientRequest clientReq = null;
        try
        {
            clientReq = unsealClientRequest(clientRequest);
        } catch (Exception e)
        {
            System.err.println("Error unsealing client request when adding auction listing");
            e.printStackTrace();
        }

        int clientID = clientReq.getClientID();
        if (clientID <= 0)
        {
            clientID = clientIDCount;
            clientIDCount++;
        } else if (!clientDetails.containsKey(clientID))
        {
            return new Integer[]{-1,-1};
        }

        Integer newItemID = itemIDCount;
        Integer newAuctionID = auctionIDCount;

        AuctionItem newItem = new AuctionItem(newItemID, itemTitle, itemDesc, itemCondition); // creating item based on spec
        itemList.add(newItem); // saving item for storage purposes
        itemIDCount++; // increment after safely performing operation

        RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_ALL, timeout);
        try
        {
            rpc.callRemoteMethods(null, "addAuctionItem", new Object[]{newItem}, null, requestOptions);
        } catch (Exception e)
        {
            System.err.println("Error getting auction listings from replicas. Possibly no response");
            e.printStackTrace();
        }

        AuctionListing newAuction = new AuctionListing(newItem, startingPrice, reservePrice);
        auctionListings.put(newAuctionID, newAuction); // new auction put into hashmap
        auctionIDCount++; // increment after safely performing operation

        addToClientDetails(clientID, "", "");
        clientDetails.get(clientID).addToSellerAuctionIDs(newAuctionID); // add auctionid to client's seller list

        return new Integer[]{newAuctionID, clientID};
    }

    public synchronized String closeAuctionListing(Integer auctionID, SealedObject clientRequest)
    {
        ClientRequest clientReq = null;
        try
        {
            clientReq = unsealClientRequest(clientRequest);
        } catch (Exception e)
        {
            System.err.println("Error unsealing client request when adding auction listing");
            e.printStackTrace();
        }

        int clientID = clientReq.getClientID();

        if (!auctionListings.containsKey(auctionID))
        {
            return "auctionID doesn't exist. Exiting...";
        } else if (!clientDetails.containsKey(clientID))
        {
            return "Error: Client details don't exist on server.";
        } else if (!clientDetails.get(clientID).compareSellerIDs(auctionID))
        {
            return "Wrong clientID, unable to close auction";
        }

        System.out.println("Closing auction, auctionID: " + auctionID);

        AuctionListing tempListing = auctionListings.get(auctionID);

        try
        {
            auctionListings.remove(auctionID);
            System.out.println("Auction successfully closed. AuctionID: " + auctionID);
        } catch (Exception e)
        {
            System.out.println("Auction unsuccessfully closed. Error occurred.");
            e.printStackTrace();
        }

        if (!auctionHighestBids.containsKey(auctionID))
        {
            return "There are no bids for this item. No sale made (auction still closed)";
        } else if (auctionHighestBids.get(auctionID) < tempListing.getReservePrice())
        {
            return "Reserve price hasn't been reached, no sale made (auction still closed)";
        } else
        {
            return "\nAuction successfully closed!\n\n" + printWinnerDetails(auctionID);
        }
    }

    /**
     *
     * @return int based on if adding bid was successful. 0 = success, -1 = auctionid doesn't exist, -2 = item bid lower than starting price,
     * -3 = item bid lower than current highest bid
     */
    public synchronized String addBid(Integer auctionID, Integer itemBid, SealedObject clientRequest, String userName, String userEmail)
    {
        ClientRequest cr = null;
        try
        {
            cr = unsealClientRequest(clientRequest);
        } catch (Exception e)
        {
            return "Error occurred unsealing client request. Did you provide a valid client request object?";
        }

        int clientID = cr.getClientID();
        if (clientID < 0)
        {
            clientID = clientIDCount;
            clientIDCount++;
        }

        if (!auctionListings.containsKey(auctionID))
        {
            return "Given auctionID doesn't exist! Exiting...";
        } else if (itemBid < auctionListings.get(auctionID).getStartingPrice())
        {
            return "Item bid is lower than starting price. Not accepted. Exiting...";
        }

        if (!auctionHighestBids.containsKey(auctionID))
        {
            auctionHighestBids.put(auctionID,itemBid);
        } else
        {
            if (itemBid <= auctionHighestBids.get(auctionID))
            {
                return "Item bid lower than current highest bid. Not accepted. Exiting...";
            } else
            {
                auctionHighestBids.replace(auctionID,itemBid);
            }
        }

        ItemBid newBid = new ItemBid(itemBid, clientID, userName, userEmail);

        if (auctionHighestBidDetails.containsKey(auctionID))
        {
            auctionHighestBidDetails.replace(auctionID,newBid);
        } else
        {
            auctionHighestBidDetails.put(auctionID,newBid);
        }

        addToClientDetails(clientID, userName, userEmail); // adding to client details hashmap if it doesn't already exist. if it does, continue
        clientDetails.get(clientID).addToBuyerAuctionIDs(auctionID); // adding buyer auctionid to client's details

        return "Bid successfully placed. Attached clientID is: " + clientID + ". Don't lose it!";
    }

    public Integer getHighestBid(Integer auctionID)
    {
        if (!auctionHighestBids.containsKey(auctionID))
            return -1;
        else
            return auctionHighestBids.get(auctionID);
    }

    public AuctionListingWithDetails getAuctionListings()
    {
        AuctionListingWithDetails auctionListingWithDetails = new AuctionListingWithDetails();

        for (Integer auctionID : auctionListings.keySet())
        {
            Integer highestBid = 0;
            if (auctionHighestBids.containsKey(auctionID))
            {
                highestBid = auctionHighestBids.get(auctionID);
            }
            auctionListingWithDetails.addListing(auctionID,auctionListings.get(auctionID));
            auctionListingWithDetails.addHighestBid(auctionID,highestBid);
        }

        //System.out.println(auctionListingWithDetails);
        return auctionListingWithDetails;
    }

    public List<AuctionListing> getAuctionListingsForReplicas()
    {
        List<AuctionListing> al = new ArrayList<>();

        for (Integer auctionID : auctionListings.keySet())
        {
            al.add(auctionListings.get(auctionID));
        }

        return al;
    }

    public List<AuctionItem> getAuctionItemsForReplicas()
    {
        return itemList;
    }

    private boolean compareItemID(int itemID, AuctionItem item)
    {
        if (itemID == item.getItemID())
            return true;
        return false;
    }

    private SecretKey createSecretKey()
    {
        SecretKey secretKey = null;
        try
        {
            // Generating the key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = new SecureRandom(); // cryptographically strong rng
            int keyBitSize = 256;

            keyGen.init(keyBitSize,secureRandom);
            secretKey = keyGen.generateKey(); // shared AES session key generated
        } catch (NoSuchAlgorithmException e)
        {
            System.err.println("Wrong algorithm provided for key generator");
            e.printStackTrace();
            System.exit(1);
        }
        return secretKey;
    }

    private Cipher createCipher(SecretKey sk)
    {
        Cipher cipher = null;
        try
        {
            // Creating the cipher
            cipher = Cipher.getInstance(ENCRYPTION_METHOD);
            cipher.init(cipher.ENCRYPT_MODE, sk);
        } catch (Exception e)
        {
            System.err.println("Error has occurred creating cipher");
            e.printStackTrace();
            System.exit(1);
        }
        return cipher;
    }

    private void storeSecretKey(SecretKey sk)
    {
        try
        {
            // Storing the secret key
            String keyStoreFilePath = "../keys/aeskeys.jceks";
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            //System.out.println("Key Store: " + keyStore);
            //System.out.println("Secret Key: " + sk);

            keyStore.load(null, null);

            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(sk);
            keyStore.setEntry("AES-Session-Key", secretKeyEntry, new KeyStore.PasswordProtection("".toCharArray()));

            java.io.FileOutputStream fp = new java.io.FileOutputStream(keyStoreFilePath);
            keyStore.store(fp,"".toCharArray());
        } catch (KeyStoreException e)
        {
            System.err.println("Error has occurred getting keystore instance");
            e.printStackTrace();
            System.exit(1);
        } catch (FileNotFoundException e)
        {
            System.out.println("File not found for storing key");
        } catch (Exception e)
        {
            System.err.println("Other exception has occurred storing secret key");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private SealedObject createSealedItem(AuctionItem itemToSeal)
    {
        SealedObject sealedObj = null;
        try
        {
            Cipher cipher = createCipher(secretKey);
            sealedObj = new SealedObject(itemToSeal,cipher);
        } catch (Exception e)
        {
            System.out.println("Encryption exception has occurred");
            e.printStackTrace();
            System.exit(1);
        }
        return sealedObj;
    }

    private ClientRequest unsealClientRequest(SealedObject cr)
    {
        ClientRequest request = null;
        try
        {
            request = (ClientRequest) cr.getObject(secretKey);
        } catch (Exception e)
        {
            System.err.println("Something went wrong unsealing client request.");
        }
        return request;
    }

    public SealedObject getSpec(int itemID, SealedObject clientReq) throws RemoteException
    {
        System.out.println("Handling client request: get sealed auction item");

        for (AuctionItem item : itemList)
        {
            if (compareItemID(itemID, item))
                return createSealedItem(item);
        }
        return null;
    }

    public AuctionItem getSpec(int itemID, int clientID) throws RemoteException
    {
        System.out.println("Handling client request: get auction item");

        for (AuctionItem item : itemList)
        {
            if (compareItemID(itemID, item))
                return item;
        }
        return null;
    }

    public void printItemList()
    {
        System.out.println(itemList.toString());
    }

    public void printAllItemLists()
    {
        RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_ALL, timeout);
        try
        {
            rpc.callRemoteMethods(null, "printItemList", null, null, requestOptions);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        printItemList();
    }

    public static void main(String[] args)
    {
        try
        {
            Server s = new Server();
            ICalc stub = (ICalc) UnicastRemoteObject.exportObject(s,0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(REGISTRY_ENTRY_NAME, stub);
            System.out.println("✅  server running...");

            // starting channel and joining group
            s.setupChannel();

            while (true)
            {
                int n = Integer.parseInt(s.getInput(""));

                if (n == -1)
                {
                    System.exit(0);
                } else if (n == 0)
                {
                    s.displayServerUsage();
                } else if (n == 1)
                {
                    s.sendMessage();
                    s.receiveMessage();
                } else if (n == 2)
                {
                    s.viewAccepted();
                } else if (n == 3)
                {
                    System.out.println("Item state is: " + s.getItemState());
                } else if (n == 4)
                {
                    s.printAllItemLists();
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