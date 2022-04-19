package microservice.quic_rpc.network.servicegovernance;

import java.util.Map;

public interface LoadBalance {
    Map<String, Object> getInstance(String application);

}
