package com.example.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun localDateToString(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
    return date.format(formatter)
}