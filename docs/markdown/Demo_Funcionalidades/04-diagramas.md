# 4. Diagramas Mermaid

Los diagramas se guardan en `docs/diagramas/` con extensión `.mmd`. En el
Markdown se referencian como una imagen normal:

```markdown
![Descripción del diagrama](nombre-del-diagrama.mmd)
```

El compilador detecta la referencia, convierte el diagrama a PNG para el PDF y
también genera una versión PDF del diagrama.

## 4.1 Diagrama de flujo

![Flujo de compilación](demo-flujo.mmd)

## 4.2 Diagrama de secuencia

![Secuencia de compilación](demo-secuencia.mmd)

## 4.3 Diagrama de clases

![Modelo de clases](demo-clases.mmd)

## 4.4 Diagrama de estados

![Estados de un documento](demo-estado.mmd)

## 4.5 Diagrama entidad-relación

![Modelo entidad-relación](demo-er.mmd)

## 4.6 Diagrama de Gantt

![Plan de compilación](demo-gantt.mmd)

## 4.7 Referencias dentro de bloques de código

La siguiente línea está dentro de un bloque de código, por lo que no debe
compilarse ni sustituirse por una imagen:

```markdown
![Este ejemplo no se compila](demo-flujo.mmd)
```

Este comportamiento permite documentar la sintaxis sin generar diagramas
adicionales por accidente.
