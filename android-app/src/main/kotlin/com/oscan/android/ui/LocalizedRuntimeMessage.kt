package com.oscan.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oscan.android.R

/**
 * Localizes legacy messages still emitted as strings by workflow ViewModels.
 * New workflow messages should use typed states; this boundary prevents raw
 * implementation and exception text from leaking into translated UI.
 */
@Composable
internal fun localizedRuntimeMessage(message: String): String {
    val resource = when {
        message == "This photo could not be added. Remove it or try again." -> R.string.scan_photo_add_failed
        message == "That photo could not be added. Try again." -> R.string.scan_photo_retry
        message == "Enter a document name." -> R.string.scan_name_required
        message == "This image could not be imported. Choose it again or remove it." -> R.string.scan_image_import_failed
        message == "This image could not be prepared. Try again or remove it." -> R.string.scan_image_prepare_failed
        message == "This image could not be opened. Try again or remove it." -> R.string.scan_image_open_failed
        message == "The document could not be saved. Try again." -> R.string.scan_save_failed
        message == "Straightening page…" -> R.string.scan_straightening
        message == "Applying treatment…" -> R.string.scan_applying_treatment
        message == "Saving page to this scan…" -> R.string.scan_saving_page
        message == "Loading page…" -> R.string.scan_loading_page
        message == "Restoring page…" -> R.string.scan_restoring_page
        message == "Saving page updates…" -> R.string.editor_saving_updates
        message == "Cache cleaned" -> R.string.msg_cache_cleaned
        message == "Document moved to Trash" -> R.string.msg_document_trashed
        message.endsWith("documents moved to Trash") -> R.string.msg_documents_trashed
        message == "Folder created" -> R.string.msg_folder_created
        message == "Folder renamed" -> R.string.msg_folder_renamed
        message == "Folder deleted. Documents moved to Unfiled." -> R.string.msg_folder_deleted
        message == "Documents moved" -> R.string.msg_documents_moved
        message == "Added to Favorites" -> R.string.msg_favorite_added
        message == "Removed from Favorites" -> R.string.msg_favorite_removed
        message == "Document restored" -> R.string.msg_document_restored
        message == "Document permanently deleted" -> R.string.msg_document_deleted
        message == "Documents restored" -> R.string.msg_documents_restored
        message == "Documents permanently deleted" -> R.string.msg_documents_deleted
        message == "Trash emptied" -> R.string.msg_trash_emptied
        message == "Page removed" -> R.string.msg_page_removed
        message.endsWith("exported successfully") -> R.string.msg_export_success
        message.startsWith("Export failed") -> R.string.msg_export_failed
        message.startsWith("Share failed") -> R.string.msg_share_failed
        message == "Incorrect passcode" -> R.string.vault_incorrect_passcode
        message == "Vault configured successfully" -> R.string.vault_configured
        message.startsWith("Failed to setup Vault") -> R.string.vault_setup_failed
        message.startsWith("Unlock failed") -> R.string.vault_unlock_failed
        message == "Vault reset completed" -> R.string.vault_reset_complete
        message == "Vault passcode updated" -> R.string.vault_passcode_updated
        message == "Current passcode is incorrect" -> R.string.vault_current_incorrect
        message.startsWith("Failed to change passcode") -> R.string.vault_passcode_change_failed
        message == "Vault disabled" -> R.string.vault_disabled
        message.startsWith("Failed to disable Vault") -> R.string.vault_disable_failed
        message.startsWith("Moved ") && message.endsWith(" to Vault") -> R.string.vault_move_in_complete
        message.startsWith("Failed to move to Vault") -> R.string.vault_move_in_failed
        message.startsWith("Moved document out of Vault") -> R.string.vault_move_out_complete
        message.startsWith("Failed to move out of Vault") -> R.string.vault_move_out_failed
        message == "Unlock Vault before enabling fingerprint unlock" -> R.string.vault_unlock_before_fingerprint
        message == "Fingerprint unlock enabled" -> R.string.vault_fingerprint_enabled
        message.startsWith("Fingerprint unlock failed") -> R.string.vault_fingerprint_failed
        message == "Fingerprint unlock disabled" -> R.string.vault_fingerprint_disabled
        else -> R.string.operation_failed
    }
    return stringResource(resource)
}
