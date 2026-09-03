package com.oce.rosemary

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {

    private lateinit var tvRootStatus: TextView
    private lateinit var tvCpuLittle: TextView
    private lateinit var tvCpuBig: TextView
    private lateinit var tvGpuFreq: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            readTelemetry()
            handler.postDelayed(this, 1500) // Update tiap 1.5 detik
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRootStatus = findViewById(R.id.tvRootStatus)
        tvCpuLittle = findViewById(R.id.tvCpuLittle)
        tvCpuBig = findViewById(R.id.tvCpuBig)
        tvGpuFreq = findViewById(R.id.tvGpuFreq)

        // Inisialisasi Shell Root
        Shell.getShell { shell ->
            if (shell.isRoot) {
                tvRootStatus.text = "Root: Granted (Active)"
                tvRootStatus.setTextColor(0xFF00FF66.toInt())
                handler.post(updateRunnable)
            } else {
                tvRootStatus.text = "Root: Denied / Not Rooted"
                tvRootStatus.setTextColor(0xFFFF0055.toInt())
            }
        }
    }

    private fun readTelemetry() {
        // Baca CPU Little (policy0) & Big (policy6)
        val littleFreq = readNode("/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq")
        val bigFreq = readNode("/sys/devices/system/cpu/cpufreq/policy6/scaling_cur_freq")

        // Baca GPU Freq (Node standar MediaTek)
        var gpuFreq = readNode("/sys/kernel/ged/hal/current_freq")
        if (gpuFreq == "--") {
            gpuFreq = readNode("/proc/gpufreq/gpufreq_var_dump")
        }

        tvCpuLittle.text = "LITTLE Cores: ${formatMhz(littleFreq)} MHz"
        tvCpuBig.text = "BIG Cores: ${formatMhz(bigFreq)} MHz"
        tvGpuFreq.text = "GPU Freq: ${formatMhz(gpuFreq)} MHz"
    }

    private fun readNode(path: String): String {
        val result = Shell.cmd("cat $path").exec()
        return if (result.isSuccess && result.out.isNotEmpty()) {
            result.out[0].trim()
        } else {
            "--"
        }
    }

    private fun formatMhz(raw: String): String {
        val num = raw.filter { it.isDigit() }.toLongOrNull() ?: return "--"
        return if (num > 10000) (num / 1000).toString() else num.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}
