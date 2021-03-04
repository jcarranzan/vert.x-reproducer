import io.github.bonigarcia.wdm.WebDriverManager;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.qe.CorsServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class ReproducerTest {
    private final ChromeDriver driver = getDriver();

    @BeforeAll
    static void beforeAll(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(CorsServer.class.getName(), context.succeeding(id -> {
            context.completeNow();
        }));
    }

    @Test
    void noPreflightTest(VertxTestContext context) {
        context.verify(() -> {
            driver.get("http://localhost:8080/nopreflight.html");
            driver.findElement(By.cssSelector("input")).click();
            assertTrue(driver.findElement(By.id("textDiv")).getText().contains("localhost:8080/nopreflight.html"));
            context.completeNow();
        });
    }

    private ChromeDriver getDriver() {
        WebDriverManager.chromedriver().clearPreferences();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--web-security=no",
                "--ignore-ssl-errors=yes",
                "--blink-settings=imagesEnabled=true",
                "--no-sandbox",
                "-port=17912");
        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        options.setCapability("acceptSslCerts", true);
        return new ChromeDriver(options);
    }

}
