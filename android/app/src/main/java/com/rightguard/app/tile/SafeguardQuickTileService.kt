package com.rightguard.app.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rightguard.app.ui.MainActivity

class SafeguardQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Safeguard"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        // Launch the app in safeguard mode
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("activate_safeguard", true)
        }

        startActivityAndCollapse(intent)

        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
