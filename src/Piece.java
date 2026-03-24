/**
 * Piece constants.
 * Encoding: color (0=None, 1=White, 2=Black) + type (0=None,1=P,2=N,3=B,4=R,5=Q,6=K)
 * Single int = color*10 + type
 */
public class Piece {
    // Colors
    public static final int NONE  = 0;
    public static final int WHITE = 1;
    public static final int BLACK = 2;

    // Types
    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    // Encoded pieces
    public static final int EMPTY       = 0;
    public static final int W_PAWN      = 11;
    public static final int W_KNIGHT    = 12;
    public static final int W_BISHOP    = 13;
    public static final int W_ROOK      = 14;
    public static final int W_QUEEN     = 15;
    public static final int W_KING      = 16;
    public static final int B_PAWN      = 21;
    public static final int B_KNIGHT    = 22;
    public static final int B_BISHOP    = 23;
    public static final int B_ROOK      = 24;
    public static final int B_QUEEN     = 25;
    public static final int B_KING      = 26;

    public static int color(int piece) { return piece / 10; }
    public static int type(int piece)  { return piece % 10; }
    public static int make(int color, int type) { return color * 10 + type; }

    public static boolean isWhite(int piece) { return color(piece) == WHITE; }
    public static boolean isBlack(int piece) { return color(piece) == BLACK; }
    public static boolean isEmpty(int piece) { return piece == EMPTY; }

    public static int opponent(int color) { return color == WHITE ? BLACK : WHITE; }

    // Piece material values (centipawns)
    public static final int[] VALUES = {0, 100, 320, 330, 500, 900, 20000};

    // Unicode chess symbols
    public static String symbol(int piece) {
        int c = color(piece), t = type(piece);
        if (c == WHITE) {
            switch (t) {
                case PAWN:   return "\u2659";
                case KNIGHT: return "\u2658";
                case BISHOP: return "\u2657";
                case ROOK:   return "\u2656";
                case QUEEN:  return "\u2655";
                case KING:   return "\u2654";
            }
        } else if (c == BLACK) {
            switch (t) {
                case PAWN:   return "\u265F";
                case KNIGHT: return "\u265E";
                case BISHOP: return "\u265D";
                case ROOK:   return "\u265C";
                case QUEEN:  return "\u265B";
                case KING:   return "\u265A";
            }
        }
        return " ";
    }

    public static String name(int type) {
        switch (type) {
            case PAWN:   return "Pawn";
            case KNIGHT: return "Knight";
            case BISHOP: return "Bishop";
            case ROOK:   return "Rook";
            case QUEEN:  return "Queen";
            case KING:   return "King";
            default:     return "?";
        }
    }
}
