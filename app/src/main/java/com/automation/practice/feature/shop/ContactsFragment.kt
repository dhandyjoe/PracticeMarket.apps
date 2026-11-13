package com.automation.practice.feature.shop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.core.content.ContextCompat
import com.automation.practice.R
import com.automation.practice.core.ui.compose.Pm
import com.automation.practice.core.ui.compose.PmAvatar
import com.automation.practice.core.ui.compose.PmPrimaryButton
import com.automation.practice.core.ui.compose.PmScreen
import com.automation.practice.core.ui.compose.PmText
import com.automation.practice.core.ui.compose.PmTopBar
import com.automation.practice.core.ui.compose.tag
import com.automation.practice.domain.Contact
import com.automation.practice.domain.SEED_CONTACTS
import com.automation.practice.domain.search

/** PAY-03 Transfer · pilih kontak (Compose + testTag): banner izin, "Terakhir", daftar A–Z dengan sticky header (FR-PAY-03/04). */
class ContactsFragment : ShopFragment(0) {
    private var granted by mutableStateOf(false)
    private var device by mutableStateOf(emptyList<Contact>())

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) loadDevice()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (granted) loadDevice()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PmScreen {
                    ContactsScreen(
                        contacts = (SEED_CONTACTS + device.filter { d -> SEED_CONTACTS.none { it.name == d.name } }),
                        showBanner = !granted,
                        onAllow = { permission.launch(Manifest.permission.READ_CONTACTS) },
                        onBack = { nav.back() },
                        onPick = { nav.openAmount(it.name, it.phone) },
                    )
                }
            }
        }
    }

    /** Kontak perangkat (nama + nomor) bila izin diberikan; digabung dengan kontak uji bawaan. */
    private fun loadDevice() {
        device = runCatching {
            requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            )?.use { c -> generateSequence { if (c.moveToNext()) Contact(c.getString(0).orEmpty(), c.getString(1).orEmpty()) else null }.filter { it.name.isNotBlank() }.toList() }
        }.getOrNull().orEmpty()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactsScreen(contacts: List<Contact>, showBanner: Boolean, onAllow: () -> Unit, onBack: () -> Unit, onPick: (Contact) -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = contacts.search(query)
    val groups = shown.sortedBy { it.name.lowercase() }.groupBy { it.name.first().uppercaseChar() }
    val recents = contacts.filter { it.recent }

    Column(Modifier.fillMaxSize().background(Pm.surface)) {
        PmTopBar(stringResource(R.string.pay_transfer), onBack)
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(Pm.subtle).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_search), null, Modifier.padding(end = 8.dp), colorFilter = ColorFilter.tint(Pm.muted))
                val hint = stringResource(R.string.pay_search_hint)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(fontFamily = Pm.Inter, fontSize = 15.sp, color = Pm.ink),
                    cursorBrush = SolidColor(Pm.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.weight(1f).tag("pay_input_contact_search").semantics { contentDescription = hint },
                    decorationBox = { inner -> Box { if (query.isEmpty()) PmText(hint, 15, color = Pm.muted); inner() } },
                )
            }
        }
        if (showBanner) {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pm.c(R.color.pm_img_blue_bg)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PmText(stringResource(R.string.pay_contacts_banner), 13, FontWeight.SemiBold, Pm.c(R.color.pm_img_blue_ink), modifier = Modifier.weight(1f).tag("pay_txt_contacts_banner"))
                Box(
                    Modifier.padding(start = 8.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onAllow).padding(horizontal = 12.dp).tag("pay_btn_allow_contacts"),
                    contentAlignment = Alignment.Center,
                ) { PmText(stringResource(R.string.pay_allow), 14, FontWeight.Bold, Pm.c(R.color.pm_img_blue_ink)) }
            }
        }
        if (shown.isEmpty()) {
            PmText(stringResource(R.string.pay_contacts_empty), 14, color = Pm.muted, modifier = Modifier.fillMaxWidth().padding(32.dp).tag("pay_txt_empty_contacts"))
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding().tag("pay_list_contacts")) {
            if (query.isBlank() && recents.isNotEmpty()) item {
                Column(Modifier.padding(top = 8.dp)) {
                    PmText(stringResource(R.string.pay_recent), 16, FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(recents) { c ->
                            Column(Modifier.clickable { onPick(c) }.padding(vertical = 4.dp).tag("pay_item_recent"), horizontalAlignment = Alignment.CenterHorizontally) {
                                PmAvatar(inits(c.name), SEED_CONTACTS.indexOf(c), 52.dp)
                                PmText(c.name.substringBefore(' '), 12, FontWeight.SemiBold, Pm.inkSecondary, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
            groups.forEach { (letter, people) ->
                stickyHeader {
                    PmText(letter.toString(), 13, FontWeight.Bold, Pm.inkSecondary, modifier = Modifier.fillMaxWidth().background(Pm.background).padding(horizontal = 20.dp, vertical = 6.dp).tag("pay_txt_contact_header"))
                }
                items(people) { c ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onPick(c) }.padding(horizontal = 20.dp, vertical = 8.dp).tag("pay_item_contact_row"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PmAvatar(inits(c.name), SEED_CONTACTS.indexOf(c).coerceAtLeast(0))
                        Column(Modifier.padding(start = 12.dp)) {
                            PmText(c.name, 15, FontWeight.SemiBold, modifier = Modifier.tag("pay_txt_contact_name"))
                            PmText(c.phone, 13, color = Pm.muted, modifier = Modifier.tag("pay_txt_contact_phone"))
                        }
                    }
                }
            }
        }
    }
}

private fun inits(name: String) = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
