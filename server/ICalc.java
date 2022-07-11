import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface ICalc extends Remote
{
    AuctionItem getSpec(int itemID, int clientID) throws RemoteException;
    SealedObject getSpec(int itemID, SealedObject clientReq) throws RemoteException;

    Integer getHighestBid(Integer auctionID) throws RemoteException;
    AuctionListingWithDetails getAuctionListings() throws RemoteException;

    Integer[] addAuctionListing(SealedObject clientRequest, String itemTitle, String itemDesc, AuctionItem.Condition itemCondition, int startingPrice, int reservePrice) throws RemoteException;
    String addBid(Integer auctionID, Integer itemBid, SealedObject clientRequest, String userName, String userEmail) throws RemoteException;

    String closeAuctionListing(Integer auctionID, SealedObject clientRequest) throws RemoteException;
}