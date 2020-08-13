package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    //Declaring all the required variables
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    Uri myuri;
    static int counter=0;
    float rank=0.0f;

    public class Messages implements Comparable<Messages> {
        String message;
        double priority;
        boolean status;

        public Messages(String m, double p) {
            this.message = m;
            this.priority = p;
            this.status=false;
        }

        public void setStatus(){
            this.status=true;
        }

        public int compareTo(Messages m) {
            if (this.priority > m.priority) {
                return 1;
            } else if (this.priority < m.priority) {
                return -1;
            } else {
                return 0;
            }
        }

    }

    PriorityQueue<Messages> msg_queue = new PriorityQueue<Messages>();
    float obs=0.0f;
    String[] devices = { "5554", "5556", "5558", "5560", "5562"};
    String[] ports = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    ArrayList<String> received_proposals=new ArrayList<String>();
    float sequence_no = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


        Uri.Builder uribuilder = new Uri.Builder();
        uribuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uribuilder.scheme("content");
        myuri=uribuilder.build();


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        final String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        for (int i=0;i<ports.length;i++){
            System.out.println(ports[i]);
            System.out.println(portStr);
            if(myPort.equals(ports[i])){
                System.out.println("if loop obs");
                obs = (i+1)*0.1f;
            }
        }

        Log.e(TAG,"obs is"+obs);
        sequence_no+=obs;

        Log.e(TAG,"sequence number from oncreate is"+sequence_no);

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
                System.out.println("Inside Send Button Code");
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,portStr);
                //return true;
            }
            //return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            Socket datasocket = null;
            InputStream instream=null;

            try {
                //Server keeps waiting for connections all the time.
                while(true){

                    // Data Socket is created when server accepts the connection from the client
                    datasocket = serverSocket.accept();
                    sequence_no+=1;
                    Log.e(TAG,"sequence number from server "+sequence_no);
                    //The data is received in the form of input stream.
                    instream = datasocket.getInputStream();
                    DataInputStream datainstream = new DataInputStream(instream);

                    String received_message=datainstream.readUTF();

                    Log.e(TAG,"the received message is  "+received_message);

                    String received_msg[]=received_message.split("::");
                    Log.e(TAG, received_msg[0]);
                    Log.e(TAG, received_msg[1]);
                    String clientID=received_msg[1];
                    String original_msg=received_msg[0];


                    Messages msg=new Messages(original_msg,sequence_no);
                    msg_queue.add(msg);

                    String proposal_message = original_msg+ "::" + sequence_no + "::" + clientID+"";

                    DataOutputStream dataoutstream = new DataOutputStream(datasocket.getOutputStream());
                    dataoutstream.writeUTF(proposal_message);
                    Log.e(TAG,"the sent proposal is  "+proposal_message);

                    while(true){
                        try {
                            String received_message2 = datainstream.readUTF();

                            Log.e(TAG, "the received agreement is " + received_message2);
                            String received_msg2[] = received_message.split("::");

                            Log.e(TAG, received_msg2[0]);
                            Log.e(TAG, received_msg2[1]);

                            String clientID2 = received_msg2[2];
                            String original_msg2 = received_msg2[0];
                            String received_agreed_prop = received_msg2[1];

                            Queue<Messages> q = new LinkedBlockingQueue<Messages>();

                            while (!msg_queue.isEmpty()) {
                                Messages entry = msg_queue.poll();
                                if (entry.message.equals(original_msg2)) {
                                    entry.priority = Double.parseDouble(received_agreed_prop);
                                    entry.setStatus();
                                }
                                q.offer(entry);
                            }

                            while (!q.isEmpty()) {
                                msg_queue.add(q.poll());
                            }

                            while (!msg_queue.isEmpty() && msg_queue.peek().status == true) {

                                Messages m = msg_queue.poll();
                                ContentValues cv = new ContentValues();
                                cv.put("key", Integer.toString(counter++));
                                cv.put("value", m.message);
                                getContentResolver().insert(myuri, cv);


                                publishProgress(m.message);
                                break;
                            }
                        }
                        catch(Exception e){
                            Log.e(TAG,"the sender has failed.");

                            msg_queue.remove(msg);
                            break;
                        }
                        //This received message is sent to the onProgressUpdate function
                        //http://developer.android.com/reference/android/os/AsyncTask.html
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */


            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */

    Socket[] socket=new Socket[5];
    DataOutputStream[] dataout=new DataOutputStream[5];
    DataInputStream[] datain=new DataInputStream[5];

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String myID = msgs[1];
                String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};

                for(int i=0;i<remotePort.length;i++){
                    Log.e(TAG, "client task opening socket to "+remotePort[i]);
                    String remoteport=remotePort[i];
                    socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remoteport));
                    Log.e(TAG, "client socked opened");
                    String msgToSend = msgs[0]+"::"+myID+"::"+"f";

                    //The message is sent to the server using the outputstream
                    dataout[i] = new DataOutputStream(socket[i].getOutputStream());

                    dataout[i].writeUTF(msgToSend);

                    try {

                        //The client reads the message from the server
                        datain[i] = new DataInputStream(socket[i].getInputStream());
                        String propmsg = datain[i].readUTF();
                        Log.e(TAG, "proposal received");
                        received_proposals.add(propmsg);
                        Log.e(TAG, "The received proposed sequence number is " + propmsg);
                    }
                    catch(Exception e){
                        Log.e(TAG,"Failure Detected!");
                        continue;
                    }
                }

                String rec_msg=null;
                float max=0;
                String[] eachsplit={};
                Log.e(TAG, "The received replies are " +received_proposals.size());
                for(String eachrp:received_proposals){
                    Log.e(TAG,"eachrp is "+eachrp);
                    eachsplit = eachrp.split("::");
                    float prop_seq=Float.parseFloat(eachsplit[1]);
                    if(prop_seq>max){
                        max=prop_seq;
                        rec_msg=eachrp;
                    }
                    Log.e(TAG,"the pro seq is "+prop_seq);
                    Log.e(TAG,"the max is "+max);

                }
                String[] rec_msg_split = rec_msg.split("::");
                String agreed_msg= rec_msg_split[0]+"::"+max+"::"+rec_msg_split[2]+"::"+"a";
                Log.e(TAG,"agreed message in server "+agreed_msg);

                for(int i=0;i<remotePort.length;i++){

//                    String remoteport=remotePort[i];
                    Socket sckt = socket[i];

                    String msgToSend = agreed_msg;

                    DataOutputStream dataout2 = new DataOutputStream(sckt.getOutputStream());
                    dataout2.writeUTF(msgToSend);
                    dataout2.flush();
                    sckt.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
