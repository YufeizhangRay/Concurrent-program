# Concurrent  
  
## 发编程学习实践  
  
### 什么情况下应该使用多线程  
  
线程出现的目的是什么？解决进程中多任务的实时性问题？其实简单来说，也就是解决“阻塞”的问题，阻塞的意思就是程序运行到某个函数或过程后等待某些事件发生而暂时停止 CPU 占用的情况，也就是说会使得 CPU 闲置。还有一些场景就是比如对于一个函数中的运算逻辑的性能问题，我们可以通过多线程的技术，使得一个函数中的多个逻辑运算通过多线程技术达到一个并行执行，从而提升性能。  
所以，多线程最终解决的就是“等待”的问题，所以简单总结的使用场景：
>通过并行计算提高程序执行性能  
需要等待网络、I/O响应导致耗费大量的执行时间，可以采用异步线程的方 式来减少阻塞。  
  
#### tomcat7以前的IO模型  
多线程的应用场景  
>客户端阻塞 如果客户端只有一个线程，这个线程发起读取文件的操作必须等待 IO 流返回，线程(客户端)才能做其他的事。  
>线程级别阻塞 BIO 客户端一个线程情况下，一个线程导致整个客户端阻塞。那么我们可以使用多线程，一部分线程在等待 IO 操作返回其他线程可以继续做
  其他的事。此时从客户端角度来说，客户端没有闲着。  
![]()  
  
### 如何应用多线程  
  
在 Java 中，有多种方式来实现多线程。继承 Thread 类、实现 Runnable 接 口、使用 ExecutorService、Callable、Future 实现带返回结果的多线程。  
  
#### 继承 Thread 类创建线程  
Thread 类本质上是实现了 Runnable 接口的一个实例，代表一个线程的实例。 启动线程的唯一方法就是通过 Thread 类的 start()实例方法。start()方法是一个 native 方法，它会启动一个新线程，并执行 run()方法。这种方式实现多线程很简单，通过自己的类直接 extend Thread，并复写 run()方法，就可以启动新线 程并执行自己定义的 run()方法。  
```
public class MyThread extends Thread {
   public void run() {
   System.out.println("MyThread.run()");
   }
   }
   MyThread myThread1 = new MyThread();
   MyThread myThread2 = new MyThread();
   myThread1.start();
   myThread2.start();
```
#### 实现 Runnable 接口创建线程  
如果自己的类已经 extends 另一个类，就无法直接 extends Thread，此时，可以实现一个 Runnable 接口。  
```
 public class MyThread extends OtherClass implements Runnable {
   public void run() {
   System.out.println("MyThread.run()");
   }
   }
```
#### 实现 Callable 接口通过 FutureTask 包装器来创建 Thread 线程
有的时候，我们可能需要让一步执行的线程在执行完成以后，提供一个返回值给到当前的主线程，主线程需要依赖这个值进行后续的逻辑处理，那么这个时候，就需要用到带返回值的线程了。Java 中提供了这样的实现方式
```
      public class CallableDemo implements Callable<String> {
   public static void main(String[] args) throws ExecutionException,
   InterruptedException {
   ExecutorService executorService=
   Executors.newFixedThreadPool(1);
   CallableDemo callableDemo=new CallableDemo();
   Future<String> future=executorService.submit(callableDemo);
   System.out.println(future.get());
   executorService.shutdown();
   }
   @Override
   public String call() throws Exception {
   int a=1;
   int b=2;
   System.out.println(a+b);
   return "执行结果:"+(a+b);
   }
   }
```
  
### 如何把多线程用得更加优雅  
  
合理的利用异步操作，可以大大提升程序的处理性能，下面这个案例，如果看过 zookeeper 源码的同学应该都见过通过阻塞队列以及多线程的方式，实现对请求的异步化处理，提升处理性能  
#### Request  
```
        public class Request {
      private String name;
      public String getName() {
   return name;

    }
      public void setName(String name) {
   this.name = name;
   }
      @Override
   public String toString() {
   return "Request{" +
   "name='" + name + '\'' +
   '}';
   }
   }
```
#### RequestProcessor  
```
public interface RequestProcessor {
      void processRequest(Request request);
   }
```
#### PrintProcessor
``` 
public class PrintProcessor extends Thread implements
   RequestProcessor{
      LinkedBlockingQueue<Request> requests = new
   LinkedBlockingQueue<Request>();
      private final RequestProcessor nextProcessor;
      public PrintProcessor(RequestProcessor nextProcessor) {
   this.nextProcessor = nextProcessor;
   }
      @Override
   public void run() {
   while (true) {

    try {
   Request request=requests.take();
   System.out.println("print data:"+request.getName());
   nextProcessor.processRequest(request);
   } catch (InterruptedException e) {
   e.printStackTrace();
   }
   }
   }
   //处理请求
   public void processRequest(Request request) {
   requests.add(request);
   }
   }
```
#### SaveProcessor
```
       public class SaveProcessor extends Thread implements
   RequestProcessor{
      LinkedBlockingQueue<Request> requests = new
   LinkedBlockingQueue<Request>();
      @Override
   public void run() {
   while (true) {
   try {
   Request request=requests.take();
   System.out.println("begin save request
   info:"+request);
   } catch (InterruptedException e) {
   e.printStackTrace();
   }
   }
   }
   //处理请求
   public void processRequest(Request request) {
   requests.add(request);
   }
    }
 ```
#### Demo
```
       public class Demo {
      PrintProcessor printProcessor;
      protected Demo(){
   SaveProcessor saveProcessor=new SaveProcessor();
   saveProcessor.start();
   printProcessor=new PrintProcessor(saveProcessor);
   printProcessor.start();
   }
      private void doTest(Request request){
   printProcessor.processRequest(request);
   }
      public static void main(String[] args) {
   Request request=new Request();
   request.setName("Mic");
   new Demo().doTest(request);
      }
   }
```
### Java并发编程的基础
线程作为操作系统调度的最小单元，并且能够让多线程同时执行，极大的提高了程序的性能，在多核环境下的优势更加明显。但是在使用多线程的过程中，如果对它的特性和原理不够理解的话，很容易造成各种问题。  
  
#### 线程的状态
Java 线程既然能够创建，那么也势必会被销毁，所以线程是存在生命周期的， 那么我们接下来从线程的生命周期开始去了解线程。  
线程一共有 6 种状态(NEW、RUNNABLE、BLOCKED、WAITING、 TIME_WAITING、TERMINATED)。  
>NEW:初始状态，线程被构建，但是还没有调用 start 方法。  
>RUNNABLED:运行状态，JAVA 线程把操作系统中的就绪和运行两种状态统一称为“运行中”。  
>BLOCKED:阻塞状态，表示线程进入等待状态,也就是线程因为某种原因放弃了 CPU 使用权，阻塞也分为几种情况。  
>>等待阻塞:运行的线程执行wait方法，jvm会把当前线程放入到等待队列。  
>>同步阻塞:运行的线程在获取对象的同步锁时，若该同步锁被其他线程锁占 用了，那么 jvm 会把当前的线程放入到锁池中。  
>>其他阻塞:运行的线程执行Thread.sleep或者t.join方法，或者发出了I/O 请求时，JVM 会把当前线程设置为阻塞状态，当 sleep 结束、join 线程终止、 io 处理完毕则线程恢复。  
>TIME_WAITING:超时等待状态，超时以后自动返回。  
>TERMINATED:终止状态，表示当前线程执行完毕。  
   
![]()  
  
#### 通过代码演示线程的状态  
编写如下代码  
```
       public class ThreadStatus {
      public static void main(String[] args) {
   //TIME_WAITING
   new Thread(()->{
   while(true){
   try {
   TimeUnit.SECONDS.sleep(100);
   } catch (InterruptedException e) {
   e.printStackTrace();
   }
   }
   },"timewaiting").start();

    //WAITING，线程在 ThreadStatus 类锁上通过 wait 进行等待 new Thread(()->{
while(true){
synchronized (ThreadStatus.class){
                     try {
                         ThreadStatus.class.wait();
                    } catch (InterruptedException e) {
                         e.printStackTrace();
} }
             }
         },"Waiting").start();
//线程在 ThreadStatus 加锁后，不会释放锁
new Thread(new BlockedDemo(),"BlockDemo-01").start(); new Thread(new BlockedDemo(),"BlockDemo-02").start();
}
static class BlockedDemo extends Thread{
public void run(){
synchronized (BlockedDemo.class){
                 while(true){
                     try {
                         TimeUnit.SECONDS.sleep(100);
                    } catch (InterruptedException e) {
                         e.printStackTrace();
                    }
} }
} }
}
```
#### 通过相应命令显示线程状态  
>打开终端或者命令提示符，键入“jps”，(JDK1.5 提供的一个显示当前所有 java 进程 pid 的命令)，可以获得相应进程的 pid。  
>根据上一步骤获得的 pid，继续输入 jstack pid(jstack 是 java 虚拟机自带的 一种堆栈跟踪工具。jstack 用于打印出给定的 java 进程 ID 或 core file 或远程 调试服务的 Java 堆栈信息)。  
  
### 线程的停止  
线程的启动过程大家都非常熟悉，但是记住，线程的终止，并不是简单的调用 stop 命令去。虽然 api 仍然可以调用，但是和其他的线程控制方法如 suspend、resume 一样都是过期了的不建议使用。就拿 stop 来说，stop 方法在结束一个线程时并不会保证线程的资源正常释放，因此会导致程序可能出现一些不确定的状态。  
要优雅的去中断一个线程，在线程中提供了一个 interrupt 方法。  
  
#### interrupt方法  
当其他线程通过调用当前线程的 interrupt 方法，表示向当前线程打个招呼， 告诉他可以中断线程的执行了，至于什么时候中断，取决于当前线程自己。  
线程通过检查资深是否被中断来进行相应，可以通过 isInterrupted()来判断是否被中断。  
通过下面这个例子，来实现了线程终止的逻辑。  
```
        public class InterruptDemo {
   private static int i;
   public static void main(String[] args) throws
   InterruptedException {
   Thread thread=new Thread(()->{
   while(!Thread.currentThread().isInterrupted()){
   i++;
   }
   System.out.println("Num:"+i);
   },"interruptDemo");
   thread.start();
   TimeUnit.SECONDS.sleep(1);
   thread.interrupt();
   }
    }
```
这种通过标识位或者中断操作的方式能够使线程在终止时有机会去清理资源，而不是武断地将线程停止，因此这种终止线程的做法显得更加安全和优雅。
  
#### Thread.interrupted  
上面的案例中，通过 interrupt，设置了一个标识告诉线程可以终止了，线程中还提供了静态方法 Thread.interrupted()对设置中断标识的线程复位。比如在上面的案例中，外面的线程调用 thread.interrupt 来设置中断标识，而在线程里面，又通过 Thread.interrupted 把线程的标识又进行了复位。  
```
        public class InterruptDemo {
   public static void main(String[] args) throws
   InterruptedException{
   Thread thread=new Thread(()->{
   while(true){
   boolean ii=Thread.currentThread().isInterrupted();
   if(ii){
   System.out.println("before:"+ii);
   Thread.interrupted();//对线程进行复位，中断标识为
   false
   System.out.println("after:"+Thread.currentThread()
   .isInterrupted());
   }
   }
   });
   thread.start();
   TimeUnit.SECONDS.sleep(1);
   thread.interrupt();//设置中断标识,中断标识为 true
   }
   }
 ```
   
