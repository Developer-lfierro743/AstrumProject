package com.novusforge.astrum.core;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IIVQuestionnaire: Identity Intent Verification pre-entry questionnaire.
 * Powering the "Fort Knox" engine with behavioral intent validation.
 */
public class IIVQuestionnaire extends JFrame {

    private static final String RESULT_FILE = System.getProperty("user.home") + "/iiv_result.dat";
    
    // Brand Colors
    private static final Color BG_COLOR = new Color(0x1A1A2E);
    private static final Color ACCENT_COLOR = new Color(0xC9A84C); // Gold
    private static final Color TEXT_COLOR = new Color(0xEAEAEA);
    private static final Color BUTTON_COLOR = new Color(0x1A3A5C);
    
    private static final Color SUCCESS_COLOR = new Color(0x2ECC71); // Green
    private static final Color WARNING_COLOR = new Color(0xC9A84C); // Gold
    private static final Color DANGER_COLOR = new Color(0xE74C3C);  // Red

    private int currentQuestionIndex = 0;
    private int totalScore = 0;
    
    private final List<Question> questions = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final JRadioButton[] options = new JRadioButton[4];
    private final JLabel questionLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, 12);
    private final JButton nextButton = new JButton("Next");
    private final JPanel mainPanel = new JComponent() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(BG_COLOR);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    } .getParent() != null ? null : new JPanel(new BorderLayout()); // Custom theme panel

    public IIVQuestionnaire() {
        setupData();
        
        // Check if already completed
        IIVResult result = loadResult();
        if (result != null) {
            if (result.decision != Decision.BLOCK) {
                launchGame();
                return;
            } else {
                showResults(result); // Show denial again
                return;
            }
        }

        setupUI();
    }

    private void setupUI() {
        setTitle("Project Astrum — Identity Verification");
        setSize(700, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Header: Progress
        progressBar.setValue(0);
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(BUTTON_COLOR);
        progressBar.setStringPainted(true);
        content.add(progressBar, BorderLayout.NORTH);

        // Center: Question and Options
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_COLOR);

        questionLabel.setForeground(TEXT_COLOR);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        centerPanel.add(questionLabel);
        centerPanel.add(Box.createVerticalStrut(30));

        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
            options[i].setBackground(BG_COLOR);
            options[i].setForeground(TEXT_COLOR);
            options[i].setFont(new Font("Arial", Font.PLAIN, 16));
            options[i].addActionListener(e -> nextButton.setEnabled(true));
            buttonGroup.add(options[i]);
            centerPanel.add(options[i]);
            centerPanel.add(Box.createVerticalStrut(10));
        }
        content.add(centerPanel, BorderLayout.CENTER);

        // Footer: Next
        nextButton.setBackground(BUTTON_COLOR);
        nextButton.setForeground(TEXT_COLOR);
        nextButton.setFocusPainted(false);
        nextButton.setFont(new Font("Arial", Font.BOLD, 16));
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> nextQuestion());
        content.add(nextButton, BorderLayout.SOUTH);

        add(content);
        displayQuestion();
        setVisible(true);
    }

    private void displayQuestion() {
        Question q = questions.get(currentQuestionIndex);
        questionLabel.setText("<html><div style='width: 500px;'>" + (currentQuestionIndex + 1) + ". " + q.text + "</div></html>");
        buttonGroup.clearSelection();
        nextButton.setEnabled(false);
        
        for (int i = 0; i < 4; i++) {
            options[i].setText(q.options[i]);
        }
        progressBar.setValue(currentQuestionIndex);
    }

    private void nextQuestion() {
        // Calculate score
        Question q = questions.get(currentQuestionIndex);
        for (int i = 0; i < 4; i++) {
            if (options[i].isSelected()) {
                totalScore += q.scores[i];
                break;
            }
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion();
        } else {
            processResults();
        }
    }

    private void processResults() {
        Decision decision;
        String message;
        Color color;

        if (totalScore <= 5) {
            decision = Decision.ALLOW;
            message = "Identity Verification passed. Welcome to Astrum.";
            color = SUCCESS_COLOR;
        } else if (totalScore <= 12) {
            decision = Decision.WARN;
            message = "Some responses flagged for review. You may enter but your session will be monitored.";
            color = WARNING_COLOR;
        } else {
            decision = Decision.BLOCK;
            message = "Entry denied. Your responses indicate behavior patterns incompatible with the Astrum community standards.";
            color = DANGER_COLOR;
        }

        IIVResult result = new IIVResult(decision, totalScore);
        
        // SAVE RESULT IMMEDIATELY AND FORCE FLUSH
        saveResult(result);
        
        // Verify file was created
        File file = new File(RESULT_FILE);
        if (!file.exists()) {
            System.err.println("[IIV] ERROR: Failed to save result file!");
        } else {
            System.out.println("[IIV] Result saved to: " + RESULT_FILE);
            System.out.println("[IIV] Decision: " + decision + " | Score: " + totalScore);
        }
        
        showResults(result);
    }

    private void showResults(IIVResult res) {
        getContentPane().removeAll();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel scoreLabel = new JLabel("Your Score: " + res.score);
        scoreLabel.setForeground(TEXT_COLOR);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(scoreLabel, gbc);

        JLabel tierLabel = new JLabel("Result: " + res.decision);
        Color accent = (res.decision == Decision.ALLOW) ? SUCCESS_COLOR : 
                       (res.decision == Decision.WARN) ? WARNING_COLOR : DANGER_COLOR;
        tierLabel.setForeground(accent);
        tierLabel.setFont(new Font("Arial", Font.BOLD, 28));
        panel.add(tierLabel, gbc);

        JLabel msgLabel = new JLabel("<html><div style='text-align: center; width: 400px;'>" + 
            (res.decision == Decision.ALLOW ? "Identity Verification passed. Welcome to Astrum." :
             res.decision == Decision.WARN ? "Some responses flagged for review. You may enter but your session will be monitored." :
             "Entry denied. Your responses indicate behavior patterns incompatible with the Astrum community standards.") + 
            "</div></html>");
        msgLabel.setForeground(TEXT_COLOR);
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        panel.add(msgLabel, gbc);

        if (res.decision != Decision.BLOCK) {
            JLabel launchLabel = new JLabel("Launching game shortly...");
            launchLabel.setForeground(ACCENT_COLOR);
            panel.add(launchLabel, gbc);
            revalidate();
            repaint();
            
            new Timer(res.decision == Decision.ALLOW ? 2000 : 3000, e -> launchGame()).start();
        } else {
            JButton close = new JButton("Close");
            close.setBackground(BUTTON_COLOR);
            close.setForeground(TEXT_COLOR);
            close.addActionListener(e -> System.exit(0));
            panel.add(close, gbc);
            revalidate();
            repaint();
        }
        
        add(panel);
        setVisible(true);
    }

    private void launchGame() {
        System.out.println("[IIV] Launching Project Astrum...");
        dispose();
        // Here we would normally call the game engine entry point
    }

    private void saveResult(IIVResult result) {
        try {
            File file = new File(RESULT_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(result);
                oos.flush();
                oos.close();
            }
            
            System.out.println("[IIV] Result file saved: " + file.getAbsolutePath());
            System.out.println("[IIV] File exists: " + file.exists());
            System.out.println("[IIV] File size: " + file.length() + " bytes");
        } catch (IOException e) {
            System.err.println("[IIV] FAILED to save result: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private IIVResult loadResult() {
        File file = new File(RESULT_FILE);
        if (!file.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (IIVResult) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private void setupData() {
        // Category 1: Cheating and Macros
        questions.add(new Question("If you discovered a timing exploit that gave you an unfair advantage in competitive play what would you do?",
            new String[]{"Use it to win, everyone does it", "Use it privately where no one can see", "Report it to the developers", "Ignore it and play normally"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("How do you feel about players who use macros or automated inputs in competitive modes?",
            new String[]{"It is a valid strategy if you can get away with it", "It is fine as long as it is not obvious", "It is unfair and ruins the experience for everyone", "I have never thought about it"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("If your account was flagged for suspicious timing consistency in 90 percent of your actions what would you do?",
            new String[]{"Deny everything and blame the system", "Try to hide it better next time", "Accept the review and cooperate", "Stop the behavior immediately"},
            new int[]{3, 3, 0, 0}));

        // Category 2: Griefing and Server Destruction
        questions.add(new Question("You join a server and find an unprotected base full of another player's work. What do you do?",
            new String[]{"Destroy everything, it is their fault for not protecting it", "Take some valuable items and leave", "Leave it alone or tell them it is unprotected", "Help them protect it"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("A group of friends invites you to join a coordinated raid to destroy a large community server. What do you do?",
            new String[]{"Join immediately, it sounds fun", "Watch but do not participate", "Refuse and report the plan to server staff", "Warn the server community"},
            new int[]{3, 1, 0, 0}));
        questions.add(new Question("How do you feel about organized griefing groups like those that have targeted Minecraft servers historically?",
            new String[]{"They are legendary and impressive", "It is just a game, no real harm done", "It is toxic and ruins communities", "I have no interest in that kind of play"},
            new int[]{3, 2, 0, 0}));

        // Category 3: Minor Safety and Grooming
        questions.add(new Question("A much younger player messages you frequently and seems to look up to you. What do you do?",
            new String[]{"Take advantage of the attention and build a private relationship", "Keep chatting privately and ask personal questions", "Be friendly in public chats but maintain normal boundaries", "Encourage them to play with people their own age"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("Someone asks you to move a conversation from the game chat to a private platform like Discord or phone. What do you do?",
            new String[]{"Always agree, private conversations are fine", "Agree if they seem trustworthy", "Keep all conversations in the game where they are visible", "Report the request to a moderator if it feels wrong"},
            new int[]{2, 1, 0, 0}));
        questions.add(new Question("How do you feel about players asking others their age in game chat?",
            new String[]{"It is normal and fine to ask anyone", "It depends on context", "It can be a warning sign and should be monitored", "It should be blocked by default in public chat"},
            new int[]{2, 1, 0, 0}));

        // Category 4: Identity and Honesty
        questions.add(new Question("Would you ever create a fake persona including a different name, gender, or voice to gain an advantage or reputation in a game community?",
            new String[]{"Yes if it helped me win or gain followers", "Maybe for fun or to avoid harassment", "No, I prefer to be known as I actually am", "I have never considered it"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("If you were caught using a fake identity to deceive a community for over a year what would you do?",
            new String[]{"Double down and deny everything", "Quietly disappear and start over with a new account", "Come clean publicly and apologize", "I would never do this in the first place"},
            new int[]{3, 2, 0, 0}));
        questions.add(new Question("How important is honesty and transparency in online gaming communities?",
            new String[]{"Not important, it is just a game", "Somewhat important but winning matters more", "Very important, communities depend on trust", "Essential, I hold myself to this standard"},
            new int[]{2, 1, 0, 0}));
    }

    private static class Question {
        String text;
        String[] options;
        int[] scores;

        Question(String text, String[] options, int[] scores) {
            this.text = text;
            this.options = options;
            this.scores = scores;
        }
    }

    private enum Decision { ALLOW, WARN, BLOCK }

    private static class IIVResult implements Serializable {
        Decision decision;
        int score;

        IIVResult(Decision decision, int score) {
            this.decision = decision;
            this.score = score;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(IIVQuestionnaire::new);
    }
}
