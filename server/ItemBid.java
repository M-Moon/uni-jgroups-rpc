public class ItemBid
{
    private Integer itemBid;
    private Integer clientID;
    private String userName;
    private String userEmail;

    public ItemBid(Integer itemBid, Integer clientID, String userName, String userEmail)
    {
        this.itemBid = itemBid;
        this.clientID = clientID;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    public Integer getItemBid()
    {
        return itemBid;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getUserEmail()
    {
        return userEmail;
    }
}