#### 其他的线程复位  
除了通过 Thread.interrupted 方法对线程中断标识进行复位以外，还有一种被动复位的场景，就是对抛出 InterruptedException 异常的方法，在 InterruptedException 抛出之前，JVM 会先把线程的中断标识位清除，然后才会抛出 InterruptedException，这个时候如果调用 isInterrupted 方法，将会返回 false。
```
      public class InterruptDemo {
   public static void main(String[] args) throws
   InterruptedException{
   Thread thread=new Thread(()->{
   while(true){
   try {
   Thread.sleep(10000);
   } catch (InterruptedException e) {
   //抛出该异常，会将复位标识设置为 false
   e.printStackTrace();
   }
   }
   });
   thread.start();
   TimeUnit.SECONDS.sleep(1);
   thread.interrupt();//设置复位标识为 true
   TimeUnit.SECONDS.sleep(1);
   System.out.println(thread.isInterrupted());//false
   }
   }
```
有同学在问线程为什么要复位?首先我们来看看线程执行 interrupt 以后的源码是做了什么?  

thread.cpp  
```
      void Thread::interrupt(Thread* thread) {
   trace("interrupt", thread);
   debug_only(check_for_dangling_thread_pointer(thread);)
   }
```
os_linux.cpp  
```
void os::interrupt(Thread* thread) { assert(Thread::current() == thread ||
Threads_lock->owned_by_self(),
"possibility of dangling Thread pointer");
OSThread* osthread = thread->osthread();
if (!osthread->interrupted()) { osthread->set_interrupted(true);
// More than one thread can get here with the same value of
osthread,
// resulting in multiple notifications. We do, however, want the
store
// to interrupted() to be visible to other threads before we
execute unpark().
OrderAccess::fence();
ParkEvent * const slp = thread->_SleepEvent ; if (slp != NULL) slp->unpark() ;
}
}
// For JSR166. Unpark even if interrupt status already was set
if (thread->is_Java_thread()) ((JavaThread*)thread)->parker()->unpark();
ParkEvent * ev = thread->_ParkEvent ; if (ev != NULL) ev->unpark() ;
```
其实就是通过 unpark 去唤醒当前线程，并且设置一个标识位为 true。 并没有 所谓的中断线程的操作，所以实际上，线程复位可以用来实现多个线程之间的
通信。  

#### volatile标志位停止线程  
除了通过 interrupt 标识为去中断线程以外，我们还可以通过下面这种方式，定义一个 volatile 修饰的成员变量，来控制线程的终止。这实际上是应用了 volatile 能够实现多线程之间共享变量的可见性这一特点来实现的。  
```
public class VolatileDemo {
   private volatile static boolean stop=false;
   public static void main(String[] args) throws
   InterruptedException {
   Thread thread=new Thread(()->{
   int i=0;
   while(!stop){
   i++;
   }
   });
   thread.start();
   System.out.println("begin start thread");
   Thread.sleep(1000);
   stop=true;
   }
   }
```
  
### 线程的安全性问题  
  
大家都知道，线程会存在安全性问题，那接下来我们从原理层面去了解线程为什么会存在安全性问题，并且我们应该怎么去解决这类的问题。  
其实线程安全问题可以总结为: 可见性、原子性、有序性这几个问题，我们搞懂了这几个问题并且知道怎么解决，那么多线程安全性问题也就不是问题了。  
  
#### CPU高速缓存
线程是 CPU 调度的最小单元，线程涉及的目的最终仍然是更充分的利用计算机 处理的效能，但是绝大部分的运算任务不能只依靠处理器“计算”就能完成，处理器还需要与内存交互，比如读取运算数据、存储运算结果，这个 I/O 操作是很难消除的。而由于计算机的存储设备与处理器的运算速度差距非常大，所以现代计算机系统都会增加一层读写速度尽可能接近处理器运算速度的高速缓存来作为内存和处理器之间的缓冲:将运算需要使用的数据复制到缓存中，让运算能快速进行，当运算结束后再从缓存同步到内存之中。  
![]()  
  
高速缓存从下到上越接近 CPU 速度越快，同时容量也越小。现在大部分的处理 器都有二级或者三级缓存，从下到上依次为 L3 cache, L2 cache, L1 cache。缓
存又可以分为指令缓存和数据缓存，指令缓存用来缓存程序的代码，数据缓存。  
用来缓存程序的数据  
>L1 Cache，一级缓存，本地 core 的缓存，分成 32K 的数据缓存 L1d 和 32k 指 令缓存 L1i，访问 L1 需要 3cycles，耗时大约 1ns;  
>L2 Cache，二级缓存，本地 core 的缓存，被设计为 L1 缓存与共享的 L3 缓存 之间的缓冲，大小为 256K，访问 L2 需要 12cycles，耗时大约 3ns;  
>L3 Cache，三级缓存，在同插槽的所有 core 共享 L3 缓存，分为多个 2M 的 段，访问 L3 需要 38cycles，耗时大约 12ns;  
  
### 缓存一致性问题  
  
CPU-0 读取主存的数据，缓存到 CPU-0 的高速缓存中，CPU-1 也做了同样的事 情，而 CPU-1 把 count 的值修改成了 2，并且同步到 CPU-1 的高速缓存，但
是这个修改以后的值并没有写入到主存中，CPU-0 访问该字节，由于缓存没有 更新，所以仍然是之前的值，就会导致数据不一致的问题。
引发这个问题的原因是因为多核心 CPU 情况下存在指令并行执行，而各个 CPU 核心之间的数据不共享从而导致缓存一致性问题，为了解决这个问题，CPU 生产厂商提供了相应的解决方案。  
  
#### 总线锁  
当一个 CPU 对其缓存中的数据进行操作的时候，往总线中发送一个 Lock 信 号。其他处理器的请求将会被阻塞，那么该处理器可以独占共享内存。总线锁相当于把 CPU 和内存之间的通信锁住了，所以这种方式会导致 CPU 的性能下降，所以 P6 系列以后的处理器，出现了另外一种方式，就是缓存锁。  
  
#### 缓存锁  
如果缓存在处理器缓存行中的内存区域在 LOCK 操作期间被锁定，当它执行锁操作回写内存时，处理不在总线上声明 LOCK 信号，而是修改内部的缓存地址，然后通过缓存一致性机制来保证操作的原子性，因为缓存一致性机制会阻止同时修改被两个以上处理器缓存的内存区域的数据，当其他处理器回写已经被锁定的缓存行的数据时会导致该缓存行无效。  
所以如果声明了 CPU 的锁机制，会生成一个 LOCK 指令，会产生两个作用。  
>1.Lock 前缀指令会引起引起处理器缓存回写到内存，在 P6 以后的处理器中， LOCK 信号一般不锁总线，而是锁缓存。  
>2.一个处理器的缓存回写到内存会导致其他处理器的缓存无效。  
  
#### 缓存一致性协议  
处理器上有一套完整的协议，来保证 Cache 的一致性，比较经典的应该就是 MESI 协议了，它的方法是在 CPU 缓存中保存一个标记位，这个标记为有四种状态。
>M(Modified) 修改缓存，当前 CPU 缓存已经被修改，表示已经和内存中的数据不一致了。  
>I(Invalid) 失效缓存，说明 CPU 的缓存已经不能使用了。  
>E(Exclusive) 独占缓存，当前cpu的缓存和内存中数据保持一直，而且其他处理器没有缓存该数据。  
>S(Shared) 共享缓存，数据和内存中数据一致，并且该数据存在多个 cpu 缓存中。  
  
每个 Core 的 Cache 控制器不仅知道自己的读写操作，也监听其它 Cache 的读写操作，这就是嗅探(snooping)"协议。  
  
CPU 的读取会遵循几个原则  
>1.如果缓存的状态是 I，那么就从内存中读取，否则直接从缓存读取。  
>2.如果缓存处于 M 或者 E 的 CPU 嗅探到其他 CPU 有读的操作，就把自己的缓存写入到内存，并把自己的状态设置为S。  
>3.只有缓存状态是M或E的时候，CPU才可以修改缓存中的数据，修改后，缓存状态变为 MC。  
  
CPU 的优化执行  
除了增加高速缓存以为，为了更充分利用处理器内内部的运算单元，处理器可能会对输入的代码进行乱序执行优化，处理器会在计算之后将乱序执行的结果充足，保证该结果与顺序执行的结果一直，但并不保证程序中各个语句计算的先后顺序与输入代码中的顺序一致，这个是处理器的优化执行；还有一个就是编程语言的编译器也会有类似的优化，比如做指令重排来提升性能。  
  
#### 并发编程的问题  
前面说的和硬件有关的概念你可能听得有点蒙，还不知道他到底和软件有啥关系，其实原子性、可见性、有序性问题，是我们抽象出来的概念，他们的核心本质就是刚刚提到的缓存一致性问题、处理器优化问题导致的指令重排序问题。  
  
比如缓存一致性就导致可见性问题、处理器的乱序执行会导致原子性问题、指令重排会导致有序性问题。为了解决这些问题，所以在 JVM 中引入了 JMM 的概念。  
  
### Java内存模型  
  
内存模型定义了共享内存系统中多线程程序读写操作行为的规范，来屏蔽各种 硬件和操作系统的内存访问差异，来实现 Java 程序在各个平台下都能达到一致
的内存访问效果。Java 内存模型的主要目标是定义程序中各个变量的访问规则，也就是在虚拟机中将变量存储到内存以及从内存中取出变量(这里的变量，指的是共享变量，也就是实例对象、静态字段、数组对象等存储在堆内存中的变量。而对于局部变量这类的，属于线程私有，不会被共享)这类的底层细节。通过这些规则来规范对内存的读写操作，从而保证指令执行的正确性。  
  
它与处理器有关、与缓存有关、与并发有关、与编译器也有关。他解决了 CPU多级缓存、处理器优化、指令重排等导致的内存访问问题，保证了并发场景下的可见性、原子性和有序性。内存模型解决并发问题主要采用两种方式：限制处理器优化和使用内存屏障。  
   
  
Java 内存模型定义了线程和内存的交互方式，在 JMM 抽象模型中，分为主内存、工作内存。主内存是所有线程共享的，工作内存是每个线程独有的。线程对变量的所有操作(读取、赋值)都必须在工作内存中进行，不能直接读写主内存中的变量。并且不同的线程之间无法访问对方工作内存中的变量，线程间的变量值的传递都需要通过主内存来完成，他们三者的交互关系如下。  
![]()  
  
