"""
Script de Seed para poblar la base de datos CelFinder con datos realistas.
Requiere: pip install pymongo bcrypt
"""
import pymongo
from pymongo import MongoClient
from datetime import datetime, timedelta
import random
import uuid
import base64
import hashlib
import json

# ─── CONEXIÓN ──────────────────────────────────────────────────────────────────
client = MongoClient("mongodb://localhost:27017/")
db = client["celfinder"]

print("✅ Conectado a MongoDB - base de datos: celfinder")

# ─── HELPERS ───────────────────────────────────────────────────────────────────
def new_id():
    return str(uuid.uuid4())

def dt_past(days=0, hours=0):
    """Fecha en el pasado."""
    return datetime.now() - timedelta(days=days, hours=hours)

def bcrypt_hash(password):
    """Genera hash BCrypt compatible con Spring Security (fuerza 10)."""
    import subprocess, sys
    # Usa python-bcrypt si está disponible
    try:
        import bcrypt as bc
        hashed = bc.hashpw(password.encode(), bc.gensalt(rounds=10))
        return hashed.decode()
    except ImportError:
        # Hash fijo conocido para "123456" generado con bcrypt(10)
        return "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

PASSWORD_HASH = bcrypt_hash("12345678")
print(f"🔑 Password hash listo")

# ─── 1. LIMPIAR DATOS DE PRUEBA BASURA (optional) ──────────────────────────────
print("\n🗑  Limpiando productos de prueba con nombres absurdos...")
result = db.productos.delete_many({
    "nombre": {"$regex": "^(test|prueba|broma|asdf|qwerty|xxx|aaa|123|demo)", "$options": "i"}
})
print(f"   Eliminados: {result.deleted_count} productos basura")

# ─── 2. USUARIOS ───────────────────────────────────────────────────────────────
print("\n👥 Insertando usuarios...")

ciudades = [
    ("Barranquilla", "Atlántico"),
    ("Bogotá", "Cundinamarca"),
    ("Medellín", "Antioquia"),
    ("Cartagena", "Bolívar"),
    ("Cali", "Valle del Cauca"),
    ("Bucaramanga", "Santander"),
    ("Santa Marta", "Magdalena"),
    ("Pereira", "Risaralda"),
]

usuarios_data = [
    # vendedores
    {"nombre": "carlos_tech", "email": "carlos.garzon@gmail.com", "rol": "ROLE_VENDEDOR", "ciudad": "Barranquilla", "depto": "Atlántico"},
    {"nombre": "andrea_moviles", "email": "andrea.diaz@hotmail.com", "rol": "ROLE_VENDEDOR", "ciudad": "Medellín", "depto": "Antioquia"},
    {"nombre": "tienda_digital_col", "email": "tiendadigital@gmail.com", "rol": "ROLE_VENDEDOR", "ciudad": "Bogotá", "depto": "Cundinamarca"},
    {"nombre": "luis_phones", "email": "luis.herrera@outlook.com", "rol": "ROLE_VENDEDOR", "ciudad": "Cali", "depto": "Valle del Cauca"},
    {"nombre": "juan_celulares", "email": "juan.martinez@gmail.com", "rol": "ROLE_VENDEDOR", "ciudad": "Cartagena", "depto": "Bolívar"},
    # compradores
    {"nombre": "maria_compra", "email": "maria.lopez@gmail.com", "rol": "ROLE_USUARIO", "ciudad": "Bucaramanga", "depto": "Santander"},
    {"nombre": "pedro_usuario", "email": "pedro.reyes@yahoo.com", "rol": "ROLE_USUARIO", "ciudad": "Santa Marta", "depto": "Magdalena"},
    {"nombre": "sofia_buyer", "email": "sofia.vargas@gmail.com", "rol": "ROLE_USUARIO", "ciudad": "Pereira", "depto": "Risaralda"},
    {"nombre": "diego_movil", "email": "diego.castro@gmail.com", "rol": "ROLE_USUARIO", "ciudad": "Barranquilla", "depto": "Atlántico"},
    {"nombre": "laura_tech", "email": "laura.mendez@outlook.com", "rol": "ROLE_USUARIO", "ciudad": "Medellín", "depto": "Antioquia"},
    {"nombre": "miguel_comprador", "email": "miguel.sierra@gmail.com", "rol": "ROLE_USUARIO", "ciudad": "Bogotá", "depto": "Cundinamarca"},
    {"nombre": "valentina_c", "email": "valentina.cruz@gmail.com", "rol": "ROLE_USUARIO", "ciudad": "Cali", "depto": "Valle del Cauca"},
]

usuarios_ids = {}
usuarios_insertados = 0

for u in usuarios_data:
    existing = db.usuarios.find_one({"nombreUsuario": u["nombre"]})
    if existing:
        usuarios_ids[u["nombre"]] = existing["_id"]
        print(f"   ⚡ Usuario ya existe: {u['nombre']}")
        continue

    uid = new_id()
    doc = {
        "_id": uid,
        "nombreUsuario": u["nombre"],
        "contrasena": PASSWORD_HASH,
        "roles": [u["rol"]],
        "email": u["email"],
        "ciudad": u["ciudad"],
        "departamento": u["depto"],
        "telefono": f"3{random.randint(10,25)}{random.randint(1000000,9999999)}",
        "estadoCuenta": "activa",
        "fechaCreacion": dt_past(days=random.randint(30, 180)),
        "fechaNacimiento": datetime(random.randint(1985, 2000), random.randint(1, 12), random.randint(1, 28)),
        "edad": random.randint(22, 38),
        "imagenPerfil": None,
        "fondoPerfil": None,
    }
    db.usuarios.insert_one(doc)
    usuarios_ids[u["nombre"]] = uid
    usuarios_insertados += 1

print(f"   ✅ {usuarios_insertados} nuevos usuarios insertados")

# Mapas rápidos
vendedores = ["carlos_tech", "andrea_moviles", "tienda_digital_col", "luis_phones", "juan_celulares"]
compradores = ["maria_compra", "pedro_usuario", "sofia_buyer", "diego_movil", "laura_tech", "miguel_comprador", "valentina_c"]

def uid(nombre): return usuarios_ids[nombre]

# ─── 3. CATEGORÍAS ─────────────────────────────────────────────────────────────
print("\n📂 Insertando categorías...")

def make_espec(nombre, tipo, obligatorio=True, unidad="", opciones=None, placeholder=""):
    e = {
        "nombre": nombre,
        "tipo": tipo,
        "obligatorio": obligatorio,
        "unidad": unidad,
        "opciones": opciones or [],
        "placeholder": placeholder,
    }
    return e

