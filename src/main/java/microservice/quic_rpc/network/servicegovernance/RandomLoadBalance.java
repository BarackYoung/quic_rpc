package microservice.quic_rpc.network.servicegovernance;

import com.google.gson.Gson;
import microservice.quic_rpc.network.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class RandomLoadBalance implements LoadBalance{
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalance.class);

    @Value("${rpc.eureka.url}")
    private String registerCenterUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Map<String, Object> getInstance(String applicationName) {
        String url = registerCenterUrl + "/apps/" + applicationName;
        Map<String, Map<String, Object>> applicationInfoMap = restTemplate.getForObject(url, HashMap.class);
        if (applicationInfoMap == null || !applicationInfoMap.containsKey("application") || !applicationInfoMap.get("application").containsKey("instance")){
            throw new ServiceNotFoundException();
        }
        Object listObj = applicationInfoMap.get("application").get("instance");
        Gson gson = new Gson();
        String listJson = gson.toJson(listObj);
        List<Map<String, Object>> listMap = gson.fromJson(listJson, List.class);
        LOGGER.info("get service number:" + listMap.size());
        LOGGER.info(listObj.toString());
        Random random = new Random();
        Map<String, Object> map = listMap.get(random.nextInt(listMap.size()));
        Map<String, Object> portMap = gson.fromJson(gson.toJson(map.get("port")), Map.class);
        Map<String, Object> resultMap = new HashMap<>();
        Double doublePort = Double.parseDouble(portMap.get("$").toString());
        resultMap.put("ipAddr", map.get("ipAddr"));
        resultMap.put("port", doublePort.intValue());
        return resultMap;
    }

}
