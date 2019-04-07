package cassabin.protocol.frame

import cassabin.protocol.notations.OptionPayload

final case class ColumnSpec(tableSpec:Option[TableSpec], columnName:String, tpe:TypeOption)
object ColumnSpec {

}


