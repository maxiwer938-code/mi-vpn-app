package com.example.mivpn

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var backend: GoBackend
    private lateinit var status: TextView
    private lateinit var button: Button
    private lateinit var input: EditText
    private var connected = false

    private val tunnel = object : Tunnel {
        override fun getName(): String = "mivpn"
        override fun onStateChange(newState: Tunnel.State) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = GoBackend(applicationContext)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        status = TextView(this).apply { textSize = 22f }
        input = EditText(this).apply {
            hint = "Pegá acá tu configuración WireGuard"
            minLines = 10
            gravity = Gravity.TOP
        }
        button = Button(this).apply { setOnClickListener { onToggle() } }

        layout.addView(status)
        layout.addView(input)
        layout.addView(button)
        setContentView(layout)
        update()
    }

    private fun update() {
        status.text = if (connected) "CONECTADO" else "DESCONECTADO"
        button.text = if (connected) "Detener" else "Iniciar"
    }

    private fun onToggle() {
        val intent = GoBackend.VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 1)
        } else {
            toggle()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) toggle()
    }

    private fun toggle() {
        thread {
            try {
                if (connected) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                } else {
                    val config = Config.parse(
                        ByteArrayInputStream(input.text.toString().toByteArray())
                    )
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }
                connected = !connected
                runOnUiThread { update() }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
