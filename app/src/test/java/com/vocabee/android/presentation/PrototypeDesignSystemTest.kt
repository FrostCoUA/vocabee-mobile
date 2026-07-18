package com.vocabee.android.core.presentation.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the redesign expansion of the design-system catalogs:
 * - dictionary cover accents 8 → 12 (first 8 must stay stable for persisted coverIndex),
 * - topic icons 15 → 20 (persisted iconIndex follows the append-only canonical storage order,
 *   the redesign picker order is a display-only permutation).
 */
class PrototypeDesignSystemTest {

    private val legacyAccents = listOf(
        "indigo" to Color(0xFF4F46E5),
        "blue" to Color(0xFF5B7BFE),
        "violet" to Color(0xFF7C5CF6),
        "grape" to Color(0xFF410FA3),
        "royal" to Color(0xFF3E63DD),
        "plum" to Color(0xFF9333EA),
        "teal" to Color(0xFF0E9FA5),
        "amber" to Color(0xFFE0820C),
    )

    private val legacyIconOrder = listOf(
        PrototypeIcon.Plane,
        PrototypeIcon.Book,
        PrototypeIcon.Film,
        PrototypeIcon.Brief,
        PrototypeIcon.Grad,
        PrototypeIcon.Food,
        PrototypeIcon.Ball,
        PrototypeIcon.Music,
        PrototypeIcon.Leaf,
        PrototypeIcon.Laptop,
        PrototypeIcon.Bag,
        PrototypeIcon.Heart,
        PrototypeIcon.Child,
        PrototypeIcon.Chat,
        PrototypeIcon.Star,
    )

    @Test
    fun topicThemesContainTwelveRedesignAccents() {
        assertEquals(12, PrototypeTopicThemes.size)
    }

    @Test
    fun firstEightAccentsKeepLegacyOrderAndColors() {
        legacyAccents.forEachIndexed { index, (key, color) ->
            assertEquals(key, PrototypeTopicThemes[index].key)
            assertEquals(color, PrototypeTopicThemes[index].color)
        }
    }

    @Test
    fun newAccentsAppendedAfterAmberInRedesignOrder() {
        assertEquals("rose", PrototypeTopicThemes[8].key)
        assertEquals(Color(0xFFD6336C), PrototypeTopicThemes[8].color)
        assertEquals("emerald", PrototypeTopicThemes[9].key)
        assertEquals(Color(0xFF17845A), PrototypeTopicThemes[9].color)
        assertEquals("navy", PrototypeTopicThemes[10].key)
        assertEquals(Color(0xFF1E40AF), PrototypeTopicThemes[10].color)
        assertEquals("graphite", PrototypeTopicThemes[11].key)
        assertEquals(Color(0xFF52525B), PrototypeTopicThemes[11].color)
    }

    @Test
    fun coverIndexResolvesModuloTwelve() {
        assertEquals(PrototypeTopicThemes[0], prototypeTopicTheme(12))
        assertEquals(PrototypeTopicThemes[8], prototypeTopicTheme(20))
        assertEquals(PrototypeTopicThemes[11], prototypeTopicTheme(-1))
    }

    @Test
    fun topicIconsKeepLegacyStoragePrefixAndAppendNewOnes() {
        assertEquals(20, PrototypeTopicIcons.size)
        assertEquals(legacyIconOrder, PrototypeTopicIcons.subList(0, legacyIconOrder.size))
    }

    /** Ремап 15 → 20: старі персистентні iconIndex мусять резолвитись у ТІ САМІ іконки. */
    @Test
    fun legacyIconIndicesResolveToSameIconsAfterExpansion() {
        legacyIconOrder.forEachIndexed { index, icon ->
            assertEquals("iconIndex=$index", icon, prototypeTopicIcon(index))
        }
    }

    @Test
    fun appendedIconIndicesResolveToNewIcons() {
        assertEquals(PrototypeIcon.Car, prototypeTopicIcon(15))
        assertEquals(PrototypeIcon.Burger, prototypeTopicIcon(16))
        assertEquals(PrototypeIcon.Drink, prototypeTopicIcon(17))
        assertEquals(PrototypeIcon.Cat, prototypeTopicIcon(18))
        assertEquals(PrototypeIcon.Dog, prototypeTopicIcon(19))
    }

    @Test
    fun pickerOrderMatchesRedesignIconTopics() {
        val expected = listOf(
            PrototypeIcon.Plane,
            PrototypeIcon.Car,
            PrototypeIcon.Book,
            PrototypeIcon.Film,
            PrototypeIcon.Music,
            PrototypeIcon.Ball,
            PrototypeIcon.Grad,
            PrototypeIcon.Brief,
            PrototypeIcon.Laptop,
            PrototypeIcon.Food,
            PrototypeIcon.Burger,
            PrototypeIcon.Drink,
            PrototypeIcon.Bag,
            PrototypeIcon.Cat,
            PrototypeIcon.Dog,
            PrototypeIcon.Leaf,
            PrototypeIcon.Heart,
            PrototypeIcon.Child,
            PrototypeIcon.Chat,
            PrototypeIcon.Star,
        )
        assertEquals(expected, PrototypeTopicIconsPickerOrder)
    }

    /** Порядок пікера — лише перестановка канонічного списку; збереження йде через storage index. */
    @Test
    fun pickerSelectionsRoundTripThroughStorageIndex() {
        assertEquals(PrototypeTopicIcons.toSet(), PrototypeTopicIconsPickerOrder.toSet())
        PrototypeTopicIconsPickerOrder.forEach { icon ->
            assertEquals(icon, prototypeTopicIcon(prototypeTopicIconStorageIndex(icon)))
        }
    }

    /**
     * Головний запобіжник масового переносу геометрії з `rd-base.js`: жодна іконка не сміє
     * лишитись без даних (порожній `d`, битий шлях на кшталт `d="Layer_1"`, нульовий радіус) —
     * інакше вона мовчки рендериться порожнім місцем.
     */
    @Test
    fun everyIconHasNonEmptyGeometry() {
        PrototypeIcon.entries.forEach { icon ->
            val shapes = prototypeIconShapes(icon)
            assertTrue("$icon: порожній набір елементів", shapes.isNotEmpty())
            shapes.forEach { shape ->
                when (shape) {
                    is PrototypeIconShape.SvgPath -> {
                        val nodes = PathParser().parsePathString(shape.pathData).toNodes()
                        // Один самотній moveTo нічого не малює — потрібна хоч одна команда після нього.
                        assertTrue("$icon: шлях без команд малювання", nodes.size > 1)
                    }
                    is PrototypeIconShape.SvgRect -> {
                        assertTrue("$icon: rect без площі", shape.width > 0f && shape.height > 0f)
                    }
                    is PrototypeIconShape.SvgCircle -> {
                        assertTrue("$icon: circle з нульовим радіусом", shape.radius > 0f)
                    }
                }
            }
        }
    }

    /** Іконки таб-бару та пікера тем мусять існувати в наборі (борд: bookTab/dumbbell/user|userF). */
    @Test
    fun redesignTabAndTopicIconsArePresent() {
        val required = listOf(
            PrototypeIcon.BookTab,
            PrototypeIcon.Dumbbell,
            PrototypeIcon.User,
            PrototypeIcon.UserF,
        ) + PrototypeTopicIcons
        required.forEach { icon ->
            assertTrue("$icon: немає геометрії", prototypeIconShapes(icon).isNotEmpty())
        }
    }
}
