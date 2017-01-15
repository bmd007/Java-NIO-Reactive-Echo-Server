package p2_server_side;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Handler implements Runnable {

    private final SocketChannel _socketChannel;
    private final SelectionKey _selectionKey;

    private static final int READ_BUF_SIZE = 1024;
    private static final int WRiTE_BUF_SIZE = 1024;

    private ByteBuffer _readBuf = ByteBuffer.allocate(READ_BUF_SIZE);
    private ByteBuffer _writeBuf = ByteBuffer.allocate(WRiTE_BUF_SIZE);


    public Handler(Selector selector, SocketChannel socketChannel) throws IOException {

        _socketChannel = socketChannel;
        _socketChannel.configureBlocking(false);

        // Register _socketChannel with _selector listening on OP_READ events.
        // Callback: Handler, selected when the connection is established and ready for READ

        _selectionKey = _socketChannel.register(selector, SelectionKey.OP_READ);
        _selectionKey.attach(this);

        selector.wakeup(); // let blocking select() return

    }

    public void run() {
        if (_selectionKey.isReadable())
            try {
                read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        else if (_selectionKey.isWritable())
            write();
    }

    // Process data by echoing input to output
    synchronized void process() throws IOException {
        _readBuf.flip();

        byte[] bytes = new byte[_readBuf.remaining()];

        _readBuf.get(bytes, 0, bytes.length);

        String received = new String(bytes, Charset.forName("UTF-8"));

        if (!received.equals("finished")) {

            System.out.println("Received=>" + received);

            String[] splitOfReceived = received.split(",");

            System.out.println("Split=>" + splitOfReceived[0]);

            String answer = "";

            if (splitOfReceived != null && splitOfReceived.length == 2 && isValidInput(splitOfReceived[0], splitOfReceived[1])) {

                if (splitOfReceived[0].length()<2) {

                    String urlOfService = "http://localhost:8080/";

                    if (splitOfReceived[0].equals("L"))
                        urlOfService += "library/" + splitOfReceived[1];

                    else if (splitOfReceived[0].equals("D"))
                        urlOfService += "dominant/" + splitOfReceived[1];

                    else if (splitOfReceived[0].equals("P"))
                        urlOfService += "pool/" + splitOfReceived[1];


                    URL url = new URL(urlOfService);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() != 200)
                        answer = "A problem has reported from services which leads to malfunction";
                    else {

                        InputStream is = conn.getInputStream();

                        while (is.available() <= 0) ;
                        byte bytesss[] = new byte[1024];
                        while (-1 != is.read(bytesss)) {
                        }
                        answer = new String(bytesss);

                        conn.disconnect();
                    }

                } else {
                    //connect to services with socket directly
                    // this connection could be used more more efficient

                    Socket socketToServices = null;

                    socketToServices = new Socket();
                    socketToServices.connect(new InetSocketAddress("192.168.1.9", 55555));

                    OutputStream bw = socketToServices.getOutputStream();
                    InputStream br = socketToServices.getInputStream();

                    bw.write(received.getBytes());

                    byte bytessss[] = new byte[1024];

                    br.read(bytessss);

                    answer = String.valueOf(bytessss);

                    System.out.println("Answer from socket service is:"+ answer);

                    bw.write("finished".getBytes());

                    bw.flush();
                    bw.close();
                    br.close();
                    socketToServices.close();
                }


            } else
                answer = "Wrong Input";

            System.out.println("Answer to send=>" + answer);


            _writeBuf = ByteBuffer.wrap(answer.getBytes());

            // Set the key's interest to WRITE operation
            _selectionKey.interestOps(SelectionKey.OP_WRITE);
            _selectionKey.selector().wakeup();

        } else {

            _selectionKey.cancel();
            _socketChannel.close();
        }

    }

    boolean isValidInput(String firstInput, String secondInput) {
        if (
                (
                        firstInput.equals("L") || firstInput.equals("P") || firstInput.equals("D")
                                ||
                                firstInput.equals("L@") || firstInput.equals("P@") || firstInput.equals("D@")
                )
                        &&
                        (secondInput.length() == 7)
                )
            return true;
        else
            return false;
    }

    synchronized void read() throws IOException {

        int numBytes = _socketChannel.read(_readBuf);

        if (numBytes == -1) {
            _selectionKey.cancel();
            _socketChannel.close();
            System.out.println("read(): client connection might have been dropped!");
        } else
            ReactoreServer.getWorkerPool().execute(new Runnable() {
                public void run() {
                    try {
                        process();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
    }


    void write() {
        int numBytes = 0;

        try {
            numBytes = _socketChannel.write(_writeBuf);
            System.out.println("Number of written bytes::" + numBytes);
            if (numBytes > 0) {
                _readBuf.clear();
                _writeBuf.clear();

                // Set the key's interest-set back to READ operation
                _selectionKey.interestOps(SelectionKey.OP_READ);
                _selectionKey.selector().wakeup();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
