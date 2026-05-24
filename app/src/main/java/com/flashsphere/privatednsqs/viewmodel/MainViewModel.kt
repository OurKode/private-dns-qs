package com.flashsphere.privatednsqs.viewmodel

import android.content.ComponentName
import android.service.quicksettings.TileService
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsOnToggle
import com.flashsphere.privatednsqs.datastore.getFlow
import com.flashsphere.privatednsqs.datastore.getStateFlow
import com.flashsphere.privatednsqs.datastore.requireUnlock
import com.flashsphere.privatednsqs.datastore.update
import com.flashsphere.privatednsqs.service.PrivateDnsTileService
import com.flashsphere.privatednsqs.ui.ChangesSavedMessage
import com.flashsphere.privatednsqs.ui.InvalidProviderFieldsMessage
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.ProviderAddedMessage
import com.flashsphere.privatednsqs.ui.ProviderAlreadyExistsMessage
import com.flashsphere.privatednsqs.ui.ProviderDeletedMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(
    private val application: PrivateDnsApplication,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val dataStore = application.dataStore
    private val privateDns = PrivateDns(application)

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.SUSPEND
    )
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _openHelpDialogFlow = savedStateHandle.getMutableStateFlow("open_help_menu", false)
    val openHelpDialogFlow = _openHelpDialogFlow.asStateFlow()

    val dnsOffChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_OFF_TOGGLE)
    val dnsAutoChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_AUTO_TOGGLE)
    val dnsOnChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_ON_TOGGLE)
    val requireUnlockChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.REQUIRE_UNLOCK)

    private val _dnsHostname = MutableStateFlow(privateDns.getHostname() ?: "")
    val dnsHostname = _dnsHostname.asStateFlow()

    private val _activeDnsMode = MutableStateFlow(privateDns.getDnsMode())
    val activeDnsMode = _activeDnsMode.asStateFlow()

    val dnsHostnameTextFieldState = TextFieldState(_dnsHostname.value)

    val customDnsProviders: StateFlow<List<DnsProvider>> = dataStore.getFlow(PreferenceKeys.CUSTOM_DNS_PROVIDERS)
        .map { parseProviders(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = parseProviders(PreferenceKeys.CUSTOM_DNS_PROVIDERS.defaultValue)
        )

    val newProviderNameState = TextFieldState("")
    val newProviderHostnameState = TextFieldState("")

    private fun parseProviders(jsonStr: String): List<DnsProvider> {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<DnsProvider>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name", "")
                val hostname = obj.optString("hostname", "")
                if (name.isNotEmpty() && hostname.isNotEmpty()) {
                    list.add(DnsProvider(name, hostname))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeProviders(providers: List<DnsProvider>): String {
        val jsonArray = JSONArray()
        for (provider in providers) {
            val obj = JSONObject()
            obj.put("name", provider.name)
            obj.put("hostname", provider.hostname)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    init {
        _openHelpDialogFlow.value = !hasPermission()
    }

    fun openHelpDialog(open: Boolean) {
        _openHelpDialogFlow.value = open
    }

    fun reloadDnsHostname() {
        val updatedHostname = privateDns.getHostname() ?: ""

        dnsHostnameTextFieldState.text.trim().toString().let {
            if (it != updatedHostname && it == _dnsHostname.value) {
                dnsHostnameTextFieldState.setTextAndPlaceCursorAtEnd(updatedHostname)
            }
        }
        _dnsHostname.value = updatedHostname
        _activeDnsMode.value = privateDns.getDnsMode()
    }

    fun dnsOffChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOffToggle(checked) }
    }
    fun dnsAutoChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsAutoToggle(checked) }
    }
    fun dnsOnChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOnToggle(checked) }
    }
    fun requireUnlockChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.requireUnlock(checked) }
    }
    fun showSnackbarMessage(message: SnackbarMessage) {
        viewModelScope.launch {
            // wait until there's at least 1 subscriber before emitting
            _snackbarMessages.subscriptionCount.first { it > 0 }
            _snackbarMessages.emit(message)
        }
    }
    fun hasPermission(): Boolean {
        return privateDns.hasPermission()
    }
    fun save() {
        if (!hasPermission()) {
            showSnackbarMessage(NoPermissionMessage)
            return
        }
        val dnsHostName = dnsHostnameTextFieldState.text.trim().toString()
        if (dnsHostName.isEmpty()) {
            showSnackbarMessage(NoDnsHostnameMessage)
            return
        }
        privateDns.setHostname(dnsHostName)
        _dnsHostname.value = dnsHostName
        _activeDnsMode.value = privateDns.getDnsMode()
        requestTileUpdate()
        showSnackbarMessage(ChangesSavedMessage)
    }

    fun selectDnsProvider(provider: DnsProvider) {
        if (!hasPermission()) {
            showSnackbarMessage(NoPermissionMessage)
            return
        }
        val hostname = provider.hostname.trim()
        if (hostname.isEmpty()) {
            showSnackbarMessage(NoDnsHostnameMessage)
            return
        }
        privateDns.setHostname(hostname)
        privateDns.setDnsMode(DnsMode.On)
        dnsHostnameTextFieldState.setTextAndPlaceCursorAtEnd(hostname)
        _dnsHostname.value = hostname
        _activeDnsMode.value = DnsMode.On

        viewModelScope.launch {
            if (!dnsOnChecked.value) {
                dataStore.dnsOnToggle(true)
            }
            requestTileUpdate()
        }
        showSnackbarMessage(ChangesSavedMessage)
    }

    fun addCustomDnsProvider() {
        val name = newProviderNameState.text.trim().toString()
        val hostname = newProviderHostnameState.text.trim().toString()
        if (name.isEmpty() || hostname.isEmpty()) {
            showSnackbarMessage(InvalidProviderFieldsMessage)
            return
        }

        viewModelScope.launch {
            val currentList = customDnsProviders.value.toMutableList()
            if (currentList.any { it.hostname.equals(hostname, ignoreCase = true) }) {
                showSnackbarMessage(ProviderAlreadyExistsMessage)
                return@launch
            }

            currentList.add(DnsProvider(name, hostname))
            val json = serializeProviders(currentList)
            dataStore.update(PreferenceKeys.CUSTOM_DNS_PROVIDERS, json)

            newProviderNameState.setTextAndPlaceCursorAtEnd("")
            newProviderHostnameState.setTextAndPlaceCursorAtEnd("")
            showSnackbarMessage(ProviderAddedMessage)
        }
    }

    fun deleteCustomDnsProvider(provider: DnsProvider) {
        viewModelScope.launch {
            val currentList = customDnsProviders.value.filter { it.hostname != provider.hostname }
            val json = serializeProviders(currentList)
            dataStore.update(PreferenceKeys.CUSTOM_DNS_PROVIDERS, json)
            showSnackbarMessage(ProviderDeletedMessage)
        }
    }

    private fun requestTileUpdate() {
        runCatching {
            TileService.requestListeningState(
                application,
                ComponentName(application, PrivateDnsTileService::class.java)
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as PrivateDnsApplication
                val savedStateHandle = extras.createSavedStateHandle()
                return MainViewModel(application, savedStateHandle) as T
            }
        }
    }
}