package cn.zyf.concurrent.program;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class ThreadPoolDemo implements Runnable{

    static ExecutorService executorService=MyExecutors.newMyExecutors();

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        ThreadPoolExecutor tpe=(ThreadPoolExecutor)executorService;
        for(int i=0;i<100;i++){
            executorService.execute(new ThreadPoolDemo());
        }
        executorService.shutdown();
    }
}
