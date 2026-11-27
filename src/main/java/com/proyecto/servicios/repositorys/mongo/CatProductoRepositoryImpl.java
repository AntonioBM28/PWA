package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.model.ProductoFiltro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CatProductoRepositoryImpl implements CatProductoRepositoryCustom {

    private static final String ID = "_id";
    private static final String SERVICIO = "servicio";
    private static final String ID_SERVICIO = "idServicio";
    private static final String TIPO_SERVICIO = "idCatTipoServicio";
    private static final String PRECIO = "precio";

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

    @Override
    public Page<CatProducto> buscar(ProductoFiltro filtro, Pageable pagina) {
        Query query = new Query();
        criterios(filtro).forEach(query::addCriteria);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CatProducto.class);
        List<CatProducto> productos = mongoTemplate.find(query.with(pagina), CatProducto.class);
        return PageableExecutionUtils.getPage(productos, pagina, () -> total);
    }

    /** Cada filtro con valor agrega una condicion; todas se combinan con AND. */
    static List<Criteria> criterios(ProductoFiltro filtro) {
        List<Criteria> criterios = new ArrayList<>();
        if (filtro.getTexto() != null) {
            // El texto del usuario se escapa: nunca se interpreta como expresion regular
            Pattern contiene = Pattern.compile(Pattern.quote(filtro.getTexto()), Pattern.CASE_INSENSITIVE);
            criterios.add(new Criteria().orOperator(
                    Criteria.where("nombre").regex(contiene),
                    Criteria.where(SERVICIO).regex(contiene)));
        }
        if (filtro.getIdServicio() != null) {
            criterios.add(Criteria.where(ID_SERVICIO).is(filtro.getIdServicio()));
        }
        if (filtro.getIdCatTipoServicio() != null) {
            criterios.add(Criteria.where(TIPO_SERVICIO).is(filtro.getIdCatTipoServicio()));
        }
        if (filtro.getPrecioMin() != null || filtro.getPrecioMax() != null) {
            Criteria precio = Criteria.where(PRECIO);
            if (filtro.getPrecioMin() != null) {
                precio.gte(filtro.getPrecioMin());
            }
            if (filtro.getPrecioMax() != null) {
                precio.lte(filtro.getPrecioMax());
            }
            criterios.add(precio);
        }
        return criterios;
    }

    @Override
    public List<CatProducto> serviciosDistintos() {
        Aggregation agregacion = Aggregation.newAggregation(
                Aggregation.sort(Sort.by(SERVICIO, ID)),
                Aggregation.group(ID_SERVICIO).first("$$ROOT").as("producto"),
                Aggregation.replaceRoot("producto"),
                Aggregation.sort(Sort.by(SERVICIO)));
        return mongoTemplate.aggregate(agregacion, CatProducto.class, CatProducto.class).getMappedResults();
    }

    @Override
    public List<Integer> tiposServicio() {
        return mongoTemplate.findDistinct(new Query(), TIPO_SERVICIO, CatProducto.class, Integer.class).stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }
}
