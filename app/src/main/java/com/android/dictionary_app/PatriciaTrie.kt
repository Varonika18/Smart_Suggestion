package com.android.dictionary_app

// not giving excepted suggestion in error correction case
class PatriciaTrie {
    private inner class TrieNode(
        var word: String = "",
        var frequency: Int = 0,
        var isEndOfWord: Boolean = false
    ) {
        val children = mutableMapOf<Char, TrieNode>()
    }

    private val root = TrieNode()

    fun insert(word: String, frequency: Int) {
        var currentNode = root
        var currentWord = word
        while (currentWord.isNotEmpty()) {
            val firstChar = currentWord[0]
            val child = currentNode.children[firstChar]

            if (child == null) {
                val newNode = TrieNode(currentWord, frequency, isEndOfWord = true)
                currentNode.children[firstChar] = newNode
                return
            }

            val commonPrefixLength = commonPrefixLength(currentWord, child.word)
            if (commonPrefixLength == child.word.length) {
                currentWord = currentWord.substring(commonPrefixLength)
                currentNode = child
            } else {
                val newChild = TrieNode(
                    word = child.word.substring(commonPrefixLength),
                    frequency = child.frequency,
                    isEndOfWord = child.isEndOfWord
                )
                newChild.children.putAll(child.children)

                child.word = child.word.substring(0, commonPrefixLength)
                child.children.clear()
                child.children[newChild.word[0]] = newChild
                child.isEndOfWord = false

                if (currentWord.length > commonPrefixLength) {
                    val remainingWord = currentWord.substring(commonPrefixLength)
                    val newNode = TrieNode(remainingWord, frequency, isEndOfWord = true)
                    child.children[remainingWord[0]] = newNode
                    setEndOfNode(newNode)
                } else {
                    child.isEndOfWord = true
                    child.frequency = frequency
                }
                return
            }
        }
        currentNode.isEndOfWord = true
        currentNode.frequency = frequency
    }

    fun search(prefix: String): List<Pair<String, Int>> {
        val results = mutableSetOf<Pair<String, Int>>()
        searchWithErrors(root, prefix, 0, StringBuilder(), results, 0)
        return results
            .sortedByDescending { it.second }
            .take(20)
    }

    private fun searchWithErrors(
        node: TrieNode,
        prefix: String,
        index: Int,
        currentPrefix: StringBuilder,
        results: MutableSet<Pair<String, Int>>,
        errorCount: Int
    ) {
        if (errorCount > 2) return
        if (index == prefix.length) {
            if (node.isEndOfWord) {
                val word = mergeStrings(currentPrefix.toString(), node.word)
                val score = node.frequency / (errorCount + 1)
                results.add(word to score)
            }
            collectSuggestions(node, currentPrefix, results, errorCount)
            return
        }

        val currentChar = prefix.getOrNull(index) ?: return

        node.children[currentChar]?.let { child ->
            currentPrefix.append(child.word)
            searchWithErrors(child, prefix, index + child.word.length, currentPrefix, results, errorCount)
            currentPrefix.delete(currentPrefix.length - child.word.length, currentPrefix.length)
            return
        }

        for ((key, child) in node.children) {
            currentPrefix.append(child.word)
            searchWithErrors(child, prefix, index + child.word.length, currentPrefix, results, errorCount + 1)
            currentPrefix.delete(currentPrefix.length - child.word.length, currentPrefix.length)
        }

        searchWithErrors(node, prefix, index + 1, currentPrefix, results, errorCount + 1)
    }

    private fun collectSuggestions(
        node: TrieNode,
        prefix: StringBuilder,
        results: MutableSet<Pair<String, Int>>,
        errorCount: Int
    ) {
        if (node.isEndOfWord) {
            val word = mergeStrings(prefix.toString(), node.word)
            val score = node.frequency / (errorCount + 1)
            results.add(word to score)
        }
        for (child in node.children.values) {
            val newPrefix = prefix.append(child.word)
            collectSuggestions(child, newPrefix, results, errorCount)
            prefix.delete(prefix.length - child.word.length, prefix.length)
        }
    }



    private fun commonPrefixLength(word1: String, word2: String): Int {
        val minLength = minOf(word1.length, word2.length)
        var i = 0
        while (i < minLength && word1[i] == word2[i]) {
            i++
        }
        return i
    }

    private fun setEndOfNode(node: TrieNode) {
        if (node.children.isNotEmpty()) {
            val lastChild = node.children.values.last()
            node.isEndOfWord = true
            setEndOfNode(lastChild)
        } else {
            node.isEndOfWord = true
        }
    }
    fun mergeStrings(string1: String, string2: String): String {
        var overlapIndex = 0

        for (i in string1.indices) {
            if (string2.startsWith(string1.substring(i))) {
                overlapIndex = i
                break
            }
        }

        return string1.substring(0, overlapIndex) + string2
    }
}
