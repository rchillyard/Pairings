/*
 * Copyright (c) 2020. Phasmid Software
 */

package com.phasmidsoftware.pairings

import com.phasmidsoftware.parse._
import com.phasmidsoftware.table._
import scala.util.{Success, Try}

/**
 * Main Application which reads a CSV file of player names, then forms partnerships by pairing
 * the odd-numbered lines with the even-numbered lines..
 *
 * These pairings are then converted to the format required by the Shark Bridge Teacher Console.
 *
 * The input file is expected to have a header row with at least "first" and "last" columns.
 */
object Pairings extends App {

    println(s"CsvToJSON ${args.headOption.getOrElse("")}")

    val (inputFile, outputFile) = getFileNames("Documents", "partnerships")
    val pty: Try[Table[Player]] = Table.parse[Table[Player]](scala.io.Source.fromFile(inputFile))
    val tsy: Try[Iterator[Partnership]] = for (pss <- for (pt <- pty) yield pt.rows grouped 2) yield for (ps <- pss) yield Partnership(ps)
    val sy: Try[Partnerships] = for (ts <- tsy) yield Partnerships((for (t <- ts) yield t.asArray).toArray)
    sy.foreach(w => println(s"${w.size} partnerships read from $inputFile"))
    (sy map (_.prettyPrint)).transform(outputPairingString, e => Success(System.err.println(e.getLocalizedMessage)))

    private def outputPairingString(w: String): Try[Unit] = {
        import java.io.PrintWriter
        val p: PrintWriter = new PrintWriter(outputFile)
        p.println(w)
        p.close()
        println(s"output of ${w.length} sent to $outputFile")
        Success(())
    }

    private def getFileNames(baseDirectory: String, defaultBaseName: String): (String, String) = {
        val userHome = System.getProperty("user.home")
        val baseName = if (args.length > 0) args.head else defaultBaseName
        val inputFile = s"$userHome/$baseDirectory/$baseName.csv"
        val outputFile = s"$userHome/$baseDirectory/$baseName.json"
        (inputFile, outputFile)
    }
}

/**
 * This case class represents a Player with first name, last name.
 *
 * @param first the first name.
 * @param last  the last name.
 */
case class Player(first: String, last: String) {
    def nickname: String = s"$first ${last.head}"
}

/**
 * Companion object for Player.
 */
object Player extends TableParserHelper[Player]() {
    def cellParser: CellParser[Player] = cellParser2(apply)
}

/**
 * Case class to represent a Partnership represented by two Strings.
 *
 * @param playerA the first player.
 * @param playerB the second player.
 */
case class Partnership(playerA: String, playerB: String) {
    def asArray: Array[String] = Array(playerA, playerB)
}

/**
 * Companion object to Partnership.
 */
object Partnership {
    /**
     * Factory method to create a Partnership from a sequence of two players.
     *
     * @param players the sequence of Players. Whatever length is given, we will use the nicknames of the first and last.
     * @return a Partnership.
     */
    def apply(players: Seq[Player]): Partnership = Partnership(players.head.nickname, players.last.nickname)
}

/**
 * A case class representing all Partnerships as an Array of Array of String.
 * This is the structure required by the Shark Bridge app.
 *
 * @param partnerships an Array of two-element Arrays of Strings.
 */
case class Partnerships(partnerships: Array[Array[String]]) {
    def size: Int = partnerships.length

    /**
     * Method to output these Partnerships as a Json String.
     *
     * @return a String with some embedded newlines.
     */
    def prettyPrint: String = Partnerships.prettyPrint(this)
}

import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

/**
 * Companion object for Partnerships.
 */
object Partnerships extends DefaultJsonProtocol {
    implicit val partnershipsFormat: RootJsonFormat[Partnerships] = jsonFormat1(apply)

    def prettyPrint(p: Partnerships): String = p.toJson.prettyPrint
}
