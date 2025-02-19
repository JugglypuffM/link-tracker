import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import controller.UpdateController
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    val endpoints = UpdateController().endpoints
    val swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "LinkUpdateService", "0.0.1")
    val routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

    for {
      _ <-
        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString("localhost").get)
          .withPort(Port.fromInt(8080).get)
          .withHttpApp(Router("/" -> routes).orNotFound)
          .build
          .evalTap(server =>
            IO.println(
              s"Server available at http://localhost:${server.address.getPort}"
            )
          )
          .useForever
    } yield ExitCode.Success
}
