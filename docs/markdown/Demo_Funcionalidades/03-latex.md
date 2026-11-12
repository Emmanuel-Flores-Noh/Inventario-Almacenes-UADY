# 3. Fórmulas matemáticas con LaTeX

El template procesa las fórmulas con `ziamath` y las inserta en el PDF como
imágenes SVG, por lo que mantienen nitidez al ampliar el documento.

## 3.1 Fórmulas en línea

La equivalencia entre masa y energía se resume en $E = mc^2$. También se puede
escribir como \(E^2 = p^2c^2 + m^2c^4\) usando la sintaxis con paréntesis.

## 3.2 Bloque con doble dólar

$$
\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}
$$

## 3.3 Bloque con corchetes

\[
\frac{d}{dx} \left( \int_{a}^{x} f(t) dt \right) = f(x)
\]

## 3.4 Bloque con la etiqueta math

```math
\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}
```

## 3.5 Ecuaciones adicionales

$$
\nabla \cdot \vec{E} = \frac{\rho}{\varepsilon_0}
$$

$$
f(x) = \sum_{n=0}^{\infty} \frac{f^{(n)}(a)}{n!}(x-a)^n
$$

## 3.6 Fórmulas dentro de tablas

| Concepto | Expresión |
| :--- | :---: |
| Área del círculo | $A = \pi r^2$ |
| Teorema de Pitágoras | \(a^2 + b^2 = c^2\) |
| Identidad de Euler | $e^{i\pi} + 1 = 0$ |

## 3.7 Código que no debe procesarse

El siguiente bloque muestra ejemplos de sintaxis, pero debe conservarse como
texto plano:

```markdown
La energía es $E = mc^2$.

$$
\int_0^1 x^2 dx
$$
```

Si el bloque anterior aparece como código y no como fórmulas renderizadas, la
protección de bloques de código está funcionando correctamente.
