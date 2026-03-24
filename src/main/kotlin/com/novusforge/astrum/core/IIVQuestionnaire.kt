package com.novusforge.astrum.core

import java.awt.*
import java.io.*
import java.util.*
import javax.swing.*
import javax.swing.Timer

/**
 * IIVQuestionnaire: Identity Intent Verification pre-entry questionnaire.
 * Powering the "Fort Knox" engine with behavioral intent validation.
 * Kotlin conversion: Using primary constructor, data classes, and idiomatic Swing construction.
 */
class IIVQuestionnaire : JFrame() {

    companion object {
        private val RESULT_FILE = "${System.getProperty("user.home")}/iiv_result.dat"
        
        // Brand Colors
        private val BG_COLOR = Color(0x1A1A2E)
        private val ACCENT_COLOR = Color(0xC9A84C) // Gold
        private val TEXT_COLOR = Color(0xEAEAEA)
        private val BUTTON_COLOR = Color(0x1A3A5C)
        
        private val SUCCESS_COLOR = Color(0x2ECC71) // Green
        private val WARNING_COLOR = Color(0xC9A84C) // Gold
        private val DANGER_COLOR = Color(0xE74C3C)  // Red
    }

    private var currentQuestionIndex = 0
    private var totalScore = 0
    
    private val questions = mutableListOf<Question>()
    private val buttonGroup = ButtonGroup()
    private val options = arrayOfNulls<JRadioButton>(4)
    private val questionLabel = JLabel()
    private val progressBar = JProgressBar(0, 12)
    private val nextButton = JButton("Next")

    init {
        setupData()
        
        // Check if already completed
        val result = loadResult()
        if (result != null) {
            if (result.decision != Decision.BLOCK) {
                launchGame()
            } else {
                showResults(result) // Show denial again
            }
        } else {
            setupUI()
        }
    }

    private fun setupUI() {
        title = "Project Astrum — Identity Verification"
        size = Dimension(700, 600)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        
        val content = JPanel(BorderLayout(20, 20)).apply {
            background = BG_COLOR
            border = BorderFactory.createEmptyBorder(30, 40, 30, 40)
        }

        // Header: Progress
        progressBar.apply {
            value = 0
            foreground = ACCENT_COLOR
            background = BUTTON_COLOR
            isStringPainted = true
        }
        content.add(progressBar, BorderLayout.NORTH)

        // Center: Question and Options
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = BG_COLOR
        }