categorias_data = [
    {
        "nombre": "Smartphones",
        "grupo": "Smartphones y Tablets",
        "icono": "📱",
        "especificaciones": [
            make_espec("RAM", "numero", True, "GB", placeholder="Ej: 8"),
            make_espec("Almacenamiento", "numero", True, "GB", placeholder="Ej: 128"),
            make_espec("Pantalla", "numero", True, "pulgadas", placeholder="Ej: 6.5"),
            make_espec("Cámara principal", "numero", True, "MP", placeholder="Ej: 64"),
            make_espec("Batería", "numero", True, "mAh", placeholder="Ej: 4500"),
            make_espec("Sistema operativo", "seleccion", True, opciones=["Android", "iOS", "HarmonyOS"]),
            make_espec("Conectividad", "seleccion", False, opciones=["4G", "5G", "4G LTE"]),
            make_espec("Color", "texto", False, placeholder="Ej: Negro Medianoche"),
        ]
    },
    {
        "nombre": "Tablets",
        "grupo": "Smartphones y Tablets",
        "icono": "💻",
        "especificaciones": [
            make_espec("Pantalla", "numero", True, "pulgadas"),
            make_espec("RAM", "numero", True, "GB"),
            make_espec("Almacenamiento", "numero", True, "GB"),
            make_espec("Sistema operativo", "seleccion", True, opciones=["Android", "iOS", "Windows"]),
            make_espec("Batería", "numero", True, "mAh"),
            make_espec("Tiene teclado", "checkbox", False),
        ]
    },
    {
        "nombre": "Accesorios",
        "grupo": "Accesorios y Periféricos",
        "icono": "🎧",
        "especificaciones": [
            make_espec("Tipo de accesorio", "seleccion", True, opciones=["Audífonos", "Cargador", "Funda", "Cargador inalámbrico", "Cable", "Batería externa", "Soporte", "Parlante"]),
            make_espec("Compatible con", "texto", False, placeholder="Ej: Samsung, iPhone, Universal"),
            make_espec("Color", "texto", False),
        ]
    },
    {
        "nombre": "Laptops",
        "grupo": "Computadores",
        "icono": "💻",
        "especificaciones": [
            make_espec("Procesador", "texto", True, placeholder="Ej: Intel Core i7 12va gen"),
            make_espec("RAM", "numero", True, "GB"),
            make_espec("Almacenamiento", "numero", True, "GB"),
            make_espec("Tipo de almacenamiento", "seleccion", True, opciones=["SSD", "HDD", "SSD + HDD"]),
            make_espec("Pantalla", "numero", True, "pulgadas"),
            make_espec("Tarjeta gráfica", "texto", False, placeholder="Ej: NVIDIA RTX 3060"),
            make_espec("Sistema operativo", "seleccion", True, opciones=["Windows 11", "Windows 10", "macOS", "Linux"]),
        ]
    },
    {
        "nombre": "Smartwatches",
        "grupo": "Wearables",
        "icono": "⌚",
        "especificaciones": [
            make_espec("Compatible con", "seleccion", True, opciones=["Android", "iOS", "Android e iOS"]),
            make_espec("Tamaño pantalla", "numero", True, "mm"),
            make_espec("Batería", "numero", True, "días de duración"),
            make_espec("GPS incluido", "checkbox", False),
            make_espec("Resistencia al agua", "texto", False, placeholder="Ej: 5ATM"),
        ]
    },
    {
        "nombre": "Cámaras",
        "grupo": "Fotografía",
        "icono": "📷",
        "especificaciones": [
            make_espec("Tipo", "seleccion", True, opciones=["DSLR", "Mirrorless", "Compacta", "Acción"]),
            make_espec("Resolución", "numero", True, "MP"),
            make_espec("Zoom óptico", "numero", False, "x"),
            make_espec("Incluye lente", "checkbox", False),
        ]
    },
    {
        "nombre": "Consolas y Gaming",
        "grupo": "Gaming",
        "icono": "🎮",
        "especificaciones": [
            make_espec("Consola", "seleccion", True, opciones=["PlayStation 5", "PlayStation 4", "Xbox Series X", "Xbox One", "Nintendo Switch", "PC Gaming"]),
            make_espec("Almacenamiento incluido", "numero", False, "GB"),
            make_espec("Incluye juegos", "texto", False, placeholder="Ej: FIFA 24, God of War"),
            make_espec("Estado del disco", "seleccion", False, opciones=["Excelente", "Bueno", "Regular"]),
        ]
    },
]

cat_ids = {}

for cat in categorias_data:
    existing = db.categorias.find_one({"nombre": cat["nombre"]})
    if existing:
        cat_ids[cat["nombre"]] = str(existing["_id"])
        print(f"   ⚡ Categoría ya existe: {cat['nombre']}")
        continue

    cid = new_id()
    doc = {
        "_id": cid,
        "nombre": cat["nombre"],
        "grupo": cat["grupo"],
        "icono": cat["icono"],
        "especificaciones": cat["especificaciones"],
        "activa": True,
        "fechaCreacion": dt_past(days=random.randint(60, 200)),
        "creadaPor": uid("tienda_digital_col"),
    }
    db.categorias.insert_one(doc)
    cat_ids[cat["nombre"]] = cid
    print(f"   ✅ Categoría insertada: {cat['nombre']}")

# ─── 4. PRODUCTOS ──────────────────────────────────────────────────────────────
print("\n📦 Insertando productos...")

# Imagen placeholder en base64 (pequeño rectángulo de color SVG encodado)
def svg_placeholder(color, label):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
  <rect width="400" height="400" fill="{color}"/>
  <text x="200" y="200" font-family="Arial" font-size="28" fill="white" text-anchor="middle" dy=".3em">{label}</text>
