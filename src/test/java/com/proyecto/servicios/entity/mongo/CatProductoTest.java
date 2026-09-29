package com.proyecto.servicios.entity.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatProductoTest {

    @Test
    void coleccion_seTomaDeLaConfiguracion() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("gestopago.productos.coleccion", "coleccion_prueba")));
            context.refresh();

            MongoMappingContext mappingContext = new MongoMappingContext();
            mappingContext.setSimpleTypeHolder(new MongoCustomConversions(List.of()).getSimpleTypeHolder());
            mappingContext.setApplicationContext(context);

            assertEquals("coleccion_prueba", mappingContext.getRequiredPersistentEntity(CatProducto.class).getCollection());
        }
    }
}
