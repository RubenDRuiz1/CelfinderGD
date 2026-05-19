import pulp
import sys
import os
os.environ['MPLBACKEND'] = 'Agg'
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
import io
import base64
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# --- 1. PARÁMETROS DEL COMBO Y USUARIO ---
try:
    presupuesto_usuario = float(sys.argv[1]) if len(sys.argv) > 1 else 1100000.0
    json_b64 = sys.argv[2] if len(sys.argv) > 2 else ""
    if json_b64:
        payload = json.loads(base64.b64decode(json_b64).decode('utf-8'))
        categorias_data = payload.get("categorias", [])
        tracking_data = payload.get("trackingData", {"totalVisualizaciones": 0, "totalCarritos": 0})
    else:
        categorias_data = [
            {"nombre": "Smartphones", "interes": 0.5, "precio": 1200000.0, "costo": 800000.0},
            {"nombre": "Wearables", "interes": 0.9, "precio": 250000.0, "costo": 80000.0}
        ]
        tracking_data = {"totalVisualizaciones": 0, "totalCarritos": 0}
except Exception as e:
    print(f"<div>Error parseando argumentos: {e}</div>")
    sys.exit(1)

N = len(categorias_data)
if N < 2:
    print(f"<div style='padding:40px;text-align:center;font-family:sans-serif;background:#0f172a;color:#f8fafc;'><h2 style='color:#f87171;'>Se necesitan al menos 2 categorías</h2><p style='color:#94a3b8;'>Categorías recibidas: {N}</p><a href='/admin/sesiones-activas' style='color:#38bdf8;'>Volver a Sesiones</a></div>")
    sys.exit(0)

# Escalar a miles para estabilidad numérica
ESCALA = 1000.0
presupuesto_escalado = presupuesto_usuario / ESCALA
for c in categorias_data:
    c["precio"] = c["precio"] / ESCALA
    c["costo"] = c["costo"] / ESCALA

descuento_max_total = 400.0
alpha = 0.4

# --- 2. MODELO SIMPLEX ---
model = pulp.LpProblem("Optimizacion_Combo_N_TechMatch", pulp.LpMaximize)

variables = []
ganancia_comercial = 0
ahorro_ponderado = 0
precio_total_lista = 0

for i, cat in enumerate(categorias_data):
    P = cat["precio"]
    C = cat["costo"]
    interes = cat["interes"]

    low_b = min(0.10 * P, P - C) if P > C else 0
    up_b = P - C if P > C else 0

    d_var = pulp.LpVariable(f'Desc_{i}', lowBound=low_b, upBound=up_b, cat='Continuous')
    variables.append(d_var)

    ganancia_comercial += (P - d_var - C)
    ahorro_ponderado += (interes * d_var)
    precio_total_lista += P

model += (alpha * ganancia_comercial) + ((1 - alpha) * ahorro_ponderado), "Maximizar_Z"
model += pulp.lpSum(variables) >= precio_total_lista - presupuesto_escalado, "Limite_Presupuesto_Usuario"
model += pulp.lpSum(variables) <= descuento_max_total, "Descuento_Maximo_Total"

model.solve(pulp.PULP_CBC_CMD(msg=False))

z_optimo = round(pulp.value(model.objective) if pulp.value(model.objective) else 0, 2)
ahorro_total = sum(v.varValue for v in variables)
ganancia_tienda = sum((c["precio"] - variables[i].varValue - c["costo"]) for i, c in enumerate(categorias_data))
precio_combo_final = precio_total_lista - ahorro_total

# --- 3. GRÁFICO ADAPTATIVO ---
fig, ax = plt.subplots(figsize=(9, 7), facecolor='#1e293b')
ax.set_facecolor('#0f172a')

