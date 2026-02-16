package com.example.myapplication.ui;
import android.os.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import android.util.Log;


public class MyThread extends Thread{

    public static final int TASK_CHECK_CONNECTION = 0;
    public static final int TASK_SEARCH_STORES = 1;
    public static final int TASK_BUY = 2;
    public static final int TASK_RATING = 3;



    Handler handler;
    ArrayList<MyObject> items;
    String serverIp;
    int serverPort;
    int task;
    int rating;
    private String storeName;


    private static final Map<String, String> pictureUrl = new HashMap<>();

    static {
        pictureUrl.put("pepperoni", "https://cdn.pixabay.com/photo/2016/03/05/19/02/pizza-1238246_1280.jpg");
        pictureUrl.put("veggie", "https://th.bing.com/th/id/OIP.fnSnpZO4_MZAaJEc6JVJSAHaLH?cb=iwc2&rs=1&pid=ImgDetMain.jpg");
        pictureUrl.put("margherita", "https://cdn.pixabay.com/photo/2017/12/09/08/18/pizza-3007395_1280.jpg");
        pictureUrl.put("greek salad", "https://www.modernhoney.com/wp-content/uploads/2023/03/Greek-Salad-2-scaled.jpg");
        pictureUrl.put("classic burger", "https://cdn.pixabay.com/photo/2014/10/23/18/05/burger-500054_1280.jpg");
        pictureUrl.put("double cheeseburger", "https://cdn.pixabay.com/photo/2016/03/05/19/02/burger-1238246_1280.jpg");
        pictureUrl.put("chicken burger", "https://cdn.pixabay.com/photo/2017/06/02/18/24/food-2368785_1280.jpg");
        pictureUrl.put("fries", "https://cdn.pixabay.com/photo/2016/03/05/19/02/french-fries-1239246_1280.jpg");
        pictureUrl.put("chicken wings", "https://cdn.pixabay.com/photo/2016/03/27/19/49/chicken-wings-1284357_1280.jpg");
        pictureUrl.put("california roll", "https://cdn.pixabay.com/photo/2017/06/02/18/24/sushi-2368787_1280.jpg");
        pictureUrl.put("salmon nigiri", "https://cdn.pixabay.com/photo/2017/04/15/11/28/salmon-2227793_1280.jpg");
        pictureUrl.put("tuna sashimi", "https://cdn.pixabay.com/photo/2016/03/18/18/56/sashimi-1263163_1280.jpg");
        pictureUrl.put("miso soup", "https://cdn.pixabay.com/photo/2017/02/02/15/43/miso-2030388_1280.jpg");
        pictureUrl.put("pork souvlaki", "https://cdn.pixabay.com/photo/2019/09/19/19/20/souvlaki-4489815_1280.jpg");
        pictureUrl.put("chicken souvlaki", "https://cdn.pixabay.com/photo/2018/04/03/19/39/souvlaki-3290781_1280.jpg");
        pictureUrl.put("chicken burrito", "https://cdn.pixabay.com/photo/2016/11/18/16/44/burrito-1835480_1280.jpg");
        pictureUrl.put("beef taco", "https://cdn.pixabay.com/photo/2016/03/05/19/02/tacos-1238252_1280.jpg");
        pictureUrl.put("nachos", "https://cdn.pixabay.com/photo/2014/12/21/23/28/nachos-575346_1280.jpg");
        pictureUrl.put("vegan wrap", "https://cdn.pixabay.com/photo/2016/03/05/19/02/wrap-1238248_1280.jpg");
        pictureUrl.put("tofu bowl", "https://cdn.pixabay.com/photo/2017/02/06/11/53/bowl-2045071_1280.jpg");
        pictureUrl.put("quinoa salad", "https://cdn.pixabay.com/photo/2017/06/20/19/22/quinoa-2422800_1280.jpg");
        pictureUrl.put("bacon cheeseburger", "https://cdn.pixabay.com/photo/2018/04/28/11/09/bacon-cheeseburger-3356672_1280.jpg");
    }

