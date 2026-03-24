import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * GAME CONTROLLER
 * Mediates between Board (logic), BoardPanel (UI), and AI engine.
 * Handles: turn management, promotion dialog, undo, game-over detection.
 */
public class GameController {

    public enum GameMode { PVP, VS_AI }

    private Board board;
    private BoardPanel boardPanel;
    private ChessAI ai;
    private GameMode mode;
    private MainWindow window;

    // Selection state
    private int selectedRow = -1, selectedCol = -1;
    private List<Move> currentLegalMoves = new ArrayList<>();

    // Move history for undo and display
    private Stack<Board> boardHistory = new Stack<>();
    private Stack<Move>  moveHistory  = new Stack<>();
    private List<Move>   allMoves     = new ArrayList<>();  // for move list display

    // Captured pieces
    private List<Integer> capturedByWhite = new ArrayList<>();
    private List<Integer> capturedByBlack = new ArrayList<>();

    public GameController(MainWindow window, GameMode mode, int aiDepth) {
        this.window = window;
        this.mode   = mode;
        this.board  = new Board();
        this.ai     = new ChessAI(aiDepth);
    }

    public void setBoardPanel(BoardPanel bp) {
        this.boardPanel = bp;
    }

    public Board getBoard() { return board; }
    public List<Move> getAllMoves() { return allMoves; }
    public List<Integer> getCapturedByWhite() { return capturedByWhite; }
    public List<Integer> getCapturedByBlack() { return capturedByBlack; }

    // ─── SQUARE CLICK HANDLER ─────────────────────────────────────────────────

    public void onSquareClicked(int r, int c) {
        // Ignore clicks when AI is thinking or game over
        if (window.isAiThinking() || window.isGameOver()) return;
        // In VS_AI mode, only White (human) can click
        if (mode == GameMode.VS_AI && board.turn == Piece.BLACK) return;

        int piece = board.squares[r][c];

        if (selectedRow == -1) {
            // No piece selected yet — select if own piece
            if (piece != Piece.EMPTY && Piece.color(piece) == board.turn) {
                selectedRow = r; selectedCol = c;
                currentLegalMoves = board.legalMoves(r, c);
                boardPanel.selectSquare(r, c, currentLegalMoves);
            }
        } else {
            // Piece already selected
            Move target = currentLegalMoves.stream()
                .filter(m -> m.toRow == r && m.toCol == c)
                .findFirst().orElse(null);

            if (target != null) {
                // Valid destination — execute move
                executeMove(target);
            } else if (piece != Piece.EMPTY && Piece.color(piece) == board.turn) {
                // Clicked own piece — switch selection
                selectedRow = r; selectedCol = c;
                currentLegalMoves = board.legalMoves(r, c);
                boardPanel.selectSquare(r, c, currentLegalMoves);
            } else {
                // Clicked invalid square — deselect
                clearSelection();
                boardPanel.deselect();
            }
        }
    }

