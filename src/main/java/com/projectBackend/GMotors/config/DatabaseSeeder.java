package com.projectBackend.GMotors.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.projectBackend.GMotors.model.*;
import com.projectBackend.GMotors.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(
            UsuarioRepository usuarioRepo,
            RolRepository rolRepo,
            UsuarioRolRepository usuarioRolRepo,
            PasswordEncoder passwordEncoder,
            CategoriaRepository categoriaRepo,
            ProductoRepository productoRepo,
            TipoRepository tipoRepo,
            MotoRepository motoRepo,
            FacturaRepository facturaRepo,
            RegistroRepository registroRepo
    ) {
        return args -> {

            // ─── 0. CREAR ROLES SI NO EXISTEN ────────────────────────────────────
            if (rolRepo.count() == 0) {
                Rol rAdmin = new Rol(); rAdmin.setNombre("ADMIN");    rolRepo.save(rAdmin);
                Rol rCli   = new Rol(); rCli.setNombre("CLIENTE");   rolRepo.save(rCli);
                Rol rMec   = new Rol(); rMec.setNombre("MECANICO");  rolRepo.save(rMec);
            }
            // Obtener IDs reales (pueden variar en H2)
            final Long ADMIN_ROL   = rolRepo.findAll().stream().filter(r -> r.getNombre().equals("ADMIN")).findFirst().map(Rol::getId_rol).orElse(1L);
            final Long CLIENTE_ROL = rolRepo.findAll().stream().filter(r -> r.getNombre().equals("CLIENTE")).findFirst().map(Rol::getId_rol).orElse(2L);
            final Long MECANICO_ROL= rolRepo.findAll().stream().filter(r -> r.getNombre().equals("MECANICO")).findFirst().map(Rol::getId_rol).orElse(3L);

            // ─── 1. ADMIN POR DEFECTO ────────────────────────────────────────────
            Usuario admin;
            if (usuarioRepo.findByCorreo("andres@gmotors.com").isEmpty()) {
                admin = new Usuario();
                admin.setNombre_completo("Andres Admin");
                admin.setNombre_usuario("andres_admin");
                admin.setCorreo("andres@gmotors.com");
                admin.setContrasena(passwordEncoder.encode("123"));
                admin.setPais("Ecuador"); admin.setCiudad("Quito");
                admin.setDescripcion("CEDULA: 1700000001 | TELEFONO: 0991234567");
                admin = usuarioRepo.save(admin);
                guardarRol(usuarioRolRepo, admin.getId_usuario(), ADMIN_ROL.intValue());
            } else {
                admin = usuarioRepo.findByCorreo("andres@gmotors.com").get();
            }

            // ─── 2. MECÁNICOS ────────────────────────────────────────────────────
            Usuario mec1 = seedUsuario(usuarioRepo, usuarioRolRepo, passwordEncoder,
                    "Carlos Méndez",  "carlos_tec", "carlos@gmotors.com",
                    "123", "Ecuador", "Quito",
                    "CEDULA: 1700000002 | TELEFONO: 0987654321", MECANICO_ROL.intValue());

            Usuario mec2 = seedUsuario(usuarioRepo, usuarioRolRepo, passwordEncoder,
                    "Diego Paredes", "diego_mec", "diego@gmotors.com",
                    "123", "Ecuador", "Guayaquil",
                    "CEDULA: 1700000003 | TELEFONO: 0976543210", MECANICO_ROL.intValue());

            // ─── 3. CLIENTES ─────────────────────────────────────────────────────
            Usuario cli1 = seedUsuario(usuarioRepo, usuarioRolRepo, passwordEncoder,
                    "Juan Pérez",    "juanp",    "juan@correo.com",
                    "123", "Ecuador", "Quito",
                    "CEDULA: 1712345678 | TELEFONO: 0993456789", CLIENTE_ROL.intValue());

            Usuario cli2 = seedUsuario(usuarioRepo, usuarioRolRepo, passwordEncoder,
                    "María García",  "mariag",   "maria@correo.com",
                    "123", "Ecuador", "Cuenca",
                    "CEDULA: 1798765432 | TELEFONO: 0984567890", CLIENTE_ROL.intValue());

            Usuario cli3 = seedUsuario(usuarioRepo, usuarioRolRepo, passwordEncoder,
                    "Roberto Sánchez","robertos", "roberto@correo.com",
                    "123", "Ecuador", "Quito",
                    "CEDULA: 1756789012 | TELEFONO: 0971234567", CLIENTE_ROL.intValue());

            // ─── 4. CATEGORÍAS ────────────────────────────────────────────────────
            if (categoriaRepo.count() == 0) {
                seedCategoria(categoriaRepo, "Lubricantes", "Aceites y grasas para motor y transmisión");
                seedCategoria(categoriaRepo, "Repuestos", "Piezas mecánicas de reemplazo");
                seedCategoria(categoriaRepo, "Accesorios", "Equipamiento y protección del motociclista");
                seedCategoria(categoriaRepo, "Filtros", "Filtros de aceite, aire y combustible");
                seedCategoria(categoriaRepo, "Herramientas", "Herramientas especializadas para taller");
            }

            // ─── 5. PRODUCTOS ─────────────────────────────────────────────────────
            if (productoRepo.count() == 0) {
                Long catLub  = categoriaRepo.findAll().stream().filter(c -> c.getNombre().equals("Lubricantes")).findFirst().map(c -> c.getId_categoria()).orElse(1L);
                Long catRep  = categoriaRepo.findAll().stream().filter(c -> c.getNombre().equals("Repuestos")).findFirst().map(c -> c.getId_categoria()).orElse(2L);
                Long catAcc  = categoriaRepo.findAll().stream().filter(c -> c.getNombre().equals("Accesorios")).findFirst().map(c -> c.getId_categoria()).orElse(3L);
                Long catFil  = categoriaRepo.findAll().stream().filter(c -> c.getNombre().equals("Filtros")).findFirst().map(c -> c.getId_categoria()).orElse(4L);

                seedProducto(productoRepo, "Aceite Motul 10W-40 1L", "Aceite semisintético 4T de alto rendimiento", "MOTUL-10W40", "COD-001", "12.50", "16.99", 25, catLub);
                seedProducto(productoRepo, "Aceite Shell Advance AX7 1L", "Aceite mineral para motos 4T hasta 150cc", "SHELL-AX7", "COD-002", "8.90", "12.50", 30, catLub);
                seedProducto(productoRepo, "Grasa de Leva Repsol 100g", "Grasa especializada para cadena y cables", "REPSOL-GRS", "COD-003", "3.20", "5.50", 15, catLub);
                seedProducto(productoRepo, "Filtro de aceite Yamaha OEM", "Filtro original para motores Yamaha 150-250cc", "YAM-FOEM", "COD-004", "4.80", "7.99", 20, catFil);
                seedProducto(productoRepo, "Filtro de aire K&N Performance", "Filtro de alto flujo lavable y reutilizable", "KN-AIR01", "COD-005", "22.00", "35.00", 8, catFil);
                seedProducto(productoRepo, "Pastillas de freno EBC FA131", "Pastillas delanteras orgánicas para uso urbano", "EBC-FA131", "COD-006", "18.50", "28.00", 12, catRep);
                seedProducto(productoRepo, "Kit cadena DID 428VX 120 eslabones", "Kit transmisión con cadena y piñones incluidos", "DID-428VX", "COD-007", "45.00", "72.00", 6, catRep);
                seedProducto(productoRepo, "Casco integral HJC CS-R3", "Casco certificado DOT y ECE 22.05, peso 1.4 kg", "HJC-CSR3", "COD-008", "65.00", "95.00", 5, catAcc);
                seedProducto(productoRepo, "Guantes Alpinestars SP-8 V3", "Guantes cuero de verano con protecciones CE", "ALP-SP8V3", "COD-009", "38.00", "58.00", 10, catAcc);
                seedProducto(productoRepo, "Batería YTX7A-BS 12V 6Ah", "Batería GEL libre de mantenimiento", "YTX-7ABS", "COD-010", "28.00", "42.00", 9, catRep);
            }

            // ─── 6. TIPOS DE MANTENIMIENTO ───────────────────────────────────────
            if (tipoRepo.count() == 0) {
                seedTipo(tipoRepo, "Mantenimiento General", "Cambio de aceite, filtro y revisión general del vehículo");
                seedTipo(tipoRepo, "Sistema de Frenos", "Revisión, limpieza y reemplazo de pastillas y líquido de frenos");
                seedTipo(tipoRepo, "Cadena y Transmisión", "Limpieza, lubricación y ajuste de cadena, o reemplazo de kit");
                seedTipo(tipoRepo, "Diagnóstico Eléctrico", "Revisión de batería, alternador, sistema de encendido y luces");
                seedTipo(tipoRepo, "Carburación y Combustible", "Limpieza de carburador o inyectores, ajuste de mezcla aire-combustible");
            }

            // ─── 7. MOTOS ─────────────────────────────────────────────────────────
            if (motoRepo.count() == 0 && cli1 != null && cli2 != null && cli3 != null) {
                seedMoto(motoRepo, "GXT-1234", 2021, "Yamaha", "FZ 16", "La Azul", "Naked", 650, 149, cli1.getId_usuario());
                seedMoto(motoRepo, "HBK-5678", 2019, "Honda", "CB500F", "La Roja", "Naked", 12800, 471, cli1.getId_usuario());
                seedMoto(motoRepo, "KWS-9012", 2022, "Kawasaki", "Z400", "Ninja Verde", "Sport", 3200, 399, cli2.getId_usuario());
                seedMoto(motoRepo, "SUZ-3456", 2020, "Suzuki", "GSX-R600", "Silver Ghost", "Sport", 8500, 599, cli3.getId_usuario());
                seedMoto(motoRepo, "BMW-7890", 2023, "BMW", "G310R", "Bavaria", "Naked", 1500, 313, cli2.getId_usuario());
            }

            // ─── 8. REGISTROS DE SERVICIO ─────────────────────────────────────────
            if (registroRepo.count() == 0 && admin != null) {
                var motos  = motoRepo.findAll();
                var tipos  = tipoRepo.findAll();
                var encargado = mec1 != null ? mec1 : admin;

                if (!motos.isEmpty() && !tipos.isEmpty()) {
                    seedRegistro(registroRepo, facturaRepo, motos.get(0), cli1, encargado, tipos.get(0), "Primer mantenimiento preventivo", 650,   2,  45);
                    seedRegistro(registroRepo, facturaRepo, motos.get(1), cli1, admin,     tipos.get(2), "Kit de cadena gastado, reemplazo completo", 12800, 3,  30);
                    seedRegistro(registroRepo, facturaRepo, motos.get(2), cli2, encargado, tipos.get(1), "Pastillas delanteras muy desgastadas", 3200,  1,  15);
                    seedRegistro(registroRepo, facturaRepo, motos.get(3), cli3, admin,     tipos.get(3), "Batería descargada, revisión eléctrica completa", 8500, 0,   7);
                    seedRegistro(registroRepo, facturaRepo, motos.get(4), cli2, encargado, tipos.get(4), "Motor con mezcla rica, ajuste necesario", 1500,  0,   3);
                }
            }
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void guardarRol(UsuarioRolRepository urRepo, Long idUsuario, int idRol) {
        UsuarioRol ur = new UsuarioRol(idUsuario, idRol);
        ur.setEstado(1);
        urRepo.save(ur);
    }

    private Usuario seedUsuario(UsuarioRepository repo, UsuarioRolRepository urRepo,
            PasswordEncoder encoder, String nombre, String usuario, String correo,
            String pass, String pais, String ciudad, String desc, int rolId) {
        if (repo.findByCorreo(correo).isPresent()) return repo.findByCorreo(correo).get();
        Usuario u = new Usuario();
        u.setNombre_completo(nombre);
        u.setNombre_usuario(usuario);
        u.setCorreo(correo);
        u.setContrasena(encoder.encode(pass));
        u.setPais(pais);
        u.setCiudad(ciudad);
        u.setDescripcion(desc);
        u = repo.save(u);
        guardarRol(urRepo, u.getId_usuario(), rolId);
        return u;
    }

    private void seedCategoria(CategoriaRepository repo, String nombre, String desc) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(desc);
        repo.save(c);
    }

    private void seedProducto(ProductoRepository repo, String nombre, String desc,
            String codProv, String codPers, String costo, String pvp, int stock, Long catId) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(desc);
        p.setCodigo_proveedor(codProv);
        p.setCodigo_personal(codPers);
        p.setCosto(new BigDecimal(costo));
        p.setPvp(new BigDecimal(pvp));
        p.setStock(stock);
        p.setFecha_registro(LocalDate.now());
        p.setFecha_modificacion(LocalDate.now());
        p.setId_categoria(catId);
        repo.save(p);
    }

    private void seedTipo(TipoRepository repo, String nombre, String desc) {
        Tipo t = new Tipo();
        t.setNombre(nombre);
        t.setDescripcion(desc);
        repo.save(t);
    }

    private void seedMoto(MotoRepository repo, String placa, int anio, String marca,
            String modelo, String nombre, String tipo, int km, int cc, Long idUsuario) {
        Moto m = new Moto();
        m.setPlaca(placa);
        m.setAnio(anio);
        m.setMarca(marca);
        m.setModelo(modelo);
        m.setNombreMoto(nombre);
        m.setTipoMoto(tipo);
        m.setKilometraje(km);
        m.setCilindraje(cc);
        m.setId_usuario(idUsuario);
        repo.save(m);
    }

    private void seedRegistro(RegistroRepository registroRepo, FacturaRepository facturaRepo,
            Moto moto, Usuario cliente, Usuario encargado, Tipo tipo,
            String obs, int km, int estado, int diasAtras) {
        Factura f = new Factura();
        f.setFechaEmision(LocalDate.now().minusDays(diasAtras));
        f.setIdUsuario(cliente.getId_usuario());
        f.setCostoTotal(BigDecimal.ZERO);
        f = facturaRepo.save(f);

        Registro r = new Registro();
        r.setFecha(LocalDate.now().minusDays(diasAtras));
        r.setObservaciones(obs);
        r.setEstado(estado);
        r.setKilometraje(km);
        r.setFactura(f);
        r.setCliente(cliente);
        r.setEncargado(encargado);
        r.setMoto(moto);
        r.setTipo(tipo);
        registroRepo.save(r);
    }
}
