import java.util.concurrent.ThreadLocalRandom;

/**
 * Ehhez az osztályhoz nem írtam annyi kommentet, mivel próbáltam minden változónevet és függvény/eljárás nevet úgy írni
 * hogy annak működése elég egyértelmű legyen. 
 * Amennyiben szükséges, szívesen elmagyarázom a működésüket :) 
 */
public class Customer {
    private final static String[] NAME_LISTS = 
    {"Alex", "Gabi", "Andras", "Gabor", "Peter", "Jonas", 
    "Jani", "Sandor", "Gyula", "Hanna", "Anna", "Adri", 
    "Venessza", "Julia", "Natalia", "Antonia", "Andrea",
    "Karcsi", "Joe", "Pal", "Balazs", "Beno", "Barbara",
    "Bali", "Bandi", "Norbi", "Natasa", "Nandi", "Nudli",
    "Feri", "Ferenc", "Franci", "Donald", "Dora"};
    
    private final String name;
    private final boolean haircut;
    private final boolean beardTrimming;
    
    private int waitingTime;
    private boolean threadIsFinished;

    public enum CustomerStateEnum { AT_HOME, WAITING_AT_BARBER, UNDER_SERVICE, GOING_HOME}
    public volatile CustomerStateEnum customerState;

    public Customer() {
        name = NAME_LISTS[ThreadLocalRandom.current().nextInt(0,34)]; //Adok egy véletlenszerű nevet a Customernek, hogy könnyebben azonosítható legyen kit szolgálnak ki. 
       
        haircut = true; //Hajvágást biztosan kérnek
        beardTrimming = ThreadLocalRandom.current().nextBoolean(); //Szakállvágás véletlenszerű a feladat szerint.

        customerState = CustomerStateEnum.AT_HOME;
    }

    public String getName()                         { return name; }
    public boolean isCustomerWantHairCut()          { return haircut; }
    public boolean isCustomerWantBeardTrimming()    { return beardTrimming; }
    public int getWaitingTime()                     { return this.waitingTime; }
    public boolean isThreadFinished()               { return threadIsFinished; }
    
    public void addToWaitingTime(int msec)          { this.waitingTime += msec; }
    public void threadFinished()                    { this.threadIsFinished = true; }
}
