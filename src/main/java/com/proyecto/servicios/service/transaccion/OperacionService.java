package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.transaccion.OperacionFiltro;
import com.proyecto.servicios.model.transaccion.OperacionResponse;
import com.proyecto.servicios.model.transaccion.OperacionesResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.TransaccionRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.antesDe;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.conEstado;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.deCliente;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.deCuenta;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.desde;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.nombreClienteContiene;
import static com.proyecto.servicios.repositorys.onboarding.TransaccionSpecifications.productoContiene;

/**
 * Historial de operaciones (compras y pagos de servicios). El EJECUTIVO consulta las de todos los
 * clientes, como bitacora; un CLIENTE solo ve las suyas, sin importar los filtros que envie.
 */
@Service
public class OperacionService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    private static final Sort MAS_RECIENTES = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final ZoneId zona;
    private final String urlImagenes;

    public OperacionService(TransaccionRepository transaccionRepository,
                            CuentaRepository cuentaRepository,
                            ClienteRepository clienteRepository,
                            @Value("${gestopago.transacciones.zona-horaria:America/Mexico_City}") String zonaHoraria,
                            @Value("${gestopago.imagenes.url-base:" + ProductoServiceImpl.URL_IMAGENES_DEFAULT + "}")
                            String urlImagenes) {
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.zona = ZoneId.of(zonaHoraria);
        this.urlImagenes = urlImagenes.endsWith("/") ? urlImagenes : urlImagenes + "/";
    }

    @Transactional(readOnly = true)
    public OperacionesResponse consultar(OperacionFiltro filtro, JwtAuthenticationToken auth) {
        boolean ejecutivo = auth.getAuthorities().stream().anyMatch(a -> "ROLE_EJECUTIVO".equals(a.getAuthority()));
        return consultar(filtro, ejecutivo, clienteId(auth));
    }

    OperacionesResponse consultar(OperacionFiltro filtro, boolean ejecutivo, Integer clienteAutenticado) {
        Integer clienteId = filtro.getClienteId();
        String nombreCliente = filtro.getNombreCliente();
        if (!ejecutivo) {
            if (clienteAutenticado == null || (clienteId != null && !clienteId.equals(clienteAutenticado))) {
                throw new TransaccionException("Solo puedes consultar tus propias operaciones", HttpStatus.FORBIDDEN);
            }
            clienteId = clienteAutenticado;
            nombreCliente = null;
        }

        Specification<Transaccion> criterios = Specification.where(deCliente(clienteId))
                .and(nombreClienteContiene(nombreCliente))
                .and(deCuenta(filtro.getNumeroCuenta()))
                .and(conEstado(filtro.getEstado()))
                .and(productoContiene(filtro.getTexto()))
                .and(desde(inicioDelDia(filtro.getFechaDesde())))
                .and(antesDe(inicioDelDia(filtro.getFechaHasta() == null ? null : filtro.getFechaHasta().plusDays(1))));
        Page<Transaccion> pagina = transaccionRepository.findAll(criterios,
                PageRequest.of(filtro.getPagina(), filtro.getTamanio(), MAS_RECIENTES));

        List<Transaccion> transacciones = pagina.getContent();
        Map<Integer, String> cuentas = cuentaRepository.findAllById(ids(transacciones, Transaccion::getCuentaId)).stream()
                .collect(Collectors.toMap(Cuenta::getId, Cuenta::getNumeroCuenta));
        Map<Integer, String> clientes = ejecutivo
                ? clienteRepository.findAllById(ids(transacciones, Transaccion::getClienteId)).stream()
                        .collect(Collectors.toMap(Cliente::getId, OperacionService::nombreCompleto))
                : Map.of();

        OperacionesResponse response = new OperacionesResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setOperaciones(transacciones.stream()
                .map(tx -> operacion(tx, cuentas.get(tx.getCuentaId()), clientes.get(tx.getClienteId())))
                .toList());
        response.asignarPaginacion(pagina);
        return response;
    }

    private OperacionResponse operacion(Transaccion tx, String numeroCuenta, String nombreCliente) {
        OperacionResponse o = new OperacionResponse();
        o.setId(tx.getId());
        o.setFecha(tx.getFechaCreacion());
        o.setEstado(tx.getEstado());
        o.setMonto(tx.getMonto());
        o.setIdProducto(tx.getIdProducto());
        o.setIdServicio(tx.getIdServicio());
        o.setProducto(tx.getProducto());
        o.setServicio(tx.getServicio());
        o.setImagenUrl(tx.getIdServicio() == null ? null : urlImagenes + tx.getIdServicio() + ".png");
        o.setReferencia(tx.getReferencia());
        o.setNumeroCuenta(numeroCuenta);
        o.setNumeroAutorizacion(tx.getNumeroAutorizacion());
        o.setFolio(tx.getFolio());
        o.setDetalle(detalle(tx.getEstado()));
        // Los mensajes de conciliacion son internos; solo se muestra el motivo de un rechazo
        o.setMotivo(tx.getEstado() == EstadoTransaccion.RECHAZADA ? tx.getMensajeProveedor() : null);
        o.setCodigoProveedor(tx.getCodigoProveedor());
        o.setUpc(tx.getUpc());
        o.setClienteId(tx.getClienteId());
        o.setNombreCliente(nombreCliente);
        return o;
    }

    static String detalle(EstadoTransaccion estado) {
        return switch (estado) {
            case APROBADA -> "Operacion aprobada";
            case RECHAZADA -> "Operacion rechazada; el saldo fue devuelto";
            case PENDIENTE, CONCILIACION -> "Pendiente de confirmar con GestoPago; el monto esta retenido";
        };
    }

    private OffsetDateTime inicioDelDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay(zona).toOffsetDateTime();
    }

    private static Set<Integer> ids(Collection<Transaccion> transacciones, Function<Transaccion, Integer> campo) {
        return transacciones.stream().map(campo).collect(Collectors.toSet());
    }

    static String nombreCompleto(Cliente c) {
        return String.join(" ", List.of(c.getNombre(), c.getApellidoPaterno(),
                c.getApellidoMaterno() == null ? "" : c.getApellidoMaterno())).trim();
    }

    private static Integer clienteId(JwtAuthenticationToken auth) {
        Object value = auth.getToken().getClaim("clienteId");
        return value instanceof Number n ? n.intValue() : null;
    }
}
