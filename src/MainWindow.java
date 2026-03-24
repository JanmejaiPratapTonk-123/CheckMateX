import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * MAIN WINDOW — CheckMate X
 * Full Swing UI with:
 *  - Chess board (center)
 *  - Move history panel (right)
 *  - Captured pieces (left)
 *  - Algorithm info panel (left)
 *  - Status bar (bottom)
 *  - Menu bar (New Game, Undo, Mode)
 */
public class MainWindow extends JFrame {

    // ─── UI Colors / Fonts ────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(26, 26, 46);
    private static final Color BG_PANEL   = new Color(30, 30, 55);
    private static final Color BG_CARD    = new Color(40, 40, 65);
    private static final Color GOLD       = new Color(201, 168, 76);
    private static final Color GOLD_LIGHT = new Color(232, 201, 126);
    private static final Color TEXT_MAIN  = new Color(245, 239, 224);
    private static final Color TEXT_MUTED = new Color(168, 144, 96);
    private static final Color GREEN_ACC  = new Color(100, 200, 120);
    private static final Color RED_ACC    = new Color(220, 80, 80);

    private static final Font FONT_TITLE  = new Font("Serif", Font.BOLD, 22);
    private static final Font FONT_LABEL  = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_BODY   = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_PIECE  = new Font("Serif", Font.PLAIN, 16);

    private BoardPanel boardPanel;
    private GameController controller;

    // Panels updated after each move
    private JLabel statusLabel;
    private JTextArea moveListArea;
    private JLabel capturedWhiteLabel;
    private JLabel capturedBlackLabel;
    private JLabel evalLabel;
    private JLabel nodesLabel;
    private JLabel turnIndicator;

    private boolean aiThinking = false;
    private boolean gameOver   = false;

    public MainWindow() {
        super("CheckMate X");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);

        controller = new GameController(this, GameController.GameMode.VS_AI, 2);
        boardPanel  = new BoardPanel(controller.getBoard(), controller);
        controller.setBoardPanel(boardPanel);

        buildUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        onMoveMade(); // initial render
    }

    // ─── UI CONSTRUCTION ──────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // Menu bar
        setJMenuBar(buildMenuBar());

        // Top header
        add(buildHeader(), BorderLayout.NORTH);

        // Center: board
        JPanel boardWrap = new JPanel(new GridBagLayout());
        boardWrap.setBackground(BG_DARK);
        boardWrap.setBorder(new EmptyBorder(8, 12, 8, 12));
        boardWrap.add(boardPanel);
        add(boardWrap, BorderLayout.CENTER);

        // Left sidebar
        add(buildLeftPanel(), BorderLayout.WEST);

        // Right sidebar
        add(buildRightPanel(), BorderLayout.EAST);

        // Status bar
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GOLD.darker()));

        JMenu gameMenu = styledMenu("Game");
        gameMenu.add(styledMenuItem("New Game (vs AI)", e -> showNewGameDialog()));
        gameMenu.add(styledMenuItem("New Game (2 Players)", e -> {
            controller.newGame(GameController.GameMode.PVP, 2);
            gameOver = false;
            updateStatus("White to move");
        }));
        gameMenu.addSeparator();
        gameMenu.add(styledMenuItem("Undo Move", e -> {
            if (!aiThinking) { controller.undoLastMove(); gameOver = false; }
        }));
        gameMenu.addSeparator();
        gameMenu.add(styledMenuItem("Exit", e -> System.exit(0)));

        JMenu aboutMenu = styledMenu("About");
        aboutMenu.add(styledMenuItem("Algorithms Used", e -> showAlgoInfo()));

        bar.add(gameMenu);
        bar.add(aboutMenu);
        return bar;
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel();
        h.setBackground(BG_PANEL);
        h.setBorder(new MatteBorder(0, 0, 1, 0, GOLD.darker()));
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("♔  CHECKMATE X  ♚", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(GOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("DAA PROJECT  •  BACKTRACKING + MINIMAX + ALPHA-BETA PRUNING", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 9));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        h.add(Box.createVerticalStrut(6));
        h.add(title);
        h.add(sub);
        h.add(Box.createVerticalStrut(6));
        return h;
    }

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);
        p.setPreferredSize(new Dimension(185, 576));
        p.setBorder(new EmptyBorder(8, 8, 8, 4));

        // Turn indicator
        turnIndicator = styledLabel("⬤  White to Move", GOLD);
        turnIndicator.setFont(new Font("SansSerif", Font.BOLD, 12));
        p.add(wrapCard(turnIndicator, "Turn"));
        p.add(Box.createVerticalStrut(8));

        // Captured pieces
        capturedBlackLabel = styledLabel("", TEXT_MAIN);
        capturedBlackLabel.setFont(FONT_PIECE);
        capturedWhiteLabel = styledLabel("", TEXT_MAIN);
        capturedWhiteLabel.setFont(FONT_PIECE);

        JPanel capPanel = card("Captured Pieces");
        capPanel.add(styledLabel("White captured:", TEXT_MUTED));
        capPanel.add(capturedBlackLabel);
        capPanel.add(Box.createVerticalStrut(4));
        capPanel.add(styledLabel("Black captured:", TEXT_MUTED));
        capPanel.add(capturedWhiteLabel);
        p.add(capPanel);
        p.add(Box.createVerticalStrut(8));

        // Evaluation
        evalLabel  = styledLabel("±0.00", GOLD_LIGHT);
        evalLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        nodesLabel = styledLabel("0 nodes", TEXT_MUTED);

        JPanel evalPanel = card("Position Eval");
        evalPanel.add(evalLabel);
        evalPanel.add(nodesLabel);
        p.add(evalPanel);
        p.add(Box.createVerticalStrut(8));

        // Algorithm legend
        JPanel algoPanel = card("Algorithms (DAA)");
        String[] algos = {
            "✦ Backtracking",  "Legal move filter",
            "✦ Minimax",       "AI game tree search",
            "✦ Alpha-Beta",    "Prune dead branches",
            "✦ PST Eval",      "Positional heuristic"
        };
        for (int i = 0; i < algos.length; i += 2) {
            JLabel a = styledLabel(algos[i], GOLD_LIGHT);
            a.setFont(new Font("SansSerif", Font.BOLD, 10));
            JLabel b = styledLabel("  " + algos[i + 1], TEXT_MUTED);
            b.setFont(new Font("SansSerif", Font.PLAIN, 10));
            algoPanel.add(a);
            algoPanel.add(b);
        }
        p.add(algoPanel);
        p.add(Box.createVerticalGlue());

        // Buttons
        p.add(roundButton("New Game", e -> showNewGameDialog()));
        p.add(Box.createVerticalStrut(5));
        p.add(roundButton("Undo Move", e -> {
            if (!aiThinking) { controller.undoLastMove(); gameOver = false; }
        }));

        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);
        p.setPreferredSize(new Dimension(180, 576));
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        // Move history
        JPanel histPanel = card("Move History");
        moveListArea = new JTextArea(22, 14);
        moveListArea.setEditable(false);
        moveListArea.setBackground(BG_CARD);
        moveListArea.setForeground(TEXT_MAIN);
        moveListArea.setFont(FONT_MONO);
        moveListArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(moveListArea);
        scroll.setBorder(BorderFactory.createLineBorder(GOLD.darker().darker(), 1));
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setPreferredSize(new Dimension(160, 300));
        scroll.setMaximumSize(new Dimension(160, 300));
        histPanel.add(scroll);
        p.add(histPanel);
        p.add(Box.createVerticalStrut(8));

        // Piece values reference
        JPanel valPanel = card("Piece Values");
        String[][] vals = {{"♙","1"},{"♘","3"},{"♗","3"},{"♖","5"},{"♕","9"},{"♔","∞"}};
        JPanel grid = new JPanel(new GridLayout(2, 6, 4, 2));
        grid.setBackground(BG_CARD);
        for (String[] v : vals) {
            JLabel pl = new JLabel(v[0], SwingConstants.CENTER);
            pl.setFont(new Font("Serif", Font.PLAIN, 20));
            pl.setForeground(TEXT_MAIN);
            grid.add(pl);
        }
        for (String[] v : vals) {
            JLabel vl = new JLabel(v[1], SwingConstants.CENTER);
            vl.setFont(new Font("SansSerif", Font.BOLD, 10));
            vl.setForeground(TEXT_MUTED);
            grid.add(vl);
        }
        valPanel.add(grid);
        p.add(valPanel);
        p.add(Box.createVerticalGlue());

        return p;
    }

    private JPanel buildStatusBar() {
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sb.setBackground(BG_PANEL);
        sb.setBorder(new MatteBorder(1, 0, 0, 0, GOLD.darker()));
        statusLabel = new JLabel("White to move");
        statusLabel.setFont(new Font("Serif", Font.BOLD, 13));
        statusLabel.setForeground(GOLD);
        sb.add(statusLabel);
        return sb;
    }

    // ─── STATE UPDATE AFTER EACH MOVE ─────────────────────────────────────────

    public void onMoveMade() {
        boardPanel.repaint();

        // Move history text
        List<Move> moves = controller.getAllMoves();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1)).append(". ");
            sb.append(moves.get(i).toAlgebraic()).append("  ");
            if (i % 2 == 1) sb.append("\n");
        }
        moveListArea.setText(sb.toString());
        moveListArea.setCaretPosition(moveListArea.getDocument().getLength());

        // Captured
        capturedWhiteLabel.setText(capturedString(controller.getCapturedByWhite(), Piece.WHITE));
        capturedBlackLabel.setText(capturedString(controller.getCapturedByBlack(), Piece.BLACK));

        // Eval
        int eval = controller.getEvalScore();
        String sign = eval > 0 ? "+" : "";
        evalLabel.setText(sign + String.format("%.2f", eval / 100.0));
        evalLabel.setForeground(eval > 30 ? GREEN_ACC : eval < -30 ? RED_ACC : GOLD_LIGHT);
        nodesLabel.setText(controller.getLastNodesEvaluated() + " nodes evaluated");

        // Turn indicator
        if (!gameOver) {
            String turnStr = (controller.getBoard().turn == Piece.WHITE)
                ? "⬤  White to Move" : "⬤  Black to Move";
            turnIndicator.setText(turnStr);
            turnIndicator.setForeground(controller.getBoard().turn == Piece.WHITE ? GOLD_LIGHT : TEXT_MUTED);
            updateStatus((controller.getBoard().turn == Piece.WHITE ? "White" : "Black") + " to move"
                + (controller.getBoard().isInCheck(controller.getBoard().turn) ? " — CHECK!" : ""));
        }
    }

    private String capturedString(List<Integer> types, int byColor) {
        // byColor = color of the capturing player, so display opponent pieces
        int opp = Piece.opponent(byColor);
        StringBuilder sb = new StringBuilder();
        for (int t : types) sb.append(Piece.symbol(Piece.make(opp, t)));
        return sb.length() == 0 ? "—" : sb.toString();
    }

    public void onGameOver(String message) {
        gameOver = true;
        statusLabel.setText(message);
        statusLabel.setForeground(GOLD_LIGHT);
        turnIndicator.setText("Game Over");
        boardPanel.repaint();
        JOptionPane.showMessageDialog(this, message, "Game Over",
            JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(msg.contains("CHECK") ? RED_ACC : GOLD);
    }

    public boolean isAiThinking() { return aiThinking; }
    public void setAiThinking(boolean b) {
        aiThinking = b;
        if (b) updateStatus("AI is thinking...");
    }
    public boolean isGameOver() { return gameOver; }

    // ─── DIALOGS ──────────────────────────────────────────────────────────────

    private void showNewGameDialog() {
        String[] options = {"vs AI (Easy)", "vs AI (Medium)", "vs AI (Hard)", "2 Players"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select Game Mode", "New Game",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);
        if (choice < 0) return;
        GameController.GameMode mode;
        int depth;
        if (choice == 3) { mode = GameController.GameMode.PVP; depth = 2; }
        else { mode = GameController.GameMode.VS_AI; depth = choice + 1; }
        gameOver = false;
        controller.newGame(mode, depth);
        updateStatus("White to move");
    }

    private void showAlgoInfo() {
        String info =
            "ALGORITHMS USED IN CHECKMATE X\n" +
            "================================\n\n" +
            "1. BACKTRACKING (Legal Move Validation)\n" +
            "   - For each candidate move: apply it,\n" +
            "     check if king is in check, then undo.\n" +
            "   - If king is safe → legal. Else → reject.\n" +
            "   - Classic constraint-satisfaction backtracking.\n\n" +
            "2. MINIMAX (AI Game Tree Search)\n" +
            "   - Recursively explores all move sequences\n" +
            "     to depth N (1=Easy, 2=Medium, 3=Hard).\n" +
            "   - White maximizes score, Black minimizes.\n" +
            "   - Complexity: O(b^d), b≈30 (branching factor)\n\n" +
            "3. ALPHA-BETA PRUNING\n" +
            "   - Optimization of Minimax.\n" +
            "   - alpha = best score White can guarantee.\n" +
            "   - beta  = best score Black can guarantee.\n" +
            "   - if (beta <= alpha) → prune branch.\n" +
            "   - Reduces tree size from O(b^d) to O(b^(d/2)).\n\n" +
            "4. PIECE-SQUARE TABLES\n" +
            "   - Heuristic: each piece gets positional bonus\n" +
            "     based on which square it occupies.\n" +
            "   - E.g. Knights rewarded for center control.\n\n" +
            "TEAM: CheckMate X | Subject: DAA";

        JTextArea ta = new JTextArea(info);
        ta.setEditable(false);
        ta.setFont(FONT_MONO);
        ta.setBackground(BG_CARD);
        ta.setForeground(TEXT_MAIN);
        ta.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(420, 340));
        JOptionPane.showMessageDialog(this, sp, "Algorithm Details",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── UI HELPERS ───────────────────────────────────────────────────────────

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD.darker().darker(), 1),
            new EmptyBorder(6, 8, 8, 8)
        ));
        JLabel tl = new JLabel(title.toUpperCase());
        tl.setFont(new Font("SansSerif", Font.BOLD, 9));
        tl.setForeground(GOLD);
        tl.setBorder(new EmptyBorder(0, 0, 5, 0));
        p.add(tl);
        p.setMaximumSize(new Dimension(170, Integer.MAX_VALUE));
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel wrapCard(JComponent comp, String title) {
        JPanel p = card(title);
        p.add(comp);
        return p;
    }

    private JLabel styledLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(FONT_BODY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JButton roundButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setBackground(BG_CARD);
        b.setForeground(GOLD);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD.darker(), 1),
            new EmptyBorder(6, 14, 6, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.setMaximumSize(new Dimension(170, 32));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(GOLD.darker()); b.setForeground(BG_DARK); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BG_CARD); b.setForeground(GOLD); }
        });
        return b;
    }

    private JMenu styledMenu(String name) {
        JMenu m = new JMenu(name);
        m.setForeground(GOLD_LIGHT);
        m.setFont(FONT_LABEL);
        m.getPopupMenu().setBackground(BG_PANEL);
        m.getPopupMenu().setBorder(BorderFactory.createLineBorder(GOLD.darker().darker()));
        return m;
    }

    private JMenuItem styledMenuItem(String name, ActionListener al) {
        JMenuItem mi = new JMenuItem(name);
        mi.setBackground(BG_PANEL);
        mi.setForeground(TEXT_MAIN);
        mi.setFont(FONT_BODY);
        mi.addActionListener(al);
        return mi;
    }
}
