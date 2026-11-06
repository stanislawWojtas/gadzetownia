package com.example.catalog.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Gotowe w obu wersjach: odmiana „produkt / produkty / produktów”. */
class PolishPluralTest {

    private fun form(n: Int) = Formatters.polishPlural(n, "produkt", "produkty", "produktów")

    @Test
    fun polishForms() {
        assertEquals("produktów", form(0))
        assertEquals("produkt", form(1))
        assertEquals("produkty", form(2))
        assertEquals("produktów", form(5))
        assertEquals("produktów", form(12))
        assertEquals("produkty", form(22))
        assertEquals("produkty", form(194))
        assertEquals("produktów", form(112))
    }
}
