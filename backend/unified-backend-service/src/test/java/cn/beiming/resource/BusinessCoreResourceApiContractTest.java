package cn.beiming.resource;

import cn.beiming.core.BusinessCoreServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {"server.port=8130", "beiming.business-core.test-control-headers.enabled=true"})
class BusinessCoreResourceApiContractTest extends BusinessCoreResourceContractCases {
}