if N == 2:
    P1, P2 = categorias_data[0]["precio"], categorias_data[1]["precio"]
    C1, C2 = categorias_data[0]["costo"], categorias_data[1]["costo"]
    n1, n2 = categorias_data[0]["nombre"], categorias_data[1]["nombre"]
    v1, v2 = variables[0].varValue, variables[1].varValue

    d1_vals = np.linspace(0, max(precio_total_lista, descuento_max_total), 500)
    y_budget = (P1 + P2 - presupuesto_escalado) - d1_vals
    y_max_desc = descuento_max_total - d1_vals

    d1_min, d1_max = min(0.10 * P1, P1-C1), P1 - C1
    d2_min, d2_max = min(0.10 * P2, P2-C2), P2 - C2

    ax.plot(d1_vals, y_budget, color='#a855f7', linestyle='-', linewidth=2, label='Restricción Presupuesto')
    ax.plot(d1_vals, y_max_desc, color='#64748b', linestyle='-', linewidth=1.5, label='Max Descuento Global')

    y_lower = np.maximum(d2_min, y_budget)
    y_upper = np.minimum(d2_max, y_max_desc)
    valid_idx = (d1_vals >= d1_min) & (d1_vals <= d1_max) & (y_upper >= y_lower)
    ax.fill_between(d1_vals[valid_idx], y_lower[valid_idx], y_upper[valid_idx], color='#10b981', alpha=0.15, label='Región Factible')

    ax.plot(v1, v2, marker='*', color='#ec4899', markersize=18, label=f'Óptimo (Z={z_optimo})')

    ax.set_title(f'Plano Factible Simplex: {n1} vs {n2}', color='white')
    ax.set_xlabel(f'Descuento {n1}', color='#94a3b8')
    ax.set_ylabel(f'Descuento {n2}', color='#94a3b8')
    ax.set_xlim(0, max(v1 * 1.5, d1_max * 1.2))
    ax.set_ylim(0, max(v2 * 1.5, d2_max * 1.2))
else:
    nombres = [c["nombre"] for c in categorias_data]
    descuentos = [v.varValue for v in variables]

    bars = ax.bar(nombres, descuentos, color='#3b82f6', edgecolor='white', linewidth=1.5)

    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2, yval + 5, f"{yval:,.1f}k", ha='center', va='bottom', color='white', fontweight='bold')

    ax.set_title(f'Distribución Óptima de Descuentos (Combo de {N} productos)', color='white', pad=20)
    ax.set_ylabel('Valor de Descuento (Miles COP)', color='#94a3b8')
    ax.set_ylim(0, max(descuentos) * 1.3)

ax.tick_params(colors='#94a3b8')
for spine in ax.spines.values(): spine.set_color('#475569')
legend = ax.legend(loc='best', facecolor='#1e293b', edgecolor='#475569')
if legend:
    for text in legend.get_texts(): text.set_color('white')
ax.grid(True, linestyle=':', alpha=0.3, color='#64748b')

img_buffer = io.BytesIO()
plt.savefig(img_buffer, format='png', bbox_inches='tight', dpi=100)
img_buffer.seek(0)
img_base64 = base64.b64encode(img_buffer.read()).decode('utf-8')
plt.close()

# --- 4. HTML DINÁMICO ---
# Valores en COP originales para mostrar
presupuesto_mostrar = presupuesto_usuario
precio_combo_final_mostrar = precio_combo_final * ESCALA
ahorro_total_mostrar = ahorro_total * ESCALA
ganancia_tienda_mostrar = ganancia_tienda * ESCALA
z_optimo_mostrar = z_optimo

items_html = ""
for i, cat in enumerate(categorias_data):
    p_final = (cat["precio"] - variables[i].varValue) * ESCALA
    descuento = variables[i].varValue * ESCALA
    items_html += f'''
        <li>
            <span>&#128230; {cat["nombre"]} Final:</span>
            <strong>${p_final:,.0f} COP <span class="pill-badge">Desc: {descuento:,.0f}</span></strong>
        </li>
    '''

