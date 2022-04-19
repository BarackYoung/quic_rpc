package microservice.quic_rpc.network;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class Executor {
   private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100));

   public static void execute(Runnable task){
       threadPoolExecutor.execute(task);
   }

   public static Future<?> execute(Callable<?> task) {
       return threadPoolExecutor.submit(task);
   }
}
