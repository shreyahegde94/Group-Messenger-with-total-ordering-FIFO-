package edu.buffalo.cse.cse486586.groupmessenger2;
public class IDGeneratorUtil {
    public static Integer keyId=-1;
    public static class StaticIDGeneratorUtil {
        public static Integer getNewKeyId() {
            IDGeneratorUtil.keyId=IDGeneratorUtil.keyId+1;
            return IDGeneratorUtil.keyId;
        }
    }
}
