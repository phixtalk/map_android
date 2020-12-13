package com.mapapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.mapapp.intent.UserIntent
import com.mapapp.state.MainState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class MapViewModel : ViewModel(){

    var mPolyLinesData = ArrayList<PolylineData>()
    var mMapMarkers = ArrayList<Marker>()
    var mCarMarkers = ArrayList<Marker>()
    var mLatLng = ArrayList<LatLng>()

    val intentChannel = Channel<UserIntent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState>
        get() = _state

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        _state.value = MainState.Error("${e.message}")
    }

    private fun handleIn(){
        viewModelScope.launch {

        }
    }

}

data class PolylineData(val polyline: Polyline?)
