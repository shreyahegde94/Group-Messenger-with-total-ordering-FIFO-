package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 * References - https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
 * https://docs.oracle.com/javase/tutorial/essential/concurrency/deadlock.html
 * http://javarevisited.blogspot.com/2011/06/volatile-keyword-java-example-tutorial.html
 * http://www.journaldev.com/1642/java-priority-queue-priorityqueue-example
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] RPA = new String[]{"11108", "11112", "11116", "11120", "11124"};
    //REMOTE_PORT_ARRAY
    static ArrayList<String> REMOTE_PORT_LIST=new ArrayList<String>(Arrays.asList(RPA));
    static final int SERVER_PORT = 10000;
    static volatile Integer CRASHED_PORT =0;
    static volatile boolean locked = false;
    static volatile boolean firstCrash = false;

    // String Constants Used in the file
    public static String CONTENT = "content";
    public static String URI = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    public static String CANT_CREATE_SOCKET = "Can't create a ServerSocket";
    public static String PRESSING_SEND = "On Pressing Send - ";
    public static String RCVD_MSG_IN_SERVER = "Received msg in doInBackground Server from Port- ";
    public static String ACKNOWLEDGE_RECEPTION_MSG = "ACKNOWLEDGE_RECEPTION_MSG";
    public static String ACKNOWLEDGE_RECEPTION = "Received Message -";
    public static String EXCEPTION_IN_SERVER = "Exception in Server -";
    public static String SENT_MSG_DOINBACKGROUND_CLIENT = "Sent msg to server from doInBackground -";
    public static String WAITING_FOR_ACK_FROM_SERVER = "In Client - Waiting for ACK from Target Server Port -";
    public static String RECEIVED_ACL_FROM_SERVER = "In Client - Received ACK from Server -";
    public static String CLOSING_SOCKET = "Going to Close Socket";
    public static String CLIENT_UNKNOWNHOST_EXCEPTION = "ClientTask UnknownHostException";
    public static String CLIENT_SOCKET_IOEXCEPTION = "ClientTask socket IOException";
    public static String KEY = "key";
    public static String VALUE = "value";
    public static String SHARED_PREFERENCES_NAME = "gGroupMessenger2";
    public static String INSERET_METHOD = "insert method -";

    private final Uri mUri=buildUri("content", URI);
    private MessagePriorityQueue messageQueue;
    String myPort;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        messageQueue=new MessagePriorityQueue();
        messageQueue.setHighestMyProposal(Float.parseFloat("-1"));
        messageQueue.setHighestAgreedProposal(Float.parseFloat("-1"));

         /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2+"-"+Integer.parseInt(myPort)/2,CANT_CREATE_SOCKET);
            return;
        }

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
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editText1);
                TextView tview = (TextView) findViewById(R.id.textView1);
                String msg = editText.getText().toString();
//                Log.i(TAG+myPort,"In Onclick Listener-"+msg);
                msg=msg+"#"+myPort+ "\n";
                editText.setText("");
                tview.append("\t" + msg);

