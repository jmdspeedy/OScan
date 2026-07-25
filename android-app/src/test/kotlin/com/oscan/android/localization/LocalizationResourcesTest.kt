package com.oscan.android.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationResourcesTest {

    private val resDir = File("src/main/res")

    private fun loadResourceKeysAndPlaceholders(stringsFile: File): Map<String, List<String>> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(stringsFile)

        val resultMap = mutableMapOf<String, List<String>>()

        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            val translatable = node.attributes.getNamedItem("translatable")?.nodeValue
            if (translatable == "false") continue
            val textContent = node.textContent
            val placeholders = extractPlaceholders(textContent)
            resultMap[name] = placeholders
        }

        val pluralNodes = doc.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val node = pluralNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            val textContent = node.textContent
            val placeholders = extractPlaceholders(textContent)
            resultMap["plural:$name"] = placeholders
        }

        return resultMap
    }

    private fun extractPlaceholders(text: String): List<String> {
        val regex = Regex("%[0-9]+\\$[a-zA-Z]|%[a-zA-Z]")
        return regex.findAll(text).map { it.value }.distinct().sorted().toList()
    }

    @Test
    fun allEnglishResourceKeysExistInChineseAndJapanese() {
        val enFile = File(resDir, "values/strings.xml")
        val zhFile = File(resDir, "values-zh-rCN/strings.xml")
        val jaFile = File(resDir, "values-ja/strings.xml")

        assertTrue("English strings.xml must exist", enFile.exists())
        assertTrue("Chinese strings.xml must exist", zhFile.exists())
        assertTrue("Japanese strings.xml must exist", jaFile.exists())

        val enResources = loadResourceKeysAndPlaceholders(enFile)
        val zhResources = loadResourceKeysAndPlaceholders(zhFile)
        val jaResources = loadResourceKeysAndPlaceholders(jaFile)

        val missingInZh = enResources.keys - zhResources.keys
        val missingInJa = enResources.keys - jaResources.keys

        assertTrue("Missing Chinese keys: $missingInZh", missingInZh.isEmpty())
        assertTrue("Missing Japanese keys: $missingInJa", missingInJa.isEmpty())
    }

    @Test
    fun formatPlaceholdersMatchAcrossLocales() {
        val enFile = File(resDir, "values/strings.xml")
        val zhFile = File(resDir, "values-zh-rCN/strings.xml")
        val jaFile = File(resDir, "values-ja/strings.xml")

        val enResources = loadResourceKeysAndPlaceholders(enFile)
        val zhResources = loadResourceKeysAndPlaceholders(zhFile)
        val jaResources = loadResourceKeysAndPlaceholders(jaFile)

        for ((key, enPlaceholders) in enResources) {
            val zhPlaceholders = zhResources[key] ?: continue
            val jaPlaceholders = jaResources[key] ?: continue

            assertEquals(
                "Placeholder mismatch in Chinese for key: $key",
                enPlaceholders,
                zhPlaceholders
            )
            assertEquals(
                "Placeholder mismatch in Japanese for key: $key",
                enPlaceholders,
                jaPlaceholders
            )
        }
    }

    @Test
    fun composeUiDoesNotContainHardCodedUserFacingText() {
        val uiDir = File("src/main/kotlin/com/oscan/android/ui")
        val forbidden = listOf(
            Regex("""Text\s*\(\s*"([^"]+)""""),
            Regex("""contentDescription\s*=\s*"([^"]+)""""),
            Regex("""supportingText\s*=\s*"([^"]+)""""),
            Regex("""placeholder\s*=\s*\{\s*Text\s*\(\s*"([^"]+)"""")
        )
        val allowed = setOf("✓")
        val violations = mutableListOf<String>()

        uiDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    forbidden.forEach { regex ->
                        regex.findAll(line).forEach { match ->
                            val literal = match.groupValues[1]
                            if (literal !in allowed) {
                                violations += "${file.name}:${index + 1}: $literal"
                            }
                        }
                    }
                }
            }

        assertTrue(
            "Hard-coded user-facing Compose text must use string resources:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
