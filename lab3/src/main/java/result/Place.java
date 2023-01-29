package result;

public class Place {
    private String name;
    private String xid;

    public Place(String name, String xid){
        this.name = name;
        this.xid = xid;
    }

    public String getXid(){
        return xid;
    }

    public String getName() {
        return name;
    }
}
