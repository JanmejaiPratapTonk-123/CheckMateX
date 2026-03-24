import java.util.ArrayList;
import java.util.List;

/**
 * BOARD - Core chess engine.
 *
 * Key DAA algorithms here:
 *  - BACKTRACKING in legalMoves(): generate candidate, apply, check king safety, undo.
 *  - STATE MANAGEMENT: full undo/redo via Move metadata.
 */
public class Board {

    public int[][] squares = new int[8][8];

    // Castling rights
    public boolean castleWK = true, castleWQ = true;
    public boolean castleBK = true, castleBQ = true;

    // En passant target column (-1 = none)
    public int enPassantCol = -1;
    // En passant row (row of pawn that just double-pushed)
    public int enPassantRow = -1;

    public int turn = Piece.WHITE;  // whose turn it is

    public Board() {
        setupInitialPosition();
    }

    /** Deep copy constructor for AI search. */
    public Board(Board other) {
        for (int r = 0; r < 8; r++)
            squares[r] = other.squares[r].clone();
        this.castleWK     = other.castleWK;
        this.castleWQ     = other.castleWQ;
        this.castleBK     = other.castleBK;
        this.castleBQ     = other.castleBQ;
        this.enPassantCol = other.enPassantCol;
        this.enPassantRow = other.enPassantRow;
        this.turn         = other.turn;
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    public void setupInitialPosition() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                squares[r][c] = Piece.EMPTY;

        int[] backRank = { Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN,
                           Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK };

        for (int c = 0; c < 8; c++) {
            squares[0][c] = Piece.make(Piece.BLACK, backRank[c]);
            squares[1][c] = Piece.B_PAWN;
            squares[6][c] = Piece.W_PAWN;
            squares[7][c] = Piece.make(Piece.WHITE, backRank[c]);
        }

        turn = Piece.WHITE;
        castleWK = castleWQ = castleBK = castleBQ = true;
        enPassantCol = enPassantRow = -1;
    }

    // ─── MOVE GENERATION ──────────────────────────────────────────────────────

    /**
     * Returns all PSEUDO-LEGAL moves for a given square.
     * Pseudo-legal = geometrically valid but may leave king in check.
     * Legal filtering is done in legalMoves() via backtracking.
     */
    public List<Move> pseudoMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int piece = squares[r][c];
        if (piece == Piece.EMPTY) return moves;

        int color = Piece.color(piece);
        int type  = Piece.type(piece);

