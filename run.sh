#!/bin/bash
# ==========================================
#  CheckMate X - Build & Run Script
#  Requirements: JDK 11 or higher
# ==========================================

mkdir -p bin

echo "Compiling CheckMate X..."
javac -d bin src/Piece.java src/Move.java src/Board.java src/ChessAI.java \
            src/BoardPanel.java src/GameController.java src/MainWindow.java src/CheckmateX.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Launching CheckMate X..."
    java -cp bin CheckmateX
else
    echo "Compilation failed. Check errors above."
fi
