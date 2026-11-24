package cn.beiming.profile;

import cn.beiming.core.BusinessCoreServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {"server.port=8130", "profile.auth.stub-tokens-enabled=true"})
class BusinessCoreProfileApiContractTest extends BusinessCoreProfileContractCases {
}
