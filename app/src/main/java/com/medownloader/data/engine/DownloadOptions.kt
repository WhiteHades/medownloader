package com.medownloader.data.engine

data class DownloadOptions(
    val url: String,
    val filename: String? = null,
    val protocolType: EngineType = ProtocolRouter.route(url)
)
