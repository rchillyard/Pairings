/*
 * Copyright (c) 2020. Phasmid Software
 */

package com.phasmidsoftware.pairings

import com.phasmidsoftware.parse.{CellParser, TableParserHelper}
import com.phasmidsoftware.table.{Header, Table}
import scala.util.{Success, Try}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

/**
 * Pairings is an application which reads a CSV file and outputs JSON.
 *
 * The application reads a CSV file of player names, then forms partnerships by pairing
 * the odd-numbered lines with the even-numbered lines.
 *
 * As written, it is expected that
 * the input file has a header row with at least "first" and "last" columns.
 * However, it is easy to switch to a programmed header--see the definition of TableParserHelper.
 *
 * Partnerships are then converted to a "Partnerships" object (as specified by the Shark Bridge Teacher Console API)
 * and so to JSON which is output to the outputFile.
 *
 */
object Pairings extends App {

    val version = "0.0.2"

    private val wo: Option[String] = args.headOption
    println(s"Pairings ${wo.getOrElse("")}")
    println(s"Version: $version")

    private val (inputFile, outputFile) = getFileNames("Documents", wo.getOrElse("partnerships"))
    private val pty = Table.parse[Table[Player]](scala.io.Source.fromFile(inputFile))
    private val zy = pty map convertToPartnerships
    private val wy = zy map Partnerships.toJsonPretty
    wy.transform(outputPairingString, processException)

    // TODO would normally be private but is required by unit tests.
    def convertToPartnerships(pt: Table[Player]): Partnerships = {
        println(s"${pt.size} players read")
        val duplicates = for ((_, v) <- pt.toSeq.groupBy(_.nickname); if v.length > 1; p <- v) yield p
        if (duplicates.nonEmpty) throw PairingsException(s"Duplicate player nicknames for: $duplicates")
        val ts = Player.convertTable(pt)
        println(s"transformed into ${ts.size} partnerships")
        Partnerships.create(Partnership.convertTable(ts))
    }

    private def outputPairingString(w: String): Try[Unit] = {
        import java.io.PrintWriter
        val p: PrintWriter = new PrintWriter(outputFile)
        p.println(w)
        p.close()
        println(s"output of ${w.length} sent to $outputFile")
        Success(())
    }

    private def processException(e: Throwable): Try[Unit] = e match {
        case PairingsException(m) => Success(System.err.println(m))
        case _ => Success(e.printStackTrace(System.err))
    }

    private def getFileNames(baseDirectory: String, baseName: String): (String, String) = {
        val userHome = System.getProperty("user.home")
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
    lazy val cellParser: CellParser[Player] = cellParser2(apply)

    /**
     * Method to transform a Table[Player] into a Table[Partnership].
     *
     * The requirements of the application are that the rows of the Player table are grouped by twos
     * and each resulting entity (an array of length 2) is taken to form a Partnership.
     *
     * @param pt a Table[Player]
     * @return a Table[Partnership]
     */
    def convertTable(pt: Table[Player]): Table[Partnership] = pt.processRows(xs => (xs grouped 2).toSeq).flatMap(Partnership.create).
            replaceHeader(Some(Header.create("playerA", "playerB")))
}

/**
 * Case class to represent a Partnership represented by two Strings.
 *
 * @param playerA the first player.
 * @param playerB the second player.
 */
case class Partnership(playerA: String, playerB: String)

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
    def create(players: Iterable[Player]): Option[Partnership] =
        players.tail.headOption map (p => Partnership(players.head.nickname, p.nickname))

    /**
     * Method to transform a Table[Player] into a Table[PartnershipAsArray].
     *
     * The requirements of the application are that the player names are grouped as an array.
     *
     * @param pt a Table[Partnership]
     * @return a Table[PartnershipAsArray]
     */
    def convertTable(pt: Table[Partnership]): Table[PartnershipAsArray] = pt.map(p => PartnershipAsArray(Array(p.playerA, p.playerB)))
}

case class PartnershipAsArray(pair: Array[String])

/**
 * Partnerships is a case class that corresponds to the way that the JSON Output for this application is defined.
 *
 * @param partnerships an Array of Array of String.
 */
case class Partnerships(partnerships: Array[Array[String]])

object Partnerships extends DefaultJsonProtocol {

    implicit val partnershipsFormat: RootJsonFormat[Partnerships] = jsonFormat1(Partnerships.apply)

    def toJsonPretty(partnerships: Partnerships): String = partnerships.toJson.prettyPrint

    def create(t: Table[PartnershipAsArray]): Partnerships = Partnerships(for (r <- t.rows.toArray) yield r.pair)
}

case class PairingsException(m: String) extends Exception(m)