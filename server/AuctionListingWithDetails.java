import java.io.Serializable;
import java.util.HashMap;

public class AuctionListingWithDetails implements Serializable
{
    private static final long serialVersionUID = 8148569786726768755L;

    private HashMap<Integer,AuctionListing> auctionListingHashMap = new HashMap<>(); // maps auction id to listing
    private HashMap<Integer,Integer> auctionListingHighestBid = new HashMap<>(); // maps auction id to the highest bid

    public AuctionListingWithDetails()
    {
    }

    public void addListing(Integer auctionID, AuctionListing auctionListing)
    {
        auctionListingHashMap.put(auctionID,auctionListing);
    }

    public void addHighestBid(Integer auctionID, Integer highestBid)
    {
        auctionListingHighestBid.put(auctionID,highestBid);
    }

    public HashMap<Integer,AuctionListing> getAuctionListingHashMap()
    {
        return auctionListingHashMap;
    }

    public HashMap<Integer,Integer> getAuctionListingHighestBid()
    {
        return auctionListingHighestBid;
    }
}
