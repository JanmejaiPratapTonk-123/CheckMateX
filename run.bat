@echo off
REM ==========================================
REM  CheckMate X - Build & Run Script
REM  Requirements: JDK 11 or higher
REM ==========================================

if not exist "bin" mkdir bin

echo Compiling CheckMate X...
javac -d bin src\Piece.java src\Move.java src\Board.java src\ChessAI.java src\BoardPanel.java src\GameController.java src\MainWindow.java src\CheckmateX.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
    echo Launching CheckMate X...
    java -cp bin CheckmateX
) else (
    echo Compilation failed. Check errors above.
    pause
)
