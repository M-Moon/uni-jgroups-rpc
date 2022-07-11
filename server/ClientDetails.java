import java.util.ArrayList;

public class ClientDetails
{
    private int clientID;
    private String userName;
    private String userEmail;

    private ArrayList<Integer> sellerAuctionIDs;
    private ArrayList<Integer> buyerAuctionIDs;

    public ClientDetails(int clientID, String userName, String userEmail)
    {
        this.clientID = clientID;
        this.userName = userName;
        this.userEmail = userEmail;

        this.sellerAuctionIDs = new ArrayList<Integer>();
        this.buyerAuctionIDs = new ArrayList<Integer>();
    }

    public ArrayList<Integer> getSellerAuctionIDs()
    {
        return sellerAuctionIDs;
    }

    public void addToSellerAuctionIDs(int auctionID)
    {
        this.sellerAuctionIDs.add(auctionID);
    }

    public boolean compareSellerIDs(int auctionID)
    {
        if (sellerAuctionIDs.contains(auctionID))
            return true;
        return false;
    }

    public ArrayList<Integer> getBuyerAuctionIDs()
    {
        return buyerAuctionIDs;
    }

    public void addToBuyerAuctionIDs(int auctionID)
    {
        this.buyerAuctionIDs.add(auctionID);
    }

    public boolean compareBuyerIDs(int auctionID)
    {
        if (buyerAuctionIDs.contains(auctionID))
            return true;
        return false;
    }

    public int getClientID()
    {
        return clientID;
    }

    public void setClientID(int clientID)
    {
        this.clientID = clientID;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserEmail()
    {
        return userEmail;
    }

    public void setUserEmail(String userEmail)
    {
        this.userEmail = userEmail;
    }
}
