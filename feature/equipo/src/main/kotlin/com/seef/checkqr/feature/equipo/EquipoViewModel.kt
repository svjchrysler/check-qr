package com.seef.checkqr.feature.equipo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.model.Role
import com.seef.checkqr.core.model.TeamMember
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que puede estar pasando en la pantalla de equipo. */
sealed interface EstadoDeEquipo {
    data class Lista(val miembros: List<TeamMember>) : EstadoDeEquipo
    data class MostrandoQr(val carga: CargaDeInvitacion) : EstadoDeEquipo
    data object Escaneando : EstadoDeEquipo
    data class Aviso(val mensaje: String) : EstadoDeEquipo
}

@HiltViewModel
class EquipoViewModel @Inject constructor() : ViewModel() {

    private val _estado = MutableStateFlow<EstadoDeEquipo>(EstadoDeEquipo.Lista(emptyList()))
    val estado: StateFlow<EstadoDeEquipo> = _estado.asStateFlow()

    /** El rol de quien esta usando la app. Decide que se puede hacer aqui. */
    private val _miRol = MutableStateFlow(Role.DUENO)
    val miRol: StateFlow<Role> = _miRol.asStateFlow()

    /**
     * Genera una invitacion.
     *
     * PENDIENTE: el token lo tiene que emitir el backend, corto y de un solo uso.
     * Mientras `POST /v1/sesion` devuelva 501 no hay organizaciones contra las
     * que emitirlo, asi que esta pantalla no puede completar el flujo. Se deja
     * explicito en la UI en vez de generar un token local que daria una falsa
     * sensacion de que funciona.
     */
    fun invitar(rol: Role) {
        if (!_miRol.value.puedeGestionarEquipo) {
            _estado.value = EstadoDeEquipo.Aviso("Solo el dueño puede invitar gente.")
            return
        }
        _estado.value = EstadoDeEquipo.Aviso(
            "Las invitaciones necesitan la cuenta del comercio en el servidor, " +
                "que todavía no está disponible.",
        )
    }

    fun escanear() {
        _estado.value = EstadoDeEquipo.Escaneando
    }

    /** Llega desde el escaner de QR. */
    fun qrEscaneado(texto: String) {
        val carga = InvitacionQr.deTexto(texto)
        if (carga == null) {
            // Silencio a proposito: la camara ve muchos QR que no son nuestros.
            return
        }
        _estado.value = EstadoDeEquipo.Aviso(
            "Invitación de ${carga.rolTipado?.nombreVisible} leída correctamente. " +
                "Falta el servidor para poder aceptarla.",
        )
    }

    fun volver() {
        _estado.value = EstadoDeEquipo.Lista(emptyList())
    }
}
