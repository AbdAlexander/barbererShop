public class Main {
    public static void main(String[] args) {       
        BarberShop bs = new BarberShop();
        bs.startSimulation();

        /*
            A programot, amikor már teljesen jól működött több mint 100x lefutattam teszterek segítésgével,
            esetleges deadlockok, szinkronizációs hibák megtalálására, de szerencsére nem történt nem várt eredmény.

            Elméleti szinten az összes konkurens elemet következetesen használtam, és 100 lefutáson túl se kellene,
            olyat eredményeznie ennek a programnak, ahol a program rosszul vagy nem konkurensen viselkedne.

            Minden bonyolultabb programegységhez részletes leírást írtam, hogy érthetőbb legyen a gondolkodásmódom. :) 
            Próbálkoztam annyira customizálhatóvá tenni a programot, amennyire csak lehetséges...
            ...a különböző eredmények kihozatal érdekében.
        */
    }
}