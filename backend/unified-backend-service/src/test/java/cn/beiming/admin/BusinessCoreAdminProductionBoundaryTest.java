package cn.beiming.admin;

import cn.beiming.core.BusinessCoreServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {"server.port=8130", "beiming.admin.test-mode=false"})
class BusinessCoreAdminProductionBoundaryTest extends BusinessCoreAdminProductionBoundaryCases {
}