        switch (type) {
            case Piece.PAWN:   genPawnMoves(moves, r, c, color);   break;
            case Piece.KNIGHT: genKnightMoves(moves, r, c, color); break;
            case Piece.BISHOP: genSlidingMoves(moves, r, c, color, true,  false); break;
            case Piece.ROOK:   genSlidingMoves(moves, r, c, color, false, true);  break;
            case Piece.QUEEN:  genSlidingMoves(moves, r, c, color, true,  true);  break;
            case Piece.KING:   genKingMoves(moves, r, c, color);   break;
        }
        return moves;
    }

    private void genPawnMoves(List<Move> moves, int r, int c, int color) {
        int dir   = (color == Piece.WHITE) ? -1 : 1;
        int start = (color == Piece.WHITE) ? 6 : 1;
        int prRow = (color == Piece.WHITE) ? 0 : 7;
        int piece = squares[r][c];

        // Single push
        int nr = r + dir;
        if (inBounds(nr, c) && squares[nr][c] == Piece.EMPTY) {
            if (nr == prRow) {
                addPromoMoves(moves, r, c, nr, c, piece, Piece.EMPTY);
            } else {
                moves.add(new Move(r, c, nr, c, piece, Piece.EMPTY));
            }
            // Double push from start rank
            if (r == start && squares[r + 2 * dir][c] == Piece.EMPTY) {
                moves.add(new Move(r, c, r + 2 * dir, c, piece, Piece.EMPTY));
            }
        }

        // Captures (diagonal)
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (!inBounds(nr, nc)) continue;
            int target = squares[nr][nc];
            // Normal capture
            if (target != Piece.EMPTY && Piece.color(target) != color) {
                if (nr == prRow) {
                    addPromoMoves(moves, r, c, nr, nc, piece, target);
                } else {
                    moves.add(new Move(r, c, nr, nc, piece, target));
                }
            }
            // En passant
            if (enPassantCol == nc && enPassantRow == r) {
                int epCaptured = squares[r][nc]; // pawn being captured en passant
                Move m = new Move(r, c, nr, nc, piece, epCaptured, 0, false, true);
                moves.add(m);
            }
        }
    }

    private void addPromoMoves(List<Move> moves, int fr, int fc, int tr, int tc,
                               int piece, int captured) {
        for (int t : new int[]{Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT}) {
            moves.add(new Move(fr, fc, tr, tc, piece, captured, t, false, false));
        }
    }

    private void genKnightMoves(List<Move> moves, int r, int c, int color) {
        int piece = squares[r][c];
        int[][] deltas = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : deltas) {
            int nr = r + d[0], nc = c + d[1];
            if (!inBounds(nr, nc)) continue;
            int target = squares[nr][nc];
            if (target == Piece.EMPTY || Piece.color(target) != color)
                moves.add(new Move(r, c, nr, nc, piece, target));
        }
    }

    private void genSlidingMoves(List<Move> moves, int r, int c, int color,
                                 boolean diag, boolean straight) {
        int piece = squares[r][c];
        int[][] dirs = {};
        if (diag && straight)
            dirs = new int[][]{{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}};
        else if (diag)
            dirs = new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        else
            dirs = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};

        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBounds(nr, nc)) {
                int target = squares[nr][nc];
                if (target == Piece.EMPTY) {
                    moves.add(new Move(r, c, nr, nc, piece, Piece.EMPTY));
                } else {
                    if (Piece.color(target) != color)
                        moves.add(new Move(r, c, nr, nc, piece, target));
                    break; // blocked
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private void genKingMoves(List<Move> moves, int r, int c, int color) {
        int piece = squares[r][c];
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (!inBounds(nr, nc)) continue;
                int target = squares[nr][nc];
                if (target == Piece.EMPTY || Piece.color(target) != color)
                    moves.add(new Move(r, c, nr, nc, piece, target));
            }
        }
        // Castling
        int backRow = (color == Piece.WHITE) ? 7 : 0;
        if (r == backRow && c == 4) {
            boolean wk = (color == Piece.WHITE) ? castleWK : castleBK;
            boolean wq = (color == Piece.WHITE) ? castleWQ : castleBQ;
            // Kingside
            if (wk && squares[backRow][5] == Piece.EMPTY && squares[backRow][6] == Piece.EMPTY
                    && Piece.type(squares[backRow][7]) == Piece.ROOK) {
                moves.add(new Move(r, c, backRow, 6, piece, Piece.EMPTY, 0, true, false));
            }
            // Queenside
            if (wq && squares[backRow][3] == Piece.EMPTY && squares[backRow][2] == Piece.EMPTY
                    && squares[backRow][1] == Piece.EMPTY
                    && Piece.type(squares[backRow][0]) == Piece.ROOK) {
                moves.add(new Move(r, c, backRow, 2, piece, Piece.EMPTY, 0, true, false));
            }
        }
    }

    // ─── BACKTRACKING - LEGAL MOVE GENERATION ─────────────────────────────────

    /**
     * BACKTRACKING ALGORITHM:
     * For each candidate (pseudo-legal) move:
     *   1. Apply the move to the board
     *   2. Check if the moving player's king is now in check
     *   3. UNDO the move (restore state)
     *   4. If king was safe -> move is legal -> add to result
     *
     * This is textbook backtracking: try a path, check constraint,
     * backtrack if constraint violated.
     */
    public List<Move> legalMoves(int r, int c) {
        List<Move> legal = new ArrayList<>();
        List<Move> candidates = pseudoMoves(r, c);
        int color = Piece.color(squares[r][c]);

        for (Move m : candidates) {
            // Save state needed for undo
            m.savedCastleWK    = castleWK;
            m.savedCastleWQ    = castleWQ;
            m.savedCastleBK    = castleBK;
            m.savedCastleBQ    = castleBQ;
            m.savedEnPassantCol = enPassantCol;

            applyMove(m);                        // Step 1: Try the move

            if (!isInCheck(color)) {             // Step 2: Validate constraint
                legal.add(m);
            }

            undoMove(m);                         // Step 3: Backtrack (restore state)
        }
        return legal;
    }

    /** All legal moves for a given side. */
    public List<Move> allLegalMoves(int color) {
        List<Move> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (squares[r][c] != Piece.EMPTY && Piece.color(squares[r][c]) == color)
                    all.addAll(legalMoves(r, c));
        return all;
    }

    // ─── APPLY / UNDO MOVE ────────────────────────────────────────────────────

    public void applyMove(Move m) {
        int color = Piece.color(m.piece);
        int type  = Piece.type(m.piece);

        // Move piece
        squares[m.toRow][m.toCol] = m.piece;
        squares[m.fromRow][m.fromCol] = Piece.EMPTY;

        // En passant capture: remove captured pawn
        if (m.isEnPassant) {
            squares[m.fromRow][m.toCol] = Piece.EMPTY;
        }

        // Promotion
        if (m.promoteTo != 0) {
            squares[m.toRow][m.toCol] = Piece.make(color, m.promoteTo);
        }

        // Castling: move the rook
        if (m.isCastle) {
            int row = m.toRow;
            if (m.toCol == 6) { // Kingside
                squares[row][5] = squares[row][7];
                squares[row][7] = Piece.EMPTY;
            } else { // Queenside
                squares[row][3] = squares[row][0];
                squares[row][0] = Piece.EMPTY;
            }
        }

        // Update castling rights
        if (type == Piece.KING) {
            if (color == Piece.WHITE) { castleWK = false; castleWQ = false; }
            else                      { castleBK = false; castleBQ = false; }
        }
        if (type == Piece.ROOK) {
            if (m.fromRow == 7 && m.fromCol == 7) castleWK = false;
            if (m.fromRow == 7 && m.fromCol == 0) castleWQ = false;
            if (m.fromRow == 0 && m.fromCol == 7) castleBK = false;
            if (m.fromRow == 0 && m.fromCol == 0) castleBQ = false;
        }

        // Update en passant
        if (type == Piece.PAWN && Math.abs(m.toRow - m.fromRow) == 2) {
            enPassantCol = m.fromCol;
            enPassantRow = m.toRow;  // row of pawn that just moved
        } else {
            enPassantCol = -1;
            enPassantRow = -1;
        }

        turn = Piece.opponent(color);
    }

    public void undoMove(Move m) {
        int color = Piece.color(m.piece);

        // Restore piece to origin
        squares[m.fromRow][m.fromCol] = m.piece;
        squares[m.toRow][m.toCol]     = m.captured;

        // En passant: restore captured pawn
        if (m.isEnPassant) {
            squares[m.fromRow][m.toCol] = m.captured;
            squares[m.toRow][m.toCol]   = Piece.EMPTY;
        }

        // Castling: restore rook
        if (m.isCastle) {
            int row = m.toRow;
            if (m.toCol == 6) { // Kingside
                squares[row][7] = squares[row][5];
                squares[row][5] = Piece.EMPTY;
            } else { // Queenside
                squares[row][0] = squares[row][3];
                squares[row][3] = Piece.EMPTY;
            }
        }

        // Restore castling rights and en passant
        castleWK     = m.savedCastleWK;
        castleWQ     = m.savedCastleWQ;
        castleBK     = m.savedCastleBK;
        castleBQ     = m.savedCastleBQ;
        enPassantCol = m.savedEnPassantCol;
        enPassantRow = -1; // not needed for current legal move check

        turn = color; // restore turn
    }

    // ─── CHECK DETECTION ──────────────────────────────────────────────────────

    public boolean isInCheck(int color) {
        int[] king = findKing(color);
        if (king == null) return false;
        return isAttacked(king[0], king[1], Piece.opponent(color));
    }

    public int[] findKing(int color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (squares[r][c] == Piece.make(color, Piece.KING))
                    return new int[]{r, c};
        return null;
    }

    public boolean isAttacked(int r, int c, int byColor) {
        // Check if any piece of byColor attacks square (r,c)
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                int p = squares[i][j];
                if (p == Piece.EMPTY || Piece.color(p) != byColor) continue;
                // Use pseudo-moves to check attack
                for (Move m : pseudoMoves(i, j)) {
                    if (m.toRow == r && m.toCol == c) return true;
                }
            }
        return false;
    }

    public boolean isCheckmate(int color) {
        return isInCheck(color) && allLegalMoves(color).isEmpty();
    }

    public boolean isStalemate(int color) {
        return !isInCheck(color) && allLegalMoves(color).isEmpty();
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
