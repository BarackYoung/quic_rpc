package microservice.quic_rpc.network.exception;

public class AnnotationException extends RuntimeException{
    public AnnotationException() {
        super("Annotation @RpcClient not found");
    }
}