//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,PRESSING_SEND + msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private String dataReceived;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                while (true) {
                    try {
                        Socket socket = null;
                        serverSocket.setSoTimeout(4700);
                        socket = serverSocket.accept();
                        BufferedReader messageReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        dataReceived = messageReader.readLine();
//                        Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, RCVD_MSG_IN_SERVER);
//                        Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, dataReceived);
                        // messageReader.close();
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Printing Queue in Server "+messageQueue.toString());
                        boolean isBroadcasted=false;
                        String isBroadcastedStr=dataReceived.split("#Delim#")[0];
                        if (isBroadcastedStr.equals("broadcast")){
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server -Message is a broadcast message");
                            isBroadcasted=true;
                        }

                        boolean isCrashAlert=false;
                        int isCrashedIndex=dataReceived.indexOf("crash#");
                        if(isCrashedIndex!=-1){
                            CRASHED_PORT=Integer.parseInt(dataReceived.split("crash#")[1]);
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Broadcast Crash Received - Message informing the following is crashed-"+CRASHED_PORT);
                            isCrashAlert=true;
                        }
                        if(isCrashAlert){
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Broadcast Before Cleaning CRASHED Port"+CRASHED_PORT);
                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Broadcast Before Cleaning CRASHED Port"+CRASHED_PORT+"Message Queue"+messageQueue);
//                            Log.i(TAG,"Server Broadcast - Lock Acquired");
                            messageQueue.removeMessagesFromEmulator(CRASHED_PORT.toString());
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Broadcast After Cleaning CRASHED Port"+CRASHED_PORT+"Message Queue"+messageQueue);
//                            while (true){
//                                Message minMsg=messageQueue.returnMinElement();
//                                if (minMsg!=null && minMsg.isDeliverable()){
//                                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Crash Further Cleanup Because top element is deliv"+messageQueue);
//                                    popOutAndAddToProvider(minMsg.getMessageContent().substring(0, minMsg.getMessageContent().lastIndexOf("#")));
//                                }else{
//                                    break;
//                                }
//                            }
                            locked = false;
//                            Log.i(TAG,"Server Broadcast - Lock Released");

                            String ackMessage = ACKNOWLEDGE_RECEPTION_MSG + "\n";
                            OutputStream os = socket.getOutputStream();
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                            bw.write(ackMessage);
                            bw.flush();
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, ACKNOWLEDGE_RECEPTION + ackMessage);
                            socket.close();
                        }else if(isBroadcasted){
                            String tempTimeStamp=dataReceived.split("#Delim#")[1];
                            String tempEmulatorId=dataReceived.split("#Delim#")[2];
                            String tempMessageContent=dataReceived.split("#Delim#")[3];
                            String tempMessagePriority=dataReceived.split("#Delim#")[4];
                            String tempMessageDeliveralble=dataReceived.split("#Delim#")[5];
                            String tempNoOFReplies=dataReceived.split("#Delim#")[6];

                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG,"Server Broadcast - Lock Acquired");
                            if (messageQueue.getqList().size()!=0){
                                Message tempMsg=new Message(tempTimeStamp,tempEmulatorId,tempMessageContent,Float.parseFloat(tempMessagePriority),Boolean.parseBoolean(tempMessageDeliveralble), Integer.parseInt(tempNoOFReplies));

//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"- created Message out of the received string from server "+tempMsg);
                                Integer tempPos=messageQueue.searchWithoutPriority(tempMsg);
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Message Queue before deleting"+messageQueue);
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"The Position the message is found-"+tempPos.toString());
                                Boolean delresult=messageQueue.delete(messageQueue.getqList().get(tempPos));
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"delresult-"+delresult.toString());
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Message Queue before inserting new one but after deleting"+messageQueue);
                                messageQueue.insert(tempTimeStamp,tempEmulatorId,tempMessageContent,Float.parseFloat(tempMessagePriority),Boolean.parseBoolean(tempMessageDeliveralble), Integer.parseInt(tempNoOFReplies));
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Updated Message in the queue with new priority. Will check if it's the top");
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Message Queue after inserting before checking top"+messageQueue);
//                                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Message Queue Min"+messageQueue.returnMin());
                                if (messageQueue.isHighestAgreedProposals(Float.parseFloat(tempMessagePriority))){
                                    messageQueue.setHighestAgreedProposal(Float.parseFloat(tempMessagePriority));
                                }
//                                if(messageQueue.returnMin()==Float.parseFloat(tempMessagePriority)){
//                                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Rcvd Server Broadcast from"+tempEmulatorId+"Server Broadcast - Popping from the queue as it is on the top");
//                                }
                                while (true){
                                    Message minMsg=messageQueue.returnMinElement();
                                    if (minMsg!=null && minMsg.isDeliverable()){
                                        popOutAndAddToProvider(minMsg.getMessageContent().substring(0, minMsg.getMessageContent().lastIndexOf("#")));
                                    }else{
                                        break;
                                    }
                                }
                            }
                            locked = false;