所以，总的来说，JMM 是一种规范，目的是解决由于多线程通过共享内存进行通信时，存在的本地内存数据不一致、编译器会对代码指令重排序、处理器会对代码乱序执行等带来的问题。目的是保证并发编程场景中的原子性、可见性和有序性。  

### JMM怎么解决原子性、可见性、有序性的问题?  
  
在Java中提供了一系列和并发处理相关的关键字，比如volatile、Synchronized、final、juc等，这些就是Java内存模型封装了底层的实现后提供给开发人员使用的关键字，在开发多线程代码的时候，我们可以直接使用 synchronized等关键词来控制并发，使得我们不需要关心底层的编译器优化、缓存一致性的问题了，所以在Java内存模型中，除了定义了一套规范，还提供了开放的指令在底层进行封装后，提供给开发人员使用。  

#### 原子性保障
在Java中提供了两个高级的字节码指令monitorenter和monitorexit，在Java中对应的Synchronized来保证代码块内的操作是原子的。  
  
#### 可见性  
Java中的volatile关键字提供了一个功能，那就是被其修饰的变量在被修改后可以立即同步到主内存，被其修饰的变量在每次是用之前都从主内存刷新。因此，可以使用volatile来保证多线程操作时变量的可见性。  
除了volatile，Java中的synchronized和final两个关键字也可以实现可见性。  

#### 有序性  
在Java中，可以使用synchronized和volatile来保证多线程之间操作的有序性。实现方式有所区别:  
volatile关键字会禁止指令重排。synchronized关键字保证同一时刻只允许一条线程操作。  
  
### volatile如何保证可见性  
  
下载hsdis工具 ，https://sourceforge.net/projects/fcml/files/fcml-1.1.1/hsdis-1.1.1-win32-amd64.zip/download  
解压后存放到jre目录的server路径下 然后跑main函数，跑main函数之前，加入如下虚拟机参数:
-server -Xcomp -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly - 
XX:CompileCommand=compileonly,*App.getInstance(替换成实际运行的代码)  
  
volatile变量修饰的共享变量，在进行写操作的时候会多出一个lock前缀的汇编指令，这个指令在前面我们讲解CPU 高速缓存的时候提到过，会触发总线锁或者缓存锁，通过缓存一致性协议来解决可见性问题。  
  
对于声明了volatile的变量进行写操作，JVM就会向处理器发送一条Lock前缀的指令，把这个变量所在的缓存行的数据写回到系统内存，再根据我们前面提到过的MESI的缓存一致性协议，来保证多CPU下的各个高速缓存中的数据的一致性。  
  
#### volatile防止指令重排序  
指令重排的目的是为了最大化的提高CPU利用率以及性能，CPU的乱序执行优化在单核时代并不影响正确性，但是在多核时代的多线程能够在不同的核心上实现真正的并行，一旦线程之间共享数据，就可能会出现一些不可预料的问题。  
  
指令重排序必须要遵循的原则是，不影响代码执行的最终结果，编译器和处理器不会改变存在数据依赖关系的两个操作的执行顺序(这里所说的数据依赖性仅仅是针对单个处理器中执行的指令和单个线程中执行的操作)。  
  
这个语义，实际上就是as-if-serial语义，不管怎么重排序，单线程程序的执行结果不会改变，编译器、处理器都必须遵守as-if-serial语义。  

#### 多核心多线程下的指令重排影响  
```
private static int x = 0, y = 0;
private static int a = 0, b = 0;
public static void main(String[] args) throws InterruptedException {
    Thread t1 = new Thread(() -> {
a = 1;
x = b; });
    Thread t2 = new Thread(() -> {
        b = 1;
y = a; });
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    System.out.println("x=" + x + "->y=" + y);
}
```
如果不考虑编译器重排序和缓存可见性问题，上面这段代码可能会出现的结果是 x=0,y=1; x=1,y=0; x=1,y=1这三种结果，因为可能是先后执行t1/t2，也可能是反过来，还可能是t1/t2交替执行，但是这段代码的执行结果也有可能是x=0,y=0。这就是在乱序执行的情况下会导致的一种结果，因为线程t1内部的两行代码之间不存在数据依赖，因 此可以把x=b乱序到a=1之前;同时线程t2中的y=a也可以早于t1中的a=1执行，那么他们的执行顺序可能是
>t1:x=b  
>t2:b=1  
>t2:y=a  
>t1:a=1  
  
所以从上面的例子来看，重排序会导致可见性问题。但是重排序带来的问题的严重性远远大于可见性，因为并不是所有指令都是简单的读或写，比如DCL的部分初始化问题。所以单纯的解决可见性问题还不够，还需要解决处理器重排序问题。  

### 内存屏障  
  
内存屏障需要解决我们前面提到的两个问题，一个是编译器的优化乱序和CPU的执行乱序，我们可以分别使用优化屏障和内存屏障这两个机制来解决。  
  
#### 从CPU层面来了解一下什么是内存屏障  
CPU的乱序执行，本质还是，由于在多CPU的机器上，每个CPU都存在cache，当一个特定数据第一次被特定一个 CPU获取时，由于在该CPU缓存中不存在，就会从内存中去获取，被加载到CPU高速缓存中后就能从缓存中快速访问。当某个CPU进行写操作时，它必须确保其他的CPU已经将这个数据从他们的缓存中移除，这样才能让其他CPU 安全的修改数据。显然，存在多个cache时，我们必须通过一个cache一致性协议来避免数据不一致的问题，而这个通讯的过程就可能导致乱序访问的问题，也就是运行时的内存乱序访问。  
  
现在的CPU架构都提供了内存屏障功能，在x86的cpu中，实现了相应的内存屏障写屏障(store barrier)、读屏障(load barrier)和全屏障(Full Barrier)，主要的作用是
>1.防止指令之间的重排序  
>2.保证数据的可见性  
  
#### store barrier  
store barrier称为写屏障，相当于storestore barrier, 强制所有在storestore内存屏障之前的所有执行，都要在该内存屏障之前执行，并发送缓存失效的信号。所有在storestore barrier指令之后的store指令，都必须在 storestore barrier屏障之前的指令执行完后再被执行。也就是进制了写屏障前后的指令进行重排序，是的所有 store barrier之前发生的内存更新都是可见的(这里的可见指的是修改值可见以及操作结果可见)。
![]()  
  
#### load barrier  
load barrier称为读屏障，相当于loadload barrier，强制所有在load barrier读屏障之后的load指令，都在load barrier屏障之后执行。也就是进制对load barrier读屏障前后的load指令进行重排序， 配合store barrier，使得所有store barrier之前发生的内存更新，对load barrier之后的load操作是可见的。  
![]()  
  
#### Full barrier  
full barrier成为全屏障，相当于storeload，是一个全能型的屏障，因为它同时具备前面两种屏障的效果。强制了 所有在storeload barrier之前的store/load指令，都在该屏障之前被执行，所有在该屏障之后的的store/load指 令，都在该屏障之后被执行。禁止对storeload屏障前后的指令进行重排序。  
![]()  
  
总结：内存屏障只是解决顺序一致性问题，不解决缓存一致性问题，缓存一致性是由cpu的缓存锁以及MESI协议来完成的。而缓存一致性协议只关心缓存一致性，不关心顺序一致性，所以这是两个问题。  
  
#### 编译器层面如何解决指令重排序问题  
在编译器层面，通过volatile关键字，取消编译器层面的缓存和重排序。保证编译程序时在优化屏障之前的指令不会在优化屏障之后执行。这就保证了编译时期的优化不会影响到实际代码逻辑顺序。  
如果硬件架构本身已经保证了内存可见性，那么volatile就是一个空标记，不会插入相关语义的内存屏障。如果硬件架构本身不进行处理器重排序，有更强的重排序语义，那么volatile就是一个空标记，不会插入相关语义的内存屏障。  
在JMM中把内存屏障指令分为4类，通过在不同的语义下使用不同的内存屏障来进制特定类型的处理器重排序，从而来保证内存的可见性。  
>LoadLoad Barriers, load1 ; LoadLoad; load2 , 确保load1数据的装载优先于load2及所有后续装载指令的装载。  
>StoreStore Barriers，store1; storestore;store2 , 确保store1数据对其他处理器可见优先于store2及所有后续指令的存储。  
>LoadStore Barries， load1;loadstore;store2, 确保load1数据装载优先于store2以及后续的存储指令刷新到内存。  
>StoreLoad Barries， store1; storeload;load2, 确保store1数据对其他处理器变得可见，优先于load2及所有后续装载指令的装载；这条内存屏障指令是一个全能型的屏障，在前面讲cpu层面的内存屏障的时候有提到，它同时具有其他3条屏障的效果。  
  
### volatile为什么不能保证原子性  
  
我们通过下面一个例子，对一个通过volatile修饰的值进行递增。
```
public class Demo {
    volatile int i;
    public void incr(){
i++; }
    public static void main(String[] args) {
        new Demo().incr();
} }
```
然后通过javap -c Demo.class，去查看字节码。  
对一个原子递增的操作，会分为三个步骤:  
>1.读取volatile变量的值到local;  
>2.增加变量的值;  
>3.把local的值写回让其他线程可见  
  
  
### synchronized的使用  
  
在多线程并发编程中synchronized一直是元老级角色，很多人都会称呼它为重量级锁。但是，随着Java SE 1.6对 synchronized进行了各种优化之后，有些情况下它就并不那么重了，Java SE 1.6中为了减少获得锁和释放锁带来的性能消耗而引入的偏向锁和轻量级锁，以及锁的存储结构和升级过程。我们仍然沿用前面使用的案例，然后通过 synchronized关键字来修饰在inc的方法上，再看看执行结果。  
```
public class Demo{
    private static int count=0;
    public static void inc(){
        synchronized (Demo.class) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
count++; }
    }
    public static void main(String[] args) throws InterruptedException {
        for(int i=0;i<1000;i++){
            new Thread(()->Demo.inc()).start();
        }
        Thread.sleep(3000);
System.out.println("运行结果"+count); }
}
```
  
#### synchronized的三种应用方式  
synchronized有三种方式来加锁，分别是  
>1.修饰实例方法，作用于当前实例加锁，进入同步代码前要获得当前实例的锁。  
>2.静态方法，作用于当前类对象加锁，进入同步代码前要获得当前类对象的锁。  
>3.修饰代码块，指定加锁对象，对给定对象加锁，进入同步代码库前要获得给定对象的锁。  
  
#### synchronized括号后面的对象  
synchronized扩后后面的对象是一把锁，在java中任意一个对象都可以成为锁，简单来说，我们把object比喻是一个key，拥有这个key的线程才能执行这个方法，拿到这个key以后在执行方法过程中，这个key是随身携带的，并且只有一把。如果后续的线程想访问当前方法，因为没有key所以不能访问只能在门口等着，等之前的线程把key放回去。所以，synchronized锁定的对象必须是同一个，如果是不同对象，就意味着是不同的房间的钥匙，对于访问者来说是没有任何影响的。  
  
