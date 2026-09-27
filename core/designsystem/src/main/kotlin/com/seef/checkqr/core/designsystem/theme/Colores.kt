package com.seef.checkqr.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta de CheckQr.
 *
 * El verde no es decorativo: es el color del pago que entro, que es la
 * informacion que el comerciante busca de un vistazo desde el otro lado del
 * mostrador. El rojo queda reservado para el nivel 🔴 "no llegó" y para "dejó de
 * escuchar", y no se usa para nada mas, para que cuando aparezca signifique una
 * sola cosa.
 *
 * **Estan definidos los 30 roles de Material, no solo los que se usan a
 * diario.** Es necesario: cualquier rol que se deje sin definir cae al morado
 * por omision de Material, y basta con que un componente lo use — la barra de
 * navegacion usa `surfaceContainer`, los botones tonales usan
 * `secondaryContainer` — para que aparezca lila en medio de una app verde.
 */

// --- Tema claro -----------------------------------------------------------

private val verde40 = Color(0xFF00701F)
private val verde90 = Color(0xFFB7F3BE)
private val verde10 = Color(0xFF002204)

private val salvia40 = Color(0xFF52634F)
private val salvia90 = Color(0xFFD4E8CE)
private val salvia10 = Color(0xFF101F10)

private val ambar40 = Color(0xFF7A5900)
private val ambar90 = Color(0xFFFFDF95)
private val ambar10 = Color(0xFF261A00)

private val rojo40 = Color(0xFFB3261E)
private val rojo90 = Color(0xFFF9DEDC)
private val rojo10 = Color(0xFF410E0B)

private val neutro10 = Color(0xFF1A1C19)
private val neutro20 = Color(0xFF2F312D)
private val neutro90 = Color(0xFFE2E3DD)
private val neutro95 = Color(0xFFF0F1EB)
private val neutro98 = Color(0xFFFCFDF6)

internal val esquemaClaro = lightColorScheme(
    primary = verde40,
    onPrimary = Color.White,
    primaryContainer = verde90,
    onPrimaryContainer = verde10,

    secondary = salvia40,
    onSecondary = Color.White,
    secondaryContainer = salvia90,
    onSecondaryContainer = salvia10,

    tertiary = ambar40,
    onTertiary = Color.White,
    tertiaryContainer = ambar90,
    onTertiaryContainer = ambar10,

    error = rojo40,
    onError = Color.White,
    errorContainer = rojo90,
    onErrorContainer = rojo10,

    background = neutro98,
    onBackground = neutro10,
    surface = neutro98,
    onSurface = neutro10,
    surfaceVariant = Color(0xFFDDE5D9),
    onSurfaceVariant = Color(0xFF414941),

    // Los tonos de superficie que usan la barra de navegacion, las tarjetas
    // elevadas y las hojas. Sin definirlos, la barra inferior sale lila.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = neutro98,
    surfaceContainer = neutro95,
    surfaceContainerHigh = Color(0xFFEAEBE5),
    surfaceContainerHighest = Color(0xFFE4E5DF),
    surfaceDim = Color(0xFFDBDDD7),
    surfaceBright = neutro98,

    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BE),

    inverseSurface = neutro20,
    inverseOnSurface = neutro95,
    inversePrimary = Color(0xFF7BF08E),

    scrim = Color.Black,
)

// --- Tema oscuro ------------------------------------------------------------

private val verde80 = Color(0xFF7BF08E)
private val verde30 = Color(0xFF005318)
private val verde20 = Color(0xFF003910)

private val salvia80 = Color(0xFFB9CCB4)
private val salvia30 = Color(0xFF3A4B38)
private val salvia20 = Color(0xFF243424)

private val ambar80 = Color(0xFFFFDF95)
private val ambar30 = Color(0xFF5C4200)
private val ambar20 = Color(0xFF3F2E00)

private val rojo80 = Color(0xFFFFB4AB)
private val rojo30 = Color(0xFF93000A)
private val rojo20 = Color(0xFF690005)

internal val esquemaOscuro = darkColorScheme(
    primary = verde80,
    onPrimary = verde20,
    primaryContainer = verde30,
    onPrimaryContainer = Color(0xFF96FFA8),

    secondary = salvia80,
    onSecondary = salvia20,
    secondaryContainer = salvia30,
    onSecondaryContainer = Color(0xFFD4E8CE),

    tertiary = ambar80,
    onTertiary = ambar20,
    tertiaryContainer = ambar30,
    onTertiaryContainer = Color(0xFFFFDF95),

    error = rojo80,
    onError = rojo20,
    errorContainer = rojo30,
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF12140F),
    onBackground = neutro90,
    surface = Color(0xFF12140F),
    onSurface = neutro90,
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BE),

    surfaceContainerLowest = Color(0xFF0C0F0A),
    surfaceContainerLow = Color(0xFF1A1C19),
    surfaceContainer = Color(0xFF1E201C),
    surfaceContainerHigh = Color(0xFF282B26),
    surfaceContainerHighest = Color(0xFF333630),
    surfaceDim = Color(0xFF12140F),
    surfaceBright = Color(0xFF383A34),

    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),

    inverseSurface = neutro90,
    inverseOnSurface = neutro20,
    inversePrimary = verde40,

    scrim = Color.Black,
)