//                            Log.i(TAG,"Server Broadcast - Lock Released");

                            String ackMessage = ACKNOWLEDGE_RECEPTION_MSG + "\n";
                            OutputStream os = socket.getOutputStream();
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                            bw.write(ackMessage);
                            bw.flush();
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, ACKNOWLEDGE_RECEPTION + ackMessage);
                            socket.close();
                        }else{
//                            Log.i(TAG+ myPort,"Server Normal - Current status of lock- "+String.valueOf(locked));
                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG,"Server Normal - Lock Acquired");
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server - Not broadcast- New Message "+dataReceived);
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server - Not broadcast-Current highest priority before adding "+messageQueue.returnMax());
                            //Float newPrioProposal=messageQueue.returnHighestOfMyAndAgreedProposals()+Float.parseFloat("1.0")+(Float.parseFloat(myPort)/100000);
                            Double newPrioProposalDouble=Double.parseDouble(messageQueue.returnHighestOfMyAndAgreedProposals().toString());
                            Float newPrioProposal=(float)Math.floor(newPrioProposalDouble);
                            newPrioProposal=newPrioProposal+Float.parseFloat("1.0")+(Float.parseFloat(myPort)/100000);

                            messageQueue.setHighestMyProposal(newPrioProposal);
                            //Float newPrioProposal=(float)( messageQueue.returnMax()+1.0+Float.parseFloat(myPort)/100000);
                            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                            String tempEmulatorId=dataReceived.substring(dataReceived.lastIndexOf("#") + 1).trim();
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server-Not broadcast, EmulatorId got from content -"+tempEmulatorId);
                            messageQueue.insert(timeStamp,tempEmulatorId, dataReceived, newPrioProposal, false, 1);
                            locked = false;
//                            Log.i(TAG,"Server Normal - Lock Released");
                            String ackMessage = ACKNOWLEDGE_RECEPTION_MSG +"#"+newPrioProposal.toString()+ "\n";
                            OutputStream os = socket.getOutputStream();
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                            bw.write(ackMessage);
                            bw.flush();
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, ACKNOWLEDGE_RECEPTION + ackMessage);
                            socket.close();
                            publishProgress(dataReceived);
                        }

                    }catch (SocketTimeoutException e){
//                        Log.i(TAG,"Server - SocketTimeoutException EXCEPTION");
                        if (firstCrash){
                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG,"SocketTimeoutException IO Exception - Lock Acquired");
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Final Server Before Cleaning Crashed Port From Message Queue"+messageQueue);
                            if (CRASHED_PORT!=0 && messageQueue.getqList().size()>0){
                                messageQueue.removeMessagesFromEmulator(CRASHED_PORT.toString());
                            }
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server After Cleaning at end "+CRASHED_PORT+"Message Queue"+messageQueue);
                            while (true){
                                Message minMsg=messageQueue.returnMinElement();
                                if (minMsg!=null){
//                                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Cleaning Queue One By One -"+messageQueue);
                                    popOutAndAddToProvider(minMsg.getMessageContent().substring(0, minMsg.getMessageContent().lastIndexOf("#")));
                                }else{
                                    break;
                                }
                            }
                            locked = false;
//                            Log.i(TAG,"SocketTimeoutException IO Exception - Lock Released And Closing Everything In the end");
                        }else{
//                            Log.i(TAG,"Server - SocketTimeoutException EXCEPTION - FIRST CRASH");
                            firstCrash=true;
                        }


                    } catch (IOException e) {
                        Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, EXCEPTION_IN_SERVER + dataReceived);
                        e.printStackTrace();
                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG,"Server IO Exception - Lock Acquired");
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Final Server  Cleaning Port Message Queue"+messageQueue);
                            if (CRASHED_PORT!=0 && messageQueue.getqList().size()>0){
                                messageQueue.removeMessagesFromEmulator(CRASHED_PORT.toString());
                            }
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Cleaning at end "+CRASHED_PORT+"Message Queue"+messageQueue);
                            while (true){
                                Message minMsg=messageQueue.returnMinElement();
                                if (minMsg!=null && minMsg.isDeliverable()){
//                                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Server Cleanup at end Because top element is delivrable"+messageQueue);
                                    popOutAndAddToProvider(minMsg.getMessageContent().substring(0, minMsg.getMessageContent().lastIndexOf("#")));
                                }else{
                                    break;
                                }
                            }
                            locked = false;
