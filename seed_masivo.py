# -*- coding: utf-8 -*-
"""
Seed masivo para CelFinder - usa usuarios EXISTENTES en la BD
Genera suficientes datos para un tablero Power BI completo.
pip install pymongo bcrypt
"""
import sys, random, uuid, base64
from pymongo import MongoClient
from datetime import datetime, timedelta, date

sys.stdout.reconfigure(encoding='utf-8')

client = MongoClient("mongodb://localhost:27017/")
db = client["celfinder"]

print("=" * 60)
print("SEED MASIVO CELFINDER - Power BI Ready")
print("=" * 60)

# ─── LEER USUARIOS EXISTENTES ──────────────────────────────────
print("\n[1] Leyendo usuarios existentes...")
todos_usuarios = list(db.usuarios.find({}, {"_id": 1, "nombreUsuario": 1, "roles": 1, "ciudad": 1, "departamento": 1}))

# Separar por rol
vendedores = []
compradores = []
for u in todos_usuarios:
    uid = str(u["_id"])
    nombre = u.get("nombreUsuario", "")
    roles = u.get("roles", [])
    if isinstance(roles, str):
        roles = [roles]
    roles_str = " ".join(str(r) for r in roles)
    if "VENDEDOR" in roles_str or "ADMIN" in roles_str:
        vendedores.append({"id": uid, "nombre": nombre})
    elif len(nombre) > 2 and u.get("ciudad"):  # solo usuarios con datos mínimos
        compradores.append({"id": uid, "nombre": nombre})

# Garantizar tener suficientes vendedores (agregar del script anterior si existen)
print(f"   Vendedores encontrados: {len(vendedores)}")
print(f"   Compradores encontrados: {len(compradores)}")

# Si hay pocos vendedores, ascender algunos compradores a VENDEDOR en la BD
if len(vendedores) < 8:
    candidatos = compradores[:8 - len(vendedores)]
    for c in candidatos:
        db.usuarios.update_one(
            {"_id": c["id"]},
            {"$addToSet": {"roles": "ROLE_VENDEDOR"}}
        )
        c["es_vendedor"] = True
        vendedores.append(c)
        compradores.remove(c)
    print(f"   Ascendidos {len(candidatos)} usuarios a vendedor")

random.shuffle(compradores)
random.shuffle(vendedores)

if len(compradores) < 10:
    print("   WARN: Pocos compradores, usando los disponibles")

print(f"   Usando {len(vendedores)} vendedores y {len(compradores)} compradores")

# ─── HELPERS ───────────────────────────────────────────────────
def nid(): return str(uuid.uuid4())
def rdias(a, b=0): return datetime.now() - timedelta(days=random.randint(b, a))
def rdate(a, b=0): return (datetime.now() - timedelta(days=random.randint(b, a))).date()
def pick(lst): return random.choice(lst)
def pickn(lst, n): return random.sample(lst, min(n, len(lst)))

def svg_img(color, texto):
    s = f'<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="300" height="300" fill="{color}"/><text x="150" y="155" font-family="Arial" font-size="22" fill="white" text-anchor="middle">{texto}</text></svg>'
    return "data:image/svg+xml;base64," + base64.b64encode(s.encode()).decode()

COLORES = ["#1a1a2e","#16213e","#0f3460","#533483","#2b2d42","#c0392b","#1abc9c","#8e44ad","#2c3e50","#e74c3c","#3498db","#27ae60"]

# ─── CATEGORÍAS ────────────────────────────────────────────────
print("\n[2] Asegurando categorias...")
cats_def = [
    ("Smartphones",       "Smartphones y Tablets",  "📱"),
    ("Tablets",           "Smartphones y Tablets",  "💻"),
    ("Laptops",           "Computadores",            "🖥️"),
    ("Accesorios",        "Accesorios",              "🎧"),
    ("Smartwatches",      "Wearables",               "⌚"),
    ("Camaras",           "Fotografia",              "📷"),
    ("Consolas Gaming",   "Gaming",                  "🎮"),
    ("Audio",             "Audio y Video",           "🔊"),
    ("Redes y Conectividad","Computo",               "📡"),
    ("Componentes PC",    "Computo",                 "🖱️"),
]
cat_ids = {}
for nombre, grupo, icono in cats_def:
    ex = db.categorias.find_one({"nombre": nombre})
    if ex:
        cat_ids[nombre] = str(ex["_id"])
    else:
        cid = nid()
        db.categorias.insert_one({
            "_id": cid, "nombre": nombre, "grupo": grupo, "icono": icono,
            "especificaciones": [], "activa": True,
            "fechaCreacion": rdias(200, 90),
            "creadaPor": vendedores[0]["id"] if vendedores else "system"
        })
        cat_ids[nombre] = cid
        print(f"   + Categoria: {nombre}")

