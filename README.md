# ♔ CHECKMATE X ♚
### Design and Analysis of Algorithms — PBL Project

---

## Project Overview

CheckMate X is a fully functional chess application built in Java (Swing)
demonstrating core DAA concepts through the game of chess.

---

## Team Members
- [Your Name]
- [Team Member 2]
- [Team Member 3]

**Subject:** Design and Analysis of Algorithms (DAA)
**Type:** Project-Based Learning (PBL)

---

## Algorithms Implemented

### 1. Backtracking — Legal Move Validation
**File:** `Board.java` → method `legalMoves(row, col)`

```
FOR each candidate (pseudo-legal) move:
    APPLY move to board                   ← try
    IF king not in check:
        ADD to legal moves list           ← accept
    UNDO move                             ← backtrack
```

This is textbook **constraint-satisfaction backtracking**:
enumerate candidates, check constraint, backtrack if violated.

---

### 2. Minimax — AI Game Tree Search
**File:** `ChessAI.java` → method `minimax(board, depth, alpha, beta, maximizing)`

```
minimax(board, depth):
    IF depth == 0: RETURN evaluate(board)
    IF maximizing (White):
        FOR each move:
            APPLY move
            score = minimax(board, depth-1, false)
            UNDO move
            best = MAX(best, score)
    ELSE (Black):
        FOR each move:
            APPLY move
            score = minimax(board, depth-1, true)
            UNDO move
            best = MIN(best, score)
    RETURN best
```

**Time Complexity:** O(b^d) where b ≈ 30 (average chess branching factor), d = depth

---

### 3. Alpha-Beta Pruning — Optimization of Minimax
**File:** `ChessAI.java` (same method, pruning condition)

```
alpha = best score White can guarantee (starts at -∞)
beta  = best score Black can guarantee (starts at +∞)

After each node:
    IF beta <= alpha:
        BREAK  ← prune this branch (backtrack immediately)
```

**Why it works:** If the minimizer (Black) already has a path guaranteeing
score ≤ X, and the maximizer (White) finds a branch with score ≥ X,
Black will never choose this node — so we stop exploring it.

**Best case complexity:** O(b^(d/2)) — same quality result as full minimax!

---

### 4. Piece-Square Tables — Evaluation Heuristic
**File:** `ChessAI.java` → `PST_PAWN[][]`, etc.

Each square on the board has a positional bonus/penalty for each piece type.
- Knights rewarded for center squares, penalized for edges
- Rooks rewarded for open files
- King penalized for center in middlegame

---

## Features

| Feature | Status |
|---------|--------|
| All chess rules (castling, en passant, promotion) | ✅ |
| Legal move highlighting (green dots) | ✅ |
| Captured pieces display | ✅ |
| AI opponent (3 difficulty levels) | ✅ |
| Check/checkmate/stalemate detection | ✅ |
| Move history (algebraic notation) | ✅ |
| Position evaluation display | ✅ |
| Undo move | ✅ |
| 2-player mode | ✅ |

---

## How to Run

### Requirements
- Java JDK 11 or higher
- No external libraries needed (pure Java Swing)

### Windows
```
run.bat
```

### Linux / Mac
```bash
chmod +x run.sh
./run.sh
```

### Manual
```bash
mkdir bin
javac -d bin src/*.java
java -cp bin CheckmateX
```

---

## File Structure

```
CheckmateX/
├── src/
│   ├── CheckmateX.java     ← Entry point (main)
│   ├── Piece.java          ← Piece constants & symbols
│   ├── Move.java           ← Move representation + algebraic notation
│   ├── Board.java          ← Board state + BACKTRACKING move generation
│   ├── ChessAI.java        ← MINIMAX + ALPHA-BETA PRUNING AI
│   ├── BoardPanel.java     ← Swing board rendering + mouse input
│   ├── GameController.java ← Game flow, undo, AI orchestration
│   └── MainWindow.java     ← Full Swing UI (window, panels, menus)
├── run.bat                 ← Windows build & run
├── run.sh                  ← Linux/Mac build & run
└── README.md               ← This file
```

---

## Permutation & Combination Analysis

Chess is fundamentally a **combinatorial search problem**:

- At any position, White has ~30 legal moves (branching factor b ≈ 30)
- At depth 2: 30² = 900 positions explored
- At depth 3: 30³ = 27,000 positions
- Alpha-beta reduces this to roughly 30^(3/2) ≈ 164 positions at depth 3

The **number of possible chess games** is estimated at 10^120 (Shannon Number) —
vastly more than atoms in the universe. This is why intelligent pruning
(alpha-beta) is essential — it's the entire point of studying DAA!

---

*CheckMate X — Made with ♥ for DAA PBL*