#### synchronized的字节码指令  
通过javap -v 来查看对应代码的字节码指令，对于同步块的实现使用了monitorenter和monitorexit指令，前面我们在讲JMM的时候，提到过这两个指令，他们隐式的执行了Lock和UnLock操作，用于提供原子性保证。 monitorenter指令插入到同步代码块开始的位置、monitorexit指令插入到同步代码块结束位置，jvm需要保证每个monitorenter都有一个monitorexit对应。  
  
这两个指令，本质上都是对一个对象的监视器(monitor)进行获取，这个过程是排他的，也就是说同一时刻只能有一个线程获取到由synchronized所保护对象的监视器
线程执行到monitorenter指令时，会尝试获取对象所对应的monitor所有权，也就是尝试获取对象的锁；而执行 monitorexit，就是释放monitor的所有权。  
  
### synchronized的锁的原理  
  
jdk1.6以后对synchronized锁进行了优化，包含偏向锁、轻量级锁、重量级锁。在了解synchronized锁之前，我们需要了解两个重要的概念，一个是对象头、另一个是monitor。
#### Java对象头  
在Hotspot虚拟机中，对象在内存中的布局分为三块区域：对象头、实例数据和对齐填充。Java对象头是实现 synchronized的锁对象的基础，一般而言，synchronized使用的锁对象是存储在Java对象头里。它是轻量级锁和偏向锁的关键。  
  
#### Mawrk Word  
Mark Word用于存储对象自身的运行时数据，如哈希码(HashCode)、GC分代年龄、锁状态标志、线程持有的锁、偏向线程 ID、偏向时间戳等等。Java对象头一般占有两个机器码(在32位虚拟机中，1个机器码等于4字节，也就是32bit)。  
![]()  
  
#### 在源码中的体现 
如果想更深入了解对象头在JVM源码中的定义，需要关心几个文件，oop.hpp/markOop.hpp 。  
oop.hpp，每个 Java Object 在 JVM 内部都有一个 native 的 C++ 对象 oop/oopDesc 与之对应。先在oop.hpp中看 oopDesc的定义
![]()  
  
_mark 被声明在 oopDesc 类的顶部，所以这个 _mark 可以认为是一个 头部, 前面我们讲过头部保存了一些重要的状态和标识信息，在markOop.hpp文件中有一些注释说明markOop的内存布局。  
  
Monitor  
什么是Monitor?我们可以把它理解为一个同步工具，也可以描述为一种同步机制。所有的Java对象是天生的 Monitor，每个object的对象里 markOop->monitor() 里可以保存ObjectMonitor的对象。从源码层面分析一下 monitor对象。  
  
>oop.hpp下的oopDesc类是JVM对象的顶级基类，所以每个object对象都包含markOop。  
>markOop.hpp**中** markOopDesc继承自oopDesc，并扩展了自己的monitor方法，这个方法返回一个 ObjectMonitor指针对象。  
>objectMonitor.hpp,在hotspot虚拟机中，采用ObjectMonitor类来实现monitor。  
![]()  
  
### synchronized的锁升级和获取过程  
  
了解了对象头以及monitor以后，接下来去分析synchronized的锁的实现，就会非常简单了。前面讲过 synchronized的锁是进行过优化的，引入了偏向锁、轻量级锁。锁的级别从低到高逐步升级，无锁->偏向锁->轻量 级锁->重量级锁。  
  
#### 自旋锁(CAS)  
自旋锁就是让不满足条件的线程等待一段时间，而不是立即挂起。看持有锁的线程是否能够很快释放锁。怎么自旋呢?其实就是一段没有任何意义的循环。  
  
虽然它通过占用处理器的时间来避免线程切换带来的开销，但是如果持有锁的线程不能在很快释放锁，那么自旋的线程就会浪费处理器的资源，因为它不会做任何有意义的工作。所以，自旋等待的时间或者次数是有一个限度的，如果自旋超过了定义的时间仍然没有获取到锁，则该线程应该被挂起。  
  
#### 偏向锁  
大多数情况下，锁不仅不存在多线程竞争，而且总是由同一线程多次获得，为了让线程获得锁的代价更低而引入了偏向锁。当一个线程访问同步块并获取锁时，会在对象头和栈帧中的锁记录里存储锁偏向的线程ID，以后该线程在进入和退出同步块时不需要进行CAS操作来加锁和解锁，只需简单地测试一下对象头的Mark Word里是否存储着指 向当前线程的偏向锁。如果测试成功，表示线程已经获得了锁。如果测试失败，则需要再测试一下Mark Word中偏向锁的标识是否设置成1(表示当前是偏向锁)：如果没有设置，则使用CAS竞争锁；如果设置了，则尝试使用CAS 将对象头的偏向锁指向当前线程。  
  
#### 轻量级锁  
引入轻量级锁的主要目的是在多没有多线程竞争的前提下，减少传统的重量级锁使用操作系统互斥量产生的性能消耗。当关闭偏向锁功能或者多个线程竞争偏向锁导致偏向锁升级为轻量级锁，则会尝试获取轻量级锁。  
  
#### 重量级锁  
重量级锁通过对象内部的监视器(monitor)实现，其中monitor的本质是依赖于底层操作系统的Mutex Lock实现，操作系统实现线程之间的切换需要从用户态到内核态的切换，切换成本非常高。  
  
前面我们在讲Java对象头的时候，讲到了monitor这个对象，在hotspot虚拟机中，通过ObjectMonitor类来实现 monitor。他的锁的获取过程的体现会简单很多。  
![]()  
  

### wait和notify  
wait和notify是用来让线程进入等待状态以及使得线程唤醒的两个操作
```
   public class ThreadWait extends Thread{
    private Object lock;
    public ThreadWait(Object lock) {
        this.lock = lock;
}
@Override
    public void run() {
        synchronized (lock){
} }
System.out.println("开始执行 thread wait"); try {
        lock.wait();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
System.out.println("执行结束 thread wait"); }
```
```
 public class ThreadNotify extends Thread{
    private Object lock;
    public ThreadNotify(Object lock) {
    this.lock = lock;
}
@Override
public void run() {
    synchronized (lock){
} }
}
System.out.println("开始执行 thread notify"); lock.notify();
System.out.println("执行结束 thread notify");
```
  
#### wait和notify的原理  
调用wait方法，首先会获取监视器锁，获得成功以后，会让当前线程进入等待状态进入等待队列并且释放锁；然后当其他线程调用notify或者notifyall以后，会选择从等待队列中唤醒任意一个线程，而执行完notify方法以后，并不会立马唤醒线程，原因是当前的线程仍然持有这把锁，处于等待状态的线程无法获得锁。必须要等到当前的线程执 行完按monitorexit指令以后，也就是锁被释放以后，处于等待队列中的线程就可以开始竞争锁了。
![]()  
  
#### wait和notify为什么需要在synchronized里面 
wait方法的语义有两个，一个是释放当前的对象锁、另一个是使得当前线程进入阻塞队列，而这些操作都和监视器是相关的，所以wait必须要获得一个监视器锁。  
而对于notify来说也是一样，它是唤醒一个线程，既然要去唤醒，首先得知道它在哪里?所以就必须要找到这个对 象获取到这个对象的锁，然后到这个对象的等待队列中去唤醒一个线程。  
  
### 同步锁  
  
我们知道，锁是用来控制多个线程访问共享资源的方式，一般来说，一个锁能够防止多个线程同时访问共享资源，在Lock接口出现之前，Java应用程序只能依靠synchronized关键字来实现同步锁的功能，在java5以后，增加了JUC 的并发包且提供了Lock接口用来实现锁的功能，它提供了与synchroinzed关键字类似的同步功能，只是它比 synchronized更灵活，能够显示的获取和释放锁。  

### Lock的初步使用  
  
Lock是一个接口，核心的两个方法lock和unlock，它有很多的实现，比如ReentrantLock、ReentrantReadWriteLock。  
  
#### ReentrantLock  
重入锁，表示支持重新进入的锁，也就是说，如果当前线程t1通过调用lock方法获取了锁之后，再次调用lock，是不会再阻塞去获取锁的，直接增加重试次数就行了。  
```
    public class AtomicDemo {
    private static int count=0;
    static Lock lock=new ReentrantLock();
    public static void inc(){
        lock.lock();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
count++;
        lock.unlock();
    }
    public static void main(String[] args) throws InterruptedException {
        for(int i=0;i<1000;i++){
            new Thread(()->{AtomicDemo.inc();}).start();;
        }
        Thread.sleep(3000);
                 System.out.println("result:"+count);
    }
}
```
    
#### ReentrantReadWriteLock  
我们以前理解的锁，基本都是排他锁，也就是这些锁在同一时刻只允许一个线程进行访问，而读写所在同一时刻可以允许多个线程访问，但是在写线程访问时，所有的读线程和其他写线程都会被阻塞。读写锁维护了一对锁，一个 读锁、一个写锁；一般情况下，读写锁的性能都会比排它锁好，因为大多数场景读是多于写的。在读多于写的情况 下，读写锁能够提供比排它锁更好的并发性和吞吐量。  
```
   public class LockDemo {
    static Map<String,Object> cacheMap=new HashMap<>();
    static ReentrantReadWriteLock rwl=new ReentrantReadWriteLock();
    static Lock read=rwl.readLock();
    static Lock write=rwl.writeLock();
public static final Object get(String key) { System.out.println("开始读取数据"); read.lock(); //读锁
try {
            return cacheMap.get(key);
        }finally {
            read.unlock();
        }
    }
    public static final Object put(String key,Object value){
} }
write.lock(); System.out.println("开始写数据"); try{
    return cacheMap.put(key,value);
}finally {
    write.unlock();
}
```
在这个案例中，通过hashmap来模拟了一个内存缓存，然后使用读写所来保证这个内存缓存的线程安全性。当执行读操作的时候，需要获取读锁，在并发访问的时候，读锁不会被阻塞，因为读操作不会影响执行结果。  
在执行写操作是，线程必须要获取写锁，当已经有线程持有写锁的情况下，当前线程会被阻塞，只有当写锁释放以后，其他读写操作才能继续执行。使用读写锁提升读操作的并发性，也保证每次写操作对所有的读写操作的可见性。  
>读锁与读锁可以共享  
>读锁与写锁不可以共享(排他)  
>写锁与写锁不可以共享(排他)  

### Lock和synchronized的简单对比 
通过我们对Lock的使用以及对synchronized的了解，基本上可以对比出这两种锁的区别了。  
>从层次上，一个是关键字、一个是类， 这是最直观的差异。  
>从使用上，lock具备更大的灵活性，可以控制锁的释放和获取; 而synchronized的锁的释放是被动的，当出现 异常或者同步代码块执行完以后，才会释放锁。  
>lock可以判断锁的状态、而synchronized无法做到。  
>lock可以实现公平锁、非公平锁；而synchronized只有非公平锁。  
  
### AQS  
  
