package com.seef.checkqr.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta de CheckQr.
 *
 * El verde no es decorativo: es el color del pago que entro, que es la
 * informacion que el comerciante busca de un vistazo desde el otro lado del
 * mostrador. El rojo queda reservado para el nivel 🔴 "no llegó", y no se usa
 * para nada mas, para que cuando aparezca signifique una sola cosa.
 */
internal val VerdePago = Color(0xFF00701F)
internal val VerdePagoClaro = Color(0xFF7BF08E)
internal val VerdeContenedor = Color(0xFFCFF5D3)

internal val RojoNoLlego = Color(0xFFB3261E)
internal val RojoNoLlegoClaro = Color(0xFFFFB4AB)

internal val AmarilloAviso = Color(0xFF7A5900)
internal val AmarilloAvisoContenedor = Color(0xFFFFDF95)

internal val GrisTexto = Color(0xFF1A1C19)
internal val GrisFondo = Color(0xFFFCFDF6)
internal val GrisSuperficie = Color(0xFFDDE5D9)

internal val esquemaClaro = lightColorScheme(
    primary = VerdePago,
    onPrimary = Color.White,
    primaryContainer = VerdeContenedor,
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    tertiary = AmarilloAviso,
    tertiaryContainer = AmarilloAvisoContenedor,
    error = RojoNoLlego,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = GrisFondo,
    onBackground = GrisTexto,
    surface = GrisFondo,
    onSurface = GrisTexto,
    surfaceVariant = GrisSuperficie,
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
)

internal val esquemaOscuro = darkColorScheme(
    primary = VerdePagoClaro,
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF005318),
    onPrimaryContainer = VerdePagoClaro,
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243424),
    tertiary = AmarilloAvisoContenedor,
    tertiaryContainer = Color(0xFF5C4200),
    error = RojoNoLlegoClaro,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BE),
    outline = Color(0xFF8B938A),
)