    public MyThread(Handler handler, ArrayList<MyObject> items, String serverIp, int serverPort, int task) {
        this.handler = handler;
        this.items = items;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.task = task;
    }

    public MyThread(Handler handler, ArrayList<MyObject> items, String serverIp, int serverPort, int task,String storeName) {
        this.handler = handler;
        this.items = items;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.task = task;
        this.storeName = storeName;
    }
    public MyThread(Handler handler, int rating, String serverIp, int serverPort, int task,String storeName) {
        this.handler = handler;
        this.rating = rating;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.task = task;
        this.storeName = storeName;

    }
    @Override
    public void run() {
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        try {
            Log.d("MyThread", "Task received: " + task);
            Log.d("MyThread", "Trying to connect to server at " + serverIp + ":" + serverPort);
            socket = new Socket(serverIp, serverPort);

            if (task == TASK_CHECK_CONNECTION) {
                Log.d("MyThread", "TASK: CHECK_CONNECTION");
                handler.sendEmptyMessage(1);
            }

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            if (task == TASK_BUY) {
                Log.d("MyThread", "Connected to server");
                oos.writeObject(3);
                oos.writeObject("client");
                oos.flush();
                Log.d("MyThread", "TASK: ORDER");

                StringBuilder productList = new StringBuilder();
                for (MyObject item : items) {
                    if (item.getQuantity() > 0) {
                        productList.append(item.getName())
                                .append(":")
                                .append(item.getQuantity())
                                .append(",");
                    }
                }

                if (productList.length() > 0) {
                    productList.setLength(productList.length() - 1);
                }

                String purchaseData = storeName + "|" + productList.toString();
                Log.d("MyThread", "Sending purchase data: " + purchaseData);
                oos.writeObject(purchaseData);
                oos.flush();

                String response = (String) ois.readObject();
                Log.d("MyThread", "Order response: " + response);

                Message msg = handler.obtainMessage(2, response);
                handler.sendMessage(msg);
                Log.d("MyThread", "Sent message to handler with rating response");
            }
            Log.d("MyThread before rating", "Task received: " + task);

            if (task == TASK_RATING) {
                Log.d("MyThread", "Connected to server");
                Log.d("MyThread", "TASK: RATING");

                oos.writeObject(4);

                Log.d("MyThread", "Sent command: 4 (RATING)");

                oos.writeObject("client");

                Log.d("MyThread", "Sent user type: client");

                String request = storeName + "," + rating;
                Log.d("MyThread", "Constructed request: " + request);
                oos.writeObject(request);
                oos.flush();
                Log.d("MyThread", "Request sent to server");

                String response = (String) ois.readObject();
                Log.d("MyThread", "Order response: " + response);

                Message msg = handler.obtainMessage(3, response);
                handler.sendMessage(msg);
                Log.d("MyThread", "Sent message to handler with rating response");

            }
            if(task== TASK_SEARCH_STORES){
                Log.d("MyThread", "TASK: SEARCH_STORES");
                oos.writeObject(2);
                oos.writeObject("client");
                oos.writeObject(storeName);
                oos.flush();
            }

            socket.close();
            Log.d("MyThread", "Socket closed");
        } catch (IOException | ClassNotFoundException e) {
            Log.e("MyThread", "Exception occurred", e);
            e.printStackTrace();
            Message msg = handler.obtainMessage(0, e.getMessage());
            handler.sendMessage(msg);
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (IOException e) {
                Log.e("MyThread", "Failed to close ObjectInputStream", e);
            }
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
                Log.e("MyThread", "Failed to close ObjectOutputStream", e);
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                Log.e("MyThread", "Failed to close Socket", e);
            }
            Log.d("MyThread", "Resources closed (socket, oos, ois)");
        }
    }

}