Lock之所以能实现线程安全的锁，主要的核心是 AQS(AbstractQueuedSynchronizer)，AbstractQueuedSynchronizer提供了一个FIFO队列，可以看做是一个用来实现锁以及其他需要同步功能的框架。这里简称该类为AQS。AQS的使用依靠继承来完成，子类通过继承自AQS并实现所需的方法来管理同步状态。例如常见的ReentrantLock，CountDownLatch等AQS的两种功能。  
  
从使用上来说，AQS的功能可以分为两种：独占和共享。  
  
独占锁模式下，每次只能有一个线程持有锁，比如前面给大家演示的ReentrantLock就是以独占方式实现的互斥锁。  
  
共享锁模式下，允许多个线程同时获取锁，并发访问共享资源，比如ReentrantReadWriteLock。  
  
很显然，独占锁是一种悲观保守的加锁策略，它限制了读/读冲突，如果某个只读线程获取锁，则其他读线程都只能等待，这种情况下就限制了不必要的并发性，因为读操作并不会影响数据的一致性。共享锁则是一种乐观锁，它放宽了加锁策略，允许多个执行读操作的线程同时访问共享资源。  
  
### AQS的内部实现  
  
同步器依赖内部的同步队列(一个FIFO双向队列)来完成同步状态的管理，当前线程获取同步状态失败时，同步器会将当前线程以及等待状态等信息构造成为一个节点(Node)并将其加入同步队列，同时会阻塞当前线程，当同步状态释放时，会把首节点中的线程唤醒，使其再次尝试获取同步状态。  
Node的主要属性如下  
```
static final class Node {
int waitStatus; //表示节点的状态，包含cancelled(取消);condition 表示节点在等待condition
也就是在condition队列中
Node prev; //前继节点
Node next; //后继节点
Node nextWaiter; //存储在condition队列中的后继节点 Thread thread; //当前线程
}
```
AQS类底层的数据结构是使用双向链表，是队列的一种实现。包括一个head节点和一个tail节点，分别表示头结点和尾节点，其中头结点不存储Thread，仅保存next结点的引用。  
![]()  
  
当一个线程成功地获取了同步状态(或者锁)，其他线程将无法获取到同步状态，转而被构造成为节点并加入到同步队列中，而这个加入队列的过程必须要保证线程安全，因此同步器提供了一个基于CAS的设置尾节点的方法：compareAndSetTail(Node expect,Nodeupdate)，它需要传递当前线程“认为”的尾节点和当前节点，只有设置成功后，当前节点才正式与之前的尾节点建立关联。  
![]()  
  
同步队列遵循FIFO，首节点是获取同步状态成功的节点，首节点的线程在释放同步状态时，将会唤醒后继节点，而后继节点将会在获取同步状态成功时将自己设置为首节点。  
![]()  
  
设置首节点是通过获取同步状态成功的线程来完成的，由于只有一个线程能够成功获取到同步状态，因此设置头节点的方法并不需要使用CAS来保证，它只需要将首节点设置成为原首节点的后继节点并断开原首节点的next引用即可。  
  
#### compareAndSet  
AQS中，除了本身的链表结构以外，还有一个很关键的功能，就是CAS，这个是保证在多线程并发的情况下保证线程安全的前提下去把线程加入到AQS中的方法，可以简单理解为乐观锁。  
```
private final boolean compareAndSetHead(Node update) {
    return unsafe.compareAndSwapObject(this, headOffset, null, update);
}
```
这个方法里面，首先用到了unsafe类(Unsafe类是在sun.misc包下，不属于Java标准。但是很多Java的基础类库，包括一些被 广泛使用的高性能开发库都是基于Unsafe类开发的，比如Netty、Hadoop、Kafka等；Unsafe可认为是Java中留下的后门，提供了一些低层次操作，如直接内存访问、线程调度等)， 然后调用了compareAndSwapObject这个方法。
```
 public final native boolean compareAndSwapObject(Object var1, long var2, Object var4, Object var5);
```
这个是一个native方法， 第一个参数为需要改变的对象，第二个为偏移量(即之前求出来的headOffset的值)，第三个参数为期待的值，第四个为更新后的值。  
整个方法的作用是如果当前时刻的值等于预期值var4相等，则更新为新的期望值 var5，如果更新成功，则返回 true，否则返回false。  
这里传入了一个headOffset，这个headOffset是什么呢？在下面的代码中，通过unsafe.objectFieldOffset
![]()  
  
然后通过反射获取了AQS类中的成员变量，并且这个成员变量被volatile修饰的。  
![]()  
  
#### unsafe.objectFieldOffset 
headOffset这个是指类中相应字段在该类的偏移量，在这里具体即是指head这个字段在AQS类的内存中相对于该类首地址的偏移量。  
  
一个Java对象可以看成是一段内存，每个字段都得按照一定的顺序放在这段内存里，通过这个方法可以准确地告诉你某个字段相对于对象的起始内存地址的字节偏移。用于在后面的compareAndSwapObject中，去根据偏移量找 到对象在内存中的具体位置。  
  
这个方法在unsafe.cpp文件中，代码如下
```
  UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapObject(JNIEnv *env, jobject unsafe, jobject
obj, jlong offset, jobject e_h, jobject x_h))
UnsafeWrapper("Unsafe_CompareAndSwapObject"); oop x = JNIHandles::resolve(x_h); // 新值
oop e = JNIHandles::resolve(e_h); // 预期值 oop p = JNIHandles::resolve(obj);
HeapWord* addr = (HeapWord *)index_oop_from_field_offset_long(p, offset);// 在内存中的 具体位置
oop res = oopDesc::atomic_compare_exchange_oop(x, addr, e, true);// 调用了另一个方法，实 际上就是通过cas操作来替换内存中的值是否成功
jboolean success = (res == e); // 如果返回的res等于e，则判定满足compare条件(说明res应该为 内存中的当前值)，但实际上会有ABA的问题
if (success) // success为true时，说明此时已经交换成功(调用的是最底层的cmpxchg指令) update_barrier_set((void*)addr, x); // 每次Reference类型数据写操作时，都会产生一个Write
Barrier暂时中断操作，配合垃圾收集器 return success;
UNSAFE_END
```
所以其实compareAndSet这个方法，最终调用的是unsafe类的compareAndSwap，这个指令会对内存中的共享数据做原子的读写操作。  

>1.首先，cpu会把内存中将要被更改的数据与期望值做比较。  
>2.然后，当两个值相等时，cpu才会将内存中的对象替换为新的值。否则，不做变更操作。   
>3. 最后，返回操作执行结果。  
  
很显然，这是一种乐观锁的实现思路。  

### ReentrantLock的实现原理分析  
  
之所以叫重入锁是因为同一个线程如果已经获得了锁，那么后续该线程调用lock方法时不需要再次获取锁，也就是不会阻塞；重入锁提供了两种实现，一种是非公平的重入锁，另一种是公平的重入锁。怎么理解公平和非公平呢?  
如果在绝对时间上，先对锁进行获取的请求一定先被满足获得锁，那么这个锁就是公平锁，反之，就是不公平的。  
简单来说公平锁就是等待时间最长的线程最优先获取锁。  
#### 非公平锁的实现流程时序图  
![]()  
  
### ReentrantLock源码分析  
  
#### ReentrantLock.lock  
```
   public void lock() {
    sync.lock();
}
```
这个是获取锁的入口，调用了sync.lock; sync是一个实现了AQS的抽象类，这个类的主要作用是用来实现同步控制的，并且sync有两个实现，一个是NonfairSync(非公平锁)、另一个是FailSync(公平锁); 我们先来分析一下非公平锁的实现。  

#### NonfairSync.lock  
```
   final void lock() {
if (compareAndSetState(0, 1)) //这是跟公平锁的主要区别,一上来就试探锁是否空闲,如果可以插队，
则设置获得锁的线程为当前线程 //exclusiveOwnerThread属性是AQS从父类AbstractOwnableSynchronizer中继承的属性，用来保存当前占用 同步状态的线程
        setExclusiveOwnerThread(Thread.currentThread());
    else
}
acquire(1); //尝试去获取锁
```
compareAndSetState，这个方法在前面提到过了，再简单讲解一下，通过cas算法去改变state的值，而这个state 是什么呢? 在AQS中存在一个变量state，对于ReentrantLock来说，如果state=0表示无锁状态、如果state>0表示有锁状态。  
  
所以在这里，是表示当前的state如果等于0，则替换为1，如果替换成功表示获取锁成功了。  
  
由于ReentrantLock是可重入锁，所以持有锁的线程可以多次加锁，经过判断加锁线程就是当前持有锁的线程时(即 exclusiveOwnerThread==Thread.currentThread())，即可加锁，每次加锁都会将state的值+1，state等于几，就代表当前持有锁的线程加了几次锁，解锁时每解一次锁就会将state减1，state减到0后，锁就被释放掉，这时其它线程可以加锁。  

AbstractQueuedSynchronizer.acquire  
如果CAS操作未能成功，说明state已经不为0，此时继续acquire(1)操作，acquire是AQS中的方法当多个线程同时进入这个方法时，首先通过cas去修改state的状态，如果修改成功表示竞争锁成功，竞争失败的，tryAcquire会返回 false。  
```
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
selfInterrupt();
}
```
这个方法的主要作用是  
>尝试获取独占锁，获取成功则返回，否则
>自旋获取锁，并且判断中断标识，如果中断标识为true，则设置线程中断  
>addWaiter方法把当前线程封装成Node，并添加到队列的尾部  
  
#### NonfairSync.tryAcquire
tryAcquire方法尝试获取锁，如果成功就返回，如果不成功，则把当前线程和等待状态信息构适成一个Node节点，并将结点放入同步队列的尾部。然后为同步队列中的当前节点循环等待获取锁，直到成功。  
```
  protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}
```
  
#### nofairTryAcquire   
这里可以看非公平锁的涵义，即获取锁并不会严格根据争用锁的先后顺序决定。这里的实现逻辑类似synchroized关键字的偏向锁的做法，即可重入而不用进一步进行锁的竞争，也解释了ReentrantLock中Reentrant的意义。  
```
  final boolean nonfairTryAcquire(int acquires) {
final Thread current = Thread.currentThread();
int c = getState(); //获取当前的状态，前面讲过，默认情况下是0表示无锁状态 if (c == 0) {
if (compareAndSetState(0, acquires)) { //通过cas来改变state状态的值，如果更新成功，表 示获取锁成功, 这个操作外部方法lock()就做过一次，这里再做只是为了再尝试一次，尽量以最简单的方式获取锁。
            setExclusiveOwnerThread(current);
            return true;
        }
}
else if (current == getExclusiveOwnerThread()) {//如果当前线程等于获取锁的线程，表示重入， 直接累加重入次数
int nextc = c + acquires;
if (nextc < 0) // overflow 如果这个状态值越界，抛出异常;如果没有越界，则设置后返回true
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
如果状态不为0，且当前线程不是owner，则返回false。 return false; //获取锁失败，返回false
}
```
#### addWaiter
当前锁如果已经被其他线程锁持有，那么当前线程来去请求锁的时候，会进入这个方法,这个方法主要是把当前线程 封装成node，添加到AQS的链表中
```
 private Node addWaiter(Node mode) {
Node node = new Node(Thread.currentThread(), mode); //创建一个独占的Node节点,mode为排他
模式
// 尝试快速入队,如果失败则降级至full enq
Node pred = tail; // tail是AQS的中表示同步队列队尾的属性，刚开始为null，所以进行enq(node)方
法
if (pred != null) {
    node.prev = pred;
    if (compareAndSetTail(pred, node)) { // 防止有其他线程修改tail,使用CAS进行修改,如果失 败则降级至full enq
pred.next = node; // 如果成功之后旧的tail的next指针再指向新的tail,成为双向链表
            return node;
        }
}
enq(node); // 如果队列为null或者CAS设置新的tail失败 return node;
}
```
  
