package com.example.shoppingcart.it;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    // Patrón "singleton container": este campo estático se hereda y se comparte
    // entre varias subclases (CartFlowIT, ShoppingcartApplicationIT). Si se
    // gestionara con @Container (JUnit5 Testcontainers extension), la extensión
    // detendría el contenedor al terminar la PRIMERA subclase que lo usa, y las
    // siguientes reutilizarían el ApplicationContext de Spring (cacheado)
    // apuntando a un contenedor ya muerto. Por eso se arranca manualmente una
    // sola vez y no se detiene explícitamente: Ryuk (el reaper de Testcontainers)
    // lo limpia al terminar la JVM de tests.
    @ServiceConnection
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("shopping_cart_db")
                    .withUsername("sc_user")
                    .withPassword("sc_pass");

    @ServiceConnection
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    static {
        mysql.start();
        kafka.start();
    }

    // Con @ServiceConnection, Spring autoconfigura:
    //  - spring.datasource.url/username/password
    //  - spring.kafka.bootstrap-servers
    // No necesitamos Initializer ni DynamicPropertySource.
}