    private void executeMove(Move move) {
        // Handle promotion: ask user which piece
        if (Piece.type(move.piece) == Piece.PAWN &&
            (move.toRow == 0 || move.toRow == 7)) {
            // Find all promotion variants for this square
            final int fr = move.fromRow, fc = move.fromCol;
            final int tr = move.toRow,   tc = move.toCol;
            List<Move> promos = new ArrayList<>();
            for (Move m : currentLegalMoves)
                if (m.toRow == tr && m.toCol == tc && m.promoteTo != 0)
                    promos.add(m);

            if (!promos.isEmpty()) {
                if (mode == GameMode.VS_AI || board.turn == Piece.WHITE) {
                    showPromotionDialog(promos);
                    return;  // Dialog will call executeMove with chosen promo
                } else {
                    // AI auto-promotes to queen
                    move = promos.stream()
                        .filter(m -> m.promoteTo == Piece.QUEEN)
                        .findFirst().orElse(promos.get(0));
                }
            }
        }

        // Save board snapshot for undo
        boardHistory.push(new Board(board));
        moveHistory.push(move);

        // Track captures
        if (move.captured != Piece.EMPTY) {
            if (board.turn == Piece.WHITE) capturedByWhite.add(Piece.type(move.captured));
            else capturedByBlack.add(Piece.type(move.captured));
        }
        if (move.isEnPassant) {
            if (board.turn == Piece.WHITE) capturedByWhite.add(Piece.PAWN);
            else capturedByBlack.add(Piece.PAWN);
        }

        // Save move state for undo
        move.savedCastleWK  = board.castleWK;
        move.savedCastleWQ  = board.castleWQ;
        move.savedCastleBK  = board.castleBK;
        move.savedCastleBQ  = board.castleBQ;
        move.savedEnPassantCol = board.enPassantCol;

        board.applyMove(move);
        allMoves.add(move);
        boardPanel.setLastMove(move);

        clearSelection();
        boardPanel.deselect();
        window.onMoveMade();

        // Check game state
        int nextColor = board.turn;
        if (board.isCheckmate(nextColor)) {
            window.onGameOver((nextColor == Piece.WHITE ? "Black" : "White") + " wins by Checkmate!");
            return;
        }
        if (board.isStalemate(nextColor)) {
            window.onGameOver("Draw by Stalemate");
            return;
        }

        // Trigger AI if needed
        if (mode == GameMode.VS_AI && board.turn == Piece.BLACK) {
            window.setAiThinking(true);
            SwingWorker<Move, Void> worker = new SwingWorker<>() {
                @Override
                protected Move doInBackground() {
                    return ai.findBestMove(board);
                }
                @Override
                protected void done() {
                    try {
                        Move aiMove = get();
                        window.setAiThinking(false);
                        if (aiMove != null) {
                            executeMove(aiMove);
                        } else {
                            // No moves — already handled above
                        }
                    } catch (Exception ex) {
                        window.setAiThinking(false);
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        }
    }

    private void showPromotionDialog(List<Move> promos) {
        JDialog dialog = new JDialog(window, "Promote Pawn", true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        dialog.setBackground(new Color(30, 30, 50));

        JLabel lbl = new JLabel("Choose promotion piece:");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Serif", Font.BOLD, 14));
        dialog.add(lbl);

        int[] types = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        for (int t : types) {
            final int ft = t;
            Move promoMove = promos.stream()
                .filter(m -> m.promoteTo == ft)
                .findFirst().orElse(null);
            if (promoMove == null) continue;

            JButton btn = new JButton(Piece.symbol(Piece.make(board.turn, t)));
            btn.setFont(new Font("Serif", Font.PLAIN, 40));
            btn.setPreferredSize(new Dimension(70, 70));
            btn.setBackground(new Color(50, 50, 70));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setToolTipText(Piece.name(t));
            final Move chosen = promoMove;
            btn.addActionListener(e -> {
                dialog.dispose();
                executeMove(chosen);
            });
            dialog.add(btn);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(boardPanel);
        dialog.setVisible(true);
    }

    // ─── UNDO ─────────────────────────────────────────────────────────────────

    public void undoLastMove() {
        if (boardHistory.isEmpty()) return;
        // In VS_AI mode, undo both AI move and human move
        int undoCount = (mode == GameMode.VS_AI && boardHistory.size() >= 2) ? 2 : 1;
        for (int i = 0; i < undoCount && !boardHistory.isEmpty(); i++) {
            board = boardHistory.pop();
            Move undone = moveHistory.pop();
            if (!allMoves.isEmpty()) allMoves.remove(allMoves.size() - 1);
            // Undo captures
            if (undone.captured != Piece.EMPTY || undone.isEnPassant) {
                List<Integer> capList = (Piece.color(undone.piece) == Piece.WHITE)
                    ? capturedByWhite : capturedByBlack;
                if (!capList.isEmpty()) capList.remove(capList.size() - 1);
            }
        }
        boardPanel.setBoard(board);
        boardPanel.setLastMove(allMoves.isEmpty() ? null : allMoves.get(allMoves.size() - 1));
        clearSelection();
        boardPanel.deselect();
        window.onMoveMade();
    }

    // ─── NEW GAME ─────────────────────────────────────────────────────────────

    public void newGame(GameMode mode, int aiDepth) {
        this.mode = mode;
        this.ai.setDepth(aiDepth);
        board = new Board();
        boardHistory.clear(); moveHistory.clear();
        allMoves.clear();
        capturedByWhite.clear(); capturedByBlack.clear();
        boardPanel.setBoard(board);
        boardPanel.setLastMove(null);
        clearSelection();
        boardPanel.deselect();
        window.onMoveMade();
    }

    private void clearSelection() {
        selectedRow = -1; selectedCol = -1;
        currentLegalMoves.clear();
    }

    public int getLastNodesEvaluated() { return ai.getNodesEvaluated(); }
    public int getEvalScore() { return ai.evaluate(board); }
}