        questionLabel.apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.BOLD, 18)
        }
        centerPanel.add(questionLabel)
        centerPanel.add(Box.createVerticalStrut(30))

        for (i in 0 until 4) {
            options[i] = JRadioButton().apply {
                background = BG_COLOR
                foreground = TEXT_COLOR
                font = Font("Arial", Font.PLAIN, 16)
                addActionListener { nextButton.isEnabled = true }
                buttonGroup.add(this)
            }
            centerPanel.add(options[i])
            centerPanel.add(Box.createVerticalStrut(10))
        }
        content.add(centerPanel, BorderLayout.CENTER)

        // Footer: Next
        nextButton.apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            isFocusPainted = false
            font = Font("Arial", Font.BOLD, 16)
            isEnabled = false
            addActionListener { nextQuestion() }
        }
        content.add(nextButton, BorderLayout.SOUTH)

        add(content)
        displayQuestion()
        isVisible = true
    }

    private fun displayQuestion() {
        val q = questions[currentQuestionIndex]
        questionLabel.text = "<html><div style='width: 500px;'>${currentQuestionIndex + 1}. ${q.text}</div></html>"
        buttonGroup.clearSelection()
        nextButton.isEnabled = false
        
        for (i in 0 until 4) {
            options[i]?.text = q.options[i]
        }
        progressBar.value = currentQuestionIndex
    }

    private fun nextQuestion() {
        // Calculate score
        val q = questions[currentQuestionIndex]
        for (i in 0 until 4) {
            if (options[i]?.isSelected == true) {
                totalScore += q.scores[i]
                break
            }
        }

        currentQuestionIndex++
        if (currentQuestionIndex < questions.size) {
            displayQuestion()
        } else {
            processResults()
        }
    }

    private fun processResults() {
        val decision = when {
            totalScore <= 5 -> Decision.ALLOW
            totalScore <= 12 -> Decision.WARN
            else -> Decision.BLOCK
        }

        val result = IIVResult(decision, totalScore)
        
        // SAVE RESULT IMMEDIATELY AND FORCE FLUSH
        saveResult(result)
        
        // Verify file was created
        val file = File(RESULT_FILE)
        if (!file.exists()) {
            System.err.println("[IIV] ERROR: Failed to save result file!")
        } else {
            println("[IIV] Result saved to: $RESULT_FILE")
            println("[IIV] Decision: $decision | Score: $totalScore")
        }
        
        showResults(result)
    }

    private fun showResults(res: IIVResult) {
        contentPane.removeAll()
        val panel = JPanel(GridBagLayout()).apply {
            background = BG_COLOR
        }
        val gbc = GridBagConstraints().apply {
            gridx = 0
            insets = Insets(10, 10, 10, 10)
        }

        panel.add(JLabel("Your Score: ${res.score}").apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.BOLD, 24)
        }, gbc)

        val accent = when (res.decision) {
            Decision.ALLOW -> SUCCESS_COLOR
            Decision.WARN -> WARNING_COLOR
            Decision.BLOCK -> DANGER_COLOR
        }
        panel.add(JLabel("Result: ${res.decision}").apply {
            foreground = accent
            font = Font("Arial", Font.BOLD, 28)
        }, gbc)

        val msg = when (res.decision) {
            Decision.ALLOW -> "Identity Verification passed. Welcome to Astrum."
            Decision.WARN -> "Some responses flagged for review. You may enter but your session will be monitored."
            Decision.BLOCK -> "Entry denied. Your responses indicate behavior patterns incompatible with the Astrum community standards."
        }
        panel.add(JLabel("<html><div style='text-align: center; width: 400px;'>$msg</div></html>").apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 18)
        }, gbc)

        if (res.decision != Decision.BLOCK) {
            panel.add(JLabel("Launching game shortly...").apply {
                foreground = ACCENT_COLOR
            }, gbc)
            revalidate()
            repaint()
            
            Timer(if (res.decision == Decision.ALLOW) 2000 else 3000) { launchGame() }.start()
        } else {
            panel.add(JButton("Close").apply {
                background = BUTTON_COLOR
                foreground = TEXT_COLOR
                addActionListener { System.exit(0) }
            }, gbc)
            revalidate()
            repaint()
        }
        
        add(panel)
        isVisible = true
    }

    private fun launchGame() {
        println("[IIV] Launching Project Astrum...")
        dispose()
    }

    private fun saveResult(result: IIVResult) {
        try {
            val file = File(RESULT_FILE)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            
            ObjectOutputStream(FileOutputStream(file)).use { oos ->
                oos.writeObject(result)
                oos.flush()
            }
            
            println("[IIV] Result file saved: ${file.absolutePath}")
        } catch (e: IOException) {
            System.err.println("[IIV] FAILED to save result: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadResult(): IIVResult? {
        val file = File(RESULT_FILE)
        if (!file.exists()) return null
        return try {
            ObjectInputStream(FileInputStream(file)).use { it.readObject() as IIVResult }
        } catch (e: Exception) {
            null
        }
    }

    private fun setupData() {
        // Category 1: Cheating and Macros
        questions.add(Question("If you discovered a timing exploit that gave you an unfair advantage in competitive play what would you do?",
            arrayOf("Use it to win, everyone does it", "Use it privately where no one can see", "Report it to the developers", "Ignore it and play normally"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("How do you feel about players who use macros or automated inputs in competitive modes?",
            arrayOf("It is a valid strategy if you can get away with it", "It is fine as long as it is not obvious", "It is unfair and ruins the experience for everyone", "I have never thought about it"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("If your account was flagged for suspicious timing consistency in 90 percent of your actions what would you do?",
            arrayOf("Deny everything and blame the system", "Try to hide it better next time", "Accept the review and cooperate", "Stop the behavior immediately"),
            intArrayOf(3, 3, 0, 0)))

        // Category 2: Griefing and Server Destruction
        questions.add(Question("You join a server and find an unprotected base full of another player's work. What do you do?",
            arrayOf("Destroy everything, it is their fault for not protecting it", "Take some valuable items and leave", "Leave it alone or tell them it is unprotected", "Help them protect it"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("A group of friends invites you to join a coordinated raid to destroy a large community server. What do you do?",
            arrayOf("Join immediately, it sounds fun", "Watch but do not participate", "Refuse and report the plan to server staff", "Warn the server community"),
            intArrayOf(3, 1, 0, 0)))
        questions.add(Question("How do you feel about organized griefing groups like those that have targeted Minecraft servers historically?",
            arrayOf("They are legendary and impressive", "It is just a game, no real harm done", "It is toxic and ruins communities", "I have no interest in that kind of play"),
            intArrayOf(3, 2, 0, 0)))

        // Category 3: Minor Safety and Grooming
        questions.add(Question("A much younger player messages you frequently and seems to look up to you. What do you do?",
            arrayOf("Take advantage of the attention and build a private relationship", "Keep chatting privately and ask personal questions", "Be friendly in public chats but maintain normal boundaries", "Encourage them to play with people their own age"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("Someone asks you to move a conversation from the game chat to a private platform like Discord or phone. What do you do?",
            arrayOf("Always agree, private conversations are fine", "Agree if they seem trustworthy", "Keep all conversations in the game where they are visible", "Report the request to a moderator if it feels wrong"),
            intArrayOf(2, 1, 0, 0)))
        questions.add(Question("How do you feel about players asking others their age in game chat?",
            arrayOf("It is normal and fine to ask anyone", "It depends on context", "It can be a warning sign and should be monitored", "It should be blocked by default in public chat"),
            intArrayOf(2, 1, 0, 0)))

        // Category 4: Identity and Honesty
        questions.add(Question("Would you ever create a fake persona including a different name, gender, or voice to gain an advantage or reputation in a game community?",
            arrayOf("Yes if it helped me win or gain followers", "Maybe for fun or to avoid harassment", "No, I prefer to be known as I actually am", "I have never considered it"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("If you were caught using a fake identity to deceive a community for over a year what would you do?",
            arrayOf("Double down and deny everything", "Quietly disappear and start over with a new account", "Come clean publicly and apologize", "I would never do this in the first place"),
            intArrayOf(3, 2, 0, 0)))
        questions.add(Question("How important is honesty and transparency in online gaming communities?",
            arrayOf("Not important, it is just a game", "Somewhat important but winning matters more", "Very important, communities depend on trust", "Essential, I hold myself to this standard"),
            intArrayOf(2, 1, 0, 0)))
    }

    private data class Question(val text: String, val options: Array<String>, val scores: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Question) return false
            if (text != other.text) return false
            if (!options.contentEquals(other.options)) return false
            if (!scores.contentEquals(other.scores)) return false
            return true
        }
        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + options.contentHashCode()
            result = 31 * result + scores.contentHashCode()
            return result
        }
    }

    enum class Decision { ALLOW, WARN, BLOCK }

    data class IIVResult(val decision: Decision, val score: Int) : Serializable
}