# ─── CATÁLOGO DE PRODUCTOS ─────────────────────────────────────
CATALOGO = [
    # (nombre, marca, categoria, precio_min, precio_max, estado_cond)
    ("Samsung Galaxy S24 Ultra 256GB",    "Samsung",   "Smartphones",      4800000, 5500000, "nuevo"),
    ("Samsung Galaxy S24+ 256GB",         "Samsung",   "Smartphones",      3800000, 4200000, "nuevo"),
    ("Samsung Galaxy A55 5G 256GB",       "Samsung",   "Smartphones",      1400000, 1600000, "nuevo"),
    ("Samsung Galaxy A35 5G 128GB",       "Samsung",   "Smartphones",       900000, 1100000, "nuevo"),
    ("Samsung Galaxy S23 FE 128GB",       "Samsung",   "Smartphones",      1800000, 2200000, "usado"),
    ("iPhone 15 Pro Max 256GB",           "Apple",     "Smartphones",      5800000, 6500000, "nuevo"),
    ("iPhone 15 Pro 128GB",               "Apple",     "Smartphones",      4500000, 5000000, "nuevo"),
    ("iPhone 15 128GB",                   "Apple",     "Smartphones",      3400000, 3800000, "nuevo"),
    ("iPhone 14 128GB",                   "Apple",     "Smartphones",      2800000, 3200000, "usado"),
    ("iPhone 13 128GB",                   "Apple",     "Smartphones",      2100000, 2500000, "usado"),
    ("Xiaomi Redmi Note 13 Pro 256GB",    "Xiaomi",    "Smartphones",      1200000, 1450000, "nuevo"),
    ("Xiaomi 14 Ultra 512GB",             "Xiaomi",    "Smartphones",      4200000, 4800000, "nuevo"),
    ("Xiaomi Poco X6 Pro 256GB",          "Xiaomi",    "Smartphones",      1600000, 1900000, "nuevo"),
    ("Motorola Edge 40 Pro 256GB",        "Motorola",  "Smartphones",      2600000, 3000000, "nuevo"),
    ("Motorola Moto G84 256GB",           "Motorola",  "Smartphones",       800000,  950000, "nuevo"),
    ("OnePlus 12 512GB",                  "OnePlus",   "Smartphones",      4000000, 4500000, "nuevo"),
    ("Google Pixel 8 Pro 256GB",          "Google",    "Smartphones",      3800000, 4200000, "nuevo"),
    ("Realme GT 5 Pro 256GB",             "Realme",    "Smartphones",      2400000, 2800000, "nuevo"),
    ("OPPO Find X7 Ultra 512GB",          "OPPO",      "Smartphones",      4500000, 5200000, "nuevo"),
    ("Honor Magic 6 Pro 512GB",           "Honor",     "Smartphones",      3200000, 3700000, "nuevo"),
    # Tablets
    ("Samsung Galaxy Tab S9 Ultra",       "Samsung",   "Tablets",          3500000, 4000000, "nuevo"),
    ("Samsung Galaxy Tab S9 FE 256GB",    "Samsung",   "Tablets",          1700000, 2000000, "nuevo"),
    ("iPad Pro 12.9 M2 256GB",            "Apple",     "Tablets",          5200000, 6000000, "nuevo"),
    ("iPad Air 5ta Gen 256GB",            "Apple",     "Tablets",          2700000, 3000000, "nuevo"),
    ("Lenovo Tab P12 Pro",                "Lenovo",    "Tablets",          1500000, 1800000, "nuevo"),
    # Laptops
    ("MacBook Air M3 16GB 512GB",         "Apple",     "Laptops",          6500000, 7500000, "nuevo"),
    ("MacBook Pro 14 M3 Pro",             "Apple",     "Laptops",          9000000,10500000, "nuevo"),
    ("Lenovo IdeaPad Gaming 3 RTX4060",   "Lenovo",    "Laptops",          4200000, 5000000, "nuevo"),
    ("Dell XPS 15 i7 RTX4070",           "Dell",      "Laptops",          7000000, 8000000, "nuevo"),
    ("HP Victus 15 RTX4060 i7",          "HP",        "Laptops",          4500000, 5500000, "nuevo"),
    ("ASUS ROG Zephyrus G14 2024",        "ASUS",      "Laptops",          7500000, 8500000, "nuevo"),
    ("Acer Nitro 5 RTX4050",             "Acer",      "Laptops",          3500000, 4000000, "nuevo"),
    # Accesorios
    ("AirPods Pro 2da Gen USB-C",         "Apple",     "Accesorios",       1100000, 1300000, "nuevo"),
    ("Samsung Galaxy Buds3 Pro",          "Samsung",   "Accesorios",        800000,  950000, "nuevo"),
    ("Sony WH-1000XM5",                   "Sony",      "Accesorios",       1200000, 1400000, "nuevo"),
    ("Cargador Samsung 65W Super Fast",   "Samsung",   "Accesorios",        180000,  220000, "nuevo"),
    ("MagSafe Charger 15W",               "Apple",     "Accesorios",        220000,  280000, "nuevo"),
    ("Anker PowerBank 20000mAh 65W",      "Anker",     "Accesorios",        270000,  320000, "nuevo"),
    ("Funda iPhone 15 Pro MagSafe",       "Apple",     "Accesorios",        180000,  220000, "nuevo"),
    ("Protector Pantalla S24 Ultra",      "Spigen",    "Accesorios",         45000,   65000, "nuevo"),
    # Smartwatches
    ("Apple Watch Series 9 45mm",         "Apple",     "Smartwatches",     2000000, 2400000, "nuevo"),
    ("Apple Watch Ultra 2",               "Apple",     "Smartwatches",     4000000, 4600000, "nuevo"),
    ("Samsung Galaxy Watch 6 Classic 47mm","Samsung",  "Smartwatches",     1300000, 1600000, "nuevo"),
    ("Garmin Forerunner 965",             "Garmin",    "Smartwatches",     2800000, 3200000, "nuevo"),
    ("Amazfit GTR 4",                     "Amazfit",   "Smartwatches",      550000,  700000, "nuevo"),
    # Gaming
    ("PlayStation 5 Slim + DualSense",    "Sony",      "Consolas Gaming",  3100000, 3500000, "nuevo"),
    ("Xbox Series X 1TB",                 "Microsoft", "Consolas Gaming",  2800000, 3200000, "nuevo"),
    ("Nintendo Switch OLED",              "Nintendo",  "Consolas Gaming",  1200000, 1500000, "nuevo"),
    ("Steam Deck OLED 512GB",             "Valve",     "Consolas Gaming",  1900000, 2300000, "nuevo"),
    # Audio
    ("Sony WF-1000XM5",                   "Sony",      "Audio",             900000, 1100000, "nuevo"),
    ("JBL PartyBox 110",                  "JBL",       "Audio",             700000,  900000, "nuevo"),
    ("Bose SoundLink Max",                "Bose",      "Audio",            1100000, 1400000, "nuevo"),
]

