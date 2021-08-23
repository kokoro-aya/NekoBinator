package moe.irony.simplepc.applications

import moe.irony.simplepc.applications.bencode.*
import moe.irony.simplepc.instances.Result
import moe.irony.simplepc.parser.Parser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BencodeTest {
    @Test
    fun testString() {
        assertEquals(Result.Success(BString("fooo")), Parser.narrow(bencode()).parse("4:fooo"))
        assertEquals(Result.Success(BString("")), Parser.narrow(bencode()).parse("0:"))
    }
    
    @Test
    fun testInteger() {
        assertEquals(Result.Success(BInteger(32)), Parser.narrow(bencode()).parse("i32e"))
        assertEquals(Result.Success(BInteger(-2)), Parser.narrow(bencode()).parse("i-2e"))
    }
    
    @Test
    fun testList() {
        assertEquals(Result.Success(BList(listOf(BString("eggs"), BString("spam")))), Parser.narrow(bencode()).parse("l4:spam4:eggse"))
        assertEquals(Result.Success(BList(listOf())), Parser.narrow(bencode()).parse("le"))
    }
    
    @Test
    fun testDict() {
        assertEquals(Result.Success(BDictionary(mapOf(
            "spam" to BString("eggs"), "cow" to BString("moo")
        ))), Parser.narrow(bencode()).parse("d3:cow3:moo4:spam4:eggse"))
        assertEquals(Result.Success(BDictionary(mapOf(
            "spam" to BList(listOf(BString("b"), BString("a")))
        ))), Parser.narrow(bencode()).parse("d4:spaml1:a1:bee"))
        assertEquals(Result.Success(BDictionary(mapOf(
            "publisher.location" to BString("home"),
            "publisher-webpage" to BString("www.example.com"),
            "publisher" to BString("bob")
        ))), Parser.narrow(bencode()).parse("d9:publisher3:bob17:publisher-webpage15:www.example.com18:publisher.location4:homee"))
        assertEquals(Result.Success(BDictionary(mapOf(
        ))), Parser.narrow(bencode()).parse("de"))
    }
    
    @Test
    fun testFails() {
        assertTrue(Parser.narrow(bencode()).parse("5:fooo") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse(":") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("i3") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("2e") is Result.Failure)

        assertTrue(Parser.narrow(bencode()).parse("l4:spa4:eggse") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("l") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("e") is Result.Failure)

        assertTrue(Parser.narrow(bencode()).parse("d3:cow:moo4:sam4eggse") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("d4:spaml1:a1:be") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("d4:spaml1:a1:bele") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("d9:publisher3:bob17:publisher-webpage15:www.examle.com18:publisher.location4:homee") is Result.Failure)
        assertTrue(Parser.narrow(bencode()).parse("") is Result.Failure)
    }
}