html_content = f"""<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MathMatch Simplex N-D</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; font-family: 'Segoe UI', Arial, sans-serif; }}
        body {{ background-color: #0f172a; color: #f8fafc; padding: 30px; display: flex; justify-content: center; }}
        .dashboard-container {{ max-width: 950px; width: 100%; background-color: #1e293b; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); overflow: hidden; border: 1px solid #334155; position:relative; }}
        .header {{ background: linear-gradient(135deg, #1e3a8a, #0f172a); color: white; padding: 25px 30px; text-align: center; border-bottom: 1px solid #334155; }}
        .header h1 {{ font-size: 26px; font-weight: 800; }}
        .badge {{ background-color: #10b981; color: #ffffff; padding: 4px 12px; border-radius: 50px; font-size: 13px; font-weight: bold; margin-left: 8px; }}
        .content {{ padding: 35px; }}
        .grid {{ display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 30px; }}
        .section-title {{ font-size: 18px; color: #38bdf8; font-weight: 700; border-bottom: 1px solid #334155; padding-bottom: 10px; margin-bottom: 20px; }}
        .desc-text {{ font-size: 14.5px; color: #94a3b8; line-height: 1.6; margin-bottom: 20px; }}
        .chart-container {{ text-align: center; border: 1px solid #334155; border-radius: 12px; padding: 10px; background-color: #0f172a; }}
        .chart-container img {{ max-width: 100%; border-radius: 8px; }}
        .card {{ background-color: #0f172a; border-radius: 12px; padding: 25px; border: 1px solid #334155; }}
        .metrics-list {{ list-style: none; }}
        .metrics-list li {{ margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px dashed #334155; font-size: 14.5px; display: flex; justify-content: space-between; }}
        .metrics-list strong {{ color: #e2e8f0; }}
        .highlight {{ color: #ec4899; font-size: 20px; font-weight: bold; }}
        .pill-badge {{ background-color: #3b82f6; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; color: white; }}
        .alert-box {{ background-color: rgba(56, 189, 248, 0.1); border-left: 4px solid #38bdf8; padding: 15px; border-radius: 0 8px 8px 0; margin-top: 25px; font-size: 13.5px; color: #e2e8f0; line-height: 1.5; }}
    </style>
</head>
<body>
    <div class="dashboard-container">
        <div style="position:absolute; top:20px; left:20px;">
            <a href="/admin/sesiones-activas" style="color:white; text-decoration:none; font-size:14px; background:#334155; padding:8px 15px; border-radius:5px;">&lt; Volver</a>
        </div>
        <div class="header">
            <h1>&#127919; CellFinder TechMatch: Simplex N-D</h1>
            <p>Estado de Resoluci&oacute;n: <span class="badge">{pulp.LpStatus[model.status]}</span></p>
        </div>

        <div class="content">
            <div class="grid">
                <div>
                    <h3 class="section-title">An&aacute;lisis Visual ({N} Dimensiones)</h3>
                    <p class="desc-text">El motor ha procesado din&aacute;micamente un combo de <strong>{N} productos</strong> bas&aacute;ndose en el historial de precios reales vistos por el usuario &uacute;ltimos 30 d&iacute;as.</p>
                    <div class="chart-container">
                        <img src="data:image/png;base64,{img_base64}" alt="Gr&aacute;fico Adaptativo Simplex">
                    </div>
                </div>

                <div style="display: flex; flex-direction: column; gap: 20px;">
                    <!-- Tracking -->
                    <div class="card" style="border-left: 4px solid #10b981;">
                        <h3 class="section-title" style="margin-bottom: 15px; color: #10b981;">&#128202; Comportamiento de Tracking</h3>
                        <ul class="metrics-list">
                            <li><span>&#128065; Productos Vistos:</span><strong>{tracking_data['totalVisualizaciones']}</strong></li>
                            <li><span>&#128722; Carritos Creados:</span><strong>{tracking_data['totalCarritos']}</strong></li>
                        </ul>
                    </div>

                    <!-- Resultados -->
                    <div class="card">
                        <h3 class="section-title" style="margin-bottom: 15px;">Resultados &Oacute;ptimos</h3>
                        <ul class="metrics-list">
                            {items_html}
                            <li><span>&#128077; Presupuesto del Usuario:</span><strong style="color: #a78bfa;">${presupuesto_mostrar:,.0f} COP</strong></li>
                            <li><span>&#127993; Precio Combo Total:</span><strong style="color: #38bdf8;">${precio_combo_final_mostrar:,.0f} COP</strong></li>
                            <li><span>&#127873; Ahorro Total Usuario:</span><strong style="color: #10b981;">${ahorro_total_mostrar:,.0f} COP</strong></li>
                            <li><span>&#128188; Ganancia de la Tienda:</span><strong style="color: #f59e0b;">${ganancia_tienda_mostrar:,.0f} COP</strong></li>
                            <li style="flex-direction: column; align-items: flex-start; gap: 8px;">
                                <span>&#11088; Utilidad Global &Oacute;ptima (Z):</span>
                                <span class="highlight">{z_optimo_mostrar}</span>
                            </li>
                        </ul>
                    </div>

                    <div class="alert-box">
                        <strong>&#128161; Inteligencia Artificial:</strong><br>
                        El m&eacute;todo distribuy&oacute; los descuentos en {N} categor&iacute;as usando el historial real de navegaci&oacute;n para estimar precios y maximizar Z, sujeto al presupuesto de ${presupuesto_mostrar:,.0f} COP.
                    </div>

                    <div style="margin-top: 25px; text-align: center;">
                        <button onclick="alert('Oferta aplicada. El carrito del usuario ha sido actualizado.')" style="background-color: #ec4899; color: white; border: none; padding: 15px 30px; font-size: 16px; font-weight: bold; border-radius: 8px; cursor: pointer; width: 100%;">
                            🔥 Aplicar Recomendaci&oacute;n Personalizada
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>"""

print(html_content)