# ─── INSERTAR PRODUCTOS EN MASA ────────────────────────────────
print(f"\n[3] Insertando {len(CATALOGO) * 2} productos (2 por modelo, distintos vendedores)...")

# Primero limpiar productos de prueba
borrados = db.productos.delete_many({
    "nombre": {"$regex": "^(test|prueba|broma|asdf|qwerty|xxx|123|demo|aaa)", "$options": "i"}
})
if borrados.deleted_count:
    print(f"   Eliminados {borrados.deleted_count} productos basura")

prod_catalogue = []  # {id, nombre, categoria, precio, vendedorId, estadoVenta}

estados_venta_dist = ["disponible"] * 6 + ["VENDIDO"] * 3 + ["AGOTADO"] * 1

for i, (nombre, marca, cat, pmin, pmax, cond) in enumerate(CATALOGO):
    # Crear 2 unidades con distintos vendedores
    for replica in range(2):
        vend = vendedores[(i * 2 + replica) % len(vendedores)]
        ya = db.productos.find_one({"nombre": nombre, "vendedorId": vend["id"]})
        if ya:
            prod_catalogue.append({
                "id": str(ya["_id"]),
                "nombre": nombre, "categoria": cat,
                "precio": ya["precio"],
                "vendedorId": vend["id"],
                "estadoVenta": ya.get("estadoVenta", "disponible")
            })
            continue

        precio = random.randint(pmin, pmax)
        ev = pick(estados_venta_dist)
        descuento = random.choice([None, None, None, 5, 8, 10, 12, 15, 20])
        pid = nid()
        dias_pub = random.randint(1, 180)
        doc = {
            "_id": pid,
            "nombre": nombre,
            "categoria": cat,
            "precio": precio,
            "marca": marca,
            "descripcion": f"{nombre} en excelente estado. {cond.capitalize()}. Envío a todo el país. Garantía incluida.",
            "vendedorId": vend["id"],
            "fechaPublicacion": rdias(dias_pub),
            "estadoVenta": ev,
            "imagenBase64": svg_img(COLORES[i % len(COLORES)], marca),
            "estado": cond,
            "especificaciones": {},
            "stock": random.randint(1, 5) if ev == "disponible" else 0,
            "descuento": descuento,
            "compradorId": None,
        }
        db.productos.insert_one(doc)
        prod_catalogue.append({
            "id": pid, "nombre": nombre, "categoria": cat,
            "precio": precio, "vendedorId": vend["id"], "estadoVenta": ev
        })