//                            Log.i(TAG,"Server IO Exception - Lock Released After Exception");
                    }catch (Exception e){
                          locked=false;
                        Log.e(TAG,"Server Generic Exception");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;


        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tview = (TextView) findViewById(R.id.textView1);
            tview.append(strReceived + "\t\n");
            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String msgToSend = msgs[0];
                ArrayList<String> iterateWithoutSelf=new ArrayList<String>(REMOTE_PORT_LIST);
                iterateWithoutSelf.remove(myPort);

//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Client - Propose Priority For SELF before asking others for proposals");
                while(locked)
                {
                }
                locked = true;
//                Log.i(TAG,"Client - Lock Acquired for before self proposing priority");
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Client - Highest priority before adding "+messageQueue.returnMax());
                Double newPrioProposalDouble=Double.parseDouble(messageQueue.returnHighestOfMyAndAgreedProposals().toString());
                Float newPrioProposal=(float)Math.floor(newPrioProposalDouble);
                newPrioProposal=newPrioProposal+Float.parseFloat("1.0")+(Float.parseFloat(myPort)/100000);
                messageQueue.setHighestMyProposal(newPrioProposal);
                locked=false;
//                Log.i(TAG,"Client - Lock Released after after self proposing priority");

                clientProcessing(newPrioProposal.toString(),msgToSend,0,0);

                //Asking others for proposal
                int iterateSize=iterateWithoutSelf.size();
                for (int i = 0; i < iterateSize; i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(iterateWithoutSelf.get(i)));
                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);
                    bw.write(msgToSend);
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -"+SENT_MSG_DOINBACKGROUND_CLIENT + msgToSend);
                    bw.flush();
                    // bw.close();
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client-"+WAITING_FOR_ACK_FROM_SERVER+iterateWithoutSelf.get(i));
                    InputStream input_stream = socket.getInputStream();
                    BufferedReader buffered_reader = new BufferedReader(new InputStreamReader(input_stream));
                    String msgReceived = buffered_reader.readLine();
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Client - msg resecived from the server side -"+msgReceived);
                    if (msgReceived!=null) {
                        String priorityStr = msgReceived.substring(msgReceived.lastIndexOf("#") + 1).trim();
                        clientProcessing(priorityStr,msgToSend,i,iterateSize-1);
                        String copyOfMsgReceived = msgReceived;

                        copyOfMsgReceived = copyOfMsgReceived.replace(priorityStr, "");
                        if (copyOfMsgReceived.equals(ACKNOWLEDGE_RECEPTION_MSG)) {
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Client-"+RECEIVED_ACL_FROM_SERVER + msgReceived);
//                            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Client-"+CLOSING_SOCKET+"-"+iterateWithoutSelf.get(i));
                            socket.close();
                        }
                    }else{
//                        Log.i(TAG,"Client - Server crashed and hence closing the connection with -"+iterateWithoutSelf.get(i).toString());
                        CRASHED_PORT=Integer.parseInt(iterateWithoutSelf.get(i));
                        socket.close();
                        while(locked)
                        {
                        }
                        locked = true;
//                        Log.i(TAG,"Client Crashed - Lock Acquired for cleaning queue");
                        messageQueue.removeMessagesFromEmulator(CRASHED_PORT.toString());
                        //totalLiveProcessCount=4;
                        REMOTE_PORT_LIST.remove(CRASHED_PORT);
//                        Log.i(TAG,"Client Crashed - queue after cleanup"+messageQueue.toString());
                        locked=false;

                        if (i==iterateSize-1){
//                            Log.i(TAG,"Client Crashed - Special Condition Last Port crashed -"+iterateWithoutSelf.get(i).toString());
                            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                            Message tempMessage = new Message(timeStamp, myPort, msgToSend.trim(), new Float("0.0"), true, 1);
                            while(locked)
                            {

                            }
                            locked = true;
//                            Log.i(TAG,"Client Crashed Last Node- Lock Acquired for getting last highest element from queue"+messageQueue);
                            int lastHighestMsgIndex=messageQueue.searchWithoutPriority(tempMessage);
                            if(lastHighestMsgIndex!=-1){
                                Float lastHighestPriority=messageQueue.getqList().get(lastHighestMsgIndex).getPriorityValue();
                                locked=false;
//                                Log.i(TAG,"Client Crashed - Lock Released After getting the last highest element priority"+lastHighestPriority);
//                                Log.i(TAG,"Client Crashed - Broadcasting last highest element");
                                Message bcastMessage = new Message(timeStamp, myPort, msgToSend.trim(), lastHighestPriority, true, 4);
                                broadcastAMessage(bcastMessage);
                            }
                            locked=false;
//                            Log.i(TAG,"Client Crashed - Lock Released in special case");
                        }
                        broadcastCrash();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_UNKNOWNHOST_EXCEPTION);
            } catch (IOException e) {
                Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_SOCKET_IOEXCEPTION);
            }
            return null;
        }
    }

    private boolean broadcastCrash() {

        try{
            String msgToSend = "crash#"+CRASHED_PORT;
            msgToSend = msgToSend+ "\n";
            ArrayList<String> broadcastErrorToPorts=new ArrayList<String>((REMOTE_PORT_LIST));
            broadcastErrorToPorts.remove(CRASHED_PORT.toString());
            broadcastErrorToPorts.remove(myPort);
            for (int i = 0; i < broadcastErrorToPorts.size(); i++) {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(broadcastErrorToPorts.get(i)));

                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(msgToSend);
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - CRASH Broadcasting to -"+broadcastErrorToPorts.get(i)+ msgToSend);
                bw.flush();
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -"+WAITING_FOR_ACK_FROM_SERVER+"-"+broadcastErrorToPorts.get(i));
                InputStream input_stream = socket.getInputStream();
                BufferedReader buffered_reader = new BufferedReader(new InputStreamReader(input_stream));
                String msgReceived = buffered_reader.readLine();
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -CRASH  Broadcasting End");
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -CRASH Going to close target broadcast port because tis msg received-"+msgReceived);
                if(msgReceived==null){
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Target Port is dead -"+broadcastErrorToPorts);
                    socket.close();
                }else {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,RECEIVED_ACL_FROM_SERVER + msgReceived);
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLOSING_SOCKET);
                    socket.close();
                }
            }
        } catch (UnknownHostException e) {
            Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_UNKNOWNHOST_EXCEPTION);
        } catch (IOException e) {
            Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_SOCKET_IOEXCEPTION);
        }
        return false;
    }
    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private float popOutAndAddToProvider(String msgContent) {
        float popOutGuy=messageQueue.extractMin();
//        Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,"Popped Out from Queue and Delivering by Writing to the content provider"+popOutGuy);
        ContentValues keyValueToInsert = new ContentValues();
        keyValueToInsert.put(KEY, IDGeneratorUtil.StaticIDGeneratorUtil.getNewKeyId().toString());
        keyValueToInsert.put(VALUE, msgContent);
        Uri newUri = getContentResolver().insert(mUri, keyValueToInsert);
        return popOutGuy;
    }

    private boolean broadcastAMessage(Message message) {

        try{
            String msgToSend = "broadcast#Delim#"+message.getTimestamp()+"#Delim#"+message.getEmulatorId()+"#Delim#"+message.getMessageContent().trim()+"#Delim#"+String.valueOf(message.getPriorityValue())+"#Delim#"+String.valueOf(message.isDeliverable())+"#Delim#"+message.getNoOfReplies();
            msgToSend = msgToSend.replace("\n", "").replace("\r", "");
            msgToSend = msgToSend+ "\n";
            for (int i = 0; i < REMOTE_PORT_LIST.size(); i++) {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT_LIST.get(i)));

                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(msgToSend);
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -Broadcasting to -"+REMOTE_PORT_LIST.get(i)+ msgToSend);
                bw.flush();
                // bw.close();
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -"+WAITING_FOR_ACK_FROM_SERVER+"-"+REMOTE_PORT_LIST.get(i));
                InputStream input_stream = socket.getInputStream();
                BufferedReader buffered_reader = new BufferedReader(new InputStreamReader(input_stream));
                String msgReceived = buffered_reader.readLine();
                if (msgReceived==null){
                    Log.i(TAG,"Client - Server crashed and hence closing the connection with -"+REMOTE_PORT_LIST.get(i));
                    CRASHED_PORT=Integer.parseInt(REMOTE_PORT_LIST.get(i));
                    socket.close();
                    while(locked)
                    {

                    }
                    locked = true;
//                    Log.i(TAG,"Client Crashed In Broadcast- Lock Acquired for cleaning queue");
                    messageQueue.removeMessagesFromEmulator(CRASHED_PORT.toString());
                    REMOTE_PORT_LIST.remove(CRASHED_PORT);
//                    Log.i(TAG,"Client Crashed In Broadcast - queue after cleanup"+messageQueue.toString());
                    locked=false;
//                    Log.i(TAG,"Client Crashed In Broadcast - Lock Released After cleanup");
//                    Log.i(TAG,"Client - Going to Broadcast the crash to other ports");
                    // broadcastCrash();
                }else if (msgReceived.equals(ACKNOWLEDGE_RECEPTION_MSG)) {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,RECEIVED_ACL_FROM_SERVER + msgReceived);
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLOSING_SOCKET);
                    socket.close();
                }
            }

        } catch (UnknownHostException e) {
            Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_UNKNOWNHOST_EXCEPTION);
        } catch (IOException e) {
            Log.e(TAG+ myPort+"-"+Integer.parseInt(myPort)/2,CLIENT_SOCKET_IOEXCEPTION);
        }
        return false;
    }

    private void clientProcessing(String priorityStr,String msgToSend,Integer count, Integer iterateSize){

        msgToSend=msgToSend.trim();
        Float newPriority = Float.parseFloat(priorityStr);
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        Message message = new Message(timeStamp, myPort, msgToSend, newPriority, false, 1);

        while(locked)
        {

        }
        locked = true;
//        Log.i(TAG,"Client - Lock Acquired");

        if (messageQueue.size() == 0) {
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - Queue Empty, Adding first element and printing queue");
            messageQueue.insert(timeStamp, myPort, msgToSend, newPriority, false, 1);
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, messageQueue.toString());
            locked=false;
//            Log.i(TAG,"Client - Lock Released After insert on empty queue");

        } else if (messageQueue.searchWithoutPriority(message) == -1) {
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - Queue Not Empty but 1st message of this type, element and printing queue");
            messageQueue.insert(timeStamp, myPort, msgToSend, newPriority, false, 1);
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, messageQueue.toString());
            locked=false;
