import java.lang.Exception
import kotlin.math.abs

enum class File(val index: Int) {
    a(0),
    b(1),
    c(2),
    d(3),
    e(4),
    f(5),
    g(6),
    h(7),
}

enum class CaptureType {
    NORMAL, EN_PASSANT
}

const val SIDE_COUNT = 8
const val P2_INIT_RANK = 7
const val P1_INIT_RANK = 2

class Chess() {
    private val player1: String
    private val player2: String
    private val square = mutableListOf<MutableList<Char>>()
    private var player1Turn = true
    private var prevTurn = ""

    init {
        println("Pawns-Only Chess\n")
        println("""How to play:
<initialColumn><initialRow><columnToMove><rowToMove>
ex. a2a3
the piece is in a2 and will move to a3
En Passant capture is also possible
            
        """.trimMargin())
        println("First Player's name:")
        player1 = readln()
        println("Second Player's name:")
        player2 = readln()
    }

    fun setupBoard() {
        repeat(SIDE_COUNT) {
            when (it) {
                rankIndexConverter(P2_INIT_RANK) -> square.add(MutableList(8) { 'B' })
                rankIndexConverter(P1_INIT_RANK) -> square.add(MutableList(8) { 'W' })
                else -> square.add(MutableList(8) { ' ' })
            }
        }
    }

    private fun rankIndexConverter(rank: Int): Int {
        return abs(rank - SIDE_COUNT)
    }

    fun printBoard() {
        var board = "  +---+---+---+---+---+---+---+---+\n"
        repeat(SIDE_COUNT) {
            board += "${rankIndexConverter(it)} | ${square[it].joinToString(" | ")} |\n" +
                    "  +---+---+---+---+---+---+---+---+\n"
        }
        board += "    a   b   c   d   e   f   g   h\n"
        println(board)
    }

    private fun isFirstMove(initRank: Int, p1Turn: Boolean): Boolean {
        return initRank == if (p1Turn) P1_INIT_RANK else P2_INIT_RANK
    }

    private fun canMoveForward(initRank: Int, moveRank: Int, file: String, p1Turn: Boolean): Boolean {
        var adjacentNotOccupied = true
        val moveCount = initRank - moveRank
        val isMoveForward =
            (isFirstMove(
                initRank,
                p1Turn
            ) && (((if (p1Turn) -1 else 1) * (moveCount)) in 1..2)) //first move - 1 or 2 move
                    || (((if (p1Turn) -1 else 1) * (moveCount)) == 1) // else 1 move
        if (isMoveForward) {
            repeat(abs(moveCount)) {
                if (square[rankIndexConverter(initRank + (if (p1Turn) 1 else -1) * (it + 1))][File.valueOf(file).ordinal] != ' ') {
                    adjacentNotOccupied = false
                }
            }
        }
        return adjacentNotOccupied
    }

    private fun captureType(initRank: Int, moveRank: Int, moveFile: String, p1Turn: Boolean): MutableList<CaptureType> {
        val captureList = mutableListOf<CaptureType>()
        val isMoveForwardDiagonal = ((if (p1Turn) -1 else 1) * (initRank - moveRank)) == 1
        if (!isMoveForwardDiagonal) return captureList

        val haveEnemyPiece =
            square[rankIndexConverter(moveRank.toString().toInt())][File.valueOf(moveFile).ordinal] ==
                    if (p1Turn) 'B' else 'W'
        if (haveEnemyPiece) captureList.add(CaptureType.NORMAL)

        if ((square[rankIndexConverter(initRank.toString().toInt())][File.valueOf(moveFile).ordinal] ==
                    if (p1Turn) 'B' else 'W') && prevTurn == moveFile + initRank.toString()
        ) captureList.add(CaptureType.EN_PASSANT)

        return captureList
    }

    private fun movePiece(initRank: Char, initFile: Char, moveRank: Char, moveFile: Char) {
        square[rankIndexConverter(initRank.toString().toInt())][File.valueOf(initFile.toString()).ordinal] =
            ' '
        square[rankIndexConverter(moveRank.toString().toInt())][File.valueOf(moveFile.toString()).ordinal] =
            if (player1Turn) 'W' else 'B'
        prevTurn = moveFile.toString() + moveRank.toString()
    }