print(f"   Total productos en catalogo: {db.productos.count_documents({})}")

# Separar disponibles y vendidos
prods_disponibles = [p for p in prod_catalogue if p["estadoVenta"] == "disponible"]
prods_vendidos    = [p for p in prod_catalogue if p["estadoVenta"] == "VENDIDO"]

# ─── SOLICITUDES Y ENVÍOS MASIVOS ─────────────────────────────
print("\n[4] Generando solicitudes, envios, chats y notificaciones...")

ESTADOS_SEG = ["PENDIENTE", "PREPARANDO", "ENVIADO", "ENTREGADO"]
ESTADOS_ENV = ["preparando", "en camino", "enviado", "entregado"]
CIUDADES_CO = ["Barranquilla", "Bogota", "Medellin", "Cali", "Cartagena",
               "Bucaramanga", "Santa Marta", "Pereira", "Manizales", "Cucuta",
               "Ibague", "Villavicencio", "Pasto", "Neiva", "Armenia"]

sol_count = 0
env_count = 0
chat_count = 0
notif_count = 0
resena_count = 0

# Generar entre 150-200 solicitudes usando usuarios reales
num_solicitudes = 180
todos_compradores_pool = compradores.copy()
if len(todos_compradores_pool) < 5:
    # usar también vendedores como compradores de otros vendedores
    todos_compradores_pool += vendedores