### enq   
enq就是通过自旋操作把当前节点加入到队列中  
```
  private Node enq(final Node node) {
for (;;) { //无效的循环，为什么采用for(;;)，是因为它执行的指令少，不占用寄存器
Node t = tail;// 此时head, tail都为null
if (t == null) { // Must initialize// 如果tail为null则说明队列首次使用,需要进行初始化
if (compareAndSetHead(new Node()))// 设置头节点,如果失败则存在竞争,留至下一轮循环 tail = head; // 用CAS的方式创建一个空的Node作为头结点，因为此时队列中只一个头结
点，所以tail也指向head，第一次循环执行结束 } else {
//进行第二次循环时，tail不为null，进入else区域。将当前线程的Node结点的prev指向tail，然后使用CAS将 tail指向Node
//这部分代码和addWaiter代码一样，将当前节点添加到队列
            node.prev = t;
            if (compareAndSetTail(t, node)) {
t.next = node; //t此时指向tail,所以可以CAS成功，将tail重新指向CNode。此时t为更 新前的tail的值，即指向空的头结点，t.next=node，就将头结点的后续结点指向Node，返回头结点
} }
}
return t; }
```
代码运行到这里，aqs队列的结构就是这样一个表现
![]()  

#### acquireQueued   
addWaiter返回了插入的节点，作为acquireQueued方法的入参,这个方法主要用于争抢锁  
```
  final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
final Node p = node.predecessor();// 获取prev节点,若为null即刻抛出 NullPointException
if (p == head && tryAcquire(arg)) {// 如果前驱为head才有资格进行锁的抢夺 setHead(node); // 获取锁成功后就不需要再进行同步操作了,获取锁成功的线程作为新的
head节点
//凡是head节点,head.thread与head.prev永远为null, 但是head.next不为null
p.next = null; // help GC failed = false; //获取锁成功 return interrupted;
}
//如果获取锁失败，则根据节点的waitStatus决定是否需要挂起线程
if (shouldParkAfterFailedAcquire(p, node) &&
志
} }
parkAndCheckInterrupt())// 若前面为true,则执行挂起,待下次唤醒的时候检测中断的标 interrupted = true;
}
} finally {
if (failed) // 如果抛出异常则取消锁的获取,进行出队(sync queue)操作 
cancelAcquire(node);
```
原来的head节点释放锁以后，会从队列中移除，原来head节点的next节点会成为head节点
![]()  
  
#### shouldParkAfterFailedAcquire  
从上面的分析可以看出，只有队列的第二个节点可以有机会争用锁，如果成功获取锁，则此节点晋升为头节点。对于第三个及以后的节点，if (p == head)条件不成立，首先进行shouldParkAfterFailedAcquire(p, node)操作。  
shouldParkAfterFailedAcquire 方法是判断一个争用锁的线程是否应该被阻塞。它首先判断一个节点的前置节点的状态是否为Node.SIGNAL，如果是，是说明此节点已经将状态设置-如果锁释放，则应当通知它，所以它可以安全的阻塞了，返回true。
```
  private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) { int ws = pred.waitStatus; //前继节点的状态
if (ws == Node.SIGNAL)//如果是SIGNAL状态，意味着当前线程需要被unpark唤醒
return true; 如果前节点的状态大于0，即为CANCELLED状态时，则会从前节点开始逐步循环找到一个没有被“CANCELLED”节点设置 为当前节点的前节点，返回false。在下次循环执行shouldParkAfterFailedAcquire时，返回true。这个操作实 际是把队列中CANCELLED的节点剔除掉。
if (ws > 0) {// 如果前继节点是“取消”状态，则设置 “当前节点”的 “当前前继节点” 为 “‘原前继节 点'的前继节点”。
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
pred.next = node;
} else { // 如果前继节点为“0”或者“共享锁”状态，则设置前继节点为SIGNAL状态。
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```
解读:假如有t1,t2两个线程都加入到了链表中，如果head节点位置的线程一直持有锁，那么t1和t2就是挂起状态，而HEAD以及Thread1的的awaitStatus都是 SIGNAL，在多次尝试获取锁失败以后，就会通过下面的方法进行挂起(这个地方就是避免了惊群效应，每个节点只需要关心上一个节点的状态即可)。  

>SIGNAL:值为-1，表示当前节点的的后继节点将要或者已经被阻塞，在当前节点释放的时候需要unpark后继节点；  
>CONDITION:值为-2，表示当前节点在等待condition，即在condition队列中；  
>PROPAGATE:值为-3，表示releaseShared需要被传播给后续节点(仅在共享模式下使用)；  
  
#### parkAndCheckInterrupt
如果shouldParkAfterFailedAcquire返回了true，则会执行:“parkAndCheckInterrupt()”方法，它是通过 LockSupport.park(this)将当前线程挂起到WATING状态，它需要等待一个中断、unpark方法来唤醒它，通过这样 一种FIFO的机制的等待，来实现了Lock的操作。  
```
private final boolean parkAndCheckInterrupt() {
LockSupport.park(this);// LockSupport提供park()和unpark()方法实现阻塞线程和解除线程阻塞 
return Thread.interrupted();
}
```
  
#### ReentrantLock.unlock 
加锁的过程分析完以后，再来分析一下释放锁的过程，调用release方法，这个方法里面做两件事
>1.释放锁  
>2.唤醒park的线程  
```
  public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
}
    return false;
}
```
  
#### tryRelease  
这个动作可以认为就是一个设置锁状态的操作，而且是将状态减掉传入的参数值(参数是1)，如果结果状态为0，就将排它锁的Owner设置为null，以使得其它的线程有机会进行执行。在排它锁中，加锁的时候状态会增加1(当 然可以自己修改这个值)，在解锁的时候减掉1，同一个锁，在可以重入后，可能会被叠加为2、3、4这些值，只有unlock()的次数与lock()的次数对应才会将Owner线程设置为空，而且也只有这种情况下才会返回true。  
```
 protected final boolean tryRelease(int releases) {
int c = getState() - releases; // 这里是将锁的数量减1
if (Thread.currentThread() != getExclusiveOwnerThread())// 如果释放的线程和获取锁的线程 不是同一个，抛出非法监视器状态异常
  throw new IllegalMonitorStateException();
    boolean free = false;
if (c == 0) {
// 由于重入的关系，不是每次释放锁c都等于0，
// 直到最后一次释放锁时，才会把当前线程释放 free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```
  
#### LockSupport  
LockSupport类是Java6引入的一个类，提供了基本的线程同步原语。LockSupport实际上是调用了Unsafe类里的函数，归结到Unsafe里，只有两个函数：
```
  public native void unpark(Thread jthread);
public native void park(boolean isAbsolute, long time);
```
unpark函数为线程提供“许可(permit)”，线程调用park函数则等待“许可”。这个有点像信号量，但是这个“许可”是不 能叠加的，“许可”是一次性的。  
  
permit相当于0/1的开关，默认是0，调用一次unpark就加1变成了1。调用一次park会消费permit，又会变成0。 如果再调用一次park会阻塞，因为permit已经是0了。直到permit变成1。这时调用unpark会把permit设置为1。每个线程都有一个相关的permit，permit最多只有一个，重复调用unpark不会累积。  
  
在使用LockSupport之前，我们对线程做同步，只能使用wait和notify，但是wait和notify其实不是很灵活，并且耦合性很高，调用notify必须要确保某个线程处于wait状态，而park/unpark模型真正解耦了线程之间的同步，先后顺序没有没有直接关联，同时线程之间不再需要一个Object或者其它变量来存储状态，不再需要关心对方的状态。  
  
总结  
分析了独占式同步状态获取和释放过程后，做个简单的总结：  
在获取同步状态时，同步器维护一个同步队列，获取状态失败的线程都会被加入到队列中并在队列中进行自旋；移出队列(或停止自旋)的条件是前驱节点为头节点且成功获取了同步状态。在释放同步状态时，同步器调用tryRelease(int arg)方法释放同步状态，然后唤醒头节点的后继节点。  
  
### 公平锁和非公平锁的区别  
  
锁的公平性是相对于获取锁的顺序而言的，如果是一个公平锁，那么锁的获取顺序就应该符合请求的绝对时间顺序，也就是FIFO。在上面分析的例子来说，只要CAS设置同步状态成功，则表示当前线程获取了锁，而公平锁则不一样，差异点有两个。  
  
#### FairSync.tryAcquire  
```
   final void** lock() {
     acquire(1);
}
```
非公平锁在获取锁的时候，会先通过CAS进行抢占，而公平锁则不会。  
#### FairSync.tryAcquire  
```
  protected final boolean* tryAcquire(int acquires) {
     final Thread current = Thread.currentThread*();
     int c = getState();
     if (c == 0) {
         if (!hasQueuedPredecessors() &&
             compareAndSetState(0, acquires)) {
             setExclusiveOwnerThread(current);
             return true;
} }
     else if (current == getExclusiveOwnerThread()) {
         int nextc = c + acquires;
         if (nextc < 0)
             throw new Error("Maximum lock count exceeded");
         setState(nextc);
         return true;
     }
     return false;
 }
 ```
这个方法与nonfairTryAcquire(int acquires)比较，不同的地方在于判断条件多了hasQueuedPredecessors()方 法，也就是加入了同步队列中当前节点是否有前驱节点的判断，如果该方法返回true，则表示有线程比当前线程 更早地请求获取锁，因此需要等待前驱线程获取并释放锁之后才能继续获取锁。  
  
### Condition  
  
通过前的讲解，我们知道任意一个Java对象，都拥有一组监视器方法(定义在java.lang.Object上)，主要包 括wait()、notify()以及notifyAll()方法，这些方法与synchronized同步关键字配合，可以实现等待/通知模式。  
JUC包提供了Condition来对锁进行精准控制，Condition是一个多线程协调通信的工具类，可以让某些线程一起等待某个条件(condition)，只有满足条件时，线程才会被唤醒。  
   
