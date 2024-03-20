import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Comanda implements Runnable{
    private String ID;
    private int nr_produse_total,nr_produse_ramase;
    private int nr_threaduri_procesare;
    ExecutorService exec;
    Semaphore sem;
    Comanda(String ID, int nr_produse_total, int nr_threaduri_procesare, ExecutorService exec,int nr_produse_ramase,
        Semaphore sem){
        this.ID=ID;
        this.nr_produse_total=nr_produse_total;
        this.nr_produse_ramase=nr_produse_ramase;
        this.nr_threaduri_procesare=nr_threaduri_procesare;
        this.exec=exec;
        this.sem=sem;
    }
    @Override
    public void run() {
        try {
            //ne pregatim sa prelucram produsele
            BufferedReader br = new BufferedReader(new FileReader(Tema2.fisier_produse));
            String thread_si_comanda=Thread.currentThread().getName()+","+ID;
            //vedem cate produse a prelucrat thread-ul curent care a primit comanda data
            Integer produse_prelucrate = Tema2.limitare_produse_comenzi.get(thread_si_comanda);
            int prag;
            //vom actualiza numarul de produse care raman
            AtomicInteger aux1 = new AtomicInteger(nr_produse_ramase);
            String linie;
            /*
             * Acest atomic integer
            */
            AtomicInteger numarare_linii = new AtomicInteger();
            if(produse_prelucrate==null){
                AtomicInteger aux = new AtomicInteger();//thread-ul vede comanda pentru prima data
                /*
                 * Din motive de precauție, parcurgerea linie cu linie a fișierului
                 * se consideră zonă critică.
                */
                synchronized(this){
                prag=nr_produse_total/nr_threaduri_procesare+1;
                while(((linie=br.readLine())!=null)&&(aux.get()<prag)&&(aux1.get()>0)){
                    String[] componente = linie.split(",");
                    numarare_linii.incrementAndGet();
                    if((componente[0].equals(ID))&&(!Tema2.Comanda_produs_procesat.contains(numarare_linii.get()+"-"+linie))){
                        if(!Tema2.Comanda_produs_procesat.isEmpty())
                            Tema2.bw.write('\n');
                        Tema2.Comanda_produs_procesat.add(numarare_linii.get()+"-"+linie);
                        aux.incrementAndGet();//am gasit o comanda si crestem contorul
                        aux1.decrementAndGet();//am verificat un produs
                        Tema2.nr_produse_procesate.incrementAndGet();
                        Tema2.bw.write(linie+",shipped");
                    }
                }
            }
            Tema2.limitare_produse_comenzi.put(thread_si_comanda, aux.get());//actualizam intrarea in map
            }
            else{
                /*dacă thread-ul are o intrare pe comanda dată
                Preluăm într-un AtomicInteger numărul de produse pe care le-a putut prelucra
                și continuăm cu parcurgerea.
                */
                AtomicInteger aux = new AtomicInteger(produse_prelucrate.intValue());
                synchronized(this){
                    prag=nr_produse_total/nr_threaduri_procesare+1;
                    while(((linie=br.readLine())!=null)&&(aux.get()<prag)&&(aux1.get()>0)){
                        String[] componente = linie.split(",");
                        numarare_linii.incrementAndGet();
                        if((componente[0].equals(ID))&&(!Tema2.Comanda_produs_procesat.contains(numarare_linii.get()+"-"+linie))){
                            if(!Tema2.Comanda_produs_procesat.isEmpty())
                                Tema2.bw.write('\n');
                            Tema2.Comanda_produs_procesat.add(numarare_linii.get()+"-"+linie);
                            aux.incrementAndGet();
                            aux1.decrementAndGet();
                            Tema2.nr_produse_procesate.incrementAndGet();
                            Tema2.bw.write(linie+",shipped");
                        }

                    }
                }
               
                Tema2.limitare_produse_comenzi.replace(thread_si_comanda, aux.get());
            }
            br.close();
            //trimitem task-uri în continuare
            exec.submit(new Comanda(ID,nr_produse_total,nr_threaduri_procesare, exec, aux1.get(),sem));
            /*
             * Dacă numărul de produse rămase a ajuns la 0, înseamnă că s-a putut prelucra cu succes comanda,
             * drept pentru care eliberăm semaforul care ținea thread-ul responsabil de comandă îmn repaus.
             * Înseamnă că, odată cu ridicarea semaforului, comanda se consideră procesată.
            */
            if(aux1.get()==0)
                sem.release();
            if((Tema2.nr_produse_procesate.get()==Tema2.nr_produse_total.get()&&(Tema2.procesare_totala.get()!=0))){
                exec.shutdown();
                Tema2.bw.close();
            }
       
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