for idx in range(num_solicitudes):
    # Seleccionar producto y comprador aleatorios
    prod = pick(prod_catalogue)
    comprador = pick(todos_compradores_pool)
    
    # evitar que el comprador sea el mismo vendedor
    if comprador["id"] == prod["vendedorId"] and len(todos_compradores_pool) > 1:
        candidatos = [c for c in todos_compradores_pool if c["id"] != prod["vendedorId"]]
        if candidatos:
            comprador = pick(candidatos)

    # vendedor del producto
    vend_sol = next((v for v in vendedores if v["id"] == prod["vendedorId"]), pick(vendedores))

    # fechas distribuidas en los últimos 6 meses
    dias_atras = random.randint(0, 180)
    fecha_sol = rdias(dias_atras)

    # estado progresivo según tiempo
    if dias_atras > 90:
        estado = "aprobada"
        seg = "ENTREGADO"
        est_env = "entregado"
    elif dias_atras > 45:
        estado = "aprobada"
        seg = pick(["ENVIADO", "ENTREGADO"])
        est_env = "enviado" if seg == "ENVIADO" else "entregado"
    elif dias_atras > 15:
        estado = "aprobada"
        seg = pick(["PREPARANDO", "ENVIADO"])
        est_env = pick(["preparando", "en camino"])
    elif dias_atras > 3:
        estado = pick(["aprobada", "pendiente"])
        seg = "PENDIENTE" if estado == "pendiente" else "PREPARANDO"
        est_env = "preparando"
    else:
        estado = pick(["pendiente", "pendiente", "aprobada"])
        seg = "PENDIENTE"
        est_env = "preparando"

    ciudad = pick(CIUDADES_CO)
    sid = nid()

    sol_doc = {
        "_id": sid,
        "tipoSolicitud": "compra",
        "usuarioId": comprador["id"],
        "estado": estado,
        "fechaSolicitud": fecha_sol,
        "productoId": prod["id"],
        "nombreProducto": prod["nombre"],
        "nombreComprador": comprador["nombre"],
        "vendedorId": vend_sol["id"],
        "direccionComprador": f"Cra {random.randint(1,100)} #{random.randint(1,99)}-{random.randint(1,99)}, {ciudad}",
        "correoComprador": f"{comprador['nombre']}@email.com",
        "numeroContactoComprador": f"3{random.randint(10,25)}{random.randint(1000000,9999999)}",
        "descripcionVendedor": "",
        "estadoSeguimiento": seg,
        "fechaRespuesta": fecha_sol + timedelta(hours=random.randint(1, 24)) if estado == "aprobada" else None,
    }
    db.solicitudes.insert_one(sol_doc)
    sol_count += 1

    # Envío si está aprobada
    if estado == "aprobada":
        fecha_env = fecha_sol + timedelta(hours=random.randint(12, 48))
        fecha_est = (fecha_env + timedelta(days=random.randint(1, 7))).date()
        codigo = "CEL" + "".join([str(random.randint(0, 9)) for _ in range(9)])
        db.envios.insert_one({
            "_id": nid(),
            "solicitudId": sid,
            "vendedorId": vend_sol["id"],
            "compradorId": comprador["id"],
            "codigoSeguimiento": codigo,
            "estado": est_env,
            "fechaEstimada": datetime.combine(fecha_est, datetime.min.time()),
            "fechaActualizacion": fecha_env,
        })
        env_count += 1

    # Chat (1 de cada 3 solicitudes tiene chat)
    if random.random() < 0.33:
        frases_comprador = [
            "Hola, cuando puedo recibir mi pedido?",
            "Ya realizce el pago, cuanto tarda el envio?",
            "El producto viene con garantia?",
            "Podria darme el numero de guia?",
            "Muchas gracias por la atencion!",
        ]
        frases_vendedor = [
            "Hola! Gracias por tu compra. Lo enviamos en 24-48h.",
            "Si, el producto tiene garantia de fabrica.",
            "Tu guia de envio es " + "CEL" + str(random.randint(100000000, 999999999)),
            "Cualquier duda no dudes en escribirnos.",
            "Con gusto, fue un placer atenderte!",
        ]
        n_msgs = random.randint(2, 5)
        msgs = []
        base_t = fecha_sol + timedelta(hours=1)
        for mi in range(n_msgs):
            if mi % 2 == 0:
                rem = comprador["id"]
                cont = pick(frases_comprador)
            else:
                rem = vend_sol["id"]
                cont = pick(frases_vendedor)
            msgs.append({
                "remitenteId": rem,
                "contenido": cont,
                "fecha": base_t + timedelta(minutes=mi * random.randint(5, 60))
            })
        db.chats_pedidos.insert_one({
            "_id": nid(),
            "solicitudId": sid,
            "compradorId": comprador["id"],
            "vendedorId": vend_sol["id"],
            "productoId": prod["id"],
            "mensajes": msgs,
        })
        chat_count += 1

    # Notificacion al comprador
    if estado == "aprobada":
        notifs = [
            f"Tu pedido de {prod['nombre'][:40]} fue aprobado",
            f"Tu envio esta en camino - {seg}",
        ]
        for msg_n in notifs:
            db.notificaciones.insert_one({
                "_id": nid(),
                "usuarioId": comprador["id"],
                "mensaje": msg_n,
                "fecha": fecha_sol + timedelta(hours=random.randint(1, 12)),
                "leida": random.choice([True, False]),
                "enlace": "/notificaciones",
            })
            notif_count += 1

        # Notificacion al vendedor
        db.notificaciones.insert_one({
            "_id": nid(),
            "usuarioId": vend_sol["id"],
            "mensaje": f"Nueva venta: {prod['nombre'][:40]} - ${prod['precio']:,.0f}",
            "fecha": fecha_sol + timedelta(minutes=30),
            "leida": random.choice([True, True, False]),
            "enlace": "/historialSolicitudesVendedor",
        })
        notif_count += 1

    # Reseña si fue entregado
    if seg == "ENTREGADO" and random.random() < 0.5:
        puntuacion = random.choices([3, 4, 5], weights=[1, 3, 6])[0]
        titulos = {
            5: ["Excelente producto, muy satisfecho", "Lo recomiendo totalmente", "Mejor de lo esperado"],
            4: ["Muy buen producto", "Buena compra, llegó rápido", "Cumple con lo ofrecido"],
            3: ["Ok, pero esperaba más", "Regular, el empaque llegó dañado", "Funciona bien pero tardó mucho"],
        }
        comentarios = {
            5: "El producto llegó en perfecto estado y antes del tiempo estimado. El vendedor fue muy atento y respondió todas mis preguntas. 100% recomendado.",
            4: "Buena experiencia de compra. El producto es exactamente como se describe. El envío tardó un poco más de lo esperado pero llegó en perfectas condiciones.",
            3: "El producto llegó bien pero el empaque tenía golpes. El vendedor tardó en responder. Funciona correctamente, espero que mejoren el servicio.",
        }
        db.reseñas.insert_one({
            "_id": nid(),
            "productoId": prod["id"],
            "usuarioId": comprador["id"],
            "nombreUsuario": comprador["nombre"],
            "titulo": pick(titulos[puntuacion]),
            "comentario": comentarios[puntuacion],
            "puntuacion": puntuacion,
            "compraVerificada": True,
            "fotosBase64": [],
            "votosUtiles": random.randint(0, 30),
            "votosInutiles": random.randint(0, 5),
            "fecha": fecha_sol + timedelta(days=random.randint(3, 15)),
        })
        resena_count += 1

    if (idx + 1) % 30 == 0:
        print(f"   Progreso: {idx+1}/{num_solicitudes} solicitudes...")

