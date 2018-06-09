package edu.buffalo.cse.cse486586.groupmessenger2;
import android.util.Log;

import java.util.ArrayList;
 class MessagePriorityQueue {

    static final String TAG = MessagePriorityQueue.class.getSimpleName();
    private int length;
    private ArrayList<Message> qList;
    private float highestMyProposal;
    private float highestAgreedProposal;

     public float getHighestMyProposal() {
         return highestMyProposal;
     }

     public void setHighestMyProposal(float highestMyProposal) {
//         Log.i(TAG,"Setting Highest My Proposal - "+highestMyProposal);
         this.highestMyProposal = highestMyProposal;
     }

     public float getHighestAgreedProposal() {
         return highestAgreedProposal;
     }

     public void setHighestAgreedProposal(float highestAgreedProposal) {
//         Log.i(TAG,"Setting Highest Agreed Proposal - "+highestAgreedProposal);
         this.highestAgreedProposal = highestAgreedProposal;
     }

     public ArrayList<Message> getqList() {
        return qList;
    }
    public void setqList(ArrayList<Message> qList) {
        this.qList = qList;
    }

    public MessagePriorityQueue() {
        qList = new ArrayList<Message>();
        length = 0;
    }
    public boolean delete(Message message) {
        for(Message msg : qList){
            if((msg.getEmulatorId().equals(message.getEmulatorId())) && (msg.getMessageContent().equals(message.getMessageContent()) && (msg.getPriorityValue()==message.getPriorityValue()))){
                qList.remove(msg);
                return true;
            }
        }
        return false;

    }

    public Integer searchWithPriority (Message message) {
        for(int i=0;i<qList.size();i++){
            Message msg=qList.get(i);
            if((msg.getEmulatorId().equals(message.getEmulatorId())) && (msg.getMessageContent().equals(message.getMessageContent()) && (msg.getPriorityValue()==message.getPriorityValue()))){
                return i;
            }
        }
        return -1;
    }
    public Integer searchWithoutPriority (Message message) {
        for(int i=0;i<qList.size();i++){
            Message msg=qList.get(i);
            if((msg.getEmulatorId().equals(message.getEmulatorId())) && (msg.getMessageContent().equals(message.getMessageContent()))){
                return i;
            }
        }
        return -1;
    }
    public boolean insert(String timeStamp, String emulatorId, String messagecContent, float p, boolean deliverable, Integer noOfReplies) {

        if(p >= 0) {
            int mid = 0;
            int start = 0;
            int end = qList.size();
            Message le = new Message(timeStamp, emulatorId,messagecContent,p, deliverable, noOfReplies);
            if(qList.size() == 0) {
                qList.add(le);
            }
            else if(qList.size() == 1) {

                if(p > qList.get(0).getPriorityValue()) {
                    qList.add(le);
                }
                else {
                    qList.add(0, le);
                }
            }
            else {
                if(p < qList.get(0).getPriorityValue()) {
                    qList.add(0, le);
                }
                else if(p > qList.get(qList.size() - 1).getPriorityValue()) {
                    qList.add(le);
                }
                else {
                    while(true) {
                        mid = (start + end) / 2;
                        if(p > qList.get(mid).getPriorityValue()) {
                            start = mid;
                        }
                        else if(p < qList.get(mid).getPriorityValue()) {
                            end = mid;
                        }
                        if(start == end - 1) {
                            qList.add(end, le);
                            break;
                        }
                    }
                }

            }
            length++;
            return true;
        }
        else {
//            System.out.println("Please use positive number to represent priority, thanks.");
            return false;
        }
    }

    public float extractMax() {
        float max = -1;
        if(qList.size() > 0) {
            max = qList.get(qList.size() - 1).getPriorityValue();
            qList.remove(qList.size() - 1);
            length--;
            return max;
        }
        else {
            return max;
        }
    }

    public float extractMin() {
        float min = -1;
        if(qList.size() > 0) {
            min = qList.get(0).getPriorityValue();
            qList.remove(0);
            length--;
            return min;
        }
        else {
            return min;
        }
    }

    public float returnMin() {
        float min = -1;
        if(qList.size() > 0) {
            min = qList.get(0).getPriorityValue();
            return min;
        }
        else {
            return min;
        }
    }
     public Message returnMinElement() {
         float min = -1;
         if(qList.size() > 0) {
             return qList.get(0);
         }
         else {
             return null;
         }
     }

    public float returnMax() {
        float max = -1;
        if(qList.size() > 0) {
            max = qList.get(qList.size() - 1).getPriorityValue();
            return max;
        }
        else {
            return max;
        }
    }

    public int size() {
        return length;
    }
     public Float returnHighestOfMyAndAgreedProposals() {
//         Log.i(TAG,"Highest My Proposal - "+this.getHighestMyProposal());
//         Log.i(TAG,"Highest Agreed Proposal - "+this.getHighestAgreedProposal());
        if (this.getHighestMyProposal()>this.getHighestAgreedProposal()){
            return  this.getHighestMyProposal();
        }else {
            return  this.getHighestAgreedProposal();
        }
     }
     public boolean isHighestAgreedProposals(Float agreedProposal) {
//         Log.i(TAG,"Checking if the highestAgreedProposal is- "+agreedProposal);
         for(int i=0;i<qList.size();i++){
             Message msg=qList.get(i);
             if(msg.isDeliverable()){
                 if (agreedProposal<msg.getPriorityValue()){
//                     Log.i(TAG,"Checking if the highestAgreedProposal Returning false");
                     return false;
                 }
             }
         }
//         Log.i(TAG,"Checking if the highestAgreedProposal Returning true");
         return true;
     }

     public boolean removeMessagesFromEmulator(String emulatorId) {
//         Log.i(TAG,"removeMessagesFromEmulator for- "+emulatorId);
         for(int i=0;i<qList.size();i++){
             Message msg=qList.get(i);
             if(msg.getEmulatorId().equals(emulatorId)){
                 qList.remove(msg);
             }
         }
         return true;
     }

    @Override
    public String toString() {
        String qListString="";
        for (Message msg : qList){
            qListString+=msg.toString()+"\n";
        }
        return "MessagePriorityQueue{" +
                "qList=" + qListString +
                '}';
    }
}