</svg>'''
    return "data:image/svg+xml;base64," + base64.b64encode(svg.encode()).decode()

colores_placeholder = ["#1a1a2e","#16213e","#0f3460","#533483","#2b2d42","#8d99ae","#e63946","#457b9d","#1d3557","#2d6a4f"]

productos_data = [
    # ── Smartphones ──
    {"nombre": "Samsung Galaxy S24 Ultra 256GB", "marca": "Samsung", "categoria": "Smartphones", "precio": 5200000, "estado": "nuevo", "stock": 3, "vendedor": "carlos_tech", "estadoVenta": "disponible", "descuento": None,
     "specs": {"RAM": "12", "Almacenamiento": "256", "Pantalla": "6.8", "Cámara principal": "200", "Batería": "5000", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Titanium Black"},
     "desc": "El buque insignia de Samsung con S-Pen integrado, cámara de 200MP y chip Snapdragon 8 Gen 3. Pantalla Dynamic AMOLED 2X con tasa de refresco de 120Hz."},
    
    {"nombre": "iPhone 15 Pro 128GB Titanio Natural", "marca": "Apple", "categoria": "Smartphones", "precio": 4800000, "estado": "nuevo", "stock": 2, "vendedor": "andrea_moviles", "estadoVenta": "disponible", "descuento": 5,
     "specs": {"RAM": "8", "Almacenamiento": "128", "Pantalla": "6.1", "Cámara principal": "48", "Batería": "3274", "Sistema operativo": "iOS", "Conectividad": "5G", "Color": "Titanio Natural"},
     "desc": "iPhone 15 Pro con chip A17 Pro de 3nm, cámara triple de 48MP, Dynamic Island y carga USB-C. El smartphone más potente de Apple en 2024."},

    {"nombre": "Xiaomi Redmi Note 13 Pro 256GB", "marca": "Xiaomi", "categoria": "Smartphones", "precio": 1350000, "estado": "nuevo", "stock": 5, "vendedor": "tienda_digital_col", "estadoVenta": "disponible", "descuento": 10,
     "specs": {"RAM": "8", "Almacenamiento": "256", "Pantalla": "6.67", "Cámara principal": "200", "Batería": "5100", "Sistema operativo": "Android", "Conectividad": "4G LTE", "Color": "Midnight Black"},
     "desc": "Redmi Note 13 Pro con cámara de 200MP OIS, pantalla AMOLED 120Hz y carga rápida de 67W. Relación precio-rendimiento imbatible."},

    {"nombre": "Samsung Galaxy A54 128GB Blanco", "marca": "Samsung", "categoria": "Smartphones", "precio": 1150000, "estado": "nuevo", "stock": 4, "vendedor": "luis_phones", "estadoVenta": "disponible", "descuento": None,
     "specs": {"RAM": "6", "Almacenamiento": "128", "Pantalla": "6.4", "Cámara principal": "50", "Batería": "5000", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Awesome White"},
     "desc": "Galaxy A54 5G con diseño premium, cámara de 50MP con OIS y batería de 5000mAh. Triple cámara trasera ideal para fotografía cotidiana."},

    {"nombre": "Motorola Edge 40 Pro 256GB", "marca": "Motorola", "categoria": "Smartphones", "precio": 2800000, "estado": "nuevo", "stock": 2, "vendedor": "juan_celulares", "estadoVenta": "disponible", "descuento": 15,
     "specs": {"RAM": "12", "Almacenamiento": "256", "Pantalla": "6.67", "Cámara principal": "50", "Batería": "4600", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Interstellar Black"},
     "desc": "Edge 40 Pro con Snapdragon 8 Gen 2, pantalla pOLED 165Hz curva y carga turbo de 125W. Completamente impermeable IP68."},

    {"nombre": "Samsung Galaxy S23 FE 128GB", "marca": "Samsung", "categoria": "Smartphones", "precio": 2100000, "estado": "usado", "stock": 1, "vendedor": "carlos_tech", "estadoVenta": "disponible", "descuento": None,
     "specs": {"RAM": "8", "Almacenamiento": "128", "Pantalla": "6.4", "Cámara principal": "50", "Batería": "4500", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Graphite"},
     "desc": "Galaxy S23 FE en excelente estado, con solo 4 meses de uso. Incluye cargador original, caja y audífonos. Sin rayones visibles."},

    {"nombre": "OnePlus 12 512GB Verde Esmeralda", "marca": "OnePlus", "categoria": "Smartphones", "precio": 4200000, "estado": "nuevo", "stock": 1, "vendedor": "andrea_moviles", "estadoVenta": "disponible", "descuento": None,
     "specs": {"RAM": "16", "Almacenamiento": "512", "Pantalla": "6.82", "Cámara principal": "50", "Batería": "5400", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Flowy Emerald"},
     "desc": "OnePlus 12 con Snapdragon 8 Gen 3, cámara Hasselblad calibrada, carga SUPERVOOC de 100W y pantalla LTPO4 2K+ 120Hz. Flagship asesino."},

    {"nombre": "Realme GT 5 Pro 256GB", "marca": "Realme", "categoria": "Smartphones", "precio": 2650000, "estado": "nuevo", "stock": 3, "vendedor": "tienda_digital_col", "estadoVenta": "disponible", "descuento": 8,
     "specs": {"RAM": "12", "Almacenamiento": "256", "Pantalla": "6.78", "Cámara principal": "50", "Batería": "5400", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Navigator Beige"},
     "desc": "Realme GT 5 Pro con teleobjetivo periscópico y cámara subacuática única. Snapdragon 8 Gen 3 y carga de 100W. Excelente alternativa premium."},

    # ── VENDIDOS (para historial de compras) ──
    {"nombre": "iPhone 14 128GB Azul", "marca": "Apple", "categoria": "Smartphones", "precio": 3600000, "estado": "nuevo", "stock": 1, "vendedor": "carlos_tech", "estadoVenta": "VENDIDO", "descuento": None,
     "specs": {"RAM": "6", "Almacenamiento": "128", "Pantalla": "6.1", "Cámara principal": "12", "Batería": "3279", "Sistema operativo": "iOS", "Conectividad": "5G", "Color": "Blue"},
     "desc": "iPhone 14 con chip A15 Bionic. Vendido a comprador verificado.", "compradorId": "maria_compra"},

    {"nombre": "Xiaomi 13T Pro 256GB Negro", "marca": "Xiaomi", "categoria": "Smartphones", "precio": 2900000, "estado": "nuevo", "stock": 1, "vendedor": "andrea_moviles", "estadoVenta": "VENDIDO", "descuento": None,
     "specs": {"RAM": "12", "Almacenamiento": "256", "Pantalla": "6.67", "Cámara principal": "50", "Batería": "5000", "Sistema operativo": "Android", "Conectividad": "5G", "Color": "Black"},
     "desc": "Xiaomi 13T Pro vendido.", "compradorId": "pedro_usuario"},

    # ── Accesorios ──
    {"nombre": "AirPods Pro 2da Generación", "marca": "Apple", "categoria": "Accesorios", "precio": 1200000, "estado": "nuevo", "stock": 5, "vendedor": "tienda_digital_col", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Tipo de accesorio": "Audífonos", "Compatible con": "iPhone, iPad, Mac", "Color": "Blanco"},
     "desc": "AirPods Pro 2 con cancelación activa de ruido H2, audio espacial personalizado y estuche de carga MagSafe hasta 30h de batería total."},

    {"nombre": "Cargador Samsung 45W Super Fast Charging", "marca": "Samsung", "categoria": "Accesorios", "precio": 145000, "estado": "nuevo", "stock": 8, "vendedor": "luis_phones", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Tipo de accesorio": "Cargador", "Compatible con": "Samsung Galaxy S/A/Note", "Color": "Negro"},
     "desc": "Cargador oficial Samsung 45W con tecnología Super Fast Charging 2.0. Compatible con Galaxy S21, S22, S23, S24 y más. Cable USB-C incluido."},

    {"nombre": "Funda MagSafe iPhone 15 Silicona", "marca": "Apple", "categoria": "Accesorios", "precio": 185000, "estado": "nuevo", "stock": 10, "vendedor": "andrea_moviles", "estadoVenta": "disponible", "descuento": 5,
     "specs": {"Tipo de accesorio": "Funda", "Compatible con": "iPhone 15, 15 Pro", "Color": "Midnight"},
     "desc": "Funda de silicona original Apple con MagSafe integrado. Material suave al tacto, botones precisos y protección completa contra caídas."},

    {"nombre": "Batería externa Anker 20000mAh", "marca": "Anker", "categoria": "Accesorios", "precio": 280000, "estado": "nuevo", "stock": 4, "vendedor": "juan_celulares", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Tipo de accesorio": "Batería externa", "Compatible con": "Universal", "Color": "Negro"},
     "desc": "PowerBank Anker 737 con 20000mAh, carga rápida de 65W y pantalla LED. Carga teléfonos y laptops. Ideal para viajeros y gamers móviles."},

    {"nombre": "Galaxy Watch 6 Classic 47mm Negro", "marca": "Samsung", "categoria": "Smartwatches", "precio": 1450000, "estado": "nuevo", "stock": 2, "vendedor": "carlos_tech", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Compatible con": "Android e iOS", "Tamaño pantalla": "47", "Batería": "3", "GPS incluido": "true", "Resistencia al agua": "5ATM"},
     "desc": "Galaxy Watch 6 Classic con bisel giratorio físico, monitoreo de salud avanzado, ECG, GPS integrado y batería de 425mAh con hasta 3 días de uso."},

    {"nombre": "Apple Watch Series 9 45mm Aluminio", "marca": "Apple", "categoria": "Smartwatches", "precio": 2100000, "estado": "nuevo", "stock": 2, "vendedor": "andrea_moviles", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Compatible con": "iOS", "Tamaño pantalla": "45", "Batería": "2", "GPS incluido": "true", "Resistencia al agua": "WR50"},
     "desc": "Apple Watch Series 9 con chip S9 SiP de doble núcleo, gesto Double Tap, pantalla Always-On y detección de choques. Carbon Neutral."},

    {"nombre": "Galaxy Tab S9 FE 256GB WiFi", "marca": "Samsung", "categoria": "Tablets", "precio": 1800000, "estado": "nuevo", "stock": 3, "vendedor": "tienda_digital_col", "estadoVenta": "disponible", "descuento": 12,
     "specs": {"Pantalla": "10.9", "RAM": "8", "Almacenamiento": "256", "Sistema operativo": "Android", "Batería": "8000", "Tiene teclado": "false"},
     "desc": "Galaxy Tab S9 FE con pantalla TFT de 10.9 pulgadas, S-Pen incluido, IP68 y batería de 8000mAh. Perfecta para estudio y entretenimiento."},

    {"nombre": "iPad Air 5ta Gen 256GB Space Gray", "marca": "Apple", "categoria": "Tablets", "precio": 2850000, "estado": "nuevo", "stock": 1, "vendedor": "luis_phones", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Pantalla": "10.9", "RAM": "8", "Almacenamiento": "256", "Sistema operativo": "iOS", "Batería": "28000", "Tiene teclado": "false"},
     "desc": "iPad Air con chip M1 de Apple, pantalla Liquid Retina, compatible con Apple Pencil 2 y Magic Keyboard. Conector USB-C con 5 Gb/s."},

    {"nombre": "PS5 Slim Consola + DualSense", "marca": "Sony", "categoria": "Consolas y Gaming", "precio": 3200000, "estado": "nuevo", "stock": 1, "vendedor": "juan_celulares", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Consola": "PlayStation 5", "Almacenamiento incluido": "1000", "Incluye juegos": "Astro's Playroom (digital)", "Estado del disco": "Excelente"},
     "desc": "PS5 Slim con unidad de disco, 1TB SSD, DualSense incluido y soporte vertical. La más reciente versión de la consola más vendida de Sony."},

    {"nombre": "Laptop Lenovo IdeaPad Gaming 3 RTX 3060", "marca": "Lenovo", "categoria": "Laptops", "precio": 4500000, "estado": "nuevo", "stock": 2, "vendedor": "carlos_tech", "estadoVenta": "disponible", "descuento": None,
     "specs": {"Procesador": "Intel Core i7 12va gen", "RAM": "16", "Almacenamiento": "512", "Tipo de almacenamiento": "SSD", "Pantalla": "15.6", "Tarjeta gráfica": "NVIDIA RTX 3060 6GB", "Sistema operativo": "Windows 11"},
     "desc": "Laptop gamer con i7-12700H, RTX 3060, 16GB DDR5, SSD NVMe 512GB y pantalla IPS 165Hz. Ideal para gaming competitivo y diseño 3D."},
]

producto_ids = {}

for i, p in enumerate(productos_data):
    existing = db.productos.find_one({"nombre": p["nombre"]})
    if existing:
        producto_ids[p["nombre"]] = str(existing["_id"])
        print(f"   ⚡ Producto ya existe: {p['nombre'][:50]}")
        continue

    pid = new_id()
    color = colores_placeholder[i % len(colores_placeholder)]
    label_svg = p["marca"]

    vendedor_nombre = p["vendedor"]
    comprador_id = None
    if "compradorId" in p:
        comprador_nombre = p["compradorId"]
        comprador_id = uid(comprador_nombre) if comprador_nombre in usuarios_ids else None

    doc = {
        "_id": pid,
        "nombre": p["nombre"],
        "categoria": p["categoria"],
        "precio": p["precio"],
        "marca": p["marca"],
        "descripcion": p["desc"],
        "vendedorId": uid(vendedor_nombre),
        "fechaPublicacion": dt_past(days=random.randint(1, 60)),
        "estadoVenta": p["estadoVenta"],
        "imagenBase64": svg_placeholder(color, label_svg),
        "estado": p["estado"],
        "especificaciones": p["specs"],
        "stock": p["stock"],
        "descuento": p["descuento"],
        "compradorId": comprador_id,
    }
    db.productos.insert_one(doc)
    producto_ids[p["nombre"]] = pid
    print(f"   ✅ Producto insertado: {p['nombre'][:50]}")

# ─── 5. SOLICITUDES DE COMPRA ──────────────────────────────────────────────────
print("\n🛒 Insertando solicitudes de compra...")

solicitudes_data = [
    {
        "producto": "iPhone 15 Pro 128GB Titanio Natural",
        "comprador": "sofia_buyer",
        "vendedor": "andrea_moviles",
        "estado": "aprobada",
        "estadoSeguimiento": "ENTREGADO",
        "direccion": "Cra 45 #23-10, Pereira",
        "dias_atras": 25,
    },
    {
        "producto": "Samsung Galaxy S24 Ultra 256GB",
        "comprador": "diego_movil",
        "vendedor": "carlos_tech",
        "estado": "aprobada",
        "estadoSeguimiento": "ENVIADO",
        "direccion": "Cll 72 #45-20, Barranquilla",
        "dias_atras": 10,
    },
    {
        "producto": "Xiaomi Redmi Note 13 Pro 256GB",
        "comprador": "laura_tech",
        "vendedor": "tienda_digital_col",
        "estado": "aprobada",
        "estadoSeguimiento": "PREPARANDO",
        "direccion": "Cra 80 #10-15, Medellín",
        "dias_atras": 5,
    },
    {
        "producto": "AirPods Pro 2da Generación",
        "comprador": "miguel_comprador",
        "vendedor": "tienda_digital_col",
        "estado": "aprobada",
        "estadoSeguimiento": "PENDIENTE",
        "direccion": "Cll 100 #55-30, Bogotá",
        "dias_atras": 2,
    },
    {
        "producto": "Motorola Edge 40 Pro 256GB",
        "comprador": "valentina_c",
        "vendedor": "juan_celulares",
        "estado": "aprobada",
        "estadoSeguimiento": "ENVIADO",
        "direccion": "Cra 5 #10-22, Cali",
        "dias_atras": 12,
    },
    {
        "producto": "Galaxy Tab S9 FE 256GB WiFi",
        "comprador": "maria_compra",
        "vendedor": "tienda_digital_col",
        "estado": "pendiente",
        "estadoSeguimiento": None,
        "direccion": "Cll 35 #20-45, Bucaramanga",
        "dias_atras": 1,
    },
    {
        "producto": "Galaxy Watch 6 Classic 47mm Negro",
        "comprador": "pedro_usuario",
        "vendedor": "carlos_tech",
        "estado": "aprobada",
        "estadoSeguimiento": "ENTREGADO",
        "direccion": "Cra 3 #12-50, Santa Marta",
        "dias_atras": 30,
    },
    {
        "producto": "Laptop Lenovo IdeaPad Gaming 3 RTX 3060",
        "comprador": "sofia_buyer",
        "vendedor": "carlos_tech",
        "estado": "aprobada",
        "estadoSeguimiento": "PREPARANDO",
        "direccion": "Cra 45 #23-10, Pereira",
        "dias_atras": 3,
    },
]

solicitud_ids = {}

for s in solicitudes_data:
    key = f"{s['producto']}__{s['comprador']}"
    existing = db.solicitudes.find_one({"nombreProducto": s["producto"], "nombreComprador": s["comprador"]})
    if existing:
        solicitud_ids[key] = str(existing["_id"])
        print(f"   ⚡ Solicitud ya existe: {s['producto'][:40]}")
        continue

    sid = new_id()
    prod_id = producto_ids.get(s["producto"])
    fecha = dt_past(days=s["dias_atras"])

    doc = {
        "_id": sid,
        "tipoSolicitud": "compra",
        "usuarioId": uid(s["comprador"]),
        "estado": s["estado"],
        "fechaSolicitud": fecha,
        "productoId": prod_id,
        "nombreProducto": s["producto"],
        "nombreComprador": s["comprador"],
        "vendedorId": uid(s["vendedor"]),
        "direccionComprador": s["direccion"],
        "correoComprador": next((u["email"] for u in usuarios_data if u["nombre"] == s["comprador"]), ""),
        "numeroContactoComprador": f"3{random.randint(10,25)}{random.randint(1000000,9999999)}",
        "descripcionVendedor": "",
        "estadoSeguimiento": s["estadoSeguimiento"],
        "fechaRespuesta": fecha + timedelta(hours=random.randint(1, 8)) if s["estado"] == "aprobada" else None,
    }
    db.solicitudes.insert_one(doc)
    solicitud_ids[key] = sid
    print(f"   ✅ Solicitud insertada: {s['producto'][:40]} → {s['estadoSeguimiento']}")

# ─── 6. ENVÍOS ─────────────────────────────────────────────────────────────────
print("\n🚚 Insertando envíos...")

envios_data = [
    {"solicitud_key": "iPhone 15 Pro 128GB Titanio Natural__sofia_buyer", "vendedor": "andrea_moviles", "comprador": "sofia_buyer", "estado": "entregado", "dias_pasados": 20},
    {"solicitud_key": "Samsung Galaxy S24 Ultra 256GB__diego_movil", "vendedor": "carlos_tech", "comprador": "diego_movil", "estado": "enviado", "dias_pasados": 5},
    {"solicitud_key": "Xiaomi Redmi Note 13 Pro 256GB__laura_tech", "vendedor": "tienda_digital_col", "comprador": "laura_tech", "estado": "preparando", "dias_pasados": 2},
    {"solicitud_key": "Motorola Edge 40 Pro 256GB__valentina_c", "vendedor": "juan_celulares", "comprador": "valentina_c", "estado": "en camino", "dias_pasados": 7},
    {"solicitud_key": "Galaxy Watch 6 Classic 47mm Negro__pedro_usuario", "vendedor": "carlos_tech", "comprador": "pedro_usuario", "estado": "entregado", "dias_pasados": 25},
    {"solicitud_key": "Laptop Lenovo IdeaPad Gaming 3 RTX 3060__sofia_buyer", "vendedor": "carlos_tech", "comprador": "sofia_buyer", "estado": "preparando", "dias_pasados": 1},
]

for e in envios_data:
    sol_id = solicitud_ids.get(e["solicitud_key"])
    if not sol_id:
        print(f"   ⚠ Solicitud no encontrada para envío: {e['solicitud_key'][:50]}")
        continue
    existing = db.envios.find_one({"solicitudId": sol_id})
    if existing:
        print(f"   ⚡ Envío ya existe para solicitud: {sol_id[:20]}")
        continue

    codigo = "CEL" + "".join([str(random.randint(0,9)) for _ in range(9)])
    fecha_act = dt_past(days=e["dias_pasados"])
    fecha_estimada = (datetime.now() + timedelta(days=random.randint(1, 7))).date() if e["estado"] not in ["entregado"] else (datetime.now() - timedelta(days=random.randint(1, 5))).date()

    eid = new_id()
    doc = {
        "_id": eid,
        "solicitudId": sol_id,
        "vendedorId": uid(e["vendedor"]),
        "compradorId": uid(e["comprador"]),
        "codigoSeguimiento": codigo,
        "estado": e["estado"],
        "fechaEstimada": datetime.combine(fecha_estimada, datetime.min.time()),
        "fechaActualizacion": fecha_act,
    }
    db.envios.insert_one(doc)
    print(f"   ✅ Envío {codigo} - Estado: {e['estado']}")

# ─── 7. CHATS PEDIDOS (conversaciones post-venta) ─────────────────────────────
print("\n💬 Insertando chats de pedidos...")

chats_data = [
    {
        "solicitud_key": "Samsung Galaxy S24 Ultra 256GB__diego_movil",
        "comprador": "diego_movil",
        "vendedor": "carlos_tech",
        "producto": "Samsung Galaxy S24 Ultra 256GB",
        "mensajes": [
            ("diego_movil", "Hola, acabo de hacer el pedido del Galaxy S24 Ultra. ¿Cuánto tiempo tarda el envío?"),
            ("carlos_tech", "¡Hola Diego! Muchas gracias por tu compra 😊 Lo tenemos en stock, lo enviamos mañana en la mañana. Debería llegarte en 2-3 días hábiles."),
            ("diego_movil", "Perfecto, ¿viene con caja sellada y factura?"),
            ("carlos_tech", "Sí, completamente sellado, con accesorios originales Samsung y factura electrónica a tu nombre. ¿Cuál sería el NIT o cédula para la factura?"),
            ("diego_movil", "Cédula 1.045.678.320. Muchas gracias!"),
            ("carlos_tech", "Listo, quedo anotado. Ya preparé el paquete, mañana a las 9am lo entrego a Servientrega. Te mando el número de guía apenas lo tenga 📦"),
        ]
    },
    {
        "solicitud_key": "iPhone 15 Pro 128GB Titanio Natural__sofia_buyer",
        "comprador": "sofia_buyer",
        "vendedor": "andrea_moviles",
        "producto": "iPhone 15 Pro 128GB Titanio Natural",
        "mensajes": [
            ("sofia_buyer", "Buenas tardes, quería preguntar si el iPhone que compraron tiene garantía de Apple Colombia."),
            ("andrea_moviles", "¡Buenas tardes Sofía! Sí claro, tiene garantía oficial Apple Colombia de 1 año. La caja tiene el sticker de garantía activable en apple.com/co ✅"),
            ("sofia_buyer", "Genial. ¿Puedo elegir el color del cable USB-C que viene?"),
            ("andrea_moviles", "El cable que viene es el estándar de Apple en blanco. Si quieres uno de color diferente te lo puedo agregar al paquete por un pequeño adicional 😊"),
            ("sofia_buyer", "No, así está bien. ¿Ya fue enviado?"),
            ("andrea_moviles", "Sí! Ya fue entregado a la transportadora hace 2 días. Tu número de guía es TCC-8827401. Ya debería llegar hoy o mañana según la dirección en Pereira 🚀"),
            ("sofia_buyer", "Excelente, muchas gracias por todo! Super buena atención."),
            ("andrea_moviles", "Gracias a ti Sofía! Cualquier cosa estamos a la orden. ¡Disfruta tu nuevo iPhone! 🍎"),
        ]
    },
    {
        "solicitud_key": "Xiaomi Redmi Note 13 Pro 256GB__laura_tech",
        "comprador": "laura_tech",
        "vendedor": "tienda_digital_col",
        "producto": "Xiaomi Redmi Note 13 Pro 256GB",
        "mensajes": [
            ("laura_tech", "Hola buenas! Hice el pedido del Redmi Note 13 Pro. ¿Ya está disponible para envío?"),
            ("tienda_digital_col", "Hola Laura! Sí, lo tenemos disponible. Estamos preparando el paquete ahora mismo. ¿La dirección de Medellín que ingresaste sigue igual?"),
            ("laura_tech", "Sí, la misma. Cra 80 #10-15, Laureles."),
            ("tienda_digital_col", "Perfecto, anotado. El Redmi viene con cargador de 67W incluido en la caja. ¿Quieres que le agreguemos funda como se ofertaba?"),
            ("laura_tech", "Sí! La funda por favor, vi que venía de regalo en la promo."),
            ("tienda_digital_col", "Correcto, ya la incluimos sin costo adicional. Enviamos mañana y te compartimos la guía 😊"),
        ]
    },
    {
        "solicitud_key": "Motorola Edge 40 Pro 256GB__valentina_c",
        "comprador": "valentina_c",
        "vendedor": "juan_celulares",
        "producto": "Motorola Edge 40 Pro 256GB",
        "mensajes": [
            ("valentina_c", "Hola! Compré el Motorola Edge 40 Pro y quería saber si viene desbloqueado para cualquier operador."),
            ("juan_celulares", "Hola Valentina! Sí, completamente desbloqueado de fábrica para cualquier operador en Colombia y el exterior 🌐"),
            ("valentina_c", "Perfecto. ¿Tiene NFC? Quiero usarlo para pagos."),
            ("juan_celulares", "Sí! El Edge 40 Pro tiene NFC integrado. Compatible con Google Pay y otros sistemas de pago sin contacto ✅"),
            ("valentina_c", "Genial. ¿En cuántos días llega a Cali?"),
            ("juan_celulares", "Por lo general 2 días hábiles a Cali por Coordinadora. Ya está en camino, el número de guía lo enviamos al correo registrado 📧"),
            ("valentina_c", "Muchas gracias! Muy buena atención."),
        ]
    },
    {
        "solicitud_key": "Galaxy Tab S9 FE 256GB WiFi__maria_compra",
        "comprador": "maria_compra",
        "vendedor": "tienda_digital_col",
        "producto": "Galaxy Tab S9 FE 256GB WiFi",
        "mensajes": [
            ("maria_compra", "Buenas, estoy interesada en la tablet Galaxy Tab S9 FE. ¿Está disponible de inmediato?"),
            ("tienda_digital_col", "¡Hola María! Sí, tenemos disponibilidad inmediata. ¿Ya realizaste el pedido oficial?"),
            ("maria_compra", "Sí, acabo de hacerlo. ¿Incluye el S-Pen?"),
            ("tienda_digital_col", "Incluye el S-Pen sí. Samsung lo incluye en la caja como accesorio estándar para el Tab S9 FE 👍"),
            ("maria_compra", "Perfecto. ¿Y la funda? Vi que en algunas fotos salía con funda."),
            ("tienda_digital_col", "La funda no viene incluida de fábrica, pero te la podemos agregar al pedido. Tenemos funda Book Cover original de Samsung en $85.000 adicionales, ¿te interesa?"),
            ("maria_compra", "Sí, agreguen la funda por favor."),
        ]
    },
]

for chat in chats_data:
    sol_id = solicitud_ids.get(chat["solicitud_key"])
    if not sol_id:
        print(f"   ⚠ Solicitud no encontrada para chat: {chat['solicitud_key'][:50]}")
        continue
    existing = db.chats_pedidos.find_one({"solicitudId": sol_id})
    if existing:
        print(f"   ⚡ Chat ya existe para solicitud: {sol_id[:20]}")
        continue

    prod_id = producto_ids.get(chat["producto"])
    chat_id = new_id()
    
    mensajes_docs = []
    base_time = dt_past(days=random.randint(5, 20))
    for idx, (remitente_nombre, contenido) in enumerate(chat["mensajes"]):
        mensajes_docs.append({
            "remitenteId": uid(remitente_nombre),
            "contenido": contenido,
            "fecha": base_time + timedelta(minutes=idx * random.randint(5, 60)),
        })

    doc = {
        "_id": chat_id,
        "solicitudId": sol_id,
        "compradorId": uid(chat["comprador"]),
        "vendedorId": uid(chat["vendedor"]),
        "productoId": prod_id,
        "mensajes": mensajes_docs,
    }
    db.chats_pedidos.insert_one(doc)
    print(f"   ✅ Chat insertado: {chat['producto'][:40]} ({len(mensajes_docs)} mensajes)")

# ─── 8. MENSAJES GENERALES (solicitudes de vendedor) ──────────────────────────
print("\n📩 Insertando mensajes generales...")

mensajes_generales = [
    {
        "solicitud": "iPhone 15 Pro 128GB Titanio Natural__sofia_buyer",
        "pares": [
            ("sofia_buyer", "andrea_moviles", "Hola! ¿El iPhone tiene garantía extendida disponible?"),
            ("andrea_moviles", "sofia_buyer", "Hola Sofía! Sí, puedes adquirir AppleCare+ aparte. También ofrecemos garantía propia de 6 meses adicionales."),
        ]
    },
    {
        "solicitud": "Samsung Galaxy S24 Ultra 256GB__diego_movil",
        "pares": [
            ("diego_movil", "carlos_tech", "Carlos, ¿el S24 Ultra soporta carga inalámbrica inversa?"),
            ("carlos_tech", "diego_movil", "Sí Diego! Tiene carga inalámbrica de 15W y también carga inalámbrica inversa de 4.5W para recargar auriculares como los Galaxy Buds."),
            ("diego_movil", "carlos_tech", "Perfecto! Eso era lo que necesitaba saber. Gracias!"),
        ]
    },
]

for mg in mensajes_generales:
    sol_id = solicitud_ids.get(mg["solicitud"])
    if not sol_id:
        continue
    base_time = dt_past(days=random.randint(3, 15))
    for idx, (emisor_n, receptor_n, contenido) in enumerate(mg["pares"]):
        existing = db.mensajes.find_one({
            "solicitudId": sol_id,
            "emisorId": uid(emisor_n),
            "contenido": contenido,
        })
        if existing:
            continue
        doc = {
            "_id": new_id(),
            "solicitudId": sol_id,
            "emisorId": uid(emisor_n),
            "receptorId": uid(receptor_n),
            "contenido": contenido,
            "fecha": base_time + timedelta(minutes=idx * 15),
            "leido": idx % 2 == 0,
        }
        db.mensajes.insert_one(doc)
    print(f"   ✅ Mensajes insertados para solicitud: {mg['solicitud'][:40]}")

# ─── 9. NOTIFICACIONES ────────────────────────────────────────────────────────
print("\n🔔 Insertando notificaciones...")

notificaciones = [
    ("carlos_tech", "🎉 ¡Nueva venta! Diego Movil adquirió el Samsung Galaxy S24 Ultra por $5.200.000", True),
    ("carlos_tech", "🎉 ¡Nueva venta! Sofía Buyer adquirió el Laptop Lenovo IdeaPad Gaming 3", True),
    ("carlos_tech", "📦 El envío CEL882740 fue marcado como entregado correctamente", True),
    ("andrea_moviles", "🎉 ¡Nueva venta! Sofía Buyer adquirió el iPhone 15 Pro por $4.560.000", True),
    ("andrea_moviles", "⭐ Tienes una nueva reseña en iPhone 15 Pro 128GB - 5 estrellas", False),
    ("tienda_digital_col", "🛒 María Compra realizó un pedido del Galaxy Tab S9 FE", False),
    ("tienda_digital_col", "🎉 ¡Nueva venta! Laura Tech adquirió el Redmi Note 13 Pro", True),
    ("diego_movil", "✅ Tu pedido del Samsung Galaxy S24 Ultra fue aprobado y está siendo preparado", True),
    ("diego_movil", "🚚 Tu pedido fue enviado. Código de seguimiento: CEL991827364", True),
    ("sofia_buyer", "✅ Tu pedido del iPhone 15 Pro fue entregado exitosamente", True),
    ("sofia_buyer", "✅ Tu pedido del Laptop Lenovo fue aprobado. En preparación.", True),
    ("laura_tech", "🚀 Tu Redmi Note 13 Pro está siendo preparado para envío", False),
    ("valentina_c", "🚀 Tu Motorola Edge 40 Pro ya fue enviado. Llega en 2 días.", True),
    ("pedro_usuario", "✅ Tu pedido del Galaxy Watch 6 fue entregado exitosamente", True),
    ("maria_compra", "🛒 Tu pedido del Galaxy Tab S9 FE está pendiente de aprobación", False),
]

for n in notificaciones:
    usuario_nombre, mensaje, leida = n
    user_id = uid(usuario_nombre)
    existing = db.notificaciones.find_one({"usuarioId": user_id, "mensaje": mensaje})
    if existing:
        continue
    doc = {
        "_id": new_id(),
        "usuarioId": user_id,
        "mensaje": mensaje,
        "fecha": dt_past(days=random.randint(1, 20), hours=random.randint(0, 12)),
        "leida": leida,
        "enlace": "/notificaciones",
    }
    db.notificaciones.insert_one(doc)

print(f"   ✅ Notificaciones insertadas")

# ─── 10. RESEÑAS ──────────────────────────────────────────────────────────────
print("\n⭐ Insertando reseñas...")

reseñas_data = [
    {
        "producto": "iPhone 15 Pro 128GB Titanio Natural",
        "usuario": "sofia_buyer",
        "titulo": "Increíble cámara y rendimiento excepcional",
        "comentario": "Llevo 3 semanas usando el iPhone 15 Pro y estoy completamente enamorada. La cámara de 48MP es simplemente espectacular, las fotos en modo retrato son profesionales. El chip A17 Pro se nota en cada acción. La única pega es la batería que dura menos de lo que esperaba, pero Apple incluye la carga rápida USB-C que ayuda mucho. 100% recomendado.",
        "puntuacion": 5,
        "compra_verificada": True,
        "dias": 15,
    },
    {
        "producto": "Samsung Galaxy S24 Ultra 256GB",
        "usuario": "diego_movil",
        "titulo": "El mejor Android que he tenido, el S-Pen marca la diferencia",
        "comentario": "Venía de un S22+ y el salto es enorme. La pantalla Dynamic AMOLED es espectacular, el S-Pen me sirve muchísimo para estudios y trabajo. La cámara de 200MP capta detalles que antes eran imposibles. Batería excelente, llega al final del día sin problema. Cinco estrellas sin duda.",
        "puntuacion": 5,
        "compra_verificada": True,
        "dias": 8,
    },
    {
        "producto": "Xiaomi Redmi Note 13 Pro 256GB",
        "usuario": "laura_tech",
        "titulo": "Excelente relación precio-calidad",
        "comentario": "Por menos de $1.400.000 tienes una cámara de 200MP, pantalla AMOLED y carga de 67W que en 40 min llena la batería. El rendimiento en apps del día a día es fluido. Para gaming liviano también rinde bien. Pequeño detalle: la interfaz MIUI tiene bastante bloatware pero se puede limpiar fácilmente.",
        "puntuacion": 4,
        "compra_verificada": True,
        "dias": 3,
    },
    {
        "producto": "Galaxy Watch 6 Classic 47mm Negro",
        "usuario": "pedro_usuario",
        "titulo": "El mejor smartwatch Android del mercado",
        "comentario": "Llevo un mes con el Galaxy Watch 6 Classic y es una maravilla. El bisel giratorio físico es una delicia de usar. El monitoreo de salud es muy preciso, el ECG funciona perfectamente. La batería dura 2-3 días con uso intensivo. Perfecto para quien busca un reloj que además sea una herramienta de salud.",
        "puntuacion": 5,
        "compra_verificada": True,
        "dias": 20,
    },
    {
        "producto": "Motorola Edge 40 Pro 256GB",
        "usuario": "valentina_c",
        "titulo": "Carga rapidísima y diseño elegante",
        "comentario": "El Edge 40 Pro sorprende por su pantalla curva y la carga de 125W que lo llena en menos de 20 minutos. La cámara es buena sin ser la mejor del mercado. El Snapdragon 8 Gen 2 lo hace fluido en todo. Un poco resbaladizo por el diseño curvo pero nada que no solucione una funda. Muy buen equipo.",
        "puntuacion": 4,
        "compra_verificada": True,
        "dias": 10,
    },
    {
        "producto": "AirPods Pro 2da Generación",
        "usuario": "miguel_comprador",
        "titulo": "La cancelación de ruido es simplemente mágica",
        "comentario": "Trabajo en un open office ruidoso y los AirPods Pro 2 cambiaron mi vida laboral. La cancelación de ruido es de otro nivel, literalmente no escucho nada del entorno. El audio espacial con Atmos es inmersivo. Se conectan instantáneamente al iPhone. El único contra es el precio, pero valen cada peso.",
        "puntuacion": 5,
        "compra_verificada": False,
        "dias": 5,
    },
]

for r in reseñas_data:
    prod_id = producto_ids.get(r["producto"])
    if not prod_id:
        continue
    user_id = uid(r["usuario"])
    existing = db.reseñas.find_one({"productoId": prod_id, "usuarioId": user_id})
    if existing:
        print(f"   ⚡ Reseña ya existe: {r['titulo'][:40]}")
        continue

    doc = {
        "_id": new_id(),
        "productoId": prod_id,
        "usuarioId": user_id,
        "nombreUsuario": r["usuario"],
        "titulo": r["titulo"],
        "comentario": r["comentario"],
        "puntuacion": r["puntuacion"],
        "compraVerificada": r["compra_verificada"],
        "fotosBase64": [],
        "votosUtiles": random.randint(2, 45),
        "votosInutiles": random.randint(0, 5),
        "fecha": dt_past(days=r["dias"]),
    }
    db.reseñas.insert_one(doc)
    print(f"   ✅ Reseña insertada: {r['titulo'][:40]}")

# ─── RESUMEN FINAL ─────────────────────────────────────────────────────────────
print("\n" + "="*60)
print("📊 RESUMEN FINAL DE LA BASE DE DATOS")
print("="*60)

colecciones = ["usuarios", "categorias", "productos", "solicitudes", "envios", "chats_pedidos", "mensajes", "notificaciones", "reseñas"]
for col in colecciones:
    count = db[col].count_documents({})
    print(f"   {col:20s}: {count:>5} documentos")

print("="*60)
print("✅ ¡Base de datos poblada exitosamente!")
print("\nCredenciales de acceso para pruebas:")
print("  Vendedor:  carlos_tech     / 12345678")
print("  Vendedor:  andrea_moviles  / 12345678")
print("  Comprador: diego_movil     / 12345678")
print("  Comprador: sofia_buyer     / 12345678")
print("  Comprador: maria_compra    / 12345678")

client.close()