#### ConditionWait  
```
   public class ConditionDemoWait implements  Runnable{
     private Lock lock;
     private Condition condition;
     public ConditionDemoWait(Lock lock, Condition condition){
         this.lock=lock;
         this.condition=condition;
     }
@Override
     public void run() {
         System.out.println("begin -ConditionDemoWait");
         try {
             lock.lock();
             condition.await();
             System.out.println("end - ConditionDemoWait");
         } catch (InterruptedException e) {
             e.printStackTrace();
         }finally {
             lock.unlock();
} }
}
```
  
#### ConditionSignal  
```
 public class** ConditionDemoSignal implements  Runnable{
     private Lock lock;
     private Condition condition;
     public ConditionDemoSignal(Lock lock, Condition condition){
         this.lock=lock;
         this.condition=condition;
     }
@Override
     public void run() {
         System.out.println("begin -ConditionDemoSignal");
         try {
             lock.lock();
             condition.signal();
             System.out.println("end - ConditionDemoSignal");
}finally {
} }
    lock.unlock();
}
```  
通过这个案例简单实现了wait和notify的功能，当调用await方法后，当前线程会释放锁并等待，而其他线程调用 condition对象的signal或者signalall方法通知并被阻塞的线程，然后自己执行unlock释放锁，被唤醒的线程获得之 前的锁继续执行，最后释放锁。所以，condition中两个最重要的方法，一个是await，一个是signal方法。  
>await:把当前线程阻塞挂起  
>signal:唤醒阻塞的线程  

#### await  
调用Condition的await()方法(或者以await开头的方法)，会使当前线程进入等待队列并释放锁，同时线程状态变为等待状态。当从await()方法返回时，当前线程一定获取了Condition相关联的锁。  
```
 public final void await() throws InterruptedException {
     if (Thread.interrupted())
throw new InterruptedException();
Node node = addConditionWaiter(); //创建一个新的节点，节点状态为condition，采用的数据结构
仍然是链表
int savedState = fullyRelease(node); //释放当前的锁，得到锁的状态，并唤醒AQS队列中的一个线
程
//如果当前节点没有在同步队列上，即还没有被signal，则将当前线程阻塞
//isOnSyncQueue 判断当前 node 状态,如果是 CONDITION 状态,或者不在队列上了,就继续阻塞,还在队列上且 不是 CONDITION 状态了,就结束循环和阻塞
while (!isOnSyncQueue(node)) {//第一次判断的是false，因为前面已经释放锁了
LockSupport.park(this); // 第一次总是 park 自己,开始阻塞等待
// 线程判断自己在等待过程中是否被中断了,如果没有中断,则再次循环,会在 isOnSyncQueue 中判断自己是否在队
列上.
if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
break;
}
// 当这个线程醒来,会尝试拿锁, 当 acquireQueued 返回 false 就是拿到锁了.
// interruptMode != THROW_IE -> 表示这个线程没有成功将 node 入队,但 signal 执行了 enq 方
法让其入队了.
// 将这个变量设置成 REINTERRUPT.
if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
interruptMode = REINTERRUPT;
// 如果 node 的下一个等待者不是 null, 则进行清理,清理 Condition 队列上的节点.
int interruptMode = 0;
// 如果是 null ,就没有什么好清理的了.
if (node.nextWaiter != null) // clean up if cancelled
unlinkCancelledWaiters();
// 如果线程被中断了,需要抛出异常.或者什么都不做 
if (interruptMode != 0)

reportInterruptAfterWait(interruptMode);
  
 }
```

#### signal  
调用Condition的signal()方法，将会唤醒在等待队列中等待时间最长的节点(首节点)，在唤醒节点之前，会将节点移到同步队列中。  
```
  public final void signal() {
if (!isHeldExclusively()) //先判断当前线程是否获得了锁
throw new IllegalMonitorStateException();
Node first = firstWaiter; // 拿到 Condition 队列上第一个节点 if (first != null)
}
doSignal(first);
  private void doSignal(Node first) {
    do {
if ( (firstWaiter = first.nextWaiter) == null)// 如果第一个节点的下一个节点是 null, 那么, 最后一个节点也是 null.
lastWaiter = null; // 将 next 节点设置成 null first.nextWaiter = null;
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```
该方法先是 CAS 修改了节点状态，如果成功，就将这个节点放到 AQS 队列中，然后唤醒这个节点上的线程。此时，那个节点就会在 await 方法中苏醒。
```
 final boolean transferForSignal(Node node) {
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;
    Node p = enq(node);
int ws = p.waitStatus;
// 如果上一个节点的状态被取消了, 或者尝试设置上一个节点的状态为 SIGNAL 失败了(SIGNAL 表示: 他的 next 节点需要停止阻塞),
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
LockSupport.unpark(node.thread); // 唤醒输入节点上的线程. return true;
}
```
  
### CountDownLatch  
  
countdownlatch是一个同步工具类，它允许一个或多个线程一直等待，直到其他线程的操作执行完毕再执行。从命名可以解读到countdown是倒数的意思，类似于我们倒计时的概念。  
  
countdownlatch提供了两个方法，一个是countDown，一个是await， countdownlatch初始化的时候需要传入一个整数，在这个整数倒数到0之前，调用了await方法的程序都必须要等待，然后通过countDown来倒数。  

### 使用案例  
```
   public static void main(String[] args) throws InterruptedException {
    CountDownLatch countDownLatch=new CountDownLatch(3);
    new Thread(()->{
        countDownLatch.countDown();
    },"t1").start();
    new Thread(()->{
        countDownLatch.countDown();
    },"t2").start();
    new Thread(()->{
        countDownLatch.countDown();
},"t3").start();

  countDownLatch.await();
System.out.println("所有线程执行完毕"); }
```
从代码的实现来看，有点类似join的功能，但是比join更加灵活。CountDownLatch构造函数会接收一个int类型的参数作为计数器的初始值，当调用CountDownLatch的countDown方法时，这个计数器就会减一。  
  
通过await方法去阻塞去阻塞主流程  
![]()  
  
#### 使用场景  
>1.通过countdownlatch实现最大的并行请求，也就是可以让N个线程同时执行，这个我也是在课堂上写得比较多的。  
>2.比如应用程序启动之前，需要确保相应的服务已经启动，比如我们之前在讲zookeeper的时候，通过原生api连接的地方有用到countDownLatch。  
  
#### CountDownLatch源码分析
CountDownLatch类存在一个内部类Sync，上节课我们讲过，它是一个同步工具，一定继承了 AbstractQueuedSynchronizer。很显然，CountDownLatch实际上是是使得线程阻塞了，既然涉及到阻塞，就一 定涉及到AQS队列。  
  
#### await  
await函数会使得当前线程在countdownlatch倒计时到0之前一直等待，除非线程别中断；从源码中可以得知await方法会转发到Sync的acquireSharedInterruptibly方法。  
```
public void await() throws InterruptedException { 
sync.acquireSharedInterruptibly(1); 
}
```
  
#### acquireSharedInterruptibly  
这块代码主要是判断当前线程是否获取到了共享锁，我们提到过，AQS有两种锁类型，一种是共享锁，一种是独占锁，在这里用的是共享锁；为什么要用共享锁，因为CountDownLatch可以多个线程同时通过。  
```
 public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
        if (Thread.interrupted()) //判断线程是否中断 throw new InterruptedException();
if (tryAcquireShared(arg) < 0) //如果等于0则返回1，否则返回-1，返回-1表示需要阻塞 
doAcquireSharedInterruptibly(arg);
}
```
  
#### doAcquireSharedInterruptibly  
  
获取共享锁
```
 private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {
final Node node = addWaiter(Node.SHARED); //创建一个共享模式的节点添加到队列中 boolean failed = true;
try {
for (;;) { //自旋等待共享锁释放，也就是等待计数器等于0。
final Node p = node.predecessor(); //获得当前节点的前一个节点 if (p == head) {
int r = tryAcquireShared(arg);//就判断尝试获取锁
if (r >= 0) {//r>=0表示计数器已经归零了，则释放当前的共享锁
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
} }
//当前节点不是头节点，则尝试让当前线程阻塞，第一个方法是判断是否需要阻塞，第二个方法是阻塞 if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
                } finally {
if (failed)
    cancelAcquire(node);
   }}
```
  
#### setHeadAndPropagate  
![]()  
  
PROPAGATE:值为-3，表示releaseShared需要被传播给后续节点
```
 private void setHeadAndPropagate(Node node, int propagate) {
Node h = head; // 记录头节点
setHead(node); //设置当前节点为头节点 //前面传过来的propagate是1，所以会进入下面的代码
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
Node s = node.next; //获得当前节点的下一个节点，如果下一个节点是空表示当前节点为最后一个节 点，或者下一个节点是share节点
} }
if (s == null || s.isShared()) doReleaseShared(); //唤醒下一个共享节点
```
  
#### doReleaseShared
释放共享锁，通知后面的节点
```
 private void doReleaseShared() {
      for (;;) {
Node h = head; //获得头节点
if (h != null && h != tail) { //如果头节点不为空且不等于tail节点
int ws = h.waitStatus;
if (ws == Node.SIGNAL) { //头节点状态为SIGNAL，
if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) //修改当前头节点的状态为0, 避免下次再进入到这个里面
} }
continue; // loop to recheck cases unparkSuccessor(h); //释放后续节点
}
else if (ws == 0 &&
         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
continue;
}
if (h == head)
break;
// loop on failed CAS
// loop if head changed
```
  
#### countdown 
以共享模式释放锁，并且会调用tryReleaseShared函数，根据判断条件也可能会调用doReleaseShared函数
```
 public final boolean releaseShared(int arg) {
if (tryReleaseShared(arg)) { //如果为true，表示计数器已归0了
doReleaseShared(); //唤醒处于阻塞的线程
        return true;
    }
    return false;
}
```
  
#### tryReleaseShared  
这里主要是对state做原子递减，其实就是我们构造的CountDownLatch的计数器，如果等于0返回true，否则返回 false。  
```
 protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {
} }
int c = getState();
if (c == 0)
    return false;
int nextc = c-1;
if (compareAndSetState(c, nextc))
    return nextc == 0;
    }}
```
  
### Semaphore
  
semaphore也就是我们常说的信号灯，semaphore可以控制同时访问的线程个数，通过acquire获取一个许可，如果没有就等待，通过release释放一个许可。有点类似限流的作用。叫信号灯的原因也和他的用处有关，比如某商场就5个停车位，每个停车位只能停一辆车，如果这个时候来了10辆车，必须要等前面有空的车位才能进入。  
  
#### 使用案例
```
 public class Test {
 
 public static void main(String[] args) {
    Semaphore semaphore=new Semaphore(5);
    for(int i=0;i<10;i++){
        new Car(i,semaphore).start();
    }
}
static class Car extends Thread{
    private int num;
    private Semaphore semaphore;
    public Car(int num, Semaphore semaphore) {
        this.num = num;
        this.semaphore = semaphore;
    }
    public void run(){
        try {
semaphore.acquire();//获取一个许可 System.out.println("第"+num+"占用一个停车位"); TimeUnit.SECONDS.sleep(2); System.out.println("第"+num+"俩车走喽"); semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
} }
}
```
#### 使用场景  
可以实现对某些接口访问的限流。  
  
