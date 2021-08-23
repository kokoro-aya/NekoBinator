package moe.irony.simplepc.applications.bencode

import moe.irony.simplepc.parser.*
import moe.irony.simplepc.parser.Parser.Companion.`*≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺$≻`
import moe.irony.simplepc.parser.Parser.Companion.`≺*`
import moe.irony.simplepc.parser.Parser.Companion.`≺|≻`
import moe.irony.simplepc.parser.Parser.Companion.`≻≻=`
import moe.irony.simplepc.parser.Parser.Companion.pure
import moe.irony.simplepc.types.HKT
import moe.irony.simplepc.utils.constructLong
import moe.irony.simplepc.utils.constructString

sealed class Bencode
data class BInteger(val value: Long): Bencode()
data class BString(val value: String): Bencode()
data class BDictionary(val dict: Map<String, Bencode>) : Bencode(), Map<String, Bencode> by dict
data class BList(val list: List<Bencode>) : Bencode(), List<Bencode> by list

typealias BDictionaryEntry = Pair<String, Bencode>

fun List<BDictionaryEntry>.aggregate(): Map<String, Bencode> = this.associate { it }

fun unsigned(): HKT<Parser<*>, Long> =
    (List<Char>::constructLong) `≺$≻` many1(isDigit())

fun stringOfLength(i: Int): HKT<Parser<*>, List<Char>> =
    replicate(i, anyChar())

/*
 * Bencode ::= BInteger | BString | BDictionary | BList
 * BInteger ::= 'i' Number 'e'
 * BString ::= <Length> String
 * BDictionary ::= 'd' ( String Bencode )* 'e'
 * BList ::= 'l' Bencode 'e'
 */

fun bInteger(): HKT<Parser<*>, Long> =
    matchChar('i') `*≻` (unsigned() `≺|≻`((Long::unaryMinus) `≺$≻`  (matchChar('-') `*≻` unsigned()))) `≺*` matchChar('e')

fun bString(): HKT<Parser<*>, String> =
    (List<Char>::constructString) `≺$≻` ((unsigned() `≺*` matchChar(':')) `≻≻=` { count ->
        stringOfLength(count.toInt())
    })

fun bDictEntry(): HKT<Parser<*>, BDictionaryEntry> =
    bString() `≻≻=` { name -> bencode() `≻≻=` { bc ->
        Parser.pure(name to bc)
    } }

fun bDictionary(): HKT<Parser<*>, Map<String, Bencode>> =
    (List<BDictionaryEntry>::aggregate) `≺$≻` (matchChar('d') `*≻` many(bDictEntry()) `≺*` matchChar('e'))

fun bList(): HKT<Parser<*>, List<Bencode>> =
    matchChar('l') `*≻` many(bencode()) `≺*` matchChar('e')

fun bencode(): HKT<Parser<*>, Bencode> =
    recur { Parser.empty<Bencode>() `≺|≻` ((::BInteger) `≺$≻` bInteger()) `≺|≻` ((::BString) `≺$≻` bString()) `≺|≻` ((::BDictionary) `≺$≻` bDictionary()) `≺|≻` ((::BList) `≺$≻` (bList())) }
