/**
 * Represents a single chess move with all metadata needed for:
 * - Execution (apply to board)
 * - Undo (restore board state via backtracking)
 * - Display (algebraic notation)
 */
public class Move {
    public final int fromRow, fromCol;
    public final int toRow,   toCol;
    public final int piece;          // piece being moved
    public final int captured;       // piece captured (EMPTY if none)
    public final int promoteTo;      // promotion target type (0 if not promotion)
    public final boolean isCastle;   // kingside or queenside castle
    public final boolean isEnPassant;

    // Castling flags saved for undo (backtracking state restoration)
    public boolean savedCastleWK, savedCastleWQ, savedCastleBK, savedCastleBQ;
    public int savedEnPassantCol;    // -1 if none

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                int piece, int captured) {
        this(fromRow, fromCol, toRow, toCol, piece, captured, 0, false, false);
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                int piece, int captured, int promoteTo,
                boolean isCastle, boolean isEnPassant) {
        this.fromRow     = fromRow;
        this.fromCol     = fromCol;
        this.toRow       = toRow;
        this.toCol       = toCol;
        this.piece       = piece;
        this.captured    = captured;
        this.promoteTo   = promoteTo;
        this.isCastle    = isCastle;
        this.isEnPassant = isEnPassant;
    }

    /**
     * Returns algebraic notation string for move history display.
     */
    public String toAlgebraic() {
        if (isCastle) {
            return (toCol == 6) ? "O-O" : "O-O-O";
        }
        String files = "abcdefgh";
        int t = Piece.type(piece);
        String prefix = (t == Piece.PAWN) ? "" : Piece.name(t).substring(0, 1);
        String from = files.charAt(fromCol) + "" + (8 - fromRow);
        String cap  = (captured != Piece.EMPTY || isEnPassant) ? "x" : "";
        // For pawns, include file of origin on capture
        if (t == Piece.PAWN && !cap.isEmpty()) prefix = files.charAt(fromCol) + "";
        String to   = files.charAt(toCol) + "" + (8 - toRow);
        String promo = (promoteTo != 0) ? "=" + Piece.name(promoteTo).substring(0, 1) : "";
        return prefix + cap + to + promo;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
