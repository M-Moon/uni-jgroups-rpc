import java.io.Serializable;

public class AuctionListing implements Serializable
{
    private static final long serialVersionUID = 4372266379305630134L;

    private AuctionItem itemListed;
    private int startingPrice;
    private int reservePrice;

    public AuctionListing(AuctionItem item, int startingPrice, int reservePrice)
    {
        this.itemListed = item;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
    }

    public AuctionItem getItemListed()
    {
        return itemListed;
    }

    public int getStartingPrice()
    {
        return startingPrice;
    }

    public int getReservePrice()
    {
        return reservePrice;
    }
}