    fun startGame() {
        while (true) {
            println("${if (player1Turn) player1 else player2}'s turn:")
            val input = readln()
            if (input == "exit") {
                println("Bye!")
                break
            }

            if (Regex("[a-h][1-8][a-h][1-8]").matches(input)) {
                val initFile = input[0]
                val moveFile = input[2]
                val initRank = input[1]
                val moveRank = input[3]
                val initPosition =
                    square[rankIndexConverter(initRank.toString().toInt())][File.valueOf(initFile.toString()).ordinal]
                if (isSquareHavePlayerPiece(initPosition)) {
                    val isSameFile = initFile == moveFile
                    if (isSameFile && canMoveForward(
                            initRank.toString().toInt(),
                            moveRank.toString().toInt(),
                            initFile.toString(),
                            player1Turn
                        )
                    ) {
                        movePiece(initRank, initFile, moveRank, moveFile)
                        printBoard()
                        if (winOrDraw())
                            break
                        player1Turn = !player1Turn
                        continue
                    }

                    val isMovingToSide =
                        abs(File.valueOf(initFile.toString()).ordinal - File.valueOf(moveFile.toString()).ordinal) == 1
                    val captureTypes =
                        captureType(
                            initRank.toString().toInt(),
                            moveRank.toString().toInt(),
                            moveFile.toString(),
                            player1Turn
                        )
                    if (isMovingToSide && captureTypes.size > 0) {
                        movePiece(initRank, initFile, moveRank, moveFile)
                        for (captureType in captureTypes) {
                            when (captureType) {
                                CaptureType.EN_PASSANT -> square[rankIndexConverter(
                                    initRank.toString().toInt()
                                )][File.valueOf(moveFile.toString()).ordinal] =
                                    ' '

                                else -> {}
                            }
                        }
                        printBoard()
                        if (winOrDraw())
                            break
                        player1Turn = !player1Turn
                        continue
                    }

                } else {
                    println("No ${if (player1Turn) "white" else "black"} pawn at ${input.substring(0, 2)}")
                    continue
                }
            }
            println("Invalid Input")
        }
    }

    private fun winOrDraw(): Boolean {
        if (haveWinner()) {
            println("${if (player1Turn) "White" else "Black"} Wins!\nBye!")
            return true
        }
        if (isDraw()) {
            println("Stalemate!\nBye!")
            return true
        }
        return false
    }

    private fun isDraw(): Boolean {
        var p1NoMove = true
        var p2NoMove = true
        for (rank in 0 until SIDE_COUNT) {
            for (file in 0 until SIDE_COUNT) {
                val squareVal = square[rank][file]
                if (squareVal != ' ') {
                    val initRank = rankIndexConverter(rank)
                    //-------------Can move forward-------------
                    val p1Turn = squareVal == 'W'
                    val moveCount =
                        if ((if (p1Turn) P1_INIT_RANK else P2_INIT_RANK) == initRank) 2 else 1
                    repeat(moveCount) {
                        if (canMoveForward(
                                rankIndexConverter(rank),
                                initRank + (if (p1Turn) 1 else -1) * (it + 1),
                                File.values()[file].name,
                                p1Turn
                            )
                        ) {
                            if (p1Turn) p1NoMove = false else p2NoMove = false
                        }
                    }
                    //----------------Can capture----------------
                    val leftFile = file - 1
                    val rightFile = file + 1
                    try {
                        if (captureType(
                                initRank,
                                initRank + (if (p1Turn) 1 else -1),
                                File.values()[rightFile].toString(),
                                p1Turn
                            ).size > 0
                        ) {
                            if (p1Turn) p1NoMove = false else p2NoMove = false
                        }
                    } catch (e: Exception) {
                    }
                    try {
                        if (captureType(
                                initRank,
                                initRank + (if (p1Turn) 1 else -1),
                                File.values()[leftFile].toString(),
                                p1Turn
                            ).size > 0
                        ) {
                            if (p1Turn) p1NoMove = false else p2NoMove = false
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
        return p1NoMove or p2NoMove
    }

    private fun haveWinner(): Boolean {
        if (prevTurn[1].toString().toInt() == (if (player1Turn) P2_INIT_RANK + 1 else P1_INIT_RANK - 1)) return true
        if (noPiece()) return true
        return false
    }

    private fun noPiece(): Boolean {
        val wOrB = if (player1Turn) 'B' else 'W'
        for (rank in 0 until SIDE_COUNT) {
            for (file in 0 until SIDE_COUNT) {
                if (wOrB == square[rank][file])
                    return false
            }
        }
        return true
    }

    private fun isSquareHavePlayerPiece(initPosition: Char): Boolean {
        return initPosition == if (player1Turn) 'W' else 'B'
    }
}

fun main() {
    val chess = Chess()
    chess.setupBoard()
    chess.printBoard()
    chess.startGame()
}