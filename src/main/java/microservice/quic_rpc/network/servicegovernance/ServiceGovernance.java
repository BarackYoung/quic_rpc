package microservice.quic_rpc.network.servicegovernance;

import microservice.quic_rpc.network.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;

@Component
@Lazy(value = false)
public class ServiceGovernance implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceGovernance.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${rpc.application.name}")
    private String appName;

    @Value("${rpc.service.port}")
    private String serverPort;

    @Value("${rpc.eureka.url}")
    private String registerUrl;

    private String instanceId;

    private String ipAddress;

    private String hostName;



    private static Set<String> methodSet = new HashSet<>();
    static {
        methodSet.add("wait");
        methodSet.add("equals");
        methodSet.add("toString");
        methodSet.add("hashCode");
        methodSet.add("getClass");
        methodSet.add("notify");
        methodSet.add("notifyAll");
    }


    // serviceName -> serviceClass Mapping
    public static Map<String, Class> serviceNameMap = new HashMap<>();

    // serviceName -> method Mapping
    public static Map<String, Map<String, Method>> methodMap = new HashMap<>();


    @Override
    public void afterPropertiesSet() throws Exception {
        // 把服务注册到服务中心
        InetAddress addr = InetAddress.getLocalHost();
        this.ipAddress = addr.getHostAddress();
        this.hostName = addr.getHostName();
        this.instanceId = UUID.randomUUID().toString();
        LOGGER.info("[appName:"+this.appName+"][serverPort:"+serverPort+"][ipAddress:"+ipAddress+"][hostName:"+hostName+"]");
        Map<String,Map<String, Object>> request = new HashMap<>();
        Map<String, Object> dataCenterInfo = new HashMap<>();
        dataCenterInfo.put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo");
        dataCenterInfo.put("name", "MyOwn");
        Map<String, Object> portMap = new HashMap<>();
        portMap.put("$", this.serverPort);
        portMap.put("@enabled", true);
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("app", this.appName);
        infoMap.put("instanceId", this.instanceId);
        infoMap.put("ipAddr", this.ipAddress);
        infoMap.put("hostName", this.hostName);
        infoMap.put("port", portMap);
        infoMap.put("status", "UP");
        infoMap.put("dataCenterInfo", dataCenterInfo);
        request.put("instance", infoMap);

        //发送注册服务
        String url = this.registerUrl + "/" +"apps/" + this.appName;
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, request, HashMap.class);
        LOGGER.info("registerResult："+response.getBody());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 扫描@RpcService注解，获取接口名称，方法，方法的入参数到缓存中
        LOGGER.info("setApplicationContext called!!");
        Map map = applicationContext.getBeansWithAnnotation(RpcService.class);
        for (Object service : map.values()) {
            Class claz = service.getClass();
            if (claz.getInterfaces().length < 1) {
                break;
            }
            String[] serviceNames = claz.getInterfaces()[0].getName().split("\\.");
            String serviceName = serviceNames[serviceNames.length-1];
            LOGGER.info("get annotation:" + claz.getName());
            serviceNameMap.put(serviceName, claz);
            Method[] methods = claz.getMethods();
            for (Method method : methods) {
                if (methodSet.contains(method.getName())) {
                    continue;
                }
                methodMap.putIfAbsent(serviceName, new HashMap<>());
                methodMap.get(serviceName).put(method.getName(), method);
            }
        }
        LOGGER.info(serviceNameMap.toString());
        LOGGER.info(methodMap.toString());
    }

    @Scheduled(fixedRate = 5000)
    public void heartBeat() {
        String url = registerUrl + "/apps/" + appName + "/" +instanceId;
        restTemplate.put(url, null);
    }
}
