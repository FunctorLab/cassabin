package cassabin

trait LocalCassandra {
  final def cassandraIp:String = "127.0.0.1"
  final def cassandraPort:Int = 9042
}