# ─── SOLICITUDES TIPO VENDEDOR ─────────────────────────────────
print("\n[5] Agregando solicitudes de registro como vendedor...")
sol_vendedor_count = 0
for i in range(20):
    comp = pick(compradores) if compradores else pick(vendedores)
    sid2 = nid()
    dias = random.randint(5, 150)
    est = pick(["aprobada", "aprobada", "pendiente", "rechazada"])
    db.solicitudes.insert_one({
        "_id": sid2,
        "tipoSolicitud": "vendedor",
        "usuarioId": comp["id"],
        "estado": est,
        "fechaSolicitud": rdias(dias),
        "nombreCompleto": comp["nombre"].replace("_", " ").title(),
        "correo": f"{comp['nombre']}@gmail.com",
        "telefono": f"3{random.randint(10,25)}{random.randint(1000000,9999999)}",
        "direccion": f"Bogota, Colombia",
        "motivo": pick([
            "Quiero vender celulares reacondicionados",
            "Tengo distribuidora de accesorios tecnologicos",
            "Soy importador de dispositivos moviles",
            "Vendo laptops y tablets al por mayor",
        ]),
        "comentarioAdmin": "Solicitud revisada." if est != "pendiente" else None,
        "fechaRespuesta": rdias(dias - 2) if est != "pendiente" else None,
        "estadoSeguimiento": None,
    })
    sol_vendedor_count += 1

# ─── RESUMEN ───────────────────────────────────────────────────
print("\n" + "=" * 60)
print("RESUMEN FINAL - BASE DE DATOS POWER BI READY")
print("=" * 60)
cols = ["usuarios","categorias","productos","solicitudes","envios","chats_pedidos","mensajes","notificaciones","reseñas"]
for c in cols:
    n = db[c].count_documents({})
    print(f"  {c:25s}: {n:>6} documentos")
print("=" * 60)
print(f"\nNuevas solicitudes: {sol_count + sol_vendedor_count}")
print(f"Nuevos envios:      {env_count}")
print(f"Nuevos chats:       {chat_count}")
print(f"Nuevas notifs:      {notif_count}")
print(f"Nuevas reseñas:     {resena_count}")
print("\nTablas para Power BI: usuarios, productos, solicitudes, envios, reseñas, categorias")
print("OK. Seed completado.")
client.close()
