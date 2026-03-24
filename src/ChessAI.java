import java.util.List;

/**
 * CHESS AI ENGINE
 *
 * Implements two core DAA algorithms:
 *
 * 1. MINIMAX (Game Tree Search)
 *    - Recursively explores all possible move sequences to a given depth
 *    - White = maximizing player, Black = minimizing player
 *    - Time complexity: O(b^d) where b=branching factor (~30), d=depth
 *
 * 2. ALPHA-BETA PRUNING
 *    - Optimization of Minimax — prunes branches that CANNOT affect the result
 *    - alpha: best score the maximizer (White) is guaranteed so far
 *    - beta:  best score the minimizer (Black) is guaranteed so far
 *    - If beta <= alpha at any node → prune (backtrack immediately)
 *    - Reduces effective branching factor from ~30 to ~7-8
 *    - Best case: O(b^(d/2)), same result as full minimax
 */
public class ChessAI {

    private int depth;
    private int nodesEvaluated;   // for educational display

    // ─── PIECE-SQUARE TABLES ──────────────────────────────────────────────────
    // Bonus/penalty for piece placement (White's perspective, row 7=White back rank)

    private static final int[][] PST_PAWN = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] PST_KNIGHT = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };
    private static final int[][] PST_BISHOP = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };
    private static final int[][] PST_ROOK = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        { 5, 10, 10, 10, 10, 10, 10,  5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        { 0,  0,  0,  5,  5,  0,  0,  0}
    };
    private static final int[][] PST_QUEEN = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };
    private static final int[][] PST_KING = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    public ChessAI(int depth) {
        this.depth = depth;
    }

    public void setDepth(int d) { this.depth = d; }
    public int getNodesEvaluated() { return nodesEvaluated; }

    // ─── FIND BEST MOVE ───────────────────────────────────────────────────────

    public Move findBestMove(Board board) {
        nodesEvaluated = 0;
        List<Move> moves = board.allLegalMoves(Piece.BLACK);
        if (moves.isEmpty()) return null;

        Move bestMove = null;
        int bestScore = Integer.MAX_VALUE;  // Black is minimizing

        for (Move m : moves) {
            // Save move undo info
            m.savedCastleWK = board.castleWK;
            m.savedCastleWQ = board.castleWQ;
            m.savedCastleBK = board.castleBK;
            m.savedCastleBQ = board.castleBQ;
            m.savedEnPassantCol = board.enPassantCol;

            board.applyMove(m);

            // Minimax: White will try to maximize from here
            int score = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, true);

            board.undoMove(m);

            if (score < bestScore) {
                bestScore = score;
                bestMove  = m;
            }
        }
        return bestMove;
    }

    // ─── MINIMAX WITH ALPHA-BETA PRUNING ──────────────────────────────────────

    /**
     * MINIMAX ALGORITHM with ALPHA-BETA PRUNING
     *
     * @param board       current board state
     * @param depth       remaining search depth
     * @param alpha       best score WHITE can guarantee so far (lower bound)
     * @param beta        best score BLACK can guarantee so far (upper bound)
     * @param maximizing  true = White's turn (maximize), false = Black's turn (minimize)
     * @return            best evaluation score from this position
     *
     * PRUNING CONDITION:
     *   if (beta <= alpha) break;  ← this branch cannot improve result → prune
     */
    private int minimax(Board board, int depth, int alpha, int beta, boolean maximizing) {
        nodesEvaluated++;

        // Base case: leaf node → return static evaluation
        if (depth == 0) return evaluate(board);

        int color = maximizing ? Piece.WHITE : Piece.BLACK;
        List<Move> moves = board.allLegalMoves(color);

        // Terminal node: checkmate or stalemate
        if (moves.isEmpty()) {
            if (board.isInCheck(color))
                return maximizing ? -99999 : 99999; // Checkmate
            return 0;  // Stalemate
        }

        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (Move m : moves) {
                m.savedCastleWK = board.castleWK;
                m.savedCastleWQ = board.castleWQ;
                m.savedCastleBK = board.castleBK;
                m.savedCastleBQ = board.castleBQ;
                m.savedEnPassantCol = board.enPassantCol;

                board.applyMove(m);
                best = Math.max(best, minimax(board, depth - 1, alpha, beta, false));
                board.undoMove(m);

                alpha = Math.max(alpha, best);

                // ★ ALPHA-BETA PRUNING ★
                // Black already has a move guaranteeing <= beta.
                // White found >= beta here → Black will never choose this node.
                if (beta <= alpha) break;  // Prune remaining branches (backtrack)
            }
            return best;

        } else {
            int best = Integer.MAX_VALUE;
            for (Move m : moves) {
                m.savedCastleWK = board.castleWK;
                m.savedCastleWQ = board.castleWQ;
                m.savedCastleBK = board.castleBK;
                m.savedCastleBQ = board.castleBQ;
                m.savedEnPassantCol = board.enPassantCol;

                board.applyMove(m);
                best = Math.min(best, minimax(board, depth - 1, alpha, beta, true));
                board.undoMove(m);

                beta = Math.min(beta, best);

                // ★ ALPHA-BETA PRUNING ★
                // White already has a move guaranteeing >= alpha.
                // Black found <= alpha here → White will never choose this node.
                if (beta <= alpha) break;  // Prune remaining branches (backtrack)
            }
            return best;
        }
    }

    // ─── BOARD EVALUATION ─────────────────────────────────────────────────────

    /**
     * Static evaluation function — returns score from White's perspective.
     * Positive = White is better, Negative = Black is better.
     *
     * Components:
     *  1. Material count (piece values)
     *  2. Piece-square table bonus (positional value)
     */
    public int evaluate(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = board.squares[r][c];
                if (p == Piece.EMPTY) continue;
                int color = Piece.color(p);
                int type  = Piece.type(p);
                int matVal = Piece.VALUES[type];
                int posVal = getPST(type, r, c, color);
                int total  = matVal + posVal;
                score += (color == Piece.WHITE) ? total : -total;
            }
        }
        return score;
    }

    private int getPST(int type, int r, int c, int color) {
        // For Black, mirror the row (PST is from White's perspective)
        int row = (color == Piece.WHITE) ? r : (7 - r);
        switch (type) {
            case Piece.PAWN:   return PST_PAWN[row][c];
            case Piece.KNIGHT: return PST_KNIGHT[row][c];
            case Piece.BISHOP: return PST_BISHOP[row][c];
            case Piece.ROOK:   return PST_ROOK[row][c];
            case Piece.QUEEN:  return PST_QUEEN[row][c];
            case Piece.KING:   return PST_KING[row][c];
            default:           return 0;
        }
    }
}
