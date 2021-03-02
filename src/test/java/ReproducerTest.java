//import io.vertx.db2client.DB2Pool;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReproducerTest {

    @Test
    public void reproduce() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry registry = schemaParser.parse("type Query { hello: String!}");
        System.out.println(registry.types());
    }

}
