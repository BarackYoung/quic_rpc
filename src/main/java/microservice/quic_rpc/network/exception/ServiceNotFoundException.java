package microservice.quic_rpc.network.exception;

public class ServiceNotFoundException extends RuntimeException{
    public ServiceNotFoundException() {
        super("Service Not find");
    }
}
