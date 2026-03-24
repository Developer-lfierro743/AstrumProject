package com.novusforge.astrum.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountSystem extends JFrame {
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/astrum_accounts.db";
    
    // Brand Colors
    private static final Color BG_COLOR = new Color(0x1A1A2E);
    private static final Color ACCENT_COLOR = new Color(0xC9A84C); // Gold
    private static final Color TEXT_COLOR = new Color(0xEAEAEA);
    private static final Color BUTTON_COLOR = new Color(0x1A3A5C);
    private static final Color ERROR_COLOR = new Color(0xE74C3C);
    private static final Color WARN_COLOR = new Color(0xF1C40F);
    private static final Color SUCCESS_COLOR = new Color(0x2ECC71);

    private static final Color[] AVATAR_COLORS = {
        new Color(0x3498DB), new Color(0x2ECC71), new Color(0xE74C3C),
        new Color(0xF39C12), new Color(0x9B59B6), new Color(0x1ABC9C)
    };

    private enum Mode { LOGIN, REGISTER }
    private Mode currentMode = Mode.LOGIN;

    private JLabel headerLabel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel confirmPasswordLabel;
    private JPasswordField confirmPasswordField;
    private JLabel avatarLabel;
    private JPanel avatarPanel;
    private JLabel statusLabel;
    private JLabel usernameStatusLabel;
    private JButton actionButton;
    private JButton switchButton;
    
    private int selectedAvatarId = 0;
    private List<AvatarButton> avatarButtons = new ArrayList<>();
    private boolean usernameFlagged = false;

    public AccountSystem() {
        initDatabase();
        setupUI();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL," +
                    "avatar_id INTEGER DEFAULT 0," +
                    "iiv_result TEXT DEFAULT 'ALLOW'," +
                    "flagged_username BOOLEAN DEFAULT 0," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            stmt.execute(sql);
            
            // Migration for existing databases
            try {
                stmt.execute("ALTER TABLE accounts ADD COLUMN flagged_username BOOLEAN DEFAULT 0;");
            } catch (SQLException ignored) {}
            
        } catch (SQLException e) {
            System.err.println("[Database] Error: " + e.getMessage());
        }
    }

    private void setupUI() {
        setTitle("Project Astrum — Account");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBackground(BG_COLOR);
        content.setBorder(new EmptyBorder(40, 60, 40, 60));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        // Header
        headerLabel = new JLabel("Welcome Back to Astrum");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(ACCENT_COLOR);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        formPanel.add(headerLabel, gbc);

        // Username
        gbc.gridy++;
        JLabel uLabel = new JLabel("Username");
        uLabel.setForeground(TEXT_COLOR);
        formPanel.add(uLabel, gbc);

        gbc.gridy++;
        JPanel uFieldPanel = new JPanel(new BorderLayout(5, 0));
        uFieldPanel.setBackground(BG_COLOR);
        usernameField = new JTextField();
        usernameField.setBackground(BUTTON_COLOR);
        usernameField.setForeground(TEXT_COLOR);
        usernameField.setCaretColor(TEXT_COLOR);
        usernameField.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        uFieldPanel.add(usernameField, BorderLayout.CENTER);
        
        usernameStatusLabel = new JLabel("");
        usernameStatusLabel.setPreferredSize(new Dimension(200, 20));
        usernameStatusLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        uFieldPanel.add(usernameStatusLabel, BorderLayout.EAST);
        formPanel.add(uFieldPanel, gbc);

        // Password
        gbc.gridy++;
        JLabel pLabel = new JLabel("Password");
        pLabel.setForeground(TEXT_COLOR);
        formPanel.add(pLabel, gbc);

        gbc.gridy++;
        passwordField = new JPasswordField();
        passwordField.setBackground(BUTTON_COLOR);
        passwordField.setForeground(TEXT_COLOR);
        passwordField.setCaretColor(TEXT_COLOR);
        passwordField.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        formPanel.add(passwordField, gbc);

        // Confirm Password (Register only)
        gbc.gridy++;
        confirmPasswordLabel = new JLabel("Confirm Password");
        confirmPasswordLabel.setForeground(TEXT_COLOR);
        confirmPasswordLabel.setVisible(false);
        formPanel.add(confirmPasswordLabel, gbc);

        gbc.gridy++;
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBackground(BUTTON_COLOR);
        confirmPasswordField.setForeground(TEXT_COLOR);
        confirmPasswordField.setCaretColor(TEXT_COLOR);
        confirmPasswordField.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        confirmPasswordField.setVisible(false);
        formPanel.add(confirmPasswordField, gbc);

        // Avatar Selection (Register only)
        gbc.gridy++;
        avatarLabel = new JLabel("Select Avatar");
        avatarLabel.setForeground(TEXT_COLOR);
        avatarLabel.setVisible(false);
        formPanel.add(avatarLabel, gbc);

        gbc.gridy++;
        avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        avatarPanel.setBackground(BG_COLOR);
        avatarPanel.setVisible(false);
        for (int i = 0; i < AVATAR_COLORS.length; i++) {
            AvatarButton ab = new AvatarButton(i, AVATAR_COLORS[i]);
            avatarButtons.add(ab);
            avatarPanel.add(ab);
        }
        formPanel.add(avatarPanel, gbc);

        // Status/Error
        gbc.gridy++;
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(ERROR_COLOR);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(statusLabel, gbc);

        content.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel southPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        southPanel.setBackground(BG_COLOR);

        actionButton = new JButton("Login");
        actionButton.setBackground(BUTTON_COLOR);
        actionButton.setForeground(TEXT_COLOR);
        actionButton.setFocusPainted(false);
        actionButton.addActionListener(e -> handleAction());
        southPanel.add(actionButton);

        switchButton = new JButton("Switch to Register");
        switchButton.setBackground(BG_COLOR);
        switchButton.setForeground(ACCENT_COLOR);
        switchButton.setBorder(null);
        switchButton.addActionListener(e -> toggleMode());
        southPanel.add(switchButton);

        content.add(southPanel, BorderLayout.SOUTH);

        add(content);

        // Listeners for live validation
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateForm(); }
            public void removeUpdate(DocumentEvent e) { validateForm(); }
            public void changedUpdate(DocumentEvent e) { validateForm(); }
        };
        usernameField.getDocument().addDocumentListener(dl);
        passwordField.getDocument().addDocumentListener(dl);
        confirmPasswordField.getDocument().addDocumentListener(dl);

        setVisible(true);
    }

    private void toggleMode() {
        currentMode = (currentMode == Mode.LOGIN) ? Mode.REGISTER : Mode.LOGIN;
        boolean isReg = (currentMode == Mode.REGISTER);
        
        setTitle(isReg ? "Project Astrum — Register" : "Project Astrum — Login");
        headerLabel.setText(isReg ? "Join Astrum" : "Welcome Back to Astrum");
        
        confirmPasswordLabel.setVisible(isReg);
        confirmPasswordField.setVisible(isReg);
        avatarLabel.setVisible(isReg);
        avatarPanel.setVisible(isReg);
        
        actionButton.setText(isReg ? "Register" : "Login");
        switchButton.setText(isReg ? "Switch to Login" : "Switch to Register");
        
        statusLabel.setText(" ");
        validateForm();
    }

    private void validateForm() {
        if (currentMode == Mode.LOGIN) {
            actionButton.setEnabled(!usernameField.getText().isEmpty() && passwordField.getPassword().length > 0);
            usernameStatusLabel.setText("");
            return;
        }

        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        boolean userValid = user.matches("^[a-zA-Z0-9_]{3,20}$");
        usernameFlagged = false;

        if (userValid) {
            if (isUsernameTaken(user)) {
                usernameStatusLabel.setText("<html><font color='red'>Username taken</font></html>");
                userValid = false;
            } else {
                // Identity Filter Check
                CreatorIdentityFilter.FilterResult filter = CreatorIdentityFilter.check(user);
                if (filter.severity() == CreatorIdentityFilter.FilterSeverity.BLOCK) {
                    usernameStatusLabel.setText("<html><font color='red'>" + filter.reason() + "</font></html>");
                    userValid = false;
                } else if (filter.severity() == CreatorIdentityFilter.FilterSeverity.WARN) {
                    usernameStatusLabel.setText("<html><font color='orange'>" + filter.reason() + "</font></html>");
                    usernameFlagged = true;
                } else {
                    usernameStatusLabel.setText("<html><font color='green'>\u2713 Available</font></html>");
                }
            }
        } else {
            usernameStatusLabel.setText(user.isEmpty() ? "" : "<html><font color='red'>Invalid format</font></html>");
        }

        boolean passValid = pass.length() >= 6;
        boolean match = pass.equals(confirm) && !pass.isEmpty();
        
        confirmPasswordField.setBorder(BorderFactory.createLineBorder(match || confirm.isEmpty() ? ACCENT_COLOR : ERROR_COLOR));

        actionButton.setEnabled(userValid && passValid && match);
    }

    private boolean isUsernameTaken(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM accounts WHERE username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void handleAction() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String hash = HashUtils.computeSHA256(pass.getBytes());

        if (currentMode == Mode.REGISTER) {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO accounts(username, password_hash, avatar_id, flagged_username) VALUES(?, ?, ?, ?)")) {
                pstmt.setString(1, user);
                pstmt.setString(2, hash);
                pstmt.setInt(3, selectedAvatarId);
                pstmt.setBoolean(4, usernameFlagged);
                pstmt.executeUpdate();
                
                SessionManager.saveSession(user, selectedAvatarId);
                dispose();
            } catch (SQLException e) {
                statusLabel.setText("Registration failed: " + e.getMessage());
            }
        } else {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT avatar_id FROM accounts WHERE username = ? AND password_hash = ?")) {
                pstmt.setString(1, user);
                pstmt.setString(2, hash);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int avatarId = rs.getInt("avatar_id");
                        SessionManager.saveSession(user, avatarId);
                        dispose();
                    } else {
                        statusLabel.setText("Invalid username or password");
                    }
                }
            } catch (SQLException e) {
                statusLabel.setText("Login failed: " + e.getMessage());
            }
        }
    }

    private class AvatarButton extends JButton {
        private int id;
        private Color color;

        public AvatarButton(int id, Color color) {
            this.id = id;
            this.color = color;
            setPreferredSize(new Dimension(30, 30));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(null);
            addActionListener(e -> {
                selectedAvatarId = id;
                for (AvatarButton b : avatarButtons) b.repaint();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (selectedAvatarId == id) {
                g2.setColor(ACCENT_COLOR);
                g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(2, 2, getWidth()-4, getHeight()-4));
            } else {
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
            }
            g2.dispose();
        }
    }
}
