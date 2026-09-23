package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProducto;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class CatProductoRepositoryImpl implements CatProductoRepositoryCustom {

    private static final String ID = "_id";

    private final MongoTemplate mongoTemplate;

    public CatProductoRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void reemplazarCatalogo(List<CatProducto> productos) {
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CatProducto.class);
        productos.forEach(producto -> bulk.replaceOne(
                Query.query(Criteria.where(ID).is(producto.getIdProducto())),
                producto,
                FindAndReplaceOptions.options().upsert()));
        bulk.execute();

        List<Integer> idsVigentes = productos.stream().map(CatProducto::getIdProducto).toList();
        mongoTemplate.remove(Query.query(Criteria.where(ID).nin(idsVigentes)), CatProducto.class);
    }
}