//            Log.i(TAG,"Client - Lock Released After insert of first message of that type");
        } else {
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - Queue contains the message already. Need to update it accordingly");
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - Index Found -" + messageQueue.searchWithoutPriority(message));

            Float existingPriority = messageQueue.getqList().get(messageQueue.searchWithoutPriority(message)).getPriorityValue();
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - existingPriority - " + existingPriority);

            int noOfReplies = messageQueue.getqList().get(messageQueue.searchWithoutPriority(message)).getNoOfReplies();
//            Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - noOfReplies - " + noOfReplies);

            if (newPriority > existingPriority) {
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - New Priority Greater than existing priority");
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client -Therefore Deleting the old one -"+messageQueue.toString());
                int foundMessageIndex = messageQueue.searchWithoutPriority(message);
                messageQueue.getqList().remove(foundMessageIndex);
                if (count == iterateSize) {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "noOfReplies == totalLiveProcessCount, Marking for delivery");
                    messageQueue.insert(timeStamp, myPort, msgToSend, newPriority, false, noOfReplies + 1);

                    message.setDeliverable(true);
                    message.setNoOfReplies(count);
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - All replies received, Broadcasting agreed priority");
                    locked=false;
//                    Log.i(TAG,"Client - Lock Released Before entering Broadcast");
                    broadcastAMessage(message);

                } else {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - noOfReplies less than totalLiveProcessCount, Inserting only increasing noOfReplies");
                    messageQueue.insert(timeStamp, myPort, msgToSend, newPriority, false, noOfReplies + 1);
                    locked=false;
//                    Log.i(TAG,"Client - Lock Released After inserting");
                }

            } else {
//                Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - New Priority Lesser than existing priority");
                if (count == iterateSize) {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - noOfReplies == totalLiveProcessCount, Marking for delivery");
                    message.setDeliverable(true);
                    message.setNoOfReplies(count);
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - All replies received, Broadcasting agreed priority");
                    //Reverting to old priority
                    message.setPriorityValue(existingPriority);
                    locked=false;
//                    Log.i(TAG,"Client - Lock Released Before Broadcasting");
                    broadcastAMessage(message);
                    //}
                } else {
//                    Log.i(TAG+ myPort+"-"+Integer.parseInt(myPort)/2, "Client - noOfReplies less than totalLiveProcessCount, only increasing noOfReplies");
                    messageQueue.getqList().get(messageQueue.searchWithoutPriority(message)).setNoOfReplies(noOfReplies + 1);
                    locked=false;
//                    Log.i(TAG,"Client - Lock Released After insert");
                }
            }
        }
    }
}