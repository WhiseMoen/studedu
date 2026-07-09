package com.sapraliev.studedu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Клиент Supabase (проект WhiseMoen's Project).
 * Publishable-ключ не секретен (защита данных — через RLS),
 * его можно спокойно хранить в клиентском коде.
 */
object Supabase {

    private const val SUPABASE_URL = "https://mgaogctkxpqnybtlxinp.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_xMiwK07JY7xaZru4ntWjkg_9sEHeCgx"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
