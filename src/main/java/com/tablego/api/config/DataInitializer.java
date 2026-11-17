package com.tablego.api.config;

import com.tablego.api.model.*;
import com.tablego.api.repository.MesaRepository;
import com.tablego.api.repository.ProductoRepository;
import com.tablego.api.repository.ReservaRepository;
import com.tablego.api.repository.RestauranteRepository;
import com.tablego.api.repository.UserRepository;
import com.tablego.api.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera datos de prueba. Solo se ejecuta si app.seed.enabled=true.
 *
 * <ul>
 *   <li>Un usuario por rol (admin, recepcionista, cliente) + una base de clientes.</li>
 *   <li>3 restaurantes con mesas y carta de productos.</li>
 *   <li>Reservas históricas (por defecto 18 meses, ajustable con app.seed.meses) y reservas futuras
 *       próximas, con tendencias habituales: día de la semana, hora (almuerzo/cena), temporada,
 *       festivos (San Valentín, Día de la Madre, Semana Santa, diciembre), crecimiento en el tiempo,
 *       tamaño de grupo, anticipación, cancelaciones / no-shows y pedidos anticipados.</li>
 * </ul>
 * Usa semilla fija: los datos son reproducibles. Es idempotente (no duplica si ya hay reservas).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int TOTAL_CLIENTES = 80;
    private static final int DIAS_FUTUROS = 14;
    private static final int CHUNK = 500;

    private final UserService userService;
    private final UserRepository userRepository;
    private final RestauranteRepository restauranteRepository;
    private final MesaRepository mesaRepository;
    private final ProductoRepository productoRepository;
    private final ReservaRepository reservaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager em;

    private final Random rnd = new Random(42);

    @Value("${app.seed.meses:18}")
    private int meses;

    // ------------------------------------------------------------------ definiciones

    private record RestauranteDef(String nombre, String descripcion, String direccion, String ciudad,
                                  double lat, double lon, String telefono,
                                  LocalTime apertura, LocalTime cierre,
                                  double multPrecio, int[] mesas) {}

    private record ProductoDef(String nombre, String descripcion, CategoriaProducto categoria, int precio) {}

    private record Borrador(LocalDateTime fecha, int personas) {}

    private record Pendiente(Reserva reserva, LocalDateTime creado, LocalDateTime actualizado) {}

    private static final List<RestauranteDef> RESTAURANTES = List.of(
            new RestauranteDef("Sabor Caribe", "Cocina caribeña de autor en el centro histórico.",
                    "Calle de la Moneda #7-45", "Cartagena", 10.4236, -75.5513, "+57 605 6641111",
                    LocalTime.of(11, 0), LocalTime.of(23, 0), 1.25,
                    new int[]{2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 6, 6, 8}),
            new RestauranteDef("Mar y Brasa", "Pescados, mariscos y parrilla frente al mar.",
                    "Av. San Martín #8-120, Bocagrande", "Cartagena", 10.4010, -75.5570, "+57 605 6652222",
                    LocalTime.of(12, 0), LocalTime.of(22, 0), 1.0,
                    new int[]{2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 8, 10}),
            new RestauranteDef("El Fogón Turbaqueño", "Comida criolla y platos tradicionales.",
                    "Calle Real #15-30", "Turbaco", 10.3350, -75.4130, "+57 605 6533333",
                    LocalTime.of(11, 30), LocalTime.of(21, 30), 0.85,
                    new int[]{2, 2, 2, 4, 4, 4, 4, 6, 6}));

    private static final List<ProductoDef> MENU = List.of(
            new ProductoDef("Ceviche de camarón", "Camarón fresco en leche de tigre", CategoriaProducto.ENTRADA, 32000),
            new ProductoDef("Arepa de huevo", "Arepa frita rellena de huevo", CategoriaProducto.ENTRADA, 12000),
            new ProductoDef("Empanadas de carne", "Porción de 3 unidades", CategoriaProducto.ENTRADA, 15000),
            new ProductoDef("Sopa de mondongo", "Receta tradicional", CategoriaProducto.ENTRADA, 22000),
            new ProductoDef("Ensalada de aguacate", "Aguacate, tomate y cebolla", CategoriaProducto.ENTRADA, 18000),
            new ProductoDef("Mojarra frita", "Con arroz de coco y patacón", CategoriaProducto.PLATO_FUERTE, 42000),
            new ProductoDef("Cazuela de mariscos", "Cremosa, con langostinos y pescado", CategoriaProducto.PLATO_FUERTE, 58000),
            new ProductoDef("Churrasco", "300 g con papas criollas", CategoriaProducto.PLATO_FUERTE, 54000),
            new ProductoDef("Arroz con pollo", "Casero", CategoriaProducto.PLATO_FUERTE, 32000),
            new ProductoDef("Pargo rojo", "A la plancha con ensalada", CategoriaProducto.PLATO_FUERTE, 62000),
            new ProductoDef("Bandeja paisa", "Plato completo", CategoriaProducto.PLATO_FUERTE, 46000),
            new ProductoDef("Pasta al ajillo con camarones", "Linguine y camarón", CategoriaProducto.PLATO_FUERTE, 48000),
            new ProductoDef("Cheesecake de maracuyá", "Porción individual", CategoriaProducto.POSTRE, 16000),
            new ProductoDef("Arroz con coco", "Postre costeño", CategoriaProducto.POSTRE, 11000),
            new ProductoDef("Brownie con helado", "Chocolate caliente", CategoriaProducto.POSTRE, 15000),
            new ProductoDef("Limonada de coco", "Jarra individual", CategoriaProducto.BEBIDA, 12000),
            new ProductoDef("Jugo natural", "Lulo, mora o maracuyá", CategoriaProducto.BEBIDA, 9000),
            new ProductoDef("Cerveza nacional", "Botella 330 ml", CategoriaProducto.BEBIDA, 8000),
            new ProductoDef("Gaseosa", "Botella 400 ml", CategoriaProducto.BEBIDA, 6000),
            new ProductoDef("Cóctel de la casa", "Ron, frutas y hierbabuena", CategoriaProducto.BEBIDA, 26000),
            new ProductoDef("Copa de vino", "Tinto o blanco", CategoriaProducto.BEBIDA, 24000),
            new ProductoDef("Agua con gas", "Botella 500 ml", CategoriaProducto.OTRO, 7000),
            new ProductoDef("Porción de patacones", "Para compartir", CategoriaProducto.OTRO, 10000));

    private static final String[] NOMBRES = {"Carlos", "María", "Juan", "Laura", "Andrés", "Sofía", "Camilo", "Valentina",
            "Luis", "Daniela", "Jorge", "Paula", "Felipe", "Natalia", "Sebastián", "Carolina", "Diego", "Isabella",
            "Mateo", "Mariana", "Julián", "Gabriela", "Santiago", "Alejandra", "Esteban", "Juliana", "David", "Andrea"};
    private static final String[] APELLIDOS = {"García", "Rodríguez", "Martínez", "López", "Hernández", "Pérez", "Gómez",
            "Díaz", "Torres", "Ramírez", "Vargas", "Castro", "Romero", "Ortega", "Mendoza", "Herrera", "Jiménez", "Ruiz",
            "Cabrera", "Fernández", "Suárez", "Pacheco", "Arrieta", "Barrios"};
    private static final String[] NOTAS = {"Cumpleaños", "Aniversario", "Mesa cerca de la ventana", "Alergia a los mariscos",
            "Silla para bebé", "Celebración de grado", "Reunión de trabajo", "Sin gluten", "Mesa tranquila"};

    // ------------------------------------------------------------------ ejecución

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("Administrador", "admin@tablego.com", "Admin12345!", Role.ADMINISTRADOR);
        seedUser("Recepcionista", "recepcion@tablego.com", "Recepcion12345!", Role.RECEPCIONISTA);
        seedUser("Cliente", "cliente@tablego.com", "Cliente12345!", Role.CLIENTE);

        List<User> clientes = seedClientes();
        seedCatalogo();
        asignarPersonal();
        seedReservas(clientes);
    }

    private void seedUser(String nombre, String email, String password, Role role) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userService.create(nombre, email, password, role);
            log.info("Usuario de prueba creado: {} ({})", email, role);
        }
    }

    // ------------------------------------------------------------------ clientes y catálogo

    private List<User> seedClientes() {
        for (int i = 0; i < TOTAL_CLIENTES - 1; i++) {
            String nombre = NOMBRES[i % NOMBRES.length] + " " + APELLIDOS[(i * 7) % APELLIDOS.length];
            seedUser(nombre, "cliente" + (i + 1) + "@correo.test", "Cliente12345!", Role.CLIENTE);
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CLIENTE && u.isActivo())
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
    }

    private void seedCatalogo() {
        if (restauranteRepository.count() > 0) {
            return;
        }
        for (RestauranteDef def : RESTAURANTES) {
            Restaurante r = restauranteRepository.save(Restaurante.builder()
                    .nombre(def.nombre()).descripcion(def.descripcion())
                    .direccion(def.direccion()).ciudad(def.ciudad())
                    .latitud(def.lat()).longitud(def.lon()).telefono(def.telefono())
                    .horaApertura(def.apertura()).horaCierre(def.cierre())
                    .duracionReservaMin(120).build());

            for (int i = 0; i < def.mesas().length; i++) {
                mesaRepository.save(Mesa.builder().restaurante(r).numero(i + 1).capacidad(def.mesas()[i]).build());
            }
            for (ProductoDef p : MENU) {
                long precio = Math.round(p.precio() * def.multPrecio() / 500.0) * 500L;
                productoRepository.save(Producto.builder().restaurante(r).nombre(p.nombre())
                        .descripcion(p.descripcion()).categoria(p.categoria())
                        .precio(BigDecimal.valueOf(precio)).build());
            }
            log.info("Restaurante de prueba creado: {} ({} mesas, {} productos)",
                    def.nombre(), def.mesas().length, MENU.size());
        }
    }

    private void asignarPersonal() {
        List<Restaurante> restaurantes = restauranteRepository.findAll();
        if (restaurantes.isEmpty()) {
            return;
        }
        Restaurante principal = restaurantes.stream()
                .min(Comparator.comparing(Restaurante::getId)).orElseThrow();
        for (User u : userRepository.findAll()) {
            boolean personal = u.getRole() == Role.ADMINISTRADOR || u.getRole() == Role.RECEPCIONISTA;
            if (personal && u.getRestaurante() == null) {
                u.setRestaurante(principal);
                userRepository.save(u);
            }
        }
    }

    // ------------------------------------------------------------------ reservas

    private void seedReservas(List<User> clientes) {
        if (reservaRepository.count() > 0) {
            log.info("Ya existen reservas; no se generan datos históricos.");
            return;
        }
        List<Restaurante> restaurantes = restauranteRepository.findAll();
        if (restaurantes.isEmpty() || clientes.isEmpty()) {
            return;
        }

        Map<Long, List<Mesa>> mesasPorRest = mesaRepository.findAll().stream()
                .filter(Mesa::isActiva)
                .collect(Collectors.groupingBy(m -> m.getRestaurante().getId()));
        Map<Long, Map<CategoriaProducto, List<Producto>>> menuPorRest = productoRepository.findAll().stream()
                .filter(p -> p.isActivo() && p.isDisponible())
                .collect(Collectors.groupingBy(p -> p.getRestaurante().getId(),
                        Collectors.groupingBy(Producto::getCategoria)));

        LocalDateTime ahora = LocalDateTime.now(ZONA);
        LocalDate hoy = ahora.toLocalDate();
        LocalDate inicio = hoy.minusMonths(meses);
        LocalDate fin = hoy.plusDays(DIAS_FUTUROS);
        long diasPasados = Math.max(1, ChronoUnit.DAYS.between(inicio, hoy));

        List<Pendiente> buffer = new ArrayList<>();
        long total = 0;

        for (Restaurante rest : restaurantes) {
            List<Mesa> mesas = mesasPorRest.getOrDefault(rest.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Mesa::getCapacidad).thenComparing(Mesa::getNumero))
                    .collect(Collectors.toList());
            if (mesas.isEmpty()) {
                continue;
            }
            Map<CategoriaProducto, List<Producto>> menu = menuPorRest.getOrDefault(rest.getId(), Map.of());
            int dur = rest.getDuracionReservaMin();
            int maxCap = mesas.stream().mapToInt(Mesa::getCapacidad).max().orElse(2);
            double lambdaBase = mesas.stream().mapToInt(Mesa::getCapacidad).sum() / 6.0;
            List<LocalTime> slots = slotsDe(rest);

            for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
                // 1. Creamos una variable efectivamente final para esta iteración
                LocalDate diaActual = dia;

                double[] esp = factorEspecial(diaActual);
                double crecimiento = diaActual.isAfter(hoy) ? 1.15
                        : 0.75 + 0.40 * (ChronoUnit.DAYS.between(inicio, diaActual) / (double) diasPasados);
                double pipeline = diaActual.isAfter(hoy)
                        ? Math.max(0.15, 1.0 - ChronoUnit.DAYS.between(hoy, diaActual) / 18.0) : 1.0;
                double lambda = lambdaBase * factorDiaSemana(diaActual.getDayOfWeek()) * factorMes(diaActual.getMonthValue())
                        * crecimiento * esp[0] * pipeline * (0.85 + 0.30 * rnd.nextDouble());

                int n = poisson(lambda);
                if (n == 0) {
                    continue;
                }

                // 2. Usamos 'diaActual' en lugar de 'dia' dentro del stream/lambda
                double[] pesosHora = slots.stream()
                        .mapToDouble(s -> pesoHora(s, diaActual.getDayOfWeek(), esp[1])).toArray();
                        
                List<Borrador> borradores = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    LocalTime hora = slots.get(elegirPonderado(pesosHora));
                    borradores.add(new Borrador(LocalDateTime.of(diaActual, hora),
                            tamanoGrupo(diaActual.getDayOfWeek(), maxCap)));
                }
                borradores.sort(Comparator.comparing(Borrador::fecha));

                Map<Long, LocalDateTime> libreDesde = new HashMap<>();
                for (Borrador b : borradores) {
                    buffer.add(construirReserva(rest, b, mesas, libreDesde, clientes, menu, ahora, dur));
                    total++;
                    if (buffer.size() >= CHUNK) {
                        volcar(buffer);
                    }
                }
            }
        }
        volcar(buffer);
        log.info("Reservas de prueba generadas: {} (de {} a {})", total, inicio, fin);
    }

    private Pendiente construirReserva(Restaurante rest, Borrador b, List<Mesa> mesas,
                                     Map<Long, LocalDateTime> libreDesde, List<User> clientes,
                                     Map<CategoriaProducto, List<Producto>> menu,
                                     LocalDateTime ahora, int dur) {
        LocalDateTime fecha = b.fecha();
        EstadoReserva estado = decidirEstado(fecha, ahora, dur, b.personas());

        Mesa mesa = null;
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CANCELADA) {
            mesa = buscarMesa(mesas, libreDesde, fecha, b.personas());
            if (mesa == null) {
                estado = fecha.isAfter(ahora) ? EstadoReserva.PENDIENTE : EstadoReserva.CANCELADA;
            } else {
                libreDesde.put(mesa.getId(), fecha.plusMinutes(dur));
            }
        }

        LocalDateTime creado = fechaCreacion(fecha, ahora);
        LocalDateTime actualizado = fechaActualizacion(estado, creado, fecha, dur, ahora);

        User cliente = clientes.get((int) (clientes.size() * Math.pow(rnd.nextDouble(), 2.0)));
        Reserva reserva = Reserva.builder()
                .cliente(cliente).restaurante(rest).mesa(mesa)
                .fechaHora(fecha).numPersonas(b.personas()).estado(estado)
                .notas(rnd.nextDouble() < 0.12 ? NOTAS[rnd.nextInt(NOTAS.length)] : null)
                .build();

        boolean almuerzo = fecha.getHour() < 17;
        for (ReservaItem it : generarItems(reserva, menu, almuerzo, esFinDeSemana(fecha.getDayOfWeek()))) {
            reserva.getItems().add(it);
        }
        return new Pendiente(reserva, creado, actualizado);
    }

    private EstadoReserva decidirEstado(LocalDateTime fecha, LocalDateTime ahora, int dur, int personas) {
        double r = rnd.nextDouble();
        boolean finDeSemana = esFinDeSemana(fecha.getDayOfWeek());
        double pCancel = 0.10 + (personas >= 6 ? 0.05 : 0.0);
        double pNoShow = 0.04 + (finDeSemana ? 0.0 : 0.02);

        if (fecha.plusMinutes(dur).isBefore(ahora)) {
            if (r < pCancel) return EstadoReserva.CANCELADA;
            if (r < pCancel + pNoShow) return EstadoReserva.NO_SHOW;
            return EstadoReserva.COMPLETADA;
        }
        if (fecha.isBefore(ahora)) {
            return r < pCancel ? EstadoReserva.CANCELADA : EstadoReserva.EN_CURSO;
        }
        long dias = ChronoUnit.DAYS.between(ahora.toLocalDate(), fecha.toLocalDate());
        if (r < 0.05) return EstadoReserva.CANCELADA;
        double pConfirm = dias <= 2 ? 0.85 : dias <= 7 ? 0.55 : 0.25;
        return rnd.nextDouble() < pConfirm ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
    }

    private Mesa buscarMesa(List<Mesa> mesas, Map<Long, LocalDateTime> libreDesde,
                            LocalDateTime fecha, int personas) {
        for (Mesa m : mesas) { // ordenadas por capacidad ascendente: usa la más ajustada
            if (m.getCapacidad() >= personas
                    && !libreDesde.getOrDefault(m.getId(), LocalDateTime.MIN).isAfter(fecha)) {
                return m;
            }
        }
        return null;
    }

    /** Anticipación: muchas el mismo día / día anterior, la mayoría en la semana previa, pocas con semanas. */
    private LocalDateTime fechaCreacion(LocalDateTime fecha, LocalDateTime ahora) {
        double u = rnd.nextDouble();
        double horas;
        if (u < 0.35) horas = 2 + rnd.nextDouble() * 22;
        else if (u < 0.70) horas = 24 + rnd.nextDouble() * 144;
        else if (u < 0.92) horas = 168 + rnd.nextDouble() * 336;
        else horas = 504 + rnd.nextDouble() * 576;
        LocalDateTime creado = fecha.minusSeconds((long) (horas * 3600));
        if (creado.isAfter(ahora)) {
            creado = ahora.minusMinutes(1 + rnd.nextInt(600));
        }
        return creado;
    }

    private LocalDateTime fechaActualizacion(EstadoReserva estado, LocalDateTime creado,
                                             LocalDateTime fecha, int dur, LocalDateTime ahora) {
        LocalDateTime upd = switch (estado) {
            case PENDIENTE -> creado;
            case CONFIRMADA, CANCELADA -> creado.plusMinutes(5 + rnd.nextInt(60 * 36));
            case EN_CURSO, NO_SHOW -> fecha.plusMinutes(15 + rnd.nextInt(30));
            case COMPLETADA -> fecha.plusMinutes(dur - 20 + rnd.nextInt(40));
        };
        if ((estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.CANCELADA) && upd.isAfter(fecha)) {
            upd = fecha;
        }
        if (upd.isAfter(ahora)) upd = ahora;
        if (upd.isBefore(creado)) upd = creado;
        return upd;
    }

    // ------------------------------------------------------------------ pedido anticipado

    private List<ReservaItem> generarItems(Reserva reserva, Map<CategoriaProducto, List<Producto>> menu,
                                           boolean almuerzo, boolean finde) {
        if (menu.isEmpty() || rnd.nextDouble() > 0.78) { // ~78% pre-ordena
            return List.of();
        }
        Map<Long, ReservaItem> acumulado = new LinkedHashMap<>();
        for (int i = 0; i < reserva.getNumPersonas(); i++) {
            pedir(reserva, menu, CategoriaProducto.PLATO_FUERTE, 0.95, acumulado);
            pedir(reserva, menu, CategoriaProducto.BEBIDA, almuerzo ? 0.85 : 0.75, acumulado);
            pedir(reserva, menu, CategoriaProducto.ENTRADA, almuerzo ? 0.30 : 0.45, acumulado);
            pedir(reserva, menu, CategoriaProducto.POSTRE, (almuerzo ? 0.22 : 0.35) + (finde ? 0.10 : 0), acumulado);
            pedir(reserva, menu, CategoriaProducto.OTRO, 0.06, acumulado);
        }
        return new ArrayList<>(acumulado.values());
    }

    private void pedir(Reserva reserva, Map<CategoriaProducto, List<Producto>> menu, CategoriaProducto cat,
                       double prob, Map<Long, ReservaItem> acumulado) {
        List<Producto> lista = menu.get(cat);
        if (lista == null || lista.isEmpty() || rnd.nextDouble() > prob) {
            return;
        }
        // sesgo: los primeros de la lista son los más populares
        Producto p = lista.get((int) (lista.size() * Math.pow(rnd.nextDouble(), 1.6)));
        ReservaItem existente = acumulado.get(p.getId());
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + 1);
        } else {
            acumulado.put(p.getId(), ReservaItem.builder().reserva(reserva).producto(p)
                    .cantidad(1).precioUnitario(p.getPrecio()).build());
        }
    }

    // ------------------------------------------------------------------ persistencia por lotes

    /**
     * Guarda el lote y luego corrige created_at / updated_at con SQL, porque @CreationTimestamp y
     * @UpdateTimestamp los fijarían a "ahora" y se perdería la historia temporal.
     */
    private void volcar(List<Pendiente> buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        List<Reserva> reservas = buffer.stream().map(Pendiente::reserva).collect(Collectors.toList());
        reservaRepository.saveAll(reservas);
        em.flush();

        jdbcTemplate.batchUpdate("UPDATE reservas SET created_at = ?, updated_at = ? WHERE id = ?",
                buffer, buffer.size(), (ps, p) -> {
                    ps.setTimestamp(1, Timestamp.from(p.creado().atZone(ZONA).toInstant()));
                    ps.setTimestamp(2, Timestamp.from(p.actualizado().atZone(ZONA).toInstant()));
                    ps.setLong(3, p.reserva().getId());
                });

        em.clear();
        buffer.clear();
    }

    // ------------------------------------------------------------------ tendencias

    private static boolean esFinDeSemana(DayOfWeek d) {
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private static double factorDiaSemana(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> 0.55;
            case TUESDAY -> 0.65;
            case WEDNESDAY -> 0.75;
            case THURSDAY -> 0.95;
            case FRIDAY -> 1.45;
            case SATURDAY -> 1.70;
            case SUNDAY -> 1.25;
        };
    }

    /** Temporada alta: diciembre-enero y mitad de año; baja: sep. y feb.-mar. */
    private static double factorMes(int mes) {
        return switch (mes) {
            case 1 -> 1.25;
            case 2 -> 1.00;
            case 3 -> 0.95;
            case 4 -> 1.05;
            case 5 -> 1.00;
            case 6 -> 1.05;
            case 7 -> 1.15;
            case 8 -> 0.95;
            case 9 -> 0.90;
            case 10 -> 1.00;
            case 11 -> 1.05;
            default -> 1.35;
        };
    }

    /** @return {multiplicador de volumen, sesgo hacia almuerzo (1 = normal)} */
    private static double[] factorEspecial(LocalDate d) {
        int y = d.getYear();
        if (d.getMonthValue() == 2 && d.getDayOfMonth() == 14) return new double[]{1.8, 0.3};
        if (d.getMonthValue() == 12 && d.getDayOfMonth() == 24) return new double[]{0.5, 1.0};
        if (d.getMonthValue() == 12 && d.getDayOfMonth() == 25) return new double[]{0.6, 1.5};
        if (d.getMonthValue() == 12 && d.getDayOfMonth() == 31) return new double[]{1.4, 0.4};
        if (d.getMonthValue() == 1 && d.getDayOfMonth() == 1) return new double[]{0.4, 1.0};

        LocalDate diaMadre = LocalDate.of(y, 5, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
        if (d.equals(diaMadre)) return new double[]{2.0, 2.2};
        if (d.equals(diaMadre.minusDays(1))) return new double[]{1.3, 1.0};

        LocalDate pascua = pascua(y);
        if (!d.isBefore(pascua.minusDays(4)) && !d.isAfter(pascua)) return new double[]{1.5, 1.0};
        if (!d.isBefore(pascua.minusDays(7)) && d.isBefore(pascua.minusDays(4))) return new double[]{1.2, 1.0};
        return new double[]{1.0, 1.0};
    }

    /** Domingo de Pascua (algoritmo gregoriano anónimo). */
    private static LocalDate pascua(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25;
        int g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(y, mes, dia);
    }

    /** Dos picos: almuerzo (~13:00) y cena (~20:00); la cena pesa más viernes/sábado. */
    private static double pesoHora(LocalTime t, DayOfWeek dow, double sesgoAlmuerzo) {
        double h = t.getHour() + t.getMinute() / 60.0;
        boolean finde = esFinDeSemana(dow);
        double almuerzo = Math.exp(-Math.pow(h - 13.0, 2) / (2 * 0.9 * 0.9)) * (finde ? 1.2 : 1.0) * sesgoAlmuerzo;
        double cenaBase = dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY ? 1.35
                : dow == DayOfWeek.SUNDAY ? 0.7 : 1.0;
        double cena = 1.3 * Math.exp(-Math.pow(h - 20.0, 2) / (2 * 1.1 * 1.1)) * cenaBase;
        return 0.05 + almuerzo + cena;
    }

    private int tamanoGrupo(DayOfWeek dow, int maxCap) {
        double[] w = {3, 34, 13, 26, 7, 8, 2, 4, 1, 2}; // tamaños 1..10: dominan parejas y grupos de 4
        if (esFinDeSemana(dow)) {
            for (int i = 4; i < w.length; i++) w[i] *= 1.5;
        }
        return Math.min(elegirPonderado(w) + 1, maxCap);
    }

    // ------------------------------------------------------------------ utilidades

    private List<LocalTime> slotsDe(Restaurante r) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime t = r.getHoraApertura();
        while (!t.plusMinutes(r.getDuracionReservaMin()).isAfter(r.getHoraCierre())) {
            slots.add(t);
            t = t.plusMinutes(30);
        }
        return slots;
    }

    private int elegirPonderado(double[] pesos) {
        double total = 0;
        for (double p : pesos) total += p;
        double x = rnd.nextDouble() * total;
        for (int i = 0; i < pesos.length; i++) {
            x -= pesos[i];
            if (x <= 0) return i;
        }
        return pesos.length - 1;
    }

    /** Muestreo de Poisson (Knuth). */
    private int poisson(double lambda) {
        double l = Math.exp(-lambda), p = 1.0;
        int k = 0;
        do {
            k++;
            p *= rnd.nextDouble();
        } while (p > l);
        return k - 1;
    }
}