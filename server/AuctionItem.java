import java.io.Serializable;

public class AuctionItem implements Serializable
{
    private static final long serialVersionUID = 4372266379305630134L;

    public enum Condition
    {
        NEW,
        USED,
        DAMAGED,
        BROKEN
    }

    private int itemID;
    private String itemTitle;
    private String itemDescription;
    private Condition itemCondition;

    public AuctionItem(int itemID, String itemTitle, String itemDescription, Condition itemCondition)
    {
        this.itemID = itemID;
        this.itemTitle = itemTitle;
        this.itemDescription = itemDescription;
        this.itemCondition = itemCondition;
    }

    public int getItemID()
    {
        return itemID;
    }

    public String getItemTitle()
    {
        return itemTitle;
    }

    public String getItemDescription()
    {
        return itemDescription;
    }

    public Condition getItemCondition()
    {
        return itemCondition;
    }
}