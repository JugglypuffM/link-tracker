package resilience

import cats.effect.IO

private[resilience] sealed trait CircuitState

private[resilience] object CircuitState {
  final case class Closed(failures: Int = 0) extends CircuitState
  final case class Open(until: Long) extends CircuitState
  case object HalfOpen extends CircuitState

  val initial: CircuitState = Closed()
}
