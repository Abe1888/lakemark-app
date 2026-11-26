package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.localization.AppStrings
import com.example.localization.LocalAppStrings
import com.example.localization.RemoteTranslation
import com.example.localization.TranslationCatalog
import com.example.localization.TranslationCategory
import com.example.localization.TranslationRecord
import com.example.localization.TranslationStatus
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusNewText
import com.example.ui.theme.StatusReadyBg
import com.example.ui.theme.StatusReadyText
import com.example.ui.theme.lakemarkFont

fun LazyListScope.manageLanguageSectionItems(
    records: List<TranslationRecord>,
    searchQuery: String,
    selectedCategory: TranslationCategory?,
    selectedStatus: TranslationStatus?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (TranslationCategory?) -> Unit,
    onSelectStatus: (TranslationStatus?) -> Unit,
    onEditRecord: (TranslationRecord) -> Unit,
    palette: BrandPalette,
    s: AppStrings,
) {
    val translatedCount = records.count { it.status == TranslationStatus.TRANSLATED }
    val attentionCount = records.size - translatedCount

    val filtered = records.filter { rec ->
        val catOk = selectedCategory == null || rec.category == selectedCategory
        val statusOk = selectedStatus == null || rec.status == selectedStatus
        val q = searchQuery.trim()
        val queryOk = q.isEmpty() ||
            rec.en.contains(q, ignoreCase = true) ||
            rec.am.contains(q, ignoreCase = true) ||
            rec.key.contains(q, ignoreCase = true)
        catOk && statusOk && queryOk
    }

    item {
        val font = lakemarkFont()
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_management_header"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = palette.plum,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.get("manage_lang_title", "Language Management"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = palette.text,
                        )
                        Text(
                            text = s.get("manage_lang_subtitle", "Edit Amharic & English translations synced live across all users."),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = palette.mut,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.alt,
                    ) {
                        Text(
                            text = "$translatedCount/${records.size}",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = palette.plum,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = {
                        Text(
                            text = s.get("manage_lang_search", "Search English, Amharic, or key…"),
                            fontFamily = font,
                            fontSize = 13.sp,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.plum,
                        unfocusedBorderColor = palette.line,
                        focusedContainerColor = palette.bg,
                        unfocusedContainerColor = palette.bg,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_search_input"),
                )

                // Status filter pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LangFilterChip(
                        label = "${s.get("lang_status_all", "All")} (${records.size})",
                        selected = selectedStatus == null,
                        onClick = { onSelectStatus(null) },
                        palette = palette,
                        testTag = "lang_status_filter_all",
                    )
                    TranslationStatus.entries.forEach { st ->
                        val count = records.count { it.status == st }
                        LangFilterChip(
                            label = "${s.translationStatusLabel(st)} ($count)",
                            selected = selectedStatus == st,
                            onClick = { onSelectStatus(if (selectedStatus == st) null else st) },
                            palette = palette,
                            testTag = "lang_status_filter_${st.key}",
                        )
                    }
                }

                // Category filter pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LangFilterChip(
                        label = s.get("cat_all", "All"),
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        palette = palette,
                        testTag = "lang_cat_filter_all",
                    )
                    TranslationCategory.entries.forEach { cat ->
                        LangFilterChip(
                            label = s.translationCategoryLabel(cat),
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                            palette = palette,
                            testTag = "lang_cat_filter_${cat.name.lowercase()}",
                        )
                    }
                }

                if (attentionCount > 0) {
                    Text(
                        text = "$attentionCount ${s.get("lang_status_untranslated", "needs review")}",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = StatusNewText,
                    )
                }
            }
        }
    }

    if (filtered.isEmpty()) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = s.get("manage_lang_no_matches", "No translation keys match your filters."),
                    fontFamily = lakemarkFont(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = palette.mut,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
    } else {
        items(filtered, key = { it.key }) { rec ->
            TranslationRecordCard(
                record = rec,
                palette = palette,
                s = s,
                onEdit = { onEditRecord(rec) },
            )
        }
    }
}

@Composable
private fun LangFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    palette: BrandPalette,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) palette.plum else palette.alt)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
    ) {
        Text(
            text = label,
            fontFamily = lakemarkFont(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.5.sp,
            color = if (selected) palette.onPlum else palette.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun TranslationRecordCard(
    record: TranslationRecord,
    palette: BrandPalette,
    s: AppStrings,
    onEdit: () -> Unit,
) {
    val font = lakemarkFont()
    val (statusBg, statusFg) = when (record.status) {
        TranslationStatus.TRANSLATED -> StatusReadyBg to StatusReadyText
        TranslationStatus.UNTRANSLATED, TranslationStatus.OUTDATED -> StatusNewBg to StatusNewText
        TranslationStatus.MISSING -> StatusCancelledBg to StatusCancelledText
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("lang_row_${record.key}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${s.translationCategoryLabel(record.category)} · ${record.key}",
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = palette.mut,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = s.translationStatusLabel(record.status),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = statusFg,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = s.get("manage_lang_edit_title", "Edit translation"),
                        tint = palette.plum,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = record.en,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = palette.text,
            )
            Text(
                text = record.am.ifBlank { "—" },
                fontFamily = com.example.ui.theme.EthiopicFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                color = if (record.am.isBlank()) StatusCancelledText else palette.plum,
            )
        }
    }
}

@Composable
fun TranslationEditorDialog(
    record: TranslationRecord,
    onDismiss: () -> Unit,
    onSave: (en: String, am: String) -> Unit,
    onRestoreDefault: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    var enText by remember(record.key) { mutableStateOf(record.en) }
    var amText by remember(record.key) { mutableStateOf(record.am) }
    var errorMsg by remember(record.key) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.bg,
            border = BorderStroke(2.dp, palette.plum),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .widthIn(max = 480.dp)
                .testTag("translation_editor_dialog"),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = s.get("manage_lang_edit_title", "Edit translation"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = palette.text,
                )
                Text(
                    text = "${s.translationCategoryLabel(record.category)} · ${record.key}",
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = palette.mut,
                )
                OutlinedTextField(
                    value = enText,
                    onValueChange = {
                        enText = it
                        errorMsg = null
                    },
                    label = { Text(s.get("manage_lang_en_label", "English source"), fontFamily = font) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translation_en_input"),
                )
                OutlinedTextField(
                    value = amText,
                    onValueChange = {
                        amText = it
                        errorMsg = null
                    },
                    label = { Text(s.get("manage_lang_am_label", "Amharic translation"), fontFamily = font) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translation_am_input"),
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StatusCancelledText,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (record.isCustomized) {
                        TextButton(onClick = onRestoreDefault) {
                            Text(
                                text = s.get("manage_lang_restore_default", "Restore default"),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                color = palette.mut,
                            )
                        }
                    } else {
                        Box(Modifier.size(1.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(s.get("btn_cancel", "Cancel"), fontFamily = font, color = palette.mut)
                        }
                        Button(
                            onClick = {
                                if (enText.trim().isEmpty()) {
                                    errorMsg = "English text cannot be empty."
                                } else {
                                    onSave(enText.trim(), amText.trim())
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.acc,
                                contentColor = palette.onAcc,
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("translation_save_button"),
                        ) {
                            Text(s.get("btn_save", "Save"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}
