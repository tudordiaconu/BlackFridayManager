import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderThread implements Runnable {
    private int offset;
    private int readCapacity;
    private ExecutorService tpe;
    private final String folder;

    public OrderThread(int offset, int readCapacity, ExecutorService tpe, String folder) {
        this.offset = offset;
        this.readCapacity = readCapacity;
        this.tpe = tpe;
        this.folder = folder;
    }


    @Override
    public void run() {
        String ordersSource = this.folder + "/orders.txt";
        File ordersInput = new File(ordersSource);
        RandomAccessFile orderReader = null;

        // initializing the reader for the orders' input file
        try {
            orderReader = new RandomAccessFile(ordersInput, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        StringBuilder content = new StringBuilder();
        // case when we start reading from the beginning of the file
        if (offset == 0) {
            try {
                orderReader.seek(0);
            } catch (IOException e) {
                System.out.println("Offset bigger than file size");
            }
            try {
                readLines(orderReader, content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            // checking if the previous char is '\n', which would mean we're
            // at the beginning of a new line
            try {
                orderReader.seek(offset - 1);
            } catch (IOException e) {
                System.out.println("Offset bigger than file size");
            }

            char c = 0;
            try {
                c = (char) orderReader.readByte();
            } catch (IOException e) {
                System.out.println("cannot read");
            }

            if (c == '\n') {
                try {
                    readLines(orderReader, content);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // if we're not at the beginning of a new line, we move
                // our offset until we get to the next line and then we
                // start reading
                while (c != '\n') {
                    try {
                        c = (char) orderReader.readByte();
                        readCapacity--;
                    } catch (IOException e) {
                        break;
                    }
                }

                try {
                    readLines(orderReader, content);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // function that reads the amount of lines that the threads is made to read
    private void readLines(RandomAccessFile raf, StringBuilder content) throws IOException {
        Path orderPath = Paths.get("orders_out.txt");

        String[] arrOfString;
        while (readCapacity > 0) {
            char s;

            // reading one byte at a time
            try {
                s = (char) raf.readByte();
                content.append(s);
                readCapacity--;
            } catch (IOException e) {
                break;
            }

            // if finished reading and the offset is not placed at the beginning of
            // a new line, we continue reading while we get to the end of the current line
            if (readCapacity == 0 && s != '\n') {
                readCapacity++;
            }
        }

        if (!content.isEmpty()) {
            // getting each line
            arrOfString = String.valueOf(content).split("\n");

            for (var string : arrOfString) {
                // getting each orderId and its order' number of products
                String[] orderAndProducts = string.split(",");

                int productsNumber = Integer.parseInt(orderAndProducts[1]);

                // atomic variable that counts how many products of the order have been shipped
                AtomicInteger shippedProductsNumber = new AtomicInteger(0);
                
                // submitting a task for each product belonging to the order
                for (int i = 0; i < Integer.parseInt(orderAndProducts[1]); ++i) {
                    tpe.submit(new ProductThread(orderAndProducts[0], shippedProductsNumber, i, this.folder));
                }

                // the order thread waits until all its products are shipped
                while(true) {
                    if (productsNumber == 0) {
                        break;
                    }

                    // when all the products are shipped, then the order is shipped
                    if (productsNumber == shippedProductsNumber.intValue()) {
                        Files.writeString(orderPath, string + ",shipped\n", StandardOpenOption.APPEND);
                        break;
                    }
                }
            }
        }
    }
}
