from pymongo import MongoClient
client = MongoClient("mongodb://localhost:27017/")
db = client["celfinder"]
usuarios = list(db.usuarios.find({}, {"_id":1,"nombreUsuario":1,"roles":1,"email":1,"ciudad":1,"estadoCuenta":1}))
print(f"Total usuarios: {len(usuarios)}")
for u in usuarios:
    uid = str(u["_id"])[:24]
    nombre = u.get("nombreUsuario","?")
    roles = u.get("roles",[])
    ciudad = u.get("ciudad","?")
    estado = u.get("estadoCuenta","?")
    print(f"  {uid} | {nombre} | {roles} | {ciudad} | {estado}")
# También contar productos existentes
prods = db.productos.count_documents({})
print(f"\nProductos existentes: {prods}")
cats = db.categorias.count_documents({})
print(f"Categorias existentes: {cats}")
client.close()
