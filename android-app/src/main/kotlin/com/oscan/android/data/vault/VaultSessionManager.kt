package com.oscan.android.data.vault

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.oscan.android.data.vault.crypto.VaultCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveVaultKeys(
    val vmk: ByteArray,
    val docSubkey: ByteArray,
    val metaSubkey: ByteArray
) {
    fun wipe() {
        VaultCrypto.wipe(vmk)
        VaultCrypto.wipe(docSubkey)
        VaultCrypto.wipe(metaSubkey)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ActiveVaultKeys
        return vmk.contentEquals(other.vmk) &&
                docSubkey.contentEquals(other.docSubkey) &&
                metaSubkey.contentEquals(other.metaSubkey)
    }

    override fun hashCode(): Int {
        var result = vmk.contentHashCode()
        result = 31 * result + docSubkey.contentHashCode()
        result = 31 * result + metaSubkey.contentHashCode()
        return result
    }
}

sealed class VaultSessionState {
    object NotConfigured : VaultSessionState()
    data class Locked(val lockoutRemainingSeconds: Int = 0) : VaultSessionState()
    data class Unlocked(val keys: ActiveVaultKeys) : VaultSessionState()
}

class VaultSessionManager : DefaultLifecycleObserver {
    private val _sessionState = MutableStateFlow<VaultSessionState>(VaultSessionState.Locked())
    val sessionState: StateFlow<VaultSessionState> = _sessionState.asStateFlow()

    private var activeKeys: ActiveVaultKeys? = null

    init {
        // Register auto-lock listener when app goes to background
        runCatching {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    fun setNotConfigured() {
        lock()
        _sessionState.value = VaultSessionState.NotConfigured
    }

    fun setLocked(lockoutRemainingSeconds: Int = 0) {
        lock()
        _sessionState.value = VaultSessionState.Locked(lockoutRemainingSeconds)
    }

    fun unlockWithVmk(vmk: ByteArray) {
        activeKeys?.wipe()
        val docSubkey = VaultCrypto.deriveSubkey(vmk, "vault_doc_key")
        val metaSubkey = VaultCrypto.deriveSubkey(vmk, "vault_meta_key")
        val keys = ActiveVaultKeys(vmk, docSubkey, metaSubkey)
        activeKeys = keys
        _sessionState.value = VaultSessionState.Unlocked(keys)
    }

    fun lock() {
        activeKeys?.wipe()
        activeKeys = null
        if (_sessionState.value !is VaultSessionState.NotConfigured) {
            _sessionState.value = VaultSessionState.Locked(0)
        }
    }

    fun getActiveKeys(): ActiveVaultKeys? = activeKeys

    fun isUnlocked(): Boolean = _sessionState.value is VaultSessionState.Unlocked

    override fun onStop(owner: LifecycleOwner) {
        // Immediate background lock
        lock()
    }
}
