package cn.beiming.admin;

import cn.beiming.core.BusinessCoreServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {
        "server.port=8130",
        "beiming.admin.test-mode=true",
        "beiming.business-core.test-control-headers.enabled=true"
})
class BusinessCoreAdminApiContractTest extends BusinessCoreAdminContractCases {
}
