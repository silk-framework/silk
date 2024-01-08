package org.silkframework.runtime.caching


import org.lmdbjava.LmdbException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.util.{ConfigTestTrait, TestFileUtils}

import java.nio.ByteBuffer
import java.nio.file.Files
import java.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.silkframework.util.FileUtils._

class PersistentSortedKeyValueStoreTest extends AnyFlatSpec with Matchers {
  behavior of "Persistent sorted key value store"

  val values = Seq("", " ", "123", "abc", "something longer with unicode char ඔ" * 10)

  it should "check if it is working in general" in {
    // This check should work
    PersistentSortedKeyValueStore.check()

    // Create a file where LMDB would place its database in order to break it
    val dir = Files.createTempDirectory("ldmbBootTest")
    val dbBaseDir = dir.resolve("tmp")
    dbBaseDir.toFile.mkdirs()
    Files.createFile(dbBaseDir.resolve("bootTest"))

    // The check should fail now
    ConfigTestTrait.withConfig(("caches.persistence.directory" -> Some(dir.toFile.getCanonicalPath))) {
      an [LmdbException] shouldBe thrownBy { PersistentSortedKeyValueStore.check().get }
    }

    // Cleanup
    dir.toFile.deleteRecursive()
  }

  it should "store and retrieve single string values to/from the store" in {
    withStore() { store =>
      for(value <- values) {
        store.put(value, value, None)
      }
      for(value <- values) {
        store.getString(value) mustBe Some(value)
      }
    }
  }

  it should "store and retrieve single string values using a single transaction" in {
    withStore() { store =>
      store.withWriteTransaction { txn =>
        for (value <- values) {
          store.put(value, value, Some(txn))
        }
        txn.commit()
      }
      store.withReadTransaction { txn =>
        for(value <- values) {
          store.getString(value, Some(txn)) mustBe Some(value)
        }
      }
    }
  }

  it should "store and retrieve single string values using a cursor" in {
    withStore() { store =>
      store.withCursor { cursor =>
        for (value <- values) {
          cursor.put(value, value)
        }
        for(value <- values) {
          cursor.position(value) mustBe true
          cursor.stringValue() mustBe Some(value)
        }
      }
    }
  }

  it should "store and retrieve values with compression turned on" in {
    withStore(compression = true) {store =>
      store.withCursor { cursor =>
        for (value <- values) {
          cursor.put(value, value)
        }
        // Test cursor
        for (value <- values) {
          cursor.position(value) mustBe true
          cursor.stringValue() mustBe Some(value)
        }
      }
      // Test iterator
      store.iterateStringEntries().toArray.toSeq mustBe values.sorted.map(v => (v, v))
    }
  }

  it should "benchmark" ignore {
    val size = 10 * 1000 * 1000
    var idx = 0
    val start = System.currentTimeMillis()
    withStore(compression = true) { store =>
      var lastTime = System.currentTimeMillis()
      store.withCursor { cursor =>
        while (idx < size) {
          idx += 1
          cursor.put(idx.toString, "value" + idx)
          if (idx % 100000 == 0) {
            println(idx + " " + (System.currentTimeMillis() - lastTime) + "ms")
            lastTime = System.currentTimeMillis()
          }
        }
      }
      val readStart = System.currentTimeMillis()
      idx = 0
      store.withCursor { cursor =>
        while (idx < size) {
          idx += 1
          cursor.position(idx.toString)
          cursor.stringValue() mustBe Some("value" + idx)
          if (idx % 100000 == 0) {
            println(idx + " " + (System.currentTimeMillis() - lastTime) + "ms")
            lastTime = System.currentTimeMillis()
          }
        }
      }
      val end = System.currentTimeMillis()
      println(s"Took ${(end - start) / 1000}s, write: ${(readStart - start) / 1000}s, read: ${(end - readStart) / 1000}s")
    }
  }

