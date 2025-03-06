package bot

object Commands {
  val START = "start"
  val HELP = "help"
  val TRACK = "track"
  val UNTRACK = "untrack"
  val LIST = "list"
  val NO = "нет"
  
  val botCommands: Set[String] = Set(START, HELP, TRACK, UNTRACK, LIST)
}
