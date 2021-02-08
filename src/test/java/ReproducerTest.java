//import io.vertx.db2client.DB2Pool;

import io.vertx.db2client.DB2Pool;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReproducerTest {
    private static final String CONNECTION = "db2://db2fenc1:password@localhost:50000/testdb";

    @BeforeAll
    public static void setUp(VertxTestContext context) {
        final String create = "CREATE TABLE users (id VARCHAR(6) NOT NULL, name VARCHAR(200) NOT NULL) IN DATABASE TESTDB;";
        final String insert = "INSERT INTO users VALUES('tom', 'Tom Sawyer')";
        getPool().getConnection(result -> {
            if(result.succeeded()) {
                final SqlConnection conn = result.result();
                conn
                        .query(create)
                        .execute(ar2 -> {
                            if(ar2.succeeded()) {
                                conn
                                        .query(insert)
                                        .execute(ar3 -> {
                                            if(ar3.succeeded()) {
                                                context.completeNow();
                                            } else {
                                                context.failNow(ar3.cause());
                                            }
                                            conn.close();
                                        });
                            } else {
                                context.failNow(ar2.cause());
                                // Release the connection to the pool
                                conn.close();
                            }
                        });
            } else {
                context.failNow(result.cause());
            }
        });
    }

    @Test
    public void preparedQueryJDBC(VertxTestContext context) {
        getPool()
                .preparedQuery("SELECT * FROM users WHERE id=?")
                .execute(Tuple.of("tom"), ar->{
                    if(ar.succeeded()) {
                        Assertions.assertEquals("Tom Sawyer",ar.result().iterator().next().getString("name"));
                        System.out.println("Success");
                        context.completeNow();
                    } else {
                        context.failNow(ar.cause());
                    }
                });
    }

    @Test
    public void preparedQueryBash(VertxTestContext context) {
        getPool()
                .preparedQuery("SELECT * FROM users WHERE id=$1")
                .execute(Tuple.of("tom"), ar->{
                    if(ar.succeeded()) {
                        Assertions.assertEquals("Tom Sawyer",ar.result().iterator().next().getString("name"));
                        System.out.println("Success");
                        context.completeNow();
                    } else {
                        context.failNow(ar.cause());
                    }
                });
    }

    private static DB2Pool getPool() {
        return DB2Pool.pool(CONNECTION);
    }

    @AfterAll
    static void afterAll(VertxTestContext context) {
        final DB2Pool pool = getPool();
        pool.query("DROP TABLE users").execute(result -> {
            pool.close();
            if(result.failed()) {
                context.failNow(result.cause());
            } else {
                context.completeNow();
            }
        });
    }
}
