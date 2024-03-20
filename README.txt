331CB VÎRLAN Mihnea-Alexandru

                                Tema 2 - APD - Detalii implementare

    Aceasta tema contine 3 clase Java: Tema2, CitireComenzi și Comanda. Deși numele ultimelor 2 clase poate duce
la o ambiguitate, fiecare dintre acestea are un rol diferit, rol ce va fi explicat pe parcurs.
    În clasa Tema2, se face citirea celor două fișiere și se pregătesc 2 instanțe ale ExecutorService,
una care se ocupă de citirea comenzilor din fișierul orders.txt, iar cealaltă, prin colaborare cu prima
prelucrează produsele corespunzătoare fiecărei comenzi. Am introdus și câteva ConcurrentHashMap-uri pentru 
a mă asigura că există un echilibru când thread-urile aleg ce comenzi vor urmări, respectiv câte produse din
fiecare comandă vor fi gestionate.
    În clasa CitireComenzi are loc prelucrarea fiecărei linii din fișierul de comenzi. Verificăm mai întâi că
thread-ul care vrea să ia comanda a depășit sau nu numărul de comenzi maxim permis (adică dacă a atins un prag).
În calcularea valorii maxime, nr_total_linii/nr_threaduri+1, se ține cont de faptul că numărul de linii se
schimbă de fiecare dată când avansăm. De aceea, dacă un thread consideră că a atins un maxim local, alte thread-uri
care în map au valoarea asociată numărului de comenzi citite mai mic, vor vedea că sunt sub pragul maxim local și
în felul acesta îl vom actualiza.
    Dacă nu avem o mapare thread- număr comenzi, adăugăm una în care valoarea acestei chei 
(numele thread-ului) este initial 1. Dacă numărul de produse asociat comenzii este strict pozitiv,
se procedează astfel: trimitem un task către nivelul de procesare produse (un ExecutorService), task la care adăugăm 
și un semafor pentru a semnaliza că threadul care a citit comanda va scrie că acea comandă a fost 
procesată/expediată (shipped), doar după ce toate
produsele asociate comenzii au fost procesate, echivalent cu a trece de acquire. Semaforul este setat inițial pe 0.
    În clasa Comanda se procesează produsele specifice unei comenzi. Deoarece ne dorim ca numărul de comenzi să fie
distribuit într-o modalitate cât mai echilibrată între thread-uri, actualizez într-un ConcurrentHashMap produsele
procesate de fiecare thread corespunzător unei comenzi. Păstrăm într-o listă comanda, produsul și numărul liniei în
care se află, un remediu pentru situația duplicatelor din fișier. Parcurgem fișierul linie cu linie și dacă găsim 
o intrare comandă,produs care se potrivește cu ID-ul comenzii, intrare care nu se regăsește în listă, aceasta este 
adăugată și se consideră că produsul a fost procesat cu succes. În timp ce se face parcurgerea folosesc un 
AtomicInteger pentru a ține evidența numărului de produse ce încă nu au fost procesate 
(pentru că mai multe thread-uri se ocupă de o comandă). Am grijă ca, atunci când nu mai sunt produse de procesat,
să eliberez semaforul pentru a permite ca thread-ul care se ocupă de comandă să-i poată pune statusul shipped.
    Ca o notă de final, fiecare Thread Pool de tip ExecutorService funcționează cu exact P thread-uri. 