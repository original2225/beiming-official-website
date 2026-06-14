package cn.beiming.auth;

import cn.beiming.core.BusinessCoreServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = "server.port=8130")
class BusinessCoreAuthApiContractTest extends BusinessCoreAuthContractCases {
}
