package com.seef.checkqr.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta de CheckQr.
 *
 * Minimalista a proposito: la pantalla es casi toda blanco y negro, y el color
 * aparece solo cuando significa algo.
 *
 * - **Verde**: dinero que entro, y nada mas. El total del dia, el "sí llegó" de
 *   la verificacion y el indicador de que la app esta escuchando.
 * - **Rojo**: solo el nivel 🔴 "no llegó" y "dejó de escuchar".
 * - **Ambar**: cosas para revisar que no son urgentes.
 * - Todo lo demas es neutro.
 *
 * La razon es practica, no estetica. Si el verde esta en el total, en el boton,
 * en cada monto de la lista y en los iconos, deja de querer decir nada; y esta
 * es una app donde el comerciante tiene que poder distinguir de un vistazo "todo
 * bien" de "revisa esto".
 *
 * Estan definidos los 30 roles de Material y no solo los de uso diario:
 * cualquier rol sin definir cae al morado por omision, y basta con que un
 * componente lo use — la barra de navegacion usa `surfaceContainer`, los botones
 * tonales usan `secondaryContainer` — para que aparezca lila en medio de todo.
 */

// --- Tema claro -------------------------------------------------------------

/** Verde profundo, apagado. Un verde brillante cansa en una pantalla que se mira todo el dia. */
private val verde = Color(0xFF0A6B45)
private val verdeSuave = Color(0xFFE4F1EA)
private val verdeProfundo = Color(0xFF042A1A)

/** Neutros: la base real de la interfaz. */
private val tinta = Color(0xFF101312)
private val tintaSuave = Color(0xFF6B706D)
private val blanco = Color(0xFFFFFFFF)
private val casiBlanco = Color(0xFFFAFBFA)
private val gris5 = Color(0xFFF4F6F4)
private val gris10 = Color(0xFFEDEFEE)
private val gris15 = Color(0xFFE6E9E7)
private val linea = Color(0xFFE2E5E3)

private val ambar = Color(0xFF8A6400)
private val ambarSuave = Color(0xFFFBEFD3)
private val ambarProfundo = Color(0xFF2B1F00)

private val rojo = Color(0xFFB3261E)
private val rojoSuave = Color(0xFFFBEAE8)
private val rojoProfundo = Color(0xFF410E0B)

internal val esquemaClaro = lightColorScheme(
    primary = verde,
    onPrimary = blanco,
    primaryContainer = verdeSuave,
    onPrimaryContainer = verdeProfundo,

    // El secundario es neutro: los botones tonales tienen que verse como
    // acciones discretas, no como una segunda marca de color.
    secondary = Color(0xFF3F4441),
    onSecondary = blanco,
    secondaryContainer = gris10,
    onSecondaryContainer = Color(0xFF2A2E2C),

    tertiary = ambar,
    onTertiary = blanco,
    tertiaryContainer = ambarSuave,
    onTertiaryContainer = ambarProfundo,

    error = rojo,
    onError = blanco,
    errorContainer = rojoSuave,
    onErrorContainer = rojoProfundo,

    background = blanco,
    onBackground = tinta,
    surface = blanco,
    onSurface = tinta,
    surfaceVariant = gris5,
    onSurfaceVariant = tintaSuave,

    surfaceContainerLowest = blanco,
    surfaceContainerLow = casiBlanco,
    surfaceContainer = gris5,
    surfaceContainerHigh = gris10,
    surfaceContainerHighest = gris15,
    surfaceDim = gris10,
    surfaceBright = blanco,

    outline = Color(0xFF9AA09D),
    outlineVariant = linea,

    inverseSurface = tinta,
    inverseOnSurface = casiBlanco,
    inversePrimary = Color(0xFF6FD3A0),

    scrim = Color(0xFF000000),
)

// --- Tema oscuro -------------------------------------------------------------

private val verdeClaro = Color(0xFF6FD3A0)
private val verdeOscuro = Color(0xFF0B4A30)

private val fondoOscuro = Color(0xFF0C0E0D)
private val tintaClara = Color(0xFFE6E8E7)
private val tintaClaraSuave = Color(0xFF9AA09D)
private val lineaOscura = Color(0xFF272B29)

internal val esquemaOscuro = darkColorScheme(
    primary = verdeClaro,
    onPrimary = Color(0xFF00301C),
    primaryContainer = verdeOscuro,
    onPrimaryContainer = Color(0xFF8FEFBC),

    secondary = Color(0xFFBFC5C1),
    onSecondary = Color(0xFF283029),
    secondaryContainer = Color(0xFF212523),
    onSecondaryContainer = Color(0xFFDDE2DF),

    tertiary = Color(0xFFF0CE84),
    onTertiary = Color(0xFF3A2C00),
    tertiaryContainer = Color(0xFF2E2413),
    onTertiaryContainer = Color(0xFFF7E2B4),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF362220),
    onErrorContainer = Color(0xFFF9DEDC),

    background = fondoOscuro,
    onBackground = tintaClara,
    surface = fondoOscuro,
    onSurface = tintaClara,
    surfaceVariant = Color(0xFF161918),
    onSurfaceVariant = tintaClaraSuave,

    surfaceContainerLowest = Color(0xFF070908),
    surfaceContainerLow = Color(0xFF111413),
    surfaceContainer = Color(0xFF161918),
    surfaceContainerHigh = Color(0xFF1D2120),
    surfaceContainerHighest = Color(0xFF252927),
    surfaceDim = fondoOscuro,
    surfaceBright = Color(0xFF2A2E2C),

    outline = Color(0xFF6B706D),
    outlineVariant = lineaOscura,

    inverseSurface = tintaClara,
    inverseOnSurface = Color(0xFF1D2120),
    inversePrimary = verde,

    scrim = Color(0xFF000000),
)
