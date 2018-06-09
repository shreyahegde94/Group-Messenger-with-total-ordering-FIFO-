package edu.buffalo.cse.cse486586.groupmessenger2;
public class Message {

    protected int indexCount = 0;
    private float priorityValue;
    private String timestamp;

    @Override
    public String toString() {
        return "Message{" +
                "indexCount=" + indexCount +
                ", priorityValue=" + priorityValue +
                ", timestamp='" + timestamp + '\'' +
                ", deliverable=" + deliverable +
                ", messageContent='" + messageContent + '\'' +
                ", emulatorId='" + emulatorId + '\'' +
                ", noOfReplies=" + noOfReplies +
                '}';
    }

    private boolean deliverable;
    private String messageContent;
    private String emulatorId;
    private Integer noOfReplies;

    public Message(String timeStamp, String emulatorId, String messageContent, float p, boolean deliverable,Integer noOfReplies) {
        indexCount++;
        this.setTimestamp(timeStamp);
        this.setEmulatorId(emulatorId);
        this.setMessageContent(messageContent);
        this.setPriorityValue(p);
        this.setDeliverable(deliverable);
        this.setNoOfReplies(noOfReplies);

    }

    /**
     * @return the timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the deliverable
     */
    public boolean isDeliverable() {
        return deliverable;
    }

    /**
     * @param deliverable the deliverable to set
     */
    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    /**
     * @return the messageContent
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * @param messageContent the messageContent to set
     */
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    /**
     * @return the emulatorId
     */
    public String getEmulatorId() {
        return emulatorId;
    }

    /**
     * @param emulatorId the emulatorId to set
     */
    public void setEmulatorId(String emulatorId) {
        this.emulatorId = emulatorId;
    }

    /**
     * @return the noOfReplies
     */
    public Integer getNoOfReplies() {
        return noOfReplies;
    }

    /**
     * @param noOfReplies the noOfReplies to set
     */
    public void setNoOfReplies(Integer noOfReplies) {
        this.noOfReplies = noOfReplies;
    }



    /**
     * @return the priorityValue
     */
    public float getPriorityValue() {
        return priorityValue;
    }



    /**
     * @param priorityValue the priorityValue to set
     */
    public void setPriorityValue(float priorityValue) {
        this.priorityValue = priorityValue;
    }

}