### Semaphore源码分析  
  
semaphore也是基于AQS来实现的，内部使用state表示许可数量；它的实现方式和CountDownLatch的差异点在于acquireSharedInterruptibly中的tryAcquireShared方法的实现，这个方法是在Semaphore方法中重写的。  
  
#### acquireSharedInterruptibly  
```
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
 }
```
#### tryAcquireShared  
在semaphore中存在公平和非公平的方式，和重入锁是一样的，如果通过FairSync表示公平的信号量、 NonFairSync表示非公平的信号量;公平和非公平取决于是否按照FIFO队列中的顺序去分配Semaphore所维护的 许可，我们来看非公平锁的实现。  
  
#### nonfairTryAcquireShared  
自旋去获得一个许可，如果许可获取失败，也就是remaining<0的情况下，让当前线程阻塞。
```
 final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
}
```
  
#### releaseShared  
releaseShared方法的逻辑也很简单，就是通过线程安全的方式去增加一个许可，如果增加成功，则触发释放一个共享锁，也就是让之前处于阻塞的线程重新运行。  
```
 public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```
增加令牌数  
```
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
} }
int current = getState();
int next = current + releases;
if (next < current) // overflow
    throw new Error("Maximum permit count exceeded");
if (compareAndSetState(current, next))
return true;
  }
```
  
### 原子操作  
  
当在多线程情况下，同时更新一个共享变量，由于我们前面讲过的原子性问题，可能得不到预期的结果。如果要达到期望的结果，可以通过synchronized来加锁解决，因为synchronized会保证多线程对共享变量的访问进行排队。  
   
在Java5以后，提供了原子操作类，这些原子操作类提供了一种简单、高效以及线程安全的更新操作。而由于变量的类型很多，所以Atomic一共提供了12个类分别对应四种类型的原子更新操作，基本类型、数组类型、引用类型、属性类型。  
>基本类型对应:AtomicBoolean、AtomicInteger、AtomicLong  
>数组类型对应:AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray   
>引用类型对应:AtomicReference、AtomicReferenceFieldUpdater、AtomicMarkableReference   
>字段类型对应:AtomicIntegerFieldUpdater、AtomicLongFieldUpdater、AtomicStampedReference  
  
#### Atomic原子操作的使用  
```
  private static AtomicInteger count=new AtomicInteger(0);
public static synchronized void inc() {
    try {
        Thread.sleep(1);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    count.getAndIncrement();
}
public static void main(String[] args) throws InterruptedException {
    for(int i=0;i<1000;i++){
        new Thread(()-> {
            SafeDemo.inc();
        }).start();
    }
    Thread.sleep(4000);
    System.out.println(count.get());
}
``` 
  
#### AtomicInteger实现原理 
由于所有的原子操作类都是大同小异的，所以我们只分析其中一个原子操作类。
```
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}
```
大家又会发现一些熟悉的东西，就是unsafe。调用unsafe类中的getAndAddInt方法，这个方法如下。  
```
public final int getAndAddInt(Object var1, long var2, int var4) {
    int var5;
do {
var5 = this.getIntVolatile(var1, var2);// 方法获取对象中offset偏移地址对应的整型
field的值
} while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4)); return var5;
}
```
通过循环以及cas的方式实现原子更新，从而达到在多线程情况下仍然能够保证原子性的目的。大家会发现，只要基本的东西搞明白以后，剩下的都比较容易理解了。  
  
### 线程池  
  
Java中的线程池是运用场景最多的并发框架，几乎所有需要异步或并发执行任务的程序都可以使用线程池。线程池就像数据库连接池的作用类似，只是线程池是用来重复管理线程避免创建大量线程增加开销。所以合理的使用线程池可以
>1.降低创建线程和销毁线程的性能开销  
>2.合理的设置线程池大小可以避免因为线程数超出硬件资源瓶颈带来的问题，类似起到了限流的作用；线程是稀缺资源，如果无线创建，会造成系统稳定性问题。  
  
#### 线程池的使用  
JDK 为我们内置了几种常见线程池的实现，均可以使用 Executors 工厂类创建为了更好的控制多线程，JDK提供了一套线程框架Executor，帮助开发人员有效的进行线程控制。它们都在java.util.concurrent包中，是JDK并发包的核心。其中有一个比较重要的类：Executors，他扮演着线程工厂的角色，我们通过Executors可以创建特定功能的线程池。  
   
>newFixedThreadPool**:该方法返回一个固定数量的线程池，线程数不变，当有一个任务提交时，若线程池中空闲，则立即执行，若没有，则会被暂缓在一个任务队列中，等待有空闲的线程去执行。  
>newSingleThreadExecutor: 创建一个线程的线程池，若空闲则执行，若没有空闲线程则暂缓在任务队列中。   
>newCachedThreadPool**:返回一个可根据实际情况调整线程个数的线程池，不限制最大线程数量，若用空闲的线程则执行任务，若无任务则不创建线程。并且每一个空闲线程会在60秒后自动回收。  
>newScheduledThreadPool: 创建一个可以指定线程的数量的线程池，但是这个线程池还带有延迟和周期性执行任务的功能，类似定时器。  
```
public class Test implements Runnable{
@Override
    public void run() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName());
    }
    static ExecutorService service=Executors.newFixedThreadPool(3);
    public static void main(String[] args) {
        for(int i=0;i<100;i++) {
            service.execute(new Test());
}
        service.shutdown();
    }
}
```
设置了3个固定线程大小的线程池来跑100。 
  
#### submit和execute的区别  
执行一个任务，可以使用submit和execute，这两者有什么区别呢?   
1.execute只能接受Runnable类型的任务。  
2.submit不管是Runnable还是Callable类型的任务都可以接受，但是Runnable返回值均为void，所以使用 Future的get()获得的还是null。  
  
### ThreadpoolExecutor  
  
前面说的四种线程池构建工具，都是基于ThreadPoolExecutor 类，它的构造函数参数
```
public ThreadPoolExecutor(int corePoolSize, //核心线程数量 int maximumPoolSize, //最大线程数
long keepAliveTime, //超时时间,超出核心线程数量以外的线程空余存活时间
TimeUnit unit, //存活时间单位
BlockingQueue<Runnable> workQueue, //保存执行任务的队列 ThreadFactory threadFactory,//创建新线程使用的工厂
RejectedExecutionHandler handler //当任务无法执行的时候的处理方式 ){
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}
```
分别看一下前面提到的几个初始化工具类的构造以及原理。  
  
#### newFixedThreadPool  
```
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
}
0L, TimeUnit.MILLISECONDS,
new LinkedBlockingQueue<Runnable>());
}
```
FixedThreadPool 的核心线程数和最大线程数都是指定值，也就是说当线程池中的线程数超过核心线程数后，任务 都会被放到阻塞队列中。另外 keepAliveTime 为 0，也就是超出核心线程数量以外的线程空余存活时间。  
而这里选用的阻塞队列是 LinkedBlockingQueue，使用的是默认容量 Integer.MAX_VALUE，相当于没有上限。
这个线程池执行任务的流程如下:  
>1.线程数少于核心线程数，也就是设置的线程数时，新建线程执行任务。  
>2.线程数等于核心线程数后，将任务加入阻塞队列。  
>3.由于队列容量非常大，可以一直添加。  
>4.执行完任务的线程反复去队列中取任务执行。  
  
用途:FixedThreadPool 用于负载比较大的服务器，为了资源的合理利用，需要限制当前线程数量。  
  
#### newCachedThreadPool  
```
 public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
    60L, TimeUnit.SECONDS,
new SynchronousQueue<Runnable>());
}
```
CachedThreadPool 创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程；并且没有核心线程，非核心线程数无上限，但是每个空闲的时间只有 60 秒，超过后就会被回收。  
它的执行流程如下:  
>1.没有核心线程，直接向 SynchronousQueue 中提交任务。  
>2.如果有空闲线程，就去取出任务执行;如果没有空闲线程，就新建一个。  
>3.执行完任务的线程有 60 秒生存时间，如果在这个时间内可以接到新任务，就可以继续活下去，否则就被回收。  
  
#### newSingleThreadExecutor  
创建一个单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。  
  
### 线程池的源码分析  
ThreadPoolExecutor是线程池的核心，提供了线程池的实现。ScheduledThreadPoolExecutor继承了 ThreadPoolExecutor，并另外提供一些调度方法以支持定时和周期任务。Executers是工具类，主要用来创建线程池对象。  
  
我们把一个任务提交给线程池去处理的时候，线程池的处理过程是什么样的呢?首先直接来看看定义。  

#### 线程数量和线程池状态管理  
线程池用一个AtomicInteger来保存线程数量和线程池状态，一个int数值一共有32位，高3位用于保存运行状态，低29位用于保存线程数量。
```
 private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0)); //一个原子操作类
  private static final int COUNT_BITS = Integer.SIZE - 3; //32-3
private static final int CAPACITY = (1 << COUNT_BITS) - 1; //将1的二进制向右位移29位,再减 1表示最大线程容量
//运行状态保存在int值的高3位 (所有数值左移29位)
private static final int RUNNING    =-1  << COUNT_BITS;// 接收新任务,并执行队列中的任务
private static final int SHUTDOWN   = 0  << COUNT_BITS;// 不接收新任务,但是执行队列中的任务
private static final int STOP       =1   << COUNT_BITS;// 不接收新任务,不执行队列中的任务,中断正在执行中的任务
private static final int TIDYING    =2   << COUNT_BITS; //所有的任务都已结束,线程数量为0,处于该状态的线程池即将调用terminated()方法
private static final int TERMINATED = 3  << COUNT_BITS;// terminated()方法执行完成

// Packing and unpacking ctl
private static int runStateOf(int c)  { return c & ~CAPACITY; } //获取运行状态
private static int workerCountOf(int c)  { return c & CAPACITY; } //获取线程数量
```
#### execute  
通过线程池的核心方法了解线程池中这些参数的含义
```
 public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
       int c = ctl.get();
if (workerCountOf(c) < corePoolSize) {//1.当前池中线程比核心数少，新建一个线程执行任务 if (addWorker(command, true))
            return;
        c = ctl.get();
}
if (isRunning(c) && workQueue.offer(command)) {//2.核心池已满，但任务队列未满，添加到队列 中
int recheck = ctl.get(); //任务成功添加到队列以后，再次检查是否需要添加新的线程，因为已存在的线程可能被销毁了
if (! isRunning(recheck) && remove(command)) reject(command);//如果线程池处于非运行状态，并且把当前的任务从任务队列中移除成功，则拒
绝该任务
else if (workerCountOf(recheck) == 0)//如果之前的线程已被销毁完，新建一个线程
            addWorker(null, false);
}
else if (!addWorker(command, false)) //3.核心池已满，队列已满，试着创建一个新线程
 }
reject(command); //如果创建新线程失败了，说明线程池被关闭或者线程池完全满了，拒绝任务
 
```
流程图  
![]()  
  
 
