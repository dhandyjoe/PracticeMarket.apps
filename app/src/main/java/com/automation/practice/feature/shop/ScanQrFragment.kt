package com.automation.practice.feature.shop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentScanQrBinding
import com.automation.practice.domain.MERCHANTS
import com.google.android.material.snackbar.Snackbar

/**
 * PAY-05 Scan QR: izin kamera (dialog sistem), banner bila ditolak, QR dari galeri (FR-PAY-09).
 * shortcut: tanpa pratinjau kamera & dekoder QR; hasil scan emulator lewat deep link `practiceapp://pay/qr`,
 * gambar galeri dikenali dari nama berkas (`kopipagi`, `tokobuku`). Ganti ke CameraX + ML Kit bila perlu scan sungguhan.
 */
class ScanQrFragment : ShopFragment(R.layout.fragment_scan_qr, lightStatusBar = false) {
    private lateinit var b: FragmentScanQrBinding

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { render(it) }
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::resolveImage) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentScanQrBinding.bind(view)
        b.payHeaderScan.applySystemInsets(top = true)
        b.payBarGallery.applySystemInsets(bottom = true)
        b.payBtnClose.setOnClickListener { nav.back() }
        b.payBtnGallery.setOnClickListener { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        b.payBtnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null)))
        }
        if (granted()) render(true) else if (savedInstanceState == null) cameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        // Kembali dari Pengaturan: izin mungkin sudah diaktifkan.
        if (::b.isInitialized && granted()) render(true)
    }

    private fun granted() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun render(ok: Boolean) {
        b.payBannerDenied.isVisible = !ok
        b.payTxtBannerTitle.setText(R.string.pay_camera_denied)
        b.payTxtBannerBody.setText(R.string.pay_camera_denied_body)
        b.payBtnOpenSettings.isVisible = !ok
    }

    private fun resolveImage(uri: Uri) {
        val name = requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }.orEmpty().lowercase()
        val merchant = MERCHANTS.firstOrNull { name.contains(it.id.lowercase()) }
        if (merchant != null) nav.openQrConfirm(merchant.id, 0)
        else Snackbar.make(requireView(), R.string.pay_qr_invalid, Snackbar.LENGTH_LONG).show()
    }
}
