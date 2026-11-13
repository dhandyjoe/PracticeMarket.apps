package com.automation.practice.feature.shop

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentEditProfileBinding
import com.automation.practice.domain.Profile
import com.automation.practice.domain.isoToUtcMillis
import com.automation.practice.domain.utcMillisToIso
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

/** ACC-02 Edit Profil: foto (kamera / Photo Picker), nama, email, tanggal lahir (DatePicker), jenis kelamin (FR-ACC-02). */
class EditProfileFragment : ShopFragment(R.layout.fragment_edit_profile) {
    private lateinit var b: FragmentEditProfileBinding
    private var birth = ""
    private var photo: String? = null

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto.launch(null) else Snackbar.make(b.root, R.string.acc_camera_denied, Snackbar.LENGTH_SHORT).show()
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp -> bmp?.let(::saveBitmap) }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::copyPicked) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentEditProfileBinding.bind(view)
        b.shopTopbar.setup(R.string.acc_edit_title)
        b.accBarSave.applySystemInsets(bottom = true)

        val p = vm.session.profile.value
        b.accInputName.setText(p.name)
        b.accInputEmail.setText(p.email)
        birth = p.birth
        photo = p.photoUri
        b.accGroupGender.check(if (p.gender == GENDER_FEMALE) R.id.acc_rdo_female else if (p.gender == GENDER_MALE) R.id.acc_rdo_male else -1)
        renderPhoto()
        renderBirth()

        b.accInputName.doAfterTextChanged { validate() }
        b.accInputEmail.doAfterTextChanged { validate() }
        b.accBtnBirthdate.setOnClickListener { pickDate() }
        b.accBtnChangePhoto.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setItems(arrayOf(getString(R.string.acc_photo_camera), getString(R.string.acc_photo_gallery))) { _, which ->
                    if (which == 0) cameraPermission.launch(Manifest.permission.CAMERA)
                    else pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }.show()
        }
        b.accBtnSave.setOnClickListener {
            if (!validate()) return@setOnClickListener
            val gender = when (b.accGroupGender.checkedRadioButtonId) { R.id.acc_rdo_male -> GENDER_MALE; R.id.acc_rdo_female -> GENDER_FEMALE; else -> "" }
            vm.session.updateProfile(Profile(b.accInputName.text.toString().trim(), b.accInputEmail.text.toString().trim(), birth, gender, photo))
            Snackbar.make(view, R.string.acc_saved, Snackbar.LENGTH_SHORT).setAnchorView(b.accBarSave).show()
        }
        validate(showErrors = false)
    }

    /** Nama wajib, email berformat; tombol Simpan disabled sampai valid. */
    private fun validate(showErrors: Boolean = true): Boolean {
        val nameOk = b.accInputName.text.isNotBlank()
        val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(b.accInputEmail.text.toString().trim()).matches()
        b.accErrorName.isVisible = showErrors && !nameOk
        b.accErrorEmail.isVisible = showErrors && !emailOk
        b.accBtnSave.isEnabled = nameOk && emailOk
        return nameOk && emailOk
    }

    private fun renderBirth() {
        b.accBtnBirthdate.text = birth.ifEmpty { getString(R.string.acc_birth_hint) }
        b.accBtnBirthdate.setTextColor(requireContext().getColor(if (birth.isEmpty()) R.color.pm_ink_muted else R.color.pm_ink))
    }

    private fun renderPhoto() {
        b.accTxtPhotoInitials.text = initials(b.accInputName.text.toString().ifBlank { vm.session.profile.value.name })
        b.accImgPhoto.isVisible = photo != null
        b.accTxtPhotoInitials.isVisible = photo == null
        if (photo != null) { b.accImgPhoto.setBackgroundResource(R.drawable.bg_avatar); b.accImgPhoto.clipToOutline = true; b.accImgPhoto.setImageURI(null); b.accImgPhoto.setImageURI(Uri.parse(photo)) }
    }

    /** Tanggal awal: nilai tersimpan; kosong → 2026-01-15 di mode deterministik, selain itu hari ini (PRD §10). */
    private fun pickDate() {
        val container = requireContext().app.container
        val initial = isoToUtcMillis(birth)
            ?: if (container.settings.prefs.value.deterministic) isoToUtcMillis("2026-01-15") else MaterialDatePicker.todayInUtcMilliseconds()
        MaterialDatePicker.Builder.datePicker().setTitleText(R.string.acc_birth_pick).setSelection(initial).build().also { dp ->
            dp.addOnPositiveButtonClickListener { birth = utcMillisToIso(it); renderBirth() }
        }.show(childFragmentManager, "birth")
    }

    private fun saveBitmap(bmp: Bitmap) {
        val file = File(requireContext().filesDir, "profile.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        setPhoto(file)
    }

    private fun copyPicked(uri: Uri) {
        val file = File(requireContext().filesDir, "profile.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            if (input.available() > MAX_PHOTO_BYTES) return Snackbar.make(b.root, R.string.acc_photo_too_big, Snackbar.LENGTH_SHORT).show()
            file.outputStream().use { input.copyTo(it) }
        }
        setPhoto(file)
    }

    private fun setPhoto(file: File) {
        photo = Uri.fromFile(file).toString()
        renderPhoto()
    }

    private companion object {
        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"
        const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
    }
}
