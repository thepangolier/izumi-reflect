package izumi.reflect.benchmark

import izumi.reflect.{DebugProperties, Tag}
import izumi.reflect.macrortti.LTag
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array("-Xms2g", "-Xmx2g", "-XX:+UseG1GC", "-XX:+UnlockExperimentalVMOptions", "-XX:+UnlockDiagnosticVMOptions"))
class CacheBenchmark {

  @Param(Array("true", "false"))
  var cacheEnabled: String = _

  private var originalCacheProperty: String = _

  @Setup(Level.Trial)
  def setupTrial(): Unit = {
    originalCacheProperty = System.getProperty(DebugProperties.`izumi.reflect.rtti.cache.compile`)
    System.setProperty(DebugProperties.`izumi.reflect.rtti.cache.compile`, cacheEnabled)

    // Pre-warm the JVM and reflection caches
    (1 to 1000).foreach { _ =>
      Tag[String].tag
      Tag[Int].tag
      LTag[List[String]].tag
    }

    System.gc()
    Thread.sleep(100)
  }

  @TearDown(Level.Trial)
  def tearDownTrial(): Unit = {
    if (originalCacheProperty != null) {
      System.setProperty(DebugProperties.`izumi.reflect.rtti.cache.compile`, originalCacheProperty)
    } else {
      System.clearProperty(DebugProperties.`izumi.reflect.rtti.cache.compile`)
    }
  }

  @Benchmark
  def simpleTagCreation(bh: Blackhole): Unit = {
    bh.consume(Tag[String].tag)
    bh.consume(Tag[Int].tag)
    bh.consume(Tag[Long].tag)
    bh.consume(Tag[Double].tag)
    bh.consume(Tag[Boolean].tag)
    bh.consume(Tag[Float].tag)
    bh.consume(Tag[Short].tag)
    bh.consume(Tag[Byte].tag)
    bh.consume(Tag[Char].tag)
    bh.consume(Tag[BigInt].tag)
  }

  @Benchmark
  def complexTagCreation(bh: Blackhole): Unit = {
    bh.consume(Tag[List[String]].tag)
    bh.consume(Tag[Map[String, Int]].tag)
    bh.consume(Tag[Either[String, Int]].tag)
    bh.consume(Tag[Option[List[String]]].tag)
    bh.consume(Tag[scala.concurrent.Future[Either[Throwable, String]]].tag)
    bh.consume(Tag[Vector[Map[String, Option[Int]]]].tag)
    bh.consume(Tag[Set[Either[Exception, List[String]]]].tag)
    bh.consume(Tag[Array[Option[Long]]].tag)
  }

  @Benchmark
  def deeplyNestedTagCreation(bh: Blackhole): Unit = {
    bh.consume(Tag[Map[String, List[Either[Throwable, Option[Int]]]]].tag)
    bh.consume(Tag[List[Map[String, Either[Exception, Option[Long]]]]].tag)
    bh.consume(Tag[Either[List[String], Map[Int, Option[Double]]]].tag)
    bh.consume(Tag[Option[Either[List[String], Map[String, List[Int]]]]].tag)
    bh.consume(Tag[Vector[Set[Map[String, Either[Exception, Option[Boolean]]]]]].tag)
    bh.consume(Tag[Array[List[Map[Int, Either[Throwable, Vector[String]]]]]].tag)
  }

  @Benchmark
  def hierarchyTagCreation(bh: Blackhole): Unit = {
    // Define test types inline to avoid test dependencies
    trait TestI1
    trait TestI2 extends TestI1
    type TestSubStr <: String

    bh.consume(Tag[String].tag)
    bh.consume(Tag[TestSubStr].tag)
    bh.consume(Tag[TestI2].tag)
    bh.consume(Tag[TestI1].tag)
    bh.consume(Tag[AnyRef].tag)
    bh.consume(Tag[Any].tag)
    bh.consume(Tag[Object].tag)
  }

  @Benchmark
  def ltagCreation(bh: Blackhole): Unit = {
    bh.consume(LTag[String].tag)
    bh.consume(LTag[List[String]].tag)
    bh.consume(LTag[Map[String, Int]].tag)
    bh.consume(LTag[Either[String, Int]].tag)
    bh.consume(LTag[Option[List[String]]].tag)
    bh.consume(LTag[Vector[String]].tag)
    bh.consume(LTag[Set[Int]].tag)
    bh.consume(LTag[Array[Boolean]].tag)
  }

  @Benchmark
  def tagComparison(bh: Blackhole): Unit = {
    val stringTag = Tag[String]
    val intTag = Tag[Int]
    val longTag = Tag[Long]
    val listStringTag = Tag[List[String]]
    val mapStringIntTag = Tag[Map[String, Int]]
    val vectorStringTag = Tag[Vector[String]]
    val setIntTag = Tag[Set[Int]]
    val optionBooleanTag = Tag[Option[Boolean]]
    val eitherStringIntTag = Tag[Either[String, Int]]

    bh.consume(stringTag.tag =:= intTag.tag)
    bh.consume(stringTag.tag =:= stringTag.tag)
    bh.consume(intTag.tag =:= longTag.tag)
    bh.consume(listStringTag.tag =:= vectorStringTag.tag)
    bh.consume(mapStringIntTag.tag =:= setIntTag.tag)
    bh.consume(optionBooleanTag.tag =:= eitherStringIntTag.tag)
  }

  @Benchmark
  def subtypeChecking(bh: Blackhole): Unit = {
    // Define test types inline
    type TestSubStr <: String

    val childTags = Array(Tag[String], Tag[TestSubStr], Tag[List[String]], Tag[Vector[Int]], Tag[Set[Boolean]])
    val parentTags = Array(Tag[String], Tag[Any], Tag[AnyRef], Tag[Object], Tag[Iterable[Any]])

    var i = 0
    while (i < childTags.length) {
      var j = 0
      while (j < parentTags.length) {
        bh.consume(childTags(i).tag <:< parentTags(j).tag)
        j += 1
      }
      i += 1
    }
  }

  @Benchmark
  def providerMagnetPattern(bh: Blackhole): Unit = {
    bh.consume(implicitly[Tag[String]])
    bh.consume(implicitly[Tag[List[String]]])
    bh.consume(implicitly[Tag[Map[String, Int]]])
    bh.consume(implicitly[Tag[Vector[Boolean]]])
    bh.consume(implicitly[Tag[Set[Long]]])
    bh.consume(implicitly[Tag[Option[Double]]])
    bh.consume(implicitly[Tag[Either[String, Int]]])
  }
}

