import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ehhez az osztályhoz nem írtam annyi kommentet, mivel próbáltam minden változónevet és függvény/eljárás nevet úgy írni
 * hogy annak működése elég egyértelmű legyen. 
 * Amennyiben szükséges, szívesen elmagyarázom a működésüket :) 
 */
public class Barberer{
    private final boolean canCutHair;
    private final boolean canCutBeard;

    private int totalServiceCustomers; //Az összes kiszolgált vevő a fodrász által
    private int servicedCustomer; //Egy bizonyos napon hány vendéget szolgált ki, nap végén 0-ra vált.
    private List<Integer> servicedCustomerPerDayCollection; // Lista, mely tárolja naponta hány vevőt szolgált ki az adott fodrász.

    public enum BarbererStateEnum { AT_HOME, WAITING_FOR_CUSTOMERS, GIVING_SERVICE}
    public volatile BarbererStateEnum barbererState;

    public Barberer(boolean canCutHair, boolean canCutBeard) {
        this.canCutHair = canCutHair;
        this.canCutBeard = canCutBeard;
        this.servicedCustomerPerDayCollection = Collections.synchronizedList(new ArrayList<Integer>());

        this.barbererState = BarbererStateEnum.AT_HOME;
    }

    public boolean canCutHair()                                 { return canCutHair; }
    public boolean canCutBeard()                                { return canCutBeard; }
    public int getServicedCustomer()                            { return servicedCustomer; }
    public int getTotalServicedCustomers()                      { return totalServiceCustomers; }
    public List<Integer> getServicedCustomerPerDayCollection()  { return servicedCustomerPerDayCollection; }

    public void setServicedCustomer(int servicedCustomer)       { this.servicedCustomer = servicedCustomer; }
    public void setServicedCustomerToZero() { 
        this.totalServiceCustomers += this.servicedCustomer;  
        this.servicedCustomer = 0;
    }
}
