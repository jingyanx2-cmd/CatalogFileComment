package com.annotation.catalogfilecomment.config


data class FileCommentState(
    var comments: MutableMap<String, String> = mutableMapOf()
)