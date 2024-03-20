import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {
    public static int NR_THREADURI; 
    public static ArrayList<String>liniesiThread = new ArrayList<>();
    public static String fisier_produse;
    /*
     * Am avut nevoie de niște variabile de tip AtomicInteger pentru a face numărătoarea corectă a produselor.
     *      nr_produse_total este un AtomicInteger care reține numărul total al produselor pe parcursul citirii
     * fișierului orders.txt, având în vedere că în acel fișier se dă numărul de produse al fiecărei comenzi
     *      nr_produse_procesate este un AtomicInteger prin care facem o numărare paralelă a produselor în curs
     * de procesare de către thread-urile din ExecutorService-ul de procesare a produselor. Se dorește ca ambele
     * numere să coincidă după ce s-a prelucrat ultimul produs.
     *      procesare_totala este un AtomicInteger prin care se confirmă faptul că nu mai avem comenzi de procesat
     * (am ajuns la finalul fișierului orders.txt)
     */
    public static AtomicInteger nr_produse_total = new AtomicInteger();
    public static AtomicInteger nr_produse_procesate = new AtomicInteger();
    public static AtomicInteger procesare_totala = new AtomicInteger();
    /*
     * pentru că doresc să țin evidența fiecărui produs procesat, le voi pune într-o listă.
     * Această lista este folosită la nivelul de procesare produse și conține comanda,produsul, dar și
     * linia în care s-a întâlnit comanda și produsul, având în vedere că pot exista duplicate,
     * repetându-se o comandă și un produs de mai multe ori.
    */
    public static List<String>Comanda_produs_procesat = Collections.synchronizedList(new ArrayList<>());
    /*
     * Cu acest ConcurrentHashMap vreau ca, în cadrul unei comenzi, thread-urile din work-pool-ul responsabil
     * de produse să ia maxim nr_produse/nr_threaduri+1. Am ales asta întrucât, dacă încercam să impun 
     * limita nr_produse/nr_threaduri, ar fi fost mai dificil în acel moment să știu cine să ia alte produse,
     * având în vedere că numărul de thread-uri nu este întotdeauna un divizor al nr_produse. De aceea,
     * maximul nr_produse/nr_threaduri+1 oferă siguranța și certitudinea că fiecare thread care va procesa o 
     * comandă va lua un număr de produse până la un maxim admis.
    */
    public static ConcurrentHashMap<String,Integer> limitare_produse_comenzi = new ConcurrentHashMap<>();
    public static BufferedWriter bw,bwComenzi;
    public static void main(String[] args) throws IOException {
        File DirectorFisiereIntrare = new File(args[0]);
        File[] FisiereledeIntrare= DirectorFisiereIntrare.listFiles();
        String fisier1 = FisiereledeIntrare[0].getName();
        String fisier_comenzi;
        if(fisier1.equals("orders.txt"))
            fisier_comenzi=FisiereledeIntrare[0].getAbsolutePath();
        else
            fisier_comenzi=FisiereledeIntrare[1].getAbsolutePath();;
        
        if(fisier1.equals("order_products.txt"))
            fisier_produse=FisiereledeIntrare[0].getAbsolutePath();
        else
            fisier_produse=FisiereledeIntrare[1].getAbsolutePath();

        NR_THREADURI=Integer.parseInt(args[1]);
        BufferedReader fisierul_cu_comenzi = new BufferedReader(new FileReader(fisier_comenzi));
        ExecutorService exec = Executors.newFixedThreadPool(NR_THREADURI);
        ExecutorService procesare_produse = Executors.newFixedThreadPool(NR_THREADURI);
        AtomicInteger totalLinii = new AtomicInteger();
        bw = new BufferedWriter(new FileWriter("order_products_out.txt"));
        bwComenzi = new BufferedWriter(new FileWriter("orders_out.txt"));
        /*
         * Acest ultim ConcurrentHashMap are un rol similar celui descris mai sus,
         * asigurându-ne că thread-urile din work-pool-ul comenzilor o să ia un număr de 
         * comenzi până la un maxim
        */
        ConcurrentHashMap<String,Integer> mapare = new ConcurrentHashMap<>();
        exec.submit(new CitireComenzi(fisierul_cu_comenzi, exec,totalLinii,mapare,procesare_produse));
        
        
    }
}
