import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class CitireComenzi implements Runnable{
    private BufferedReader cititor;
    private ExecutorService exec;
    private ExecutorService procesare_produse;
    private AtomicInteger totalLinii;
    private ConcurrentHashMap<String,Integer> mapare;
    private static AtomicInteger comanda_precedenta= new AtomicInteger();
    CitireComenzi(BufferedReader cititor, ExecutorService exec,AtomicInteger totalLinii,
                ConcurrentHashMap<String,Integer>mapare,ExecutorService procesare_produse){
        this.cititor=cititor;
        this.exec=exec;
        this.totalLinii=totalLinii;
        this.mapare=mapare;
        this.procesare_produse=procesare_produse;
    }

    
    @Override
    public void run() {
        String nume_thread = Thread.currentThread().getName();
        Integer linii_citite_thread = mapare.get(nume_thread);
        int prag = totalLinii.get()/Tema2.NR_THREADURI+1;
        try{
        if(linii_citite_thread==null){
            String linie = cititor.readLine();
            if(linie==null){//nu mai avem ce citi, deci am procesat toate comenzile, deci putem inchide acest ExecutorSerivce
                Tema2.procesare_totala.incrementAndGet();
                Tema2.bwComenzi.close();
                exec.shutdown();
            }
            else{
                if((mapare.size()!=0)&&(comanda_precedenta.get()!=0))
                    Tema2.bwComenzi.write('\n');//if ajutător pentru scriere
                totalLinii.incrementAndGet();
                mapare.put(nume_thread, 1);
                String[] separare_comanda = linie.split(",");
                Semaphore sem = new Semaphore(0);
                int nr_comenzi = Integer.parseInt(separare_comanda[1]);
                comanda_precedenta.set(nr_comenzi);
                /*
                 * Din motive de precauție am considerat că trimiterea unei cereri către
                 * ExecutorService-ul de procesare a produselor este secțiune critică:
                 * nu vreau ca două thread-uri să trimită accidental produse diferite.
                 * 
                 * Exact ca in README se intampla urmatoarele: dacă comanda curentă are un număr
                 * de produse strict mai mare de 0, atunci thread-ul care a preluat comanda este 
                 * responsabil ca aceasta să scrie în fișierul order_products_out.txt statusul shipped,
                 * doar după ce toate produsele au fost verificate. Această condiție este garantată de un semafor
                 * setat initial cu 0. 
                 * 
                 * procesare_produse este un ExecutorService căruia îi trimitem cererea de a
                 * prelucra toate produsele asociate comenzii.
                */
                if(nr_comenzi>0){
                    synchronized(this){
                        Tema2.nr_produse_total.addAndGet(nr_comenzi);
                        Comanda com = new Comanda(separare_comanda[0], nr_comenzi,Tema2.NR_THREADURI,procesare_produse,nr_comenzi,sem);
                        procesare_produse.submit(com); 
                    }
                    sem.acquire();
                    Tema2.bwComenzi.write(separare_comanda[0]+","+nr_comenzi+",shipped");
                }
            }
        }
        else{
            if(linii_citite_thread.intValue()<=prag){
                String linie = cititor.readLine();
                if(linie==null){
                    Tema2.procesare_totala.incrementAndGet();
                    Tema2.bwComenzi.close();
                    exec.shutdown();
                }
                else{
                    if((mapare.size()!=0)&&(comanda_precedenta.get()!=0))
                        Tema2.bwComenzi.write('\n');
                    mapare.remove(nume_thread);
                    mapare.put(nume_thread, linii_citite_thread.intValue()+1);//actualizam numarul de comenzi citite de thread-ul curent
                    totalLinii.incrementAndGet();
                    Semaphore sem = new Semaphore(0);
                    String[] separare_comanda = linie.split(",");
                    int nr_comenzi = Integer.parseInt(separare_comanda[1]);
                    comanda_precedenta.set(nr_comenzi);
                    if(nr_comenzi>0){//analog situației de mai sus
                        synchronized(this){
                            Tema2.nr_produse_total.addAndGet(nr_comenzi);
                            Comanda com = new Comanda(separare_comanda[0], nr_comenzi,Tema2.NR_THREADURI,procesare_produse,nr_comenzi,sem);
                            procesare_produse.submit(com);
                        }
                    sem.acquire();
                    Tema2.bwComenzi.write(separare_comanda[0]+","+nr_comenzi+",shipped");
                    }
                }
            }
        }
        exec.submit(new CitireComenzi(cititor, exec,totalLinii,mapare,procesare_produse));
        //continuam prelucrarea comenzilor si trimitem continuu task-uri acestui executor (executorul comenzilor)
        }
        catch(IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
