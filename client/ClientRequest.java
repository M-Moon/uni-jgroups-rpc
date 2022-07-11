import java.io.Serializable;

public class ClientRequest implements Serializable
{
    private int clientID;

    ClientRequest(int cid)
    {
        this.clientID = cid;
    }

    public int getClientID()
    {
        return clientID;
    }

    public void setClientID(int cid)
    {
        clientID = cid;
    }
}