  it should "store arbitrary byte values as key and value" in {
    def int2byteArray(i: Int): Array[Byte] = ByteBuffer.allocate(4).putInt(i).array()
    val arrays = for(i <- 0 to 5;
        j <- 0 to 5) yield {
      val arr = new Array[Byte](8)
      Array.copy(int2byteArray(i), 0, arr, 0, 4)
      Array.copy(int2byteArray(j), 0, arr, 4, 4)
      arr
    }
    for (compression <- Seq(false, true)) {
      withStore(compression) { store =>
        store.withWriteTransaction { txn =>
          for(arr <- arrays) {
            store.put(arr, arr, Some(txn))
          }
          txn.commit()
        }
        val it = store.iterateEntries()
        val returnArrays = (for((keyBuffer, valueBuffer) <- it) yield {
          val keyArray = new Array[Byte](keyBuffer.remaining())
          val valueArray = new Array[Byte](valueBuffer.remaining())
          keyBuffer.get(keyArray)
          valueBuffer.get(valueArray)
          assert(util.Arrays.equals(keyArray, valueArray), "Key and value arrays not equal, but they should be!")
          keyArray
        }).toArray.toSeq
        returnArrays.size mustBe arrays.size
        for((expectedArray, resultArray) <- arrays.zip(returnArrays)) {
          assert(util.Arrays.equals(expectedArray, resultArray), "Expected array is not equal returned array.")
        }
      }
    }
  }

  it should "should allow iterating over the store concurrently and multiple times in a row" in {
    withStore() { store =>
      for (value <- values) {
        store.put(value, value, None)
      }
      // Multiple in a row
      for(_ <- 1 to 10) {
        for(_ <- store.iterateEntries()) {}
      }
      // Concurrently in same thread
      val it = store.iterateEntries()
      val it2 = store.iterateEntries()
      it.next()
      it2.next()
      it.close()
      it2.close()
      // Use iterator created in one thread, in another thread
      val sharedIterator = Await.result(Future(store.iterateEntries()), Duration.Inf)
      sharedIterator.next()
    }
  }

  it should "apply different strategies if a key is too large" in {
    val tooLongKeyA = "a" * 2000 + "A"
    val tooLongKeyB = "a" * 2000 + "B"
    val someValueA = "valueA"
    val someValueB = "valueB"
    withStore(tooLargeKeyStrategy = HandleTooLargeKeyStrategy.ThrowError) { store =>
      intercept[IllegalArgumentException] {
        store.put(tooLongKeyA, someValueA, None)
      }
    }
    withStore(tooLargeKeyStrategy = HandleTooLargeKeyStrategy.TruncateKey) { store =>
      intercept[IllegalArgumentException] {
        store.put(tooLongKeyA, someValueA, None)
        store.put(tooLongKeyB, someValueB, None)
        // Clash of keys
        store.size() mustBe 1
        store.getString(tooLongKeyA, None) mustBe someValueB
      }
    }
    withStore(tooLargeKeyStrategy = HandleTooLargeKeyStrategy.TruncateKeyWithHash) { store =>
      intercept[IllegalArgumentException] {
        store.put(tooLongKeyA, someValueA, None)
        store.put(tooLongKeyB, someValueB, None)
        // No clash of keys
        store.size() mustBe 2
        store.getString(tooLongKeyA, None) mustBe someValueA
        store.getString(tooLongKeyB, None) mustBe someValueB
      }
    }
  }

  def withStore[T](compression: Boolean = false, tooLargeKeyStrategy: HandleTooLargeKeyStrategy = HandleTooLargeKeyStrategy.ThrowError)
                  (block: PersistentSortedKeyValueStore => T): T = {
    TestFileUtils.withTempDirectory { dir =>
      val store = PersistentSortedKeyValueStore("id", optionalDbDirectory = Some(dir),
        config = PersistentSortedKeyValueStoreConfig(
          tooLargeKeyStrategy = HandleTooLargeKeyStrategy.ThrowError,
          compressKeys = compression,
          compressValues = compression))
      try {
        block(store)
      } finally {
        store.close()
      }
    }
  }
}
