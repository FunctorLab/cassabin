package cassabin.common

import java.util.concurrent._

import cats.effect._

trait ConcurrentMap[F[_], K, V] {
  def put(k:K, v:V): F[Unit]
  /** gets the value under `k`, if it exists, otherwise put's `v` there and returns it */
  def getOrPutIfAbsent(k:K, v:V): F[V]
  def remove(k: K):F[V]
  def get(k: K):F[Option[V]]
  def size:F[Int]
}


private final class ConcurrentMapImpl[F[_], K, V](private val back:java.util.AbstractMap[K, V])
                                                 (implicit sync:Sync[F])
  extends ConcurrentMap[F, K, V] {

  def put(k: K, v: V): F[Unit] = sync.delay(back.put(k,v))
  // todo: is there atomic op for this in concurrent package? o.O
  def getOrPutIfAbsent(k: K, v: V): F[V] = sync.delay {
    var atK = back.get(k)
    if (atK == null) {
      back.put(k, v)
      atK = v
    }
    atK
  }
  def remove(k: K):F[V] = sync.delay(back.remove(k))
  def get(k: K):F[Option[V]] = sync.delay(Option(back.get(k)))
  def size:F[Int] = sync.delay(back.size())
}

object ConcurrentMap {
  def apply[F[_], K, V]()(implicit sync:Sync[F]):F[ConcurrentMap[F, K, V]] = {
    val back = new ConcurrentHashMap[K, V]()
    sync.delay(new ConcurrentMapImpl(back))
  }
}
