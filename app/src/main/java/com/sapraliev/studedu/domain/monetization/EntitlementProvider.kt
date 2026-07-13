package com.sapraliev.studedu.domain.monetization

/**
 * Хук монетизации: единственное место, где код спрашивает «а можно?».
 * Никаких хардкодов лимитов по приложению — только через этот интерфейс.
 *
 * Сейчас всё бесплатно. Когда (и если) появится Pro через RuStore Billing,
 * добавится реализация поверх подписки — вызывающий код не меняется.
 */
interface EntitlementProvider {

    /** Максимум активных учеников; null — безлимит. */
    fun studentLimit(): Int?

    /** Доступны ли дополнительные темы оформления. */
    fun customThemesEnabled(): Boolean

    /** Доступен ли экспорт отчётов по ученикам. */
    fun exportEnabled(): Boolean
}

/** Реализация по умолчанию: всё разрешено. */
object AllFreeEntitlements : EntitlementProvider {
    override fun studentLimit(): Int? = null
    override fun customThemesEnabled(): Boolean = true
    override fun exportEnabled(): Boolean = true
}
