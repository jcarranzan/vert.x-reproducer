import io.github.bonigarcia.wdm.WebDriverManager;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.qe.CorsServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class ReproducerTest {
    private final WebDriver driver = getFirefox();

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

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    private ChromeDriver getChrome() {
        WebDriverManager.chromedriver().clearPreferences();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        return new ChromeDriver(options);
    }

    private FirefoxDriver getFirefox() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        return new FirefoxDriver(options);
    }
}
