import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BOARD PANEL — Swing component that renders the chess board.
 * Handles:
 *  - Drawing squares, pieces, highlights
 *  - Mouse clicks for selecting pieces and making moves
 *  - Legal move highlighting (green dots)
 *  - Check highlight (red king)
 */
public class BoardPanel extends JPanel implements MouseListener {

    // ─── Colors ───────────────────────────────────────────────────────────────
    private static final Color LIGHT_SQ      = new Color(240, 217, 181);
    private static final Color DARK_SQ       = new Color(181, 136, 99);
    private static final Color SEL_COLOR     = new Color(100, 200, 100, 180);
    private static final Color MOVE_DOT      = new Color(0, 0, 0, 60);
    private static final Color MOVE_CAPTURE  = new Color(200, 50, 50, 100);
    private static final Color CHECK_COLOR   = new Color(220, 50, 50, 160);
    private static final Color LAST_MOVE_FROM= new Color(200, 180, 80, 120);
    private static final Color LAST_MOVE_TO  = new Color(200, 180, 80, 160);
    private static final Color COORD_LIGHT   = new Color(181, 136, 99);
    private static final Color COORD_DARK    = new Color(240, 217, 181);

    private static final int SQ = 72;      // square size in pixels
    private static final int BOARD_SIZE = SQ * 8;

    private Board board;
    private GameController controller;

    // Selection state
    private int selectedRow = -1, selectedCol = -1;
    private List<Move> legalMovesForSelected = new ArrayList<>();
    private Move lastMove = null;

    // Fonts
    private Font pieceFont;
    private Font coordFont;

    public BoardPanel(Board board, GameController controller) {
        this.board      = board;
        this.controller = controller;
        setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        addMouseListener(this);

        // Try to load a large font for pieces
        pieceFont = new Font("Serif", Font.PLAIN, 54);
        coordFont = new Font("SansSerif", Font.BOLD, 11);
    }

    public void setBoard(Board board)     { this.board = board; }
    public void setLastMove(Move m)       { this.lastMove = m; }
    public void clearSelection()          { selectedRow = -1; selectedCol = -1; legalMovesForSelected.clear(); }

    // ─── PAINTING ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                drawSquare(g2, r, c);
            }
        }
    }

    private void drawSquare(Graphics2D g, int r, int c) {
        int x = c * SQ, y = r * SQ;
        boolean isLight = (r + c) % 2 == 0;

        // Base square color
        g.setColor(isLight ? LIGHT_SQ : DARK_SQ);
        g.fillRect(x, y, SQ, SQ);

        // Last move highlight
        if (lastMove != null) {
            if ((r == lastMove.fromRow && c == lastMove.fromCol) ||
                (r == lastMove.toRow   && c == lastMove.toCol)) {
                g.setColor(r == lastMove.toRow && c == lastMove.toCol ? LAST_MOVE_TO : LAST_MOVE_FROM);
                g.fillRect(x, y, SQ, SQ);
            }
        }

        // Selected square
        if (r == selectedRow && c == selectedCol) {
            g.setColor(SEL_COLOR);
            g.fillRect(x, y, SQ, SQ);
        }

        // Check highlight (king in check)
        if (board.isInCheck(board.turn)) {
            int[] king = board.findKing(board.turn);
            if (king != null && king[0] == r && king[1] == c) {
                g.setColor(CHECK_COLOR);
                g.fillRect(x, y, SQ, SQ);
            }
        }

        // Legal move indicator
        boolean isMovable = legalMovesForSelected.stream()
            .anyMatch(m -> m.toRow == r && m.toCol == c);
        if (isMovable) {
            if (board.squares[r][c] != Piece.EMPTY) {
                // Capture: colored ring around piece
                g.setColor(MOVE_CAPTURE);
                g.setStroke(new BasicStroke(4));
                g.drawOval(x + 3, y + 3, SQ - 6, SQ - 6);
                g.setStroke(new BasicStroke(1));
            } else {
                // Empty: small dot in center
                g.setColor(MOVE_DOT);
                int dot = 22, dx = x + (SQ - dot) / 2, dy = y + (SQ - dot) / 2;
                g.fillOval(dx, dy, dot, dot);
            }
        }

        // Coordinates (file label bottom-right, rank label top-left)
        g.setFont(coordFont);
        g.setColor(isLight ? COORD_LIGHT : COORD_DARK);
        if (c == 0) {
            g.drawString(String.valueOf(8 - r), x + 3, y + 14);
        }
        if (r == 7) {
            String file = String.valueOf("abcdefgh".charAt(c));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(file, x + SQ - fm.stringWidth(file) - 3, y + SQ - 3);
        }

        // Piece
        int piece = board.squares[r][c];
        if (piece != Piece.EMPTY) {
            drawPiece(g, piece, x, y);
        }
    }

    private void drawPiece(Graphics2D g, int piece, int x, int y) {
        String sym = Piece.symbol(piece);
        g.setFont(pieceFont);

        FontMetrics fm = g.getFontMetrics();
        int tx = x + (SQ - fm.stringWidth(sym)) / 2;
        int ty = y + (SQ + fm.getAscent() - fm.getDescent()) / 2 - 2;

        // Shadow / outline for readability
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(sym, tx + 1, ty + 1);

        // White pieces: draw with outline effect
        if (Piece.isWhite(piece)) {
            g.setColor(Color.WHITE);
            g.drawString(sym, tx, ty);
            g.setColor(new Color(60, 40, 20));
            // Thin outline
            g.drawString(sym, tx - 1, ty);
            g.drawString(sym, tx + 1, ty);
            g.drawString(sym, tx, ty - 1);
            g.drawString(sym, tx, ty + 1);
            g.setColor(Color.WHITE);
            g.drawString(sym, tx, ty);
        } else {
            g.setColor(new Color(20, 20, 20));
            g.drawString(sym, tx, ty);
        }
    }

    // ─── MOUSE HANDLING ───────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        int c = e.getX() / SQ;
        int r = e.getY() / SQ;
        if (r < 0 || r > 7 || c < 0 || c > 7) return;
        controller.onSquareClicked(r, c);
    }

    public void selectSquare(int r, int c, List<Move> moves) {
        selectedRow = r; selectedCol = c;
        legalMovesForSelected = new ArrayList<>(moves);
        repaint();
    }

    public void deselect() {
        selectedRow = -1; selectedCol = -1;
        legalMovesForSelected.clear();
        repaint();
    }

    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}
