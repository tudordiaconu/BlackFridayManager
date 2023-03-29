import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductThread implements Runnable{
    private String orderId;
    private AtomicInteger shippedProductsNumber;
    private int productCounter;
    private final String folder;

    public ProductThread(String orderId, AtomicInteger shippedProductsNumber,
                         int productCounter, String folder) {
        this.orderId = orderId;
        this.shippedProductsNumber = shippedProductsNumber;
        this.productCounter = productCounter;
        this.folder = folder;
    }

    @Override
    public void run() {

        Path outputPath = Paths.get("order_products_out.txt");
        String productsSource = this.folder + "/order_products.txt";
        File productsInput = new File(productsSource);
        BufferedReader productReader = null;

        // initializing the reader for the products' input file
        try {
            productReader = new BufferedReader(new FileReader(productsInput));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        String productLine;
        try {
            int count = this.productCounter;

            // reading each line
            while ((productLine = productReader.readLine()) != null) {
                // getting the orderId from the line
                String[] arrOfStrings = productLine.split(",");
                String currentOrder = arrOfStrings[0];

                // checking if current product belongs to this task (has the same orderId)
                if (Objects.equals(currentOrder, this.orderId)) {
                    // checking if we are on the first product that hasn't already been shipped
                    if (count == 0) {
                        Files.writeString(outputPath, productLine + ",shipped\n", StandardOpenOption.APPEND);
                        shippedProductsNumber.incrementAndGet();

                        break;
                    } else {
                        // we are on a product that has been shipped, we need to go to the next one
                        count--;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error in reading");
        }
        
        try {
            productReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
