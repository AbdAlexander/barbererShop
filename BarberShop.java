import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BarberShop {
    //Szimuláció főbb statikus beállításai, szükség esetén változtatható, így a szimuláció tovább tarthat vagy máshogy futhat le.
    private static final int MAX_CAPICITY = 5; // A fodrászüzlet kapacitása (hány vevő várakozhat egyszerre)
    private static final int SIM_HOUR = 400; // A szimuláció egy órája (msec-ben számítva)
    private static final int OPEN_AT = 9; // A fodrászüzlet nyitásának ideje (órákban számítva)
    private static final int CLOSE_AT = 17; // A fodrászüzlet zárásának ideje (órákban számítva)
    private static final int MIN_SERVICE_TIME = 20; //A fodrásznál szolgáltatásának minimum ideje  (msec-ben számítva)
    private static final int MAX_SERVICE_TIME = 200; //A fodrásznál szolgáltatásának maximum ideje  (msec-ben számítva)
    private static final int SIMULATION_SEC_PER_MSCEC = 1; // Szimuláció másodperc / msec állítója, ezzel szabályozatjuk a szimuláció egy iterációja milyen időközönként hajtódjon végre.
    private static final int SIMULATION_DAYS_LASTS_FOR = 5; // Szimuláció időtartama napok száma szerint. (napokban számítva)
    private static final int CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_OPEN = 20; // Minél nagyobb ez a szám, annál gyakrabban jönnek a vevők.
    private static final int CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_CLOSED = 5; // Minél nagyobb ez a szám, annál gyakrabban jönnek a vevők.
    //-----------------------------------------------------------------------------------------------------------------------------
    
    //Lockok
    private static Object CUSTOMER_IS_WAITING_LOCK = new Object();
    //Más Lockot azért nem használok, mert ahol még kellene szinkronizáció, ott szemafort használok.
    //-----------------------------------------------------------------------------------------------------------------------------

    //Osztály adattagok
    private boolean isClosed;
    private int msecCounter;
    private int dayCounter;
    private int unServedCustomersCounter_becauseShopIsFull;
    private int unServedCustomersCounter_becauseShopIsClosed;

    private List<Customer> customerRecords; // Itt tárolom az összes létrehozott customert, amik létrehozódnak a szimuláció alatt.
    private BlockingQueue<Customer> waitingCustomersQueue; //Lényegében az üzlet várakozóhelyisége, innen vesznek ki szemaforral védve a fodrászok customereket.
    
    private Barberer barberer_hairCut_and_beardCut;
    private Barberer barberer_onlyHairCut;
    private boolean barbererThread1_isFinished;
    private boolean barbererThread2_isFinished;

     //-----------------------------------------------------------------------------------------------------------------------------
    /**
     * Paraméter nélküli konstuktor, beállítom az összes alapértéket, amit használni fogok.
     * Azok az értékek, melyek alapból 0-ra vagy false-ra inicializálódnak, azokat nem írtam bele...
     * ...hogy minél kevesebb kódsor legyen, és minél olvashatóbb legyen a gondolatmenetem. :) 
     */
    public BarberShop() {
        isClosed = true;

        waitingCustomersQueue = new ArrayBlockingQueue<Customer>(MAX_CAPICITY);
        customerRecords = Collections.synchronizedList(new ArrayList<Customer>());
        
        barberer_hairCut_and_beardCut = new Barberer(true,true);
        barberer_onlyHairCut = new Barberer(true,false);
    }
    /**
     * Ez az eljárás szolgál azzal, hogy a szimuláció működjön.
     * Próbáltam minél inkább olvashatóbbá tenni, hogyan működik a szimuláció, ezért minden egyes funkcionális lépést külön..
     * ..eljárásba vagy függvénybe raktam.
     * 
     * Az eljárás végén azért ellenőrzöm le, hogy az összes szál lefutott-e, hogy 10000 lefutás esetén se fordulhasson elő olyan, hogy
     * előbb fut le a szimuláció vége, minthogy az egyik szál befejődött volna, amolyan biztonsagi ellenorzes...
     * ..(alapvetoen a synchronized blokkok, es a szalak varakoztatasa miatt ilyen nem történhetne meg, 
     * ..de ha mégis, akkor itt bevárjuk az utolsó szálat is.)
     * Meg lehetett volna ezt oldani 'konkurensebben' is, de úgy gondoltam, hogy valószínűleg ez a lehető legrövidebb és..
     * ..leghatékonyabb megoldás, bízom benne, hogy igazam volt.
     * Habár valószínűleg megoldható lenne a szálak .join hívásával, de ahhoz még egy listát kellene tároljak a szálakról...
     * ...azokon végigiterálva megoldható lenne, de a feladat jellege miatt, jobbnak tartom ezt a megoldást. 
     * 
     */
    public void startSimulation() {
        System.out.println("\nSzimulacio elkezdott! Kovetkezik az " + ++dayCounter + ". nap.\n");

        barberersBehavior(); //Fodrászok szálainak létrehozása, és viselkedésük lekezelése.

        while(this.dayCounter <= SIMULATION_DAYS_LASTS_FOR) {
            waitOneMsec(); // Várunk 1 mscec-ot, hogy a szimuláció számolása jól működjön!
            
            this.msecCounter++;
            checkTime(msecCounter); // Megnézzük, hogy éppen hány óra van, és az alapján változtatjuk, hogy az üzlet éppen nyitva vagy zárva lesz az adott iterációban.

            customerBehavior(); // Adott iterációban megnézzük, hogy érkezik-e a vevő a boltba, és lekezeljük az eseteket.
            
            checkEndDay(msecCounter); // Megnézzük, hogy eltelt-e egy nap, ha igen lenullázzuk a msec számlálót, és megnöveljük a napok számát.
        }

        boolean isAllThreadFinished = false;
        while(!barbererThread1_isFinished || !barbererThread2_isFinished || !isAllThreadFinished) {
            for(Customer c : customerRecords) {
                isAllThreadFinished = isAllThreadFinished || c.isThreadFinished();
                if(!c.isThreadFinished()) { //Megvárjuk, amíg befejeződik, majd új iterációt indítunk.
                    isAllThreadFinished = false;
                    break; //Csak a belső ciklusból lépünk ki.
                } 
            }
            waitOneMsec();
            System.out.println("\nVarakozas mig az osszes szal befejezodik...");
        }
        endSimulation();
    }
    /**
     * Eljárás arra szolgál, hogy amikor letelik az 5 nap (változtatható), kiírjuk a fodrászüzlet statisztikáját.
     */
    private void endSimulation() {
        System.out.println("\n------------------------------------------------------------------------");
        System.out.println("A(z) " + SIMULATION_DAYS_LASTS_FOR + " nap is befejezodott, a szimulacio veget ert.");
        System.out.println("Szimulacio statisztika:\n");

        int totalServicedCustomer = barberer_hairCut_and_beardCut.getTotalServicedCustomers() + barberer_onlyHairCut.getTotalServicedCustomers();
        System.out.println("Osszesen " + totalServicedCustomer + " vevot szolgalt ki a fodraszuzlet.");
        System.out.println("Osszesen " + this.unServedCustomersCounter_becauseShopIsClosed + " vevot nem tudott kiszolgalni, mivel zarva volt az uzlet.");
        System.out.println("Osszesen " + this.unServedCustomersCounter_becauseShopIsFull + " vevot nem tudott kiszolgalni az uzlet, mert sokan varakoztak az uzletnel. (Ha noveljuk az CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_OPEN erteket, tobbet kapunk: 200-as ertek mellett, mar szinte biztos nem lesz ido mindenkire.) \n");
        
        System.out.println("Napi statisztika:");
        for(int day = 0; day < SIMULATION_DAYS_LASTS_FOR; day++) {
            int servicedCustomerPerDay =  barberer_hairCut_and_beardCut.getServicedCustomerPerDayCollection().get(day) 
                                        + barberer_onlyHairCut.getServicedCustomerPerDayCollection().get(day);
            int printedDay = day + 1;
            System.out.println("    A(z) " + printedDay + ". napon osszesen: " + servicedCustomerPerDay + " vevot szolgalt ki a fodraszuzlet. (KEPZETT FODRASZ: " + barberer_hairCut_and_beardCut.getServicedCustomerPerDayCollection().get(day) + " szolgalt ki. CSAK HAJAT VAGO FODRASZ: " + barberer_onlyHairCut.getServicedCustomerPerDayCollection().get(day) + " szolgalt ki.)");
        }

        int totalWaitingTime = 0;
        int maxWaitingTime = 1; //Véletlenül se osszunk 0-val.
        for(Customer c : this.customerRecords) {
            totalWaitingTime += c.getWaitingTime();
            if(maxWaitingTime < c.getWaitingTime())
                maxWaitingTime = c.getWaitingTime();
        }

        double avarageWaitingTime = totalWaitingTime / maxWaitingTime;
        System.out.println("Atlagos varakozasi ido az uzletben: " + avarageWaitingTime + " msec.");
        System.out.println("------------------------------------------------------------------------\n");
        System.out.println("INFO: Ha tobb vevot szeretnenk, hogy erkezzen csak noveljuk meg a CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_OPEN (/ CLOSED) ertek(ek)et\n");
    }

    /**
     * Eljárás arra szolgál, hogy lekezelje a vevők eseteit.
     * Ha érkezik egy vendég, akkor megnézzük, hogy be tud-e menni, ha nem, megnöveljük a kiszolgálatlan vevők számosságát.
     * Ha be tud menni az üzletbe, és le tud ülni várakozni, úgy létrehozunk egy szálat annak a vevőnek, majd eltároljuk...
     * ..a vevő adatait egy listában, majd szinkronizálva elkezdenek együttdolgozni a fodrász szálakkal.
     * 
     * A szál akkor fut el, ha a vendég megkapta a GOING_HOME állapotot, ez szükséges, hogy a különböző szálak...
     * ...párhuzamosan együtt tudjanak dolgozni, ne legyen az, hogy egy customer előbb végzett, mint a fodrász végzett volna vágással.
     */
    private void customerBehavior() {
        if(customerIsComing()) {
            System.out.println("\nErkezett egy vevo az uzlet ele!");
            
            if(!isClosed && waitingCustomersQueue.size() < MAX_CAPICITY) {
                Thread customerThread = new Thread(() -> {
                    Customer customer = new Customer();
                    this.customerRecords.add(customer);

                    try {
                        synchronized(CUSTOMER_IS_WAITING_LOCK) {
                            customer.customerState = Customer.CustomerStateEnum.WAITING_AT_BARBER;

                            this.waitingCustomersQueue.put(customer);
                            System.out.println(customer.getName() + " leult az uzletbe es elkezdett varakozni.\n");
                            
                            CUSTOMER_IS_WAITING_LOCK.notifyAll();
                        }
                        
                        int waitingTime = 0;
                        while(customer.customerState == Customer.CustomerStateEnum.WAITING_AT_BARBER) {
                            waitOneMsec();
                            customer.addToWaitingTime(waitingTime++); // Ha csak 1 msec-et várakozott, amíg ez a kódsor lefutott, az nem kerül számításba
                        }
                        System.out.println(customer.getName() + " osszesen " + waitingTime + " msec varakozott mig sorra kerult.\n");

                        while(customer.customerState == Customer.CustomerStateEnum.UNDER_SERVICE) {
                            waitOneMsec();
                        } 
                        
                    } catch (InterruptedException e) {
                        System.err.println("Hiba torentet vevo ArrayBlockingQueue helyezesekor.");
                        e.printStackTrace();
                    }
                    customer.threadFinished();
                });
                customerThread.start();
            } else {
                if(isClosed) {
                    this.unServedCustomersCounter_becauseShopIsClosed++;
                    System.out.println("Egy vevo nem lett kiszolgalva, mert zarva volt az uzlet.\n");
                } else {
                    this.unServedCustomersCounter_becauseShopIsFull++;
                    System.out.println("Egy vevo nem lett kiszolgalva, mert tele volt az uzlet varakozokkal!\n");
                }
            }
        }            
    }
    /**
     * Ebben az eljárásban határozzuk meg a két fodrász, hogy dolgoznak, és elkészítjük a két thread-et számukra.
     * 
     * Semaphore-ra azért van szükség, hogy a két fodrász egyszerre ne szolgálhassa ki véletlenül ugyanazt a személyt. 
     * 
     * 
     * KEPZETT FODRASZ = A fodrász, aki hajat és szakállat is tud vágni.
     * CSAK HAJAVAGO FODRASZ = A fodrász, aki feladat szerint, csak hajat tud vágni.
     * 
     * Meg lehetett volna oldani ezt ennyi kódismétlés nélkül, de ahhoz rengeteg gettert kellett volna írjak ebbe az osztályba, hogy
     * a barberer osztály minden eshetőségről értesüljön.
     * Remélem ez a kis "lustaságom" nem fogja okozni az emelt beadandó elbukását. *Hoping face*
     */
    private void barberersBehavior() {
        Semaphore sem = new Semaphore(1);

        Thread barbererThread_hairCut_and_beardCut = new Thread(() -> {
            boolean justOpened = true;
            boolean justClosed = true;

            while(this.dayCounter <= SIMULATION_DAYS_LASTS_FOR) {
                waitOneMsec(); // szinkronizálom a főprogram futásával (ellenkező esetben óriási processzorigényt eredményez, mivel ez a szál gyorsabban akar futni, mint a főprogram szála)

                if(this.isClosed && waitingCustomersQueue.size() == 0 && barberer_hairCut_and_beardCut.barbererState == Barberer.BarbererStateEnum.AT_HOME) {
                    justOpened = true;
                    if(justClosed) {
                        justClosed = false;
                        System.out.println("A KEPZETT FODRASZ otthon van.");
                    }

                } else {
                    if(justOpened) {
                        justOpened = false;
                        System.out.println("A KEPZETT FODRASZ megerkezett az uzletbe.\n");
                    }

                    synchronized(CUSTOMER_IS_WAITING_LOCK) { //Ha érkezik vevő, vagy lejár a munkaidő és nincs több várakozó a szál folytatja futását.
                        while(waitingCustomersQueue.size() == 0 && !this.isClosed) {
                            barberer_hairCut_and_beardCut.barbererState = Barberer.BarbererStateEnum.WAITING_FOR_CUSTOMERS;

                            try {
                                System.out.println("A KEPZETT FODRASZ varakozik vevokre...\n");
                                CUSTOMER_IS_WAITING_LOCK.wait();
                            } catch (InterruptedException e) {
                                System.err.println("Hiba tortent fodrasz varakozasakor.");
                            }
                        }
                    }
                    if(this.isClosed && waitingCustomersQueue.size() == 0 && !justClosed) {
                        justClosed = true;
                        barberer_hairCut_and_beardCut
                            .getServicedCustomerPerDayCollection()
                            .add(barberer_hairCut_and_beardCut.getServicedCustomer());
                        barberer_hairCut_and_beardCut.setServicedCustomerToZero();
                        barberer_hairCut_and_beardCut.barbererState = Barberer.BarbererStateEnum.AT_HOME;

                        System.out.println("[MUNKANAP VEGE]: A KEPZETT FODRASZ kiszolgalta az utolso vevojet is, es letelt a munkaideje, igy elindul haza.");
                    } else {
                        Customer c = null; //Mivel mindkét szál értesítésre kerül, ha érkezik egy vevő, így mindkét szálnak le fog futni ez a kódsra, de csak az egyik válalhatja, a másik lehet, hogy null-t vesz ki a sorból, így le kell kezelni ezeketet az eshetőségeket.

                        try {
                            sem.acquire();
                            c = waitingCustomersQueue.poll(); //Azért poll, mivel nem szeretném, hogy addig várakozzon egy szál, amig valaki be nem kerul a sorba. take()-esetben elakadhat a szál, ha minden pont úgy alakul (igaz, nagyon ritka)
                            sem.release(); // Elengedjük, ha végzett.
                        } catch (InterruptedException e) {
                            System.err.println("Hiba tortent a szemafor megszerzesekor, vagy ArrayBlockQueue take() hivasakor.");
                            e.printStackTrace();
                        } finally {
                            sem.release();
                        }
                        
                        if(c != null) {
                            System.out.println("A KEPZETT FODRASZ hivja a kovetkezo varakazo szemelyt. ("+ c.getName() + " kovetkezik)");

                            c.customerState = Customer.CustomerStateEnum.UNDER_SERVICE;
                            barberer_hairCut_and_beardCut.barbererState = Barberer.BarbererStateEnum.GIVING_SERVICE;
    
                            int serviceTime = calculateServiceTime(c);
                            System.out.println("A KEPZETT FODRASZ kiszolgalta a vevot "  + serviceTime + " msec alatt, " + c.getName() + " boldogan tavozik.\n");
                            c.customerState = Customer.CustomerStateEnum.GOING_HOME;  

                            barberer_hairCut_and_beardCut.setServicedCustomer(barberer_hairCut_and_beardCut.getServicedCustomer() + 1);
                            barberer_hairCut_and_beardCut.barbererState = Barberer.BarbererStateEnum.WAITING_FOR_CUSTOMERS;
                        }
                    }
                }
            }
            barbererThread1_isFinished = true; //szinkronizációs okokból tettem ezt be, hogy 10000 lefutás esetén se történhessen meg az az eshetőség, hogy a szimuláció vége előbb történik meg, mint a szál lefutásának a vége. (Meg lehetett volna oldani ezt "konkurensebben" is, de azzal valószínűleg nem lenne jobb, legfeljebb elegánsabb és jóval több kódsort eredményzett volna)
        });
        Thread barbererThread_onlyHairCut = new Thread(() -> {
            boolean justOpened = true;
            boolean justClosed = true;

            while(this.dayCounter <= SIMULATION_DAYS_LASTS_FOR) {
                waitOneMsec(); // szinkronizálom a főprogram futásával (ellenkező esetben óriási processzorigényt eredményez, mivel ez a szál gyorsabban akar futni, mint a főprogram szála)

                if(this.isClosed && waitingCustomersQueue.size() == 0 && barberer_onlyHairCut.barbererState == Barberer.BarbererStateEnum.AT_HOME) {
                    justOpened = true;
                    if(justClosed) {
                        justClosed = false;
                        System.out.println("A CSAK HAJAT VAGO FODRASZ otthon van.");
                    }

                } else {
                    if(justOpened) {
                        justOpened = false;
                        System.out.println("A CSAK HAJAT VAGO FODRASZ megerkezett az uzletbe.\n");
                    }

                    synchronized(CUSTOMER_IS_WAITING_LOCK) { //Ha érkezik vevő, vagy lejár a munkaidő és nincs több várakozó a szál folytatja futását.
                        while(waitingCustomersQueue.size() == 0 && !this.isClosed) {
                            barberer_onlyHairCut.barbererState = Barberer.BarbererStateEnum.WAITING_FOR_CUSTOMERS;

                            try {
                                System.out.println("A CSAK HAJAT VAGO FODRASZ varakozik vevokre...\n");
                                CUSTOMER_IS_WAITING_LOCK.wait();
                            } catch (InterruptedException e) {
                                System.err.println("Hiba tortent fodrasz varakozasakor.");
                            }
                        }
                    }
                    if(this.isClosed && waitingCustomersQueue.size() == 0 && !justClosed) {
                        justClosed = true;
                        barberer_onlyHairCut
                            .getServicedCustomerPerDayCollection()
                            .add(barberer_onlyHairCut.getServicedCustomer());
                        barberer_onlyHairCut.setServicedCustomerToZero();
                        barberer_onlyHairCut.barbererState = Barberer.BarbererStateEnum.AT_HOME;

                        System.out.println("[MUNKANAP VEGE]: A CSAK HAJAT VAGO FODRASZ kiszolgalta az utolso vevojet is, es letelt a munkaideje, igy elindul haza.");
                    } else {
                        Customer c = null; //Mivel mindkét szál értesítésre kerül, ha érkezik egy vevő, így mindkét szálnak le fog futni ez a kódsra, de csak az egyik válalhatja, a másik lehet, hogy null-t vesz ki a sorból, így le kell kezelni ezeketet az eshetőségeket.
                        
                        try {
                            sem.acquire();
                            c = waitingCustomersQueue.peek();
                            if(c != null && !c.isCustomerWantBeardTrimming()) { // Megkérdezi a vevőt, hogy akar-e szakállnyírást, ha nem, akkor nem csinál semmit / továbbvárakozik.
                                c = waitingCustomersQueue.poll(); //Azért poll, mivel nem szeretném, hogy addig várakozzon egy szál, amig valaki be nem kerul a sorba. take()-esetben elakadhat a szál, ha minden pont úgy alakul (igaz, nagyon ritka) 
                            }
                            sem.release(); // Elengedjük, ha végzett.
                        } catch (InterruptedException e) {
                            System.err.println("Hiba tortent a szemafor megszerzesekor, vagy ArrayBlockQueue take() hivasakor.");
                            e.printStackTrace();
                        } finally {
                            sem.release();
                        }
                        
                        if(c != null) {
                            System.out.println("A CSAK HAJAT VAGO FODRASZ hivja a kovetkezo varakazo szemelyt. ("+ c.getName() + " kovetkezik)");
                            c.customerState = Customer.CustomerStateEnum.UNDER_SERVICE;
                            barberer_onlyHairCut.barbererState = Barberer.BarbererStateEnum.GIVING_SERVICE;
    
                            int serviceTime = calculateServiceTime(c);
                            System.out.println("A CSAK HAJAT VAGO FODRASZ kiszolgalta a vevot "  + serviceTime + " msec alatt, " + c.getName() + " boldogan tavozik.\n");
                            c.customerState = Customer.CustomerStateEnum.GOING_HOME;  

                            barberer_onlyHairCut.setServicedCustomer(barberer_onlyHairCut.getServicedCustomer() + 1);
                            barberer_onlyHairCut.barbererState = Barberer.BarbererStateEnum.WAITING_FOR_CUSTOMERS;
                        }
                    }
                }
            }
            barbererThread2_isFinished = true; //szinkronizációs okokból tettem ezt be, hogy 10000 lefutás esetén se történhessen meg az az eshetőség, hogy a szimuláció vége előbb történik meg, mint a szál lefutásának a vége. (Meg lehetett volna oldani ezt "konkurensebben" is, de azzal valószínűleg nem lenne jobb, legfeljebb elegánsabb és jóval több kódsort eredményzett volna)
        });
        barbererThread_onlyHairCut.start();
        barbererThread_hairCut_and_beardCut.start();
    }

    /**
     * Függvény arra szolgál, hogy minden msec-ben leellenőrizze, hogy elértük-e a nyitva tartás idejét...
     * ...ha igen a üzletet megnyitjuk, ha nem, bezárjuk.
     * @param msec A szimulációból megkapott msec számláló. 
     */
    private void checkTime(int msec) {
        if(msec < SIM_HOUR * OPEN_AT || msec > SIM_HOUR * CLOSE_AT) {
            synchronized(CUSTOMER_IS_WAITING_LOCK) {
                this.isClosed = true;
                CUSTOMER_IS_WAITING_LOCK.notifyAll(); //Azért szükséges, hogyha lejár a munkaidő, de a fodrászok még mindig várakoznak új vevőkre, akkor hazamenjenek végre...
            }
        } else {
            this.isClosed = false;
        }
    }

    /**
     * Függvény arra szolgál, hogy minden msec-ban egy véletlenszámot generáljon 1-10000 között,
     * majd leellenőrizze, hogy az adott szám kisebb-e, mint a megadott érték. 
     * Vevők, ha zárva az üzlet (tipikusan akkor, amikor mindenki alszik) ritkábban jönnek.
     * @return  Igaz, ha kisebb mint CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_CLOSED / OPEN értéke... 
     *              ...így szimulálhatjuk, hogy egy vevő érkezne az üzletbe
     *          Hamis, ha nagyobb vagy egyenlő, mint CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_CLOSED / OPEN értéke...
     *              ...ekkor nem érkezik vevő az üzletbe.
     */
    private boolean customerIsComing() {
        if(this.isClosed) { // Ha zárva van az üzlet, kisebb eséllyel jönnek a vevők 
            return ThreadLocalRandom.current().nextInt(1,10000) < CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_CLOSED;
        }
        return ThreadLocalRandom.current().nextInt(1,10000) < CUSTOMER_COMING_CHANCE_WHEN_BARBERSHOP_IS_OPEN;
    }

    /**
     * Eljárás arra szolgál, hogy leellenőrizze, hogy eltelt-e egy nap, ha igen megnöveli a napok számát, majd kiírja 
     * a standard output-ra, hogy eltelt egy nap, és következik a következő nap.
     * @param msec A szimulációból megkapott msec számláló. 
     */
    private void checkEndDay(int msec) {
        if(msec >= SIM_HOUR * 24) {
            this.msecCounter = 0;
            this.dayCounter++;

            if(dayCounter <= 5) {
                System.out.println("\n-----Eltelt egy nap. Kovetkezik a(z) " + dayCounter + ". nap.-----\n");
            }
        }
    }
    /**
     * Függvény arra szolgál, hogy várakoztassa a program / egy szál futását min és max msec között véletlenszerűen.
     * Akkor használom, mikor egy customer thread éppen szolgáltatást kap, és barberer egyik szála szolgáltatást ad.
     * @param min legkevesebb msec, amíg várakozik a program / szál.
     * @param max legnagyobb msec, amíg várakozik a program / szál.
     * @return msec érték, amennyi idő alatt végezni fog a fodrász (ez a szám kiírása kerül).
     */
    private int waitForMsec(int min, int max) {
		int msec = ThreadLocalRandom.current().nextInt(min, max);
		try {
			TimeUnit.MILLISECONDS.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        return msec;
	}

    /**
     * Segédfüggvény, arra szolgál, hogy pontosan meghatározhassuk, hogy meddig fog tartani egy adott szolgálatás.
     * Ha egy customer két szolgálatást is kér, úgy a hajvágás és a szakállnyírás ideje összeadódik. 
     * @param c Customer, aki a szolgálatás(okat) kéri.
     * @return Az összesített szolgálatási idő
     */
    private int calculateServiceTime(Customer c) {
        int serviceTime = 0;
        if(c.isCustomerWantBeardTrimming() || c.isCustomerWantHairCut()) {
            if(c.isCustomerWantHairCut()) {
                serviceTime += waitForMsec(MIN_SERVICE_TIME, MAX_SERVICE_TIME);
            }
            if(c.isCustomerWantBeardTrimming()) {
                serviceTime += waitForMsec(MIN_SERVICE_TIME, MAX_SERVICE_TIME);
            }
        }
        return serviceTime;
    }
    /**
     * Eljárás arra szolgál, hogy a főprogram futását 1 msec-cel (változtatható) várakoztassa...
     * ...hogy szimuláció feladatnak megfelelően működjön.
     */
    private void waitOneMsec() {
		try {
			TimeUnit.MILLISECONDS.sleep(SIMULATION_SEC_PER_MSCEC);
		} catch (InterruptedException e) {
            System.err.println("Hiba történet 1 msces várakozás során!");
			e.printStackTrace();
		}
	}
}
