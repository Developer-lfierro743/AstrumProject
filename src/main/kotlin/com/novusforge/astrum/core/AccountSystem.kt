package com.novusforge.astrum.core

import java.awt.*
import java.awt.geom.Ellipse2D
import java.sql.DriverManager
import java.sql.SQLException
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * AccountSystem - User authentication and registration
 * Kotlin conversion: Using Swing with idiomatic Kotlin construction and JDBC.
 */
class AccountSystem : JFrame() {
    
    companion object {
        private val DB_URL = "jdbc:sqlite:${System.getProperty("user.home")}/astrum_accounts.db"
        
        // Brand Colors
        private val BG_COLOR = Color(0x1A1A2E)
        private val ACCENT_COLOR = Color(0xC9A84C) // Gold
        private val TEXT_COLOR = Color(0xEAEAEA)
        private val BUTTON_COLOR = Color(0x1A3A5C)
        private val ERROR_COLOR = Color(0xE74C3C)
        private val WARN_COLOR = Color(0xF1C40F)
        private val SUCCESS_COLOR = Color(0x2ECC71)

        private val AVATAR_COLORS = arrayOf(
            Color(0x3498DB), Color(0x2ECC71), Color(0xE74C3C),
            Color(0xF39C12), Color(0x9B59B6), Color(0x1ABC9C)
        )
    }

    private enum class Mode { LOGIN, REGISTER }
    private var currentMode = Mode.LOGIN

    private lateinit var headerLabel: JLabel
    private lateinit var usernameField: JTextField
    private lateinit var passwordField: JPasswordField
    private lateinit var confirmPasswordLabel: JLabel
    private lateinit var confirmPasswordField: JPasswordField
    private lateinit var avatarLabel: JLabel
    private lateinit var avatarPanel: JPanel
    private lateinit var statusLabel: JLabel
    private lateinit var usernameStatusLabel: JLabel
    private lateinit var actionButton: JButton
    private lateinit var switchButton: JButton
    
    private var selectedAvatarId = 0
    private val avatarButtons = mutableListOf<AvatarButton>()
    private var usernameFlagged = false

    init {
        initDatabase()
        setupUI()
    }

    private fun initDatabase() {
        try {
            DriverManager.getConnection(DB_URL).use { conn ->
                conn.createStatement().use { stmt ->
                    val sql = """
                        CREATE TABLE IF NOT EXISTS accounts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT UNIQUE NOT NULL,
                            password_hash TEXT NOT NULL,
                            avatar_id INTEGER DEFAULT 0,
                            iiv_result TEXT DEFAULT 'ALLOW',
                            flagged_username BOOLEAN DEFAULT 0,
                            created_at TEXT DEFAULT CURRENT_TIMESTAMP
                        );
                    """.trimIndent()
                    stmt.execute(sql)
                    
                    // Migration for existing databases
                    try {
                        stmt.execute("ALTER TABLE accounts ADD COLUMN flagged_username BOOLEAN DEFAULT 0;")
                    } catch (ignored: SQLException) {}
                }
            }
        } catch (e: SQLException) {
            System.err.println("[Database] Error: ${e.message}")
        }
    }

    private fun setupUI() {
        title = "Project Astrum — Account"
        size = Dimension(700, 500)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = DO_NOTHING_ON_CLOSE

        val content = JPanel(BorderLayout(20, 20)).apply {
            background = BG_COLOR
            border = EmptyBorder(40, 60, 40, 60)
        }

        val formPanel = JPanel(GridBagLayout()).apply {
            background = BG_COLOR
        }
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            gridx = 0
        }

        // Header
        headerLabel = JLabel("Welcome Back to Astrum").apply {
            font = Font("Arial", Font.BOLD, 24)
            foreground = ACCENT_COLOR
            horizontalAlignment = SwingConstants.CENTER
        }
        gbc.gridy = 0
        formPanel.add(headerLabel, gbc)

        // Username
        gbc.gridy++
        formPanel.add(JLabel("Username").apply { foreground = TEXT_COLOR }, gbc)

