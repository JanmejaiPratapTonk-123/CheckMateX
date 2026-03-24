import javax.swing.*;

/**
 * CHECKMATE X - Chess Application
 * DAA Project | PBL Submission
 *
 * Algorithms Used:
 *  1. Backtracking     - Legal move validation (try move, check king safety, undo)
 *  2. Minimax          - AI game tree search
 *  3. Alpha-Beta Pruning - Prune minimax tree branches
 *  4. Piece-Square Tables - Board evaluation heuristic
 */
public class CheckmateX {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
