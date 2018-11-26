package cn.zyf.concurrent.blockqueue;


public interface RequestProcessor {

    void processorRequest(Request request);
}