        gbc.gridy++
        val uFieldPanel = JPanel(BorderLayout(5, 0)).apply {
            background = BG_COLOR
        }
        usernameField = JTextField().apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            caretColor = TEXT_COLOR
            border = BorderFactory.createLineBorder(ACCENT_COLOR)
        }
        uFieldPanel.add(usernameField, BorderLayout.CENTER)
        
        usernameStatusLabel = JLabel("").apply {
            preferredSize = Dimension(200, 20)
            font = Font("Arial", Font.PLAIN, 10)
        }
        uFieldPanel.add(usernameStatusLabel, BorderLayout.EAST)
        formPanel.add(uFieldPanel, gbc)

        // Password
        gbc.gridy++
        formPanel.add(JLabel("Password").apply { foreground = TEXT_COLOR }, gbc)

        gbc.gridy++
        passwordField = JPasswordField().apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            caretColor = TEXT_COLOR
            border = BorderFactory.createLineBorder(ACCENT_COLOR)
        }
        formPanel.add(passwordField, gbc)

        // Confirm Password (Register only)
        gbc.gridy++
        confirmPasswordLabel = JLabel("Confirm Password").apply {
            foreground = TEXT_COLOR
            isVisible = false
        }
        formPanel.add(confirmPasswordLabel, gbc)

        gbc.gridy++
        confirmPasswordField = JPasswordField().apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            caretColor = TEXT_COLOR
            border = BorderFactory.createLineBorder(ACCENT_COLOR)
            isVisible = false
        }
        formPanel.add(confirmPasswordField, gbc)

        // Avatar Selection (Register only)
        gbc.gridy++
        avatarLabel = JLabel("Select Avatar").apply {
            foreground = TEXT_COLOR
            isVisible = false
        }
        formPanel.add(avatarLabel, gbc)

        gbc.gridy++
        avatarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply {
            background = BG_COLOR
            isVisible = false
            for (i in AVATAR_COLORS.indices) {
                val ab = AvatarButton(i, AVATAR_COLORS[i])
                avatarButtons.add(ab)
                add(ab)
            }
        }
        formPanel.add(avatarPanel, gbc)

        // Status/Error
        gbc.gridy++
        statusLabel = JLabel(" ").apply {
            foreground = ERROR_COLOR
            horizontalAlignment = SwingConstants.CENTER
        }
        formPanel.add(statusLabel, gbc)

        content.add(formPanel, BorderLayout.CENTER)

        // Buttons
        val southPanel = JPanel(GridLayout(2, 1, 0, 10)).apply {
            background = BG_COLOR
        }

        actionButton = JButton("Login").apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            isFocusPainted = false
            addActionListener { handleAction() }
        }
        southPanel.add(actionButton)

        switchButton = JButton("Switch to Register").apply {
            background = BG_COLOR
            foreground = ACCENT_COLOR
            border = null
            addActionListener { toggleMode() }
        }
        southPanel.add(switchButton)

        content.add(southPanel, BorderLayout.SOUTH)

        add(content)

        // Listeners for live validation
        val dl = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { validateForm() }
            override fun removeUpdate(e: DocumentEvent) { validateForm() }
            override fun changedUpdate(e: DocumentEvent) { validateForm() }
        }
        usernameField.document.addDocumentListener(dl)
        passwordField.document.addDocumentListener(dl)
        confirmPasswordField.document.addDocumentListener(dl)

        isVisible = true
    }

    private fun toggleMode() {
        currentMode = if (currentMode == Mode.LOGIN) Mode.REGISTER else Mode.LOGIN
        val isReg = (currentMode == Mode.REGISTER)
        
        title = if (isReg) "Project Astrum — Register" else "Project Astrum — Login"
        headerLabel.text = if (isReg) "Join Astrum" else "Welcome Back to Astrum"
        
        confirmPasswordLabel.isVisible = isReg
        confirmPasswordField.isVisible = isReg
        avatarLabel.isVisible = isReg
        avatarPanel.isVisible = isReg
        
        actionButton.text = if (isReg) "Register" else "Login"
        switchButton.text = if (isReg) "Switch to Login" else "Switch to Register"
        
        statusLabel.text = " "
        validateForm()
    }

    private fun validateForm() {
        if (currentMode == Mode.LOGIN) {
            actionButton.isEnabled = usernameField.text.isNotEmpty() && passwordField.password.isNotEmpty()
            usernameStatusLabel.text = ""
            return
        }

        val user = usernameField.text
        val pass = String(passwordField.password)
        val confirm = String(confirmPasswordField.password)

        var userValid = user.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
        usernameFlagged = false

        if (userValid) {
            if (isUsernameTaken(user)) {
                usernameStatusLabel.text = "<html><font color='red'>Username taken</font></html>"
                userValid = false
            } else {
                // Identity Filter Check
                val filter = CreatorIdentityFilter.check(user)
                when (filter.severity) {
                    CreatorIdentityFilter.FilterSeverity.BLOCK -> {
                        usernameStatusLabel.text = "<html><font color='red'>${filter.reason}</font></html>"
                        userValid = false
                    }
                    CreatorIdentityFilter.FilterSeverity.WARN -> {
                        usernameStatusLabel.text = "<html><font color='orange'>${filter.reason}</font></html>"
                        usernameFlagged = true
                    }
                    else -> {
                        usernameStatusLabel.text = "<html><font color='green'>\u2713 Available</font></html>"
                    }
                }
            }
        } else {
            usernameStatusLabel.text = if (user.isEmpty()) "" else "<html><font color='red'>Invalid format</font></html>"
        }

        val passValid = pass.length >= 6
        val match = pass == confirm && pass.isNotEmpty()
        
        confirmPasswordField.border = BorderFactory.createLineBorder(if (match || confirm.isEmpty()) ACCENT_COLOR else ERROR_COLOR)

        actionButton.isEnabled = userValid && passValid && match
    }

    private fun isUsernameTaken(username: String): Boolean {
        return try {
            DriverManager.getConnection(DB_URL).use { conn ->
                conn.prepareStatement("SELECT 1 FROM accounts WHERE username = ?").use { pstmt ->
                    pstmt.setString(1, username)
                    pstmt.executeQuery().use { rs -> rs.next() }
                }
            }
        } catch (e: SQLException) {
            false
        }
    }

    private fun handleAction() {
        val user = usernameField.text
        val pass = String(passwordField.password)
        val hash = HashUtils.computeSHA256(pass.toByteArray())

        if (currentMode == Mode.REGISTER) {
            try {
                DriverManager.getConnection(DB_URL).use { conn ->
                    conn.prepareStatement("INSERT INTO accounts(username, password_hash, avatar_id, flagged_username) VALUES(?, ?, ?, ?)").use { pstmt ->
                        pstmt.setString(1, user)
                        pstmt.setString(2, hash)
                        pstmt.setInt(3, selectedAvatarId)
                        pstmt.setBoolean(4, usernameFlagged)
                        pstmt.executeUpdate()
                        
                        SessionManager.saveSession(user, selectedAvatarId)
                        dispose()
                    }
                }
            } catch (e: SQLException) {
                statusLabel.text = "Registration failed: ${e.message}"
            }
        } else {
            try {
                DriverManager.getConnection(DB_URL).use { conn ->
                    conn.prepareStatement("SELECT avatar_id FROM accounts WHERE username = ? AND password_hash = ?").use { pstmt ->
                        pstmt.setString(1, user)
                        pstmt.setString(2, hash)
                        pstmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                val avatarId = rs.getInt("avatar_id")
                                SessionManager.saveSession(user, avatarId)
                                dispose()
                            } else {
                                statusLabel.text = "Invalid username or password"
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                statusLabel.text = "Login failed: ${e.message}"
            }
        }
    }

    private inner class AvatarButton(private val id: Int, private val color: Color) : JButton() {
        init {
            preferredSize = Dimension(30, 30)
            isContentAreaFilled = false
            isFocusPainted = false
            border = null
            addActionListener {
                selectedAvatarId = id
                for (b in avatarButtons) b.repaint()
            }
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            if (selectedAvatarId == id) {
                g2.color = ACCENT_COLOR
                g2.fill(Ellipse2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
                g2.color = color
                g2.fill(Ellipse2D.Double(2.0, 2.0, (width - 4).toDouble(), (height - 4).toDouble()))
            } else {
                g2.color = color
                g2.fill(Ellipse2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            }
            g2.dispose()
        }
    }
}
