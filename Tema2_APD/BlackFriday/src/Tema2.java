import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tema2 {
    public static void main(String[] args) throws IOException, InterruptedException {
        // extracting the folder from the arguments
        String folder = args[0];
        Path path = Paths.get(folder + "/orders.txt");

        String ordersDest = "orders_out.txt";
        String productsDest = "order_products_out.txt";

        // emptying the orders output file so that we can write the new results
        PrintWriter writer = new PrintWriter(ordersDest);
        writer.print("");
        writer.close();

        // emptying the products output file
        writer = new PrintWriter(productsDest);
        writer.print("");
        writer.close();

        // extracting the maximum number of threads per level from the arguments
        int numThreads = Integer.parseInt(args[1]);
        Thread[] threads = new Thread[numThreads];

        // executor service for writing tasks for products' threads
        ExecutorService tpe = Executors.newFixedThreadPool(numThreads);

        // calculating the number of bytes of the input file and the capacity
        // that each thread reads
        long bytes = Files.size(path);
        int threadCapacity = (int)bytes / numThreads;

        // creating the threads that process the orders
        for (int i = 0; i < numThreads; ++i) {
            threads[i] = new Thread(new OrderThread(i * threadCapacity,
                    threadCapacity, tpe, folder));
            threads[i].start();
        }

        // joining the threads that process the orders
        for (int i = 0; i < numThreads; ++i) {
            threads[i].join();
        }

        // when the order threads are done writing, the executor
        // service shuts down
        tpe.shutdown();
    }
}
