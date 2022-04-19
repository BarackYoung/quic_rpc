package microservice.quic_rpc.network.servicegovernance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoadBalanceFactory {

    private static RandomLoadBalance randomLoadBalance;

    @Autowired
    public void setRandomLoadBalance(RandomLoadBalance myRandomLoadBalance) {
        randomLoadBalance = myRandomLoadBalance;
    }

    public static RandomLoadBalance getRandomLoadBalance() {
        return randomLoadBalance;
    }
}
