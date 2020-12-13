package com.mapapp.intent

sealed class UserIntent {

    object FetchBulkAttachments : UserIntent()
    data class DeleteCheckin(val id: Int) : UserIntent()